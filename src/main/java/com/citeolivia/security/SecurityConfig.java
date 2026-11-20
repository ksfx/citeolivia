package com.citeolivia.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Central web security configuration.
 *
 * <p>Form login against the own user table. CSRF protection stays enabled (default) – htmx sends
 * the token as a header, see {@code fragments/layout.html}. To add OIDC later, add {@code
 * http.oauth2Login(...)} here and map the OIDC user to a {@link CurrentUser}; the rest of the
 * application only depends on {@link CurrentUser}.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    /**
     * Hosts that may embed the Word add-in (Word on the web, Microsoft 365, OneDrive, SharePoint).
     * Word desktop loads the task pane top-level and needs no entry here.
     */
    static final String WORD_FRAME_ANCESTORS = "frame-ancestors 'self' https://*.office.com https://*.office365.com"
            + " https://*.officeapps.live.com https://*.cloud.microsoft https://*.microsoft365.com"
            + " https://*.sharepoint.com https://*.live.com https://*.onedrive.com";

    /** Idle timeout of add-in sessions: writing sessions are long, the web app keeps 30 minutes. */
    static final int WORD_SESSION_SECONDS = 8 * 60 * 60;

    /**
     * Word add-in (task pane inside Word). Own login page, own session cookie (see {@link
     * SessionCookieConfig}) and framing allowed only for Microsoft Office hosts. CSRF stays enabled.
     */
    @Bean
    @Order(1)
    SecurityFilterChain wordAddinSecurityFilterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler success = new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request, HttpServletResponse response, Authentication authentication)
                    throws IOException, ServletException {
                request.getSession().setMaxInactiveInterval(WORD_SESSION_SECONDS);
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
        success.setDefaultTargetUrl("/word/taskpane");
        // Always the same relative target: a saved request would be an absolute URL, and any URL that
        // does not exactly match the add-in domain makes Word desktop open it in the external browser.
        success.setAlwaysUseDefaultTargetUrl(true);
        http.securityMatcher("/word/**")
                .authorizeHttpRequests(auth -> auth.requestMatchers("/word/manifest.xml", "/word/login", "/word/*.png")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.loginPage("/word/login")
                        .usernameParameter("email")
                        .successHandler(success)
                        .failureUrl("/word/login?error")
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/word/logout")
                        .logoutSuccessUrl("/word/login?logout")
                        .permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(wordEntryPoint()))
                .headers(headers -> headers.frameOptions(frame -> frame.disable())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(WORD_FRAME_ANCESTORS)));
        return http.build();
    }

    /** Expired session: htmx and fetch calls get 401 (htmx then reloads the login page), pages a redirect. */
    private static AuthenticationEntryPoint wordEntryPoint() {
        LoginUrlAuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/word/login");
        return (request, response, exception) -> {
            if (request.getHeader("HX-Request") != null || request.getHeader("X-CiteOlivia-Addin") != null) {
                response.setHeader("HX-Redirect", request.getContextPath() + "/word/login");
                response.sendError(HttpStatus.UNAUTHORIZED.value());
            } else {
                login.commence(request, response, exception);
            }
        };
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/word-add-in",
                                "/css/**",
                                "/js/**",
                                "/fonts/**",
                                "/brand/**",
                                "/video/**",
                                "/favicon.ico",
                                "/site.webmanifest",
                                "/error")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form.loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/dashboard")
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    /** Delegating encoder: BCrypt for new hashes, still verifies other prefixed formats. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
