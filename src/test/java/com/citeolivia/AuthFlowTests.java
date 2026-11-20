package com.citeolivia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthFlowTests {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.sql("TRUNCATE app_user, spring_session CASCADE").update();
    }

    private void register(String email, String name, String password) throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("email", email)
                        .param("displayName", name)
                        .param("password", password)
                        .param("passwordConfirm", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered="));
    }

    private List<String> rolesOf(String email) {
        return jdbc.sql("""
                        SELECT r.name FROM app_user u JOIN user_role ur ON ur.user_id = u.id
                        JOIN role r ON r.id = ur.role_id WHERE u.email = :e ORDER BY r.name
                        """).param("e", email).query(String.class).list();
    }

    private long userCount() {
        return jdbc.sql("SELECT count(*) FROM app_user").query(Long.class).single();
    }

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void publicPagesAreReachable() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void brandingIsPublicAndApplied() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("<title>Anmelden · CiteOlivia</title>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/brand/logo-horizontal-farbig.svg")));
        for (String asset : List.of(
                "/brand/favicon.svg",
                "/brand/logo-horizontal-farbig.svg",
                "/favicon.ico",
                "/site.webmanifest",
                "/fonts/inter-latin-wght.woff2",
                "/fonts/poppins-latin-700.woff2")) {
            mvc.perform(get(asset)).andExpect(status().isOk());
        }
    }

    @Test
    void registrationStoresHashedPasswordAndNormalizedEmail() throws Exception {
        register("  Alice@Example.COM ", "Alice", PASSWORD);

        String hash = jdbc.sql("SELECT password_hash FROM app_user WHERE email = 'alice@example.com'")
                .query(String.class)
                .single();
        assertThat(hash).startsWith("{bcrypt}").doesNotContain(PASSWORD);
    }

    @Test
    void firstUserBecomesAdminFurtherUsersAreRegularUsers() throws Exception {
        register("first@example.com", "First", PASSWORD);
        register("second@example.com", "Second", PASSWORD);

        assertThat(rolesOf("first@example.com")).containsExactly("ADMIN", "USER");
        assertThat(rolesOf("second@example.com")).containsExactly("USER");
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        register("dup@example.com", "Dup", PASSWORD);

        mvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "DUP@example.com")
                        .param("displayName", "Dup 2")
                        .param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bereits registriert")));
        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "not-an-email")
                        .param("displayName", "X")
                        .param("password", "short")
                        .param("passwordConfirm", "different"))
                .andExpect(status().isOk());
        assertThat(userCount()).isZero();
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/register")
                        .param("email", "x@example.com")
                        .param("displayName", "X")
                        .param("password", PASSWORD)
                        .param("passwordConfirm", PASSWORD))
                .andExpect(status().isForbidden());
        assertThat(userCount()).isZero();
    }

    @Test
    void loginWithValidCredentialsCreatesJdbcSessionAndShowsDashboard() throws Exception {
        register("bob@example.com", "Bob", PASSWORD);

        MvcResult login = mvc.perform(formLogin("/login")
                        .userParameter("email")
                        .user("BOB@example.com")
                        .password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        Cookie sessionCookie = login.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).as("Spring Session cookie").isNotNull();
        assertThat(jdbc.sql("SELECT count(*) FROM spring_session WHERE principal_name = 'bob@example.com'")
                        .query(Long.class)
                        .single())
                .isEqualTo(1);

        mvc.perform(get("/dashboard").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hallo, <span>Bob</span>")));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        register("carol@example.com", "Carol", PASSWORD);

        mvc.perform(formLogin("/login")
                        .userParameter("email")
                        .user("carol@example.com")
                        .password("wrong-password-123"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void adminPageIsProtectedByMethodSecurity() throws Exception {
        mvc.perform(get("/admin/users").with(user("u@example.com").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").with(user("a@example.com").authorities(() -> "users:read")))
                .andExpect(status().isOk());
    }

    @Test
    void emailCheckReturnsHtmlFragment() throws Exception {
        register("taken@example.com", "Taken", PASSWORD);

        mvc.perform(get("/register/email-check").param("email", "taken@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bereits registriert")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<html"))));
        mvc.perform(get("/register/email-check").param("email", "free@example.com"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("noch frei")));
    }
}
