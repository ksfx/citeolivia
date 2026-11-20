package com.citeolivia.library.lookup;

import com.citeolivia.library.Contributor;
import com.citeolivia.library.ContributorRole;
import com.citeolivia.library.ReferenceDraft;
import com.citeolivia.library.ReferenceType;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Converts responses of the bibliographic services into {@link ReferenceDraft}s. No network access. */
final class LookupMappers {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern YEAR = Pattern.compile("(\\d{4})");

    private LookupMappers() {}

    /** Crossref REST API, {@code /works/{doi}}. */
    static Optional<ReferenceDraft> crossref(String json) {
        JsonNode m = JSON.readTree(json).path("message");
        if (m.isMissingNode()) {
            return Optional.empty();
        }
        ReferenceDraft d = new ReferenceDraft();
        String type = m.path("type").asString("");
        d.setType(
                switch (type) {
                    case "journal-article" -> ReferenceType.JOURNAL_ARTICLE;
                    case "book-chapter", "book-section", "book-part", "reference-entry" -> ReferenceType.BOOK_CHAPTER;
                    case "proceedings-article" -> ReferenceType.CONFERENCE_PAPER;
                    case "edited-book", "proceedings" -> ReferenceType.EDITED_BOOK;
                    case "dissertation" -> ReferenceType.THESIS;
                    case "report", "report-component" -> ReferenceType.REPORT;
                    case "posted-content", "dataset" -> ReferenceType.WEB_PAGE;
                    default -> ReferenceType.BOOK;
                });
        d.setTitle(first(m.path("title")));
        d.setSubtitle(first(m.path("subtitle")));
        d.setContainerTitle(first(m.path("container-title")));
        d.setPublisher(text(m.path("publisher")));
        d.setPlace(text(m.path("publisher-location")));
        d.setVolume(text(m.path("volume")));
        d.setIssue(text(m.path("issue")));
        d.setPages(text(m.path("page")));
        d.setDoi(text(m.path("DOI")));
        d.setIsbn(first(m.path("ISBN")));
        d.setUrl(null); // the DOI is the stable link
        d.setYear(year(m));
        String abs = text(m.path("abstract"));
        if (abs != null) {
            d.setAbstractText(
                    abs.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").strip());
        }
        if (d.getType() == ReferenceType.THESIS) {
            d.setGenre("Dissertation");
        }
        List<Contributor> people = new ArrayList<>(persons(m.path("author"), ContributorRole.AUTHOR));
        people.addAll(persons(m.path("editor"), ContributorRole.EDITOR));
        d.setContributors(people);
        return d.getTitle() == null ? Optional.empty() : Optional.of(d.normalize());
    }

    /** Open Library, {@code /api/books?bibkeys=ISBN:...&format=json&jscmd=data}. */
    static Optional<ReferenceDraft> openLibrary(String json, String isbn) {
        JsonNode root = JSON.readTree(json);
        JsonNode b = root.path("ISBN:" + isbn);
        if (b.isMissingNode()) {
            return Optional.empty();
        }
        ReferenceDraft d = book(isbn);
        d.setTitle(text(b.path("title")));
        d.setSubtitle(text(b.path("subtitle")));
        d.setPublisher(firstName(b.path("publishers")));
        d.setPlace(firstName(b.path("publish_places")));
        d.setYear(yearOf(text(b.path("publish_date"))));
        List<Contributor> authors = new ArrayList<>();
        for (JsonNode a : b.path("authors").values()) {
            authors.add(splitName(text(a.path("name")), ContributorRole.AUTHOR));
        }
        d.setContributors(authors);
        return d.getTitle() == null ? Optional.empty() : Optional.of(d.normalize());
    }

    /** Google Books, {@code /books/v1/volumes?q=isbn:...}. */
    static Optional<ReferenceDraft> googleBooks(String json, String isbn) {
        JsonNode items = JSON.readTree(json).path("items");
        if (!items.isArray() || items.isEmpty()) {
            return Optional.empty();
        }
        JsonNode v = items.path(0).path("volumeInfo");
        ReferenceDraft d = book(isbn);
        d.setTitle(text(v.path("title")));
        d.setSubtitle(text(v.path("subtitle")));
        d.setPublisher(text(v.path("publisher")));
        d.setYear(yearOf(text(v.path("publishedDate"))));
        String description = text(v.path("description"));
        d.setAbstractText(description);
        List<Contributor> authors = new ArrayList<>();
        for (JsonNode a : v.path("authors").values()) {
            authors.add(splitName(a.asString(""), ContributorRole.AUTHOR));
        }
        d.setContributors(authors);
        return d.getTitle() == null ? Optional.empty() : Optional.of(d.normalize());
    }

    /** Deutsche Nationalbibliothek SRU, schema oai_dc. */
    static Optional<ReferenceDraft> dnb(String xml, String isbn) {
        Document doc = parseXml(xml);
        String title = firstElement(doc, "title");
        if (title == null) {
            return Optional.empty();
        }
        ReferenceDraft d = book(isbn);
        // dc:title looks like "Titel : Untertitel / Verfasser"
        String t = title.replaceAll("\\s*/.*$", "");
        int colon = t.indexOf(" : ");
        if (colon > 0) {
            d.setTitle(t.substring(0, colon));
            d.setSubtitle(t.substring(colon + 3));
        } else {
            d.setTitle(t);
        }
        // dc:publisher looks like "Wiesbaden : Springer Vieweg"
        String publisher = firstElement(doc, "publisher");
        if (publisher != null) {
            int sep = publisher.indexOf(" : ");
            if (sep > 0) {
                d.setPlace(publisher.substring(0, sep));
                d.setPublisher(publisher.substring(sep + 3));
            } else {
                d.setPublisher(publisher);
            }
        }
        d.setYear(yearOf(firstElement(doc, "date")));
        List<Contributor> authors = new ArrayList<>();
        NodeList creators = doc.getElementsByTagNameNS("*", "creator");
        for (int i = 0; i < creators.getLength(); i++) {
            String c = creators.item(i)
                    .getTextContent()
                    .replaceAll("\\s*\\[.*?]", "")
                    .strip();
            if (!c.isEmpty()) {
                authors.add(splitName(c, ContributorRole.AUTHOR));
            }
        }
        d.setContributors(authors);
        return Optional.of(d.normalize());
    }

    private static ReferenceDraft book(String isbn) {
        ReferenceDraft d = new ReferenceDraft();
        d.setType(ReferenceType.BOOK);
        d.setIsbn(isbn);
        return d;
    }

    /** "Ian Goodfellow" -> (Goodfellow, Ian); "Müller, Anna" -> (Müller, Anna). */
    static Contributor splitName(String name, ContributorRole role) {
        String n = name == null ? "" : name.strip();
        int comma = n.indexOf(',');
        if (comma > 0) {
            return new Contributor(
                    role, n.substring(0, comma).strip(), n.substring(comma + 1).strip());
        }
        int space = n.lastIndexOf(' ');
        return space > 0
                ? new Contributor(role, n.substring(space + 1), n.substring(0, space))
                : new Contributor(role, n, null);
    }

    private static List<Contributor> persons(JsonNode array, ContributorRole role) {
        List<Contributor> result = new ArrayList<>();
        for (JsonNode p : array.values()) {
            String family = text(p.path("family"));
            String given = text(p.path("given"));
            String org = text(p.path("name"));
            if (family != null) {
                result.add(new Contributor(role, family, given));
            } else if (org != null) {
                result.add(new Contributor(role, org, null));
            }
        }
        return result;
    }

    private static String year(JsonNode m) {
        for (String field : List.of("issued", "published-print", "published-online", "published", "created")) {
            JsonNode y = m.path(field).path("date-parts").path(0).path(0);
            if (!y.isMissingNode() && y.asInt(0) > 0) {
                return String.valueOf(y.asInt());
            }
        }
        return null;
    }

    private static String yearOf(String date) {
        if (date == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(date);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String text(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) {
            return null;
        }
        String s = n.asString("");
        return s.isBlank() ? null : s.strip();
    }

    private static String first(JsonNode array) {
        return array.isArray() && !array.isEmpty() ? text(array.path(0)) : text(array);
    }

    private static String firstName(JsonNode array) {
        return array.isArray() && !array.isEmpty() ? text(array.path(0).path("name")) : null;
    }

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            return b.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalStateException("Ungültige Antwort der Nationalbibliothek", e);
        }
    }

    private static String firstElement(Document doc, String localName) {
        NodeList nodes = doc.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            String t = nodes.item(i).getTextContent();
            if (t != null && !t.isBlank()) {
                return t.strip();
            }
        }
        return null;
    }
}
