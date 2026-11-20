package com.citeolivia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WordAddinTests {

    private static final String PASSWORD = "correct-horse-battery";
    private static final String WORD_COOKIE = "CO_WORD_SESSION";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    private Cookie aliceApp;
    private Cookie aliceWord;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.sql("TRUNCATE app_user, spring_session CASCADE").update();
        aliceApp = registerAndLogin("alice@example.com", "Alice");
        aliceWord = wordLogin("alice@example.com").getCookie(WORD_COOKIE);
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

    private MockHttpServletResponse wordLogin(String email) throws Exception {
        return mvc.perform(formLogin("/word/login")
                        .userParameter("email")
                        .user(email)
                        .password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/word/taskpane"))
                .andReturn()
                .getResponse();
    }

    private long createProject(Cookie session, String name) throws Exception {
        String location = mvc.perform(post("/projects")
                        .cookie(session)
                        .with(csrf())
                        .param("name", name)
                        .param("thesisOutline", "false")
                        .param("_thesisOutline", "on"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        return Long.parseLong(location.replaceAll("\\D+", " ").strip().split(" ")[0]);
    }

    private long createReference(Cookie session, long projectId) throws Exception {
        String location = mvc.perform(post("/projects/" + projectId + "/references")
                        .cookie(session)
                        .with(csrf())
                        .param("draft.type", "JOURNAL_ARTICLE")
                        .param("authors", "LeCun, Yann\nBengio, Yoshua\nHinton, Geoffrey")
                        .param("draft.title", "Deep learning")
                        .param("draft.year", "2015")
                        .param("draft.containerTitle", "Nature")
                        .param("draft.volume", "521")
                        .param("draft.pages", "436-444"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private static String json(String s) {
        return s.replace('\'', '"');
    }

    @Test
    void manifestIsPublicWellFormedAndPointsToThePublicUrl() throws Exception {
        String xml = mvc.perform(get("/word/manifest.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(xml)
                .contains("<Id>0efb4387-8ce7-4ccb-943f-e36690f8dd07</Id>")
                .contains("https://www.citeolivia.com/word/taskpane")
                .contains("https://www.citeolivia.com/word/icon-80.png");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/word/icon-32.png")).andExpect(status().isOk());
    }

    @Test
    void installationGuideIsPublicAndLinkedFromHomeAndDashboard() throws Exception {
        mvc.perform(get("/word-add-in"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(content().string(containsString("CiteOlivia für Word")))
                .andExpect(content().string(containsString("href=\"/word/manifest.xml\"")))
                .andExpect(content().string(containsString("Kataloge vertrauenswürdiger Add-Ins")));
        mvc.perform(get("/word/taskpane-preview.png")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(content().string(containsString("href=\"/word-add-in\"")));
        mvc.perform(get("/dashboard").cookie(aliceApp))
                .andExpect(content().string(containsString("href=\"/word-add-in\"")));
    }

    @Test
    void taskpaneRequiresLogin() throws Exception {
        mvc.perform(get("/word/taskpane"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/word/login"));
        mvc.perform(get("/word/projects/1/panel").header("HX-Request", "true"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("HX-Redirect", "/word/login"));
    }

    /**
     * Regression: after opening the task pane unauthenticated, the login used to redirect to the saved
     * request as absolute URL (http://… behind the proxy). Word desktop opened that in the external
     * browser, where the add-in session does not exist.
     */
    @Test
    void loginAfterSavedRequestRedirectsRelativeToTaskpane() throws Exception {
        Cookie anonymous = mvc.perform(get("/word/taskpane").header("Host", "www.citeolivia.com"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getCookie(WORD_COOKIE);
        assertThat(anonymous).isNotNull();
        mvc.perform(post("/word/login")
                        .cookie(anonymous)
                        .with(csrf())
                        .header("Host", "www.citeolivia.com")
                        .param("email", "alice@example.com")
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/word/taskpane"));
    }

    @Test
    void wordLoginUsesOwnPartitionedCookieLimitedToWordPath() throws Exception {
        String setCookie = wordLogin("alice@example.com").getHeader("Set-Cookie");
        assertThat(setCookie)
                .startsWith(WORD_COOKIE + "=")
                .contains("Path=/word")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None")
                .contains("Partitioned");
        long projectId = createProject(aliceApp, "Masterarbeit");

        mvc.perform(get("/word/taskpane").cookie(aliceWord))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Masterarbeit")))
                .andExpect(content().string(containsString("office.js")));
        mvc.perform(get("/word/projects/" + projectId + "/panel").cookie(aliceWord))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-project-id=\"" + projectId + "\"")));

        // The add-in session is not valid for the web app and vice versa.
        mvc.perform(get("/dashboard").cookie(aliceWord)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/word/taskpane").cookie(aliceApp)).andExpect(status().is3xxRedirection());
    }

    @Test
    void onlyTheAddinMayBeFramedAndOnlyByOfficeHosts() throws Exception {
        mvc.perform(get("/word/login"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Frame-Options"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("https://*.officeapps.live.com")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("*;"))));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(header().string("X-Frame-Options", "DENY"));
        mvc.perform(get("/login")).andExpect(header().string("Set-Cookie", not(containsString("SameSite=None"))));
    }

    @Test
    void citationsAndBibliographyOfTheDocument() throws Exception {
        long projectId = createProject(aliceApp, "Masterarbeit");
        long referenceId = createReference(aliceApp, projectId);

        mvc.perform(post("/word/projects/" + projectId + "/citations")
                        .cookie(aliceWord)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'citations':[{'referenceId':" + referenceId + ",'pages':'436-438'},"
                                + "{'referenceId':" + referenceId + ",'pages':''},"
                                + "{'referenceId':999999,'pages':'1'}]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texts[0]").value("(LeCun et al., 2015, S. 436–438)"))
                .andExpect(jsonPath("$.texts[1]").value("(LeCun et al., 2015)"))
                .andExpect(jsonPath("$.texts[2]").doesNotExist());

        mvc.perform(post("/word/projects/" + projectId + "/bibliography")
                        .cookie(aliceWord)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'referenceIds':[" + referenceId + "," + referenceId + "]}")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LeCun, Y.")))
                .andExpect(content().string(containsString("<i>")))
                .andExpect(content().string(containsString("text-indent:-1.27cm")));

        mvc.perform(post("/word/projects/" + projectId + "/bibliography")
                        .cookie(aliceWord)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'referenceIds':[]}")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Noch keine Zitate")));

        mvc.perform(get("/word/projects/" + projectId + "/search")
                        .param("q", "lecun")
                        .cookie(aliceWord))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-reference-id=\"" + referenceId + "\"")));
    }

    @Test
    void documentRequestsNeedCsrfToken() throws Exception {
        long projectId = createProject(aliceApp, "Masterarbeit");
        mvc.perform(post("/word/projects/" + projectId + "/citations")
                        .cookie(aliceWord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'citations':[]}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void styleChangeIsSavedAndMarkedForDocumentUpdate() throws Exception {
        long projectId = createProject(aliceApp, "Masterarbeit");
        mvc.perform(post("/word/projects/" + projectId + "/style")
                        .cookie(aliceWord)
                        .with(csrf())
                        .header("HX-Request", "true")
                        .param("style", "DIN_ISO_690"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"style-changed\"")));
        assertThat(jdbc.sql("SELECT citation_style FROM project WHERE id = :p")
                        .param("p", projectId)
                        .query(String.class)
                        .single())
                .isEqualTo("DIN_ISO_690");
    }

    @Test
    void foreignProjectsAndReferencesStayInvisible() throws Exception {
        long aliceProject = createProject(aliceApp, "Masterarbeit");
        long aliceReference = createReference(aliceApp, aliceProject);
        Cookie bobApp = registerAndLogin("bob@example.com", "Bob");
        Cookie bobWord = wordLogin("bob@example.com").getCookie(WORD_COOKIE);
        long bobProject = createProject(bobApp, "Bobs Projekt");

        for (String path : List.of("/citations", "/bibliography")) {
            mvc.perform(post("/word/projects/" + aliceProject + path)
                            .cookie(bobWord)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("{'citations':[],'referenceIds':[]}")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/word/projects/" + aliceProject + "/panel").cookie(bobWord))
                .andExpect(status().isForbidden());

        // Alice's reference id inside Bob's own project resolves to nothing.
        mvc.perform(post("/word/projects/" + bobProject + "/citations")
                        .cookie(bobWord)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'citations':[{'referenceId':" + aliceReference + ",'pages':''}]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texts[0]").doesNotExist());
        mvc.perform(post("/word/projects/" + bobProject + "/bibliography")
                        .cookie(bobWord)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'referenceIds':[" + aliceReference + "]}")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("LeCun"))));
        mvc.perform(get("/word/taskpane").cookie(bobWord))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Masterarbeit"))));
    }
}
