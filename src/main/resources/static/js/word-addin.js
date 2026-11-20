/*
 * CiteOlivia Word add-in – document operations via Office.js.
 *
 * The task pane itself is server-rendered (Thymeleaf + htmx). This script only does what needs
 * Office.js: remember the project of the document, insert citations, update citations and the
 * bibliography. Citations are content controls tagged "citeolivia:cite:<referenceId>:<pages>",
 * the bibliography is a content control tagged "citeolivia:bibliography".
 */
(function () {
    "use strict";

    var TAG_CITE = "citeolivia:cite:";
    var TAG_BIB = "citeolivia:bibliography";
    var SETTING_PROJECT = "citeolivia.projectId";

    var inWord = false;
    var base = document.body.getAttribute("data-base") || "/word";

    // ---------- helpers ----------

    function meta(name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute("content") : null;
    }

    function projectId() {
        var panel = document.querySelector("[data-project-id]");
        return panel ? panel.getAttribute("data-project-id") : null;
    }

    function showStatus(kind, message) {
        var el = document.getElementById("status");
        if (!el) {
            return;
        }
        el.className = el.getAttribute("data-class-" + kind) || "";
        el.textContent = message;
    }

    function clearStatus() {
        var el = document.getElementById("status");
        if (el) {
            el.className = "hidden";
            el.textContent = "";
        }
    }

    /** POST with JSON body; returns the Response. Session expired -> back to the login page. */
    function post(path, body) {
        var headers = { "Content-Type": "application/json", "X-CiteOlivia-Addin": "1" };
        headers[meta("_csrf_header")] = meta("_csrf");
        return fetch(base + "/projects/" + projectId() + path, {
            method: "POST",
            credentials: "same-origin",
            headers: headers,
            body: JSON.stringify(body)
        }).then(function (response) {
            if (response.status === 401 || response.status === 403) {
                window.location.href = base + "/login";
                throw new Error("Bitte erneut anmelden.");
            }
            if (!response.ok) {
                return response.text().then(function (text) {
                    throw new Error(text || "Serverfehler (" + response.status + ").");
                });
            }
            return response;
        });
    }

    function inTextCitations(citations) {
        return post("/citations", { citations: citations })
            .then(function (r) { return r.json(); })
            .then(function (data) { return data.texts; });
    }

    function bibliographyHtml(referenceIds) {
        return post("/bibliography", { referenceIds: referenceIds }).then(function (r) { return r.text(); });
    }

    function citeTag(referenceId, pages) {
        return TAG_CITE + referenceId + ":" + encodeURIComponent(pages || "");
    }

    /** {referenceId, pages} of a citation tag, or null for other content controls. */
    function parseCiteTag(tag) {
        if (!tag || tag.indexOf(TAG_CITE) !== 0) {
            return null;
        }
        var rest = tag.substring(TAG_CITE.length);
        var colon = rest.indexOf(":");
        var id = Number(colon < 0 ? rest : rest.substring(0, colon));
        if (!Number.isInteger(id) || id <= 0) {
            return null;
        }
        var pages = colon < 0 ? "" : decodeURIComponent(rest.substring(colon + 1));
        return { referenceId: id, pages: pages };
    }

    function fail(error) {
        showStatus("error", (error && error.message) || String(error));
    }

    function requireWord() {
        if (!inWord) {
            showStatus("error", "Diese Aktion funktioniert nur innerhalb von Word.");
            return false;
        }
        return true;
    }

    // ---------- document operations ----------

    function insertCitation(referenceId) {
        if (!requireWord()) {
            return;
        }
        var pagesInput = document.getElementById("pages");
        var pages = pagesInput ? pagesInput.value.trim() : "";
        inTextCitations([{ referenceId: referenceId, pages: pages }])
            .then(function (texts) {
                var text = texts[0];
                if (!text) {
                    throw new Error("Diesen Titel gibt es im Projekt nicht mehr.");
                }
                return Word.run(function (context) {
                    // Insert at the end of the selection, so selected text is never overwritten.
                    var range = context.document.getSelection().insertText(text, "End");
                    var control = range.insertContentControl();
                    control.tag = citeTag(referenceId, pages);
                    control.title = "Zitat · CiteOlivia";
                    control.getRange("After").select();
                    return context.sync();
                }).then(function () {
                    if (pagesInput) {
                        pagesInput.value = "";
                    }
                    showStatus("success", "Eingefügt: " + text);
                });
            })
            .catch(fail);
    }

    /** Updates all citations (current style, current data) and the bibliography, if present. */
    function refreshDocument(insertBibliography) {
        if (!requireWord()) {
            return;
        }
        showStatus("info", "Dokument wird aktualisiert …");
        Word.run(function (context) {
            var controls = context.document.contentControls;
            controls.load("items/tag");
            return context.sync().then(function () {
                var cites = [];
                var bibs = [];
                controls.items.forEach(function (control) {
                    var parsed = parseCiteTag(control.tag);
                    if (parsed) {
                        cites.push({ control: control, referenceId: parsed.referenceId, pages: parsed.pages });
                    } else if (control.tag === TAG_BIB) {
                        bibs.push(control);
                    }
                });
                var citationRequest = cites.map(function (c) { return { referenceId: c.referenceId, pages: c.pages }; });
                var ids = cites.map(function (c) { return c.referenceId; })
                    .filter(function (id, i, all) { return all.indexOf(id) === i; });
                var needBibliography = insertBibliography || bibs.length > 0;

                return Promise.all([
                    cites.length ? inTextCitations(citationRequest) : Promise.resolve([]),
                    needBibliography ? bibliographyHtml(ids) : Promise.resolve(null)
                ]).then(function (results) {
                    var texts = results[0];
                    var html = results[1];
                    var missing = 0;
                    cites.forEach(function (c, i) {
                        if (texts[i]) {
                            c.control.insertText(texts[i], "Replace");
                        } else {
                            missing++;
                        }
                    });
                    if (html !== null) {
                        if (bibs.length === 0) {
                            var paragraph = context.document.getSelection().paragraphs.getLast()
                                .insertParagraph("", "After");
                            var control = paragraph.insertContentControl();
                            control.tag = TAG_BIB;
                            control.title = "Literaturverzeichnis · CiteOlivia";
                            bibs.push(control);
                        }
                        bibs.forEach(function (bib) { bib.insertHtml(html, "Replace"); });
                    }
                    return context.sync().then(function () {
                        var message = cites.length + (cites.length === 1 ? " Zitat" : " Zitate") + " aktualisiert";
                        if (html !== null) {
                            message += ", Literaturverzeichnis mit " + ids.length + (ids.length === 1 ? " Titel" : " Titeln");
                        }
                        message += ".";
                        if (missing > 0) {
                            showStatus("error", message + " " + missing + " Zitat(e) verweisen auf Titel, die es in diesem Projekt nicht gibt.");
                        } else {
                            showStatus("success", message);
                        }
                    });
                });
            });
        }).catch(fail);
    }

    // ---------- project of the document ----------

    function loadPanel(id) {
        clearStatus();
        var panel = document.getElementById("panel");
        if (!id) {
            panel.innerHTML = "";
            return;
        }
        htmx.ajax("GET", base + "/projects/" + encodeURIComponent(id) + "/panel", { target: "#panel", swap: "innerHTML" });
    }

    function rememberProject(id) {
        if (!inWord) {
            return;
        }
        var settings = Office.context.document.settings;
        if (id) {
            settings.set(SETTING_PROJECT, id);
        } else {
            settings.remove(SETTING_PROJECT);
        }
        settings.saveAsync();
    }

    function initProject() {
        var select = document.getElementById("project");
        if (!select) {
            return;
        }
        var saved = inWord ? Office.context.document.settings.get(SETTING_PROJECT) : null;
        if (saved && select.querySelector('option[value="' + String(saved).replace(/[^0-9]/g, "") + '"]')) {
            select.value = String(saved);
        } else if (select.options.length === 2) {
            select.selectedIndex = 1; // only one project: take it
        }
        select.addEventListener("change", function () {
            rememberProject(select.value);
            loadPanel(select.value);
        });
        if (select.value) {
            rememberProject(select.value);
            loadPanel(select.value);
        }
    }

    // ---------- wiring ----------

    document.addEventListener("click", function (event) {
        var button = event.target.closest("[data-action]");
        if (!button) {
            return;
        }
        var action = button.getAttribute("data-action");
        if (action === "cite") {
            insertCitation(Number(button.getAttribute("data-reference-id")));
        } else if (action === "bibliography") {
            refreshDocument(true);
        } else if (action === "refresh") {
            refreshDocument(false);
        }
    });

    // After a style change the server marks the new panel; then update the document.
    document.body.addEventListener("htmx:afterSwap", function () {
        var marker = document.getElementById("style-changed");
        if (marker) {
            marker.remove(); // handle once, not again on the next search
            if (inWord) {
                refreshDocument(false);
            }
        }
    });

    function start(info) {
        inWord = !!(info && info.host === Office.HostType.Word);
        if (!inWord) {
            var warning = document.getElementById("no-word");
            if (warning) {
                warning.classList.remove("hidden");
            }
        }
        initProject();
    }

    if (window.Office && Office.onReady) {
        Office.onReady(start);
    } else {
        start(null);
    }
})();
