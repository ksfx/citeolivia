package com.citeolivia.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

/**
 * Session cookies. The web app uses {@code SESSION} (SameSite=Lax). The Word add-in under {@code
 * /word/**} runs inside an iframe of Word on the web (a third-party context), where a Lax cookie is
 * never sent. It therefore gets its own cookie {@code CO_WORD_SESSION}: SameSite=None, Secure,
 * Partitioned (CHIPS) and restricted to {@code Path=/word}, so the main app never sees a cross-site
 * cookie. CSRF protection stays enabled for both.
 */
@Configuration
class SessionCookieConfig {

    static final String WORD_PATH = "/word";
    static final String WORD_COOKIE = "CO_WORD_SESSION";

    @Bean
    HttpSessionIdResolver httpSessionIdResolver() {
        DefaultCookieSerializer app = new DefaultCookieSerializer();
        app.setCookieName("SESSION");
        app.setUseHttpOnlyCookie(true);
        app.setSameSite("Lax");

        DefaultCookieSerializer word = new DefaultCookieSerializer();
        word.setCookieName(WORD_COOKIE);
        word.setCookiePath(WORD_PATH);
        word.setUseHttpOnlyCookie(true);
        word.setUseSecureCookie(true);
        word.setSameSite("None");
        word.setPartitioned(true);

        return new PathAwareResolver(resolver(app), resolver(word));
    }

    private static CookieHttpSessionIdResolver resolver(DefaultCookieSerializer serializer) {
        CookieHttpSessionIdResolver resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(serializer);
        return resolver;
    }

    static boolean isWordRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(WORD_PATH) || path.startsWith(WORD_PATH + "/");
    }

    /** Chooses the cookie by request path. */
    private static final class PathAwareResolver implements HttpSessionIdResolver {

        private final CookieHttpSessionIdResolver app;
        private final CookieHttpSessionIdResolver word;

        PathAwareResolver(CookieHttpSessionIdResolver app, CookieHttpSessionIdResolver word) {
            this.app = app;
            this.word = word;
        }

        private HttpSessionIdResolver pick(HttpServletRequest request) {
            return isWordRequest(request) ? word : app;
        }

        @Override
        public List<String> resolveSessionIds(HttpServletRequest request) {
            return pick(request).resolveSessionIds(request);
        }

        @Override
        public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
            pick(request).setSessionId(request, response, sessionId);
        }

        @Override
        public void expireSession(HttpServletRequest request, HttpServletResponse response) {
            pick(request).expireSession(request, response);
        }
    }
}
