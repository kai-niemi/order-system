package se.cloudneutral.orders.config;

import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class FlywayConfig {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    @Profile("!dev")
    public FlywayMigrationStrategy validateAndMigrate() {
        return flyway -> {
            flyway.validate();
            flyway.migrate();
        };
    }

    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy cleanOnValidationFailure() {
        return flyway -> {
            try {
                flyway.validate();
                flyway.migrate();
            } catch (FlywayValidateException e) {
                logger.warn("Flyway migration failed. Destructive recovery enabled!");
                flyway.clean();
                flyway.migrate();
            }
        };
    }
}
