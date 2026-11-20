package com.citeolivia;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Safety net: tests truncate and Flyway-clean their database. They must therefore never run against
 * the shared application database {@code citeolivia}, only against a database whose name ends in
 * {@code _test}. Fails the application context (and with it every test) otherwise – before any data
 * is touched. Only on the test classpath.
 */
@Configuration(proxyBeanMethods = false)
class TestDatabaseGuard {

    TestDatabaseGuard(Environment env) {
        check(env.getProperty("spring.datasource.url"));
    }

    static void check(String url) {
        String db = url == null ? "" : url.replaceFirst("\\?.*$", "");
        if (!db.endsWith("_test")) {
            throw new IllegalStateException(
                    "Tests must only run against a *_test database, but spring.datasource.url is '" + url
                            + "'. Run tests via ./mvnw verify (profile 'test') or set TEST_DB_URL.");
        }
    }
}
