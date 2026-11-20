package com.citeolivia;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Drops and recreates the test schema via Flyway before the test run. */
@Configuration(proxyBeanMethods = false)
@Profile("test")
class TestFlywayConfig {

    @Bean
    FlywayMigrationStrategy cleanMigrate() {
        return flyway -> {
            // defense in depth: never clean anything but a *_test database (URL of the real connection)
            try (Connection connection =
                    flyway.getConfiguration().getDataSource().getConnection()) {
                TestDatabaseGuard.check(connection.getMetaData().getURL());
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            flyway.clean();
            flyway.migrate();
        };
    }
}
