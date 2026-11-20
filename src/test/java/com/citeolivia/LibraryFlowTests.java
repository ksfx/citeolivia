package com.citeolivia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LibraryFlowTests {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    private Cookie alice;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.sql("TRUNCATE app_user, spring_session CASCADE").update();
        alice = registerAndLogin("alice@example.com", "Alice");
    }

    private Cookie registerAndLogin(String email, String name) throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("email", email)
                        .param("displayName", name)
                        .param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD))
                .andExpect(status().is3xxRedirection());
        return mvc.perform(
                        formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
    }

    private long createProject(Cookie session, boolean outline) throws Exception {
        String location = mvc.perform(post("/projects")
                        .cookie(session)
                        .with(csrf())
                        .param("name", "Masterarbeit")
                        .param("description", "Thema X")
                        .param("thesisOutline", String.valueOf(outline))
                        .param("_thesisOutline", "on"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        return Long.parseLong(location.replaceAll("\\D+", " ").strip().split(" ")[0]);
    }

    private long createReference(Cookie session, long projectId, Long categoryId) throws Exception {
        var request = post("/projects/" + projectId + "/references")
                .cookie(session)
                .with(csrf())
                .param("draft.type", "JOURNAL_ARTICLE")
                .param("authors", "LeCun, Yann\nBengio, Yoshua\nHinton, Geoffrey")
                .param("draft.title", "Deep learning")
                .param("draft.year", "2015")
                .param("draft.containerTitle", "Nature")
                .param("draft.volume", "521")
                .param("draft.issue", "7553")
                .param("draft.pages", "436-444")
                .param("draft.doi", "10.1038/nature14539");
        if (categoryId != null) {
            request.param("categoryIds", categoryId.toString());
        }
        String location = mvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long categoryId(long projectId, String name) {
        return jdbc.sql("SELECT id FROM category WHERE project_id = :p AND name = :n")
                .param("p", projectId)
                .param("n", name)
                .query(Long.class)
                .single();
    }

    @Test
    void dashboardCreatesProjectWithThesisOutline() throws Exception {
        long projectId = createProject(alice, true);
        assertThat(jdbc.sql("SELECT count(*) FROM category WHERE project_id = :p")
                        .param("p", projectId)
                        .query(Long.class)
                        .single())
                .isEqualTo(6);
        mvc.perform(get("/dashboard").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Masterarbeit")));
        mvc.perform(get("/projects/" + projectId + "/categories").cookie(alice))
                .andExpect(content().string(containsString("Theoretische Grundlagen")));
    }

    @Test
    void projectWithoutOutline() throws Exception {
        long projectId = createProject(alice, false);
        assertThat(jdbc.sql("SELECT count(*) FROM category WHERE project_id = :p")
                        .param("p", projectId)
                        .query(Long.class)
                        .single())
                .isZero();
    }

    @Test
    void referenceLifecycleWithSearchCitationAndKnowledge() throws Exception {
        long projectId = createProject(alice, true);
        long einleitung = categoryId(projectId, "Einleitung");
        long refId = createReference(alice, projectId, einleitung);
        String base = "/projects/" + projectId;

        // detail shows APA citation and generated citation key
        mvc.perform(get(base + "/references/" + refId).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(containsString("LeCun, Y., Bengio, Y., &amp; Hinton, G. (2015). Deep learning.")))
                .andExpect(content().string(containsString("\\cite{lecun2015}")));

        // htmx live search returns only the fragment
        mvc.perform(get(base + "/references")
                        .cookie(alice)
                        .header("HX-Request", "true")
                        .param("q", "hinton"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Deep learning")))
                .andExpect(content().string(not(containsString("<html"))));
        mvc.perform(get(base + "/references")
                        .cookie(alice)
                        .header("HX-Request", "true")
                        .param("q", "nichtvorhanden"))
                .andExpect(content().string(containsString("Keine Treffer")));
        mvc.perform(get(base + "/references").cookie(alice).param("category", String.valueOf(einleitung)))
                .andExpect(content().string(containsString("Deep learning")));

        // add a quote via htmx and find it in the organizer, cited with page
        mvc.perform(post(base + "/references/" + refId + "/knowledge")
                        .cookie(alice)
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("type", "QUOTE")
                        .param("coreStatement", "Definition Deep Learning")
                        .param("text", "Deep learning allows computational models …")
                        .param("pageRange", "436")
                        .param("categoryIds", String.valueOf(einleitung)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Wissenselement gespeichert")))
                .andExpect(content().string(containsString("(LeCun et al., 2015, S. 436)")));
        // a comment without page number (regression: null page range broke the card template)
        mvc.perform(post(base + "/references/" + refId + "/knowledge")
                        .cookie(alice)
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("type", "COMMENT")
                        .param("text", "Neuere Quellen ergänzen"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Neuere Quellen ergänzen")));
        mvc.perform(get(base + "/references/" + refId).cookie(alice)).andExpect(status().isOk());
        mvc.perform(get(base + "/knowledge").cookie(alice).param("category", String.valueOf(einleitung)))
                .andExpect(content().string(containsString("Definition Deep Learning")));
        mvc.perform(get(base + "/knowledge/export.md").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("## 1 Einleitung")))
                .andExpect(content()
                        .string(containsString(
                                "> „Deep learning allows computational models …“ (LeCun et al., 2015, S. 436)")));

        // switching the citation style re-formats the bibliography
        mvc.perform(post(base + "/citation-style")
                        .cookie(alice)
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("style", "DIN_ISO_690"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "LeCun, Yann; Bengio, Yoshua; Hinton, Geoffrey: Deep learning. In: Nature 521 (2015)")));

        // exports
        mvc.perform(get(base + "/export.bib").cookie(alice))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("Masterarbeit.bib")))
                .andExpect(content().string(containsString("@article{lecun2015,")));
        mvc.perform(get(base + "/export.ris").cookie(alice)).andExpect(content().string(containsString("TY  - JOUR")));

        // deleting the reference deletes its knowledge items
        mvc.perform(post(base + "/references/" + refId + "/delete")
                        .cookie(alice)
                        .with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(header().string("HX-Redirect", base + "/references"));
        assertThat(jdbc.sql("SELECT count(*) FROM knowledge_item")
                        .query(Long.class)
                        .single())
                .isZero();
    }

    @Test
    void validationAndDuplicateDetection() throws Exception {
        long projectId = createProject(alice, false);
        String base = "/projects/" + projectId;
        mvc.perform(post(base + "/references")
                        .cookie(alice)
                        .with(csrf())
                        .param("draft.type", "BOOK")
                        .param("draft.title", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bitte einen Titel angeben")));

        createReference(alice, projectId, null);
        long second = createReference(alice, projectId, null);
        assertThat(jdbc.sql("SELECT citation_key FROM reference_entry WHERE id = :id")
                        .param("id", second)
                        .query(String.class)
                        .single())
                .isEqualTo("lecun2015a");

        // citation key must be unique within the project
        mvc.perform(post(base + "/references/" + second)
                        .cookie(alice)
                        .with(csrf())
                        .param("draft.type", "BOOK")
                        .param("draft.title", "Anderer Titel")
                        .param("citationKey", "lecun2015"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("wird schon verwendet")));
    }

    @Test
    void bibTexImportSkipsDuplicates() throws Exception {
        long projectId = createProject(alice, false);
        createReference(alice, projectId, null);
        String bib = """
                @article{dup, title = {Deep learning}, doi = {10.1038/NATURE14539}, year = 2015}
                @book{neu, author = {M{\\"u}ller, Anna}, title = {Neues Buch}, publisher = {Verlag}, year = {2020}}
                """;
        mvc.perform(multipart("/projects/" + projectId + "/import")
                        .file(new MockMultipartFile(
                                "file", "lit.bib", "text/plain", bib.getBytes(StandardCharsets.UTF_8)))
                        .cookie(alice)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.sql("SELECT title FROM reference_entry WHERE project_id = :p ORDER BY id")
                        .param("p", projectId)
                        .query(String.class)
                        .list())
                .containsExactly("Deep learning", "Neues Buch");
        assertThat(jdbc.sql(
                                "SELECT last_name FROM reference_contributor c JOIN reference_entry r ON r.id = c.reference_entry_id"
                                        + " WHERE r.citation_key = 'neu'")
                        .query(String.class)
                        .single())
                .isEqualTo("Müller");
    }

    @Test
    void categoriesCanBeNestedMovedAndDeleted() throws Exception {
        long projectId = createProject(alice, true);
        String base = "/projects/" + projectId + "/categories";
        long methodik = categoryId(projectId, "Methodik");
        mvc.perform(post(base)
                .cookie(alice)
                .with(csrf())
                .param("parentId", String.valueOf(methodik))
                .param("name", "Interviews"));
        mvc.perform(
                post(base + "/" + methodik + "/move").cookie(alice).with(csrf()).param("direction", "-1"));
        mvc.perform(get(base).cookie(alice))
                .andExpect(content().string(containsString("2</span>\n                    <span>Methodik")))
                .andExpect(content().string(containsString("2.1</span>\n                    <span>Interviews")));
        mvc.perform(post(base + "/" + methodik + "/delete")
                .cookie(alice)
                .with(csrf())
                .header("HX-Request", "true"));
        assertThat(jdbc.sql("SELECT count(*) FROM category WHERE project_id = :p")
                        .param("p", projectId)
                        .query(Long.class)
                        .single())
                .isEqualTo(5);
    }

    @Test
    void otherUsersCannotAccessProject() throws Exception {
        long projectId = createProject(alice, true);
        long refId = createReference(alice, projectId, null);
        Cookie bob = registerAndLogin("bob@example.com", "Bob");

        mvc.perform(get("/projects/" + projectId + "/references").cookie(bob)).andExpect(status().isForbidden());
        mvc.perform(get("/projects/" + projectId + "/references/" + refId).cookie(bob))
                .andExpect(status().isForbidden());
        mvc.perform(get("/projects/" + projectId + "/export.bib").cookie(bob)).andExpect(status().isForbidden());
        mvc.perform(post("/projects/" + projectId + "/delete").cookie(bob).with(csrf()))
                .andExpect(status().isForbidden());
        // Alice's project (description "Thema X") must not be listed for Bob
        mvc.perform(get("/dashboard").cookie(bob)).andExpect(content().string(not(containsString("Thema X"))));

        // a reference id from another project is not found within the own project
        long bobsProject = createProject(bob, false);
        mvc.perform(get("/projects/" + bobsProject + "/references/" + refId).cookie(bob))
                .andExpect(status().isNotFound());
        assertThat(jdbc.sql("SELECT count(*) FROM project").query(Long.class).single())
                .isEqualTo(2);
    }

    @Test
    void pdfAttachmentsAreStoredInDatabase() throws Exception {
        long projectId = createProject(alice, false);
        long refId = createReference(alice, projectId, null);
        String base = "/projects/" + projectId;
        byte[] pdf = "%PDF-1.4\n1 0 obj << >> endobj\ntrailer << >>\n%%EOF\n".getBytes(StandardCharsets.ISO_8859_1);

        mvc.perform(multipart(base + "/references/" + refId + "/attachments")
                        .file(new MockMultipartFile("files", "LeCun 2015.pdf", "application/pdf", pdf))
                        .file(new MockMultipartFile(
                                "files", "virus.pdf", "application/pdf", "MZ...".getBytes(StandardCharsets.ISO_8859_1)))
                        .cookie(alice)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("message", "PDF gespeichert."))
                .andExpect(flash().attribute("error", "„virus.pdf“ ist keine PDF-Datei."));

        long attachmentId = jdbc.sql("SELECT id FROM reference_attachment WHERE reference_entry_id = :r")
                .param("r", refId)
                .query(Long.class)
                .single();
        mvc.perform(get(base + "/references/" + refId).cookie(alice))
                .andExpect(content().string(containsString("LeCun 2015.pdf")));
        mvc.perform(get(base + "/references").cookie(alice)).andExpect(content().string(containsString(">PDF</span>")));

        // download returns exactly the stored bytes
        mvc.perform(get(base + "/attachments/" + attachmentId).cookie(alice))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes(pdf));
        mvc.perform(get(base + "/attachments/" + attachmentId).cookie(alice).param("download", "true"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")));

        // the same file twice is rejected
        mvc.perform(multipart(base + "/references/" + refId + "/attachments")
                        .file(new MockMultipartFile("files", "kopie.pdf", "application/pdf", pdf))
                        .cookie(alice)
                        .with(csrf()))
                .andExpect(flash().attribute("error", "„kopie.pdf“ ist bei diesem Titel schon hinterlegt."));

        // other users cannot read or delete it
        Cookie bob = registerAndLogin("bob@example.com", "Bob");
        mvc.perform(get(base + "/attachments/" + attachmentId).cookie(bob)).andExpect(status().isForbidden());
        mvc.perform(post(base + "/attachments/" + attachmentId + "/delete")
                        .cookie(bob)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // deleting the reference deletes its files
        mvc.perform(post(base + "/references/" + refId + "/delete")
                .cookie(alice)
                .with(csrf())
                .header("HX-Request", "true"));
        assertThat(jdbc.sql("SELECT count(*) FROM reference_attachment")
                        .query(Long.class)
                        .single())
                .isZero();
    }
}
