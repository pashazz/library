package io.github.pashazz.library.config;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {
    @Bean
    public KeycloakSpringBootConfigResolver keycloakSpringBootConfigResolver () {
        return new KeycloakSpringBootConfigResolver();
    }
}
