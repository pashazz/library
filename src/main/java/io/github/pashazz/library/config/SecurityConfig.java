package io.github.pashazz.library.config;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

/**
 * This class delegates user authentication to keycloak
 */
@KeycloakConfiguration //This is WebSecurityConfiguration.
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

@Autowired
public void configureGlobal(AuthenticationManagerBuilder auth) {
    // What this does is maps Keycloak X role to Spring Security ROLE_x role.
    SimpleAuthorityMapper grantedAuthorityMapper = new SimpleAuthorityMapper();
    grantedAuthorityMapper.setPrefix("ROLE_");

    KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
    keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(grantedAuthorityMapper);
    auth.authenticationProvider(keycloakAuthenticationProvider);
}
@Bean
@Override
protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    // For a service-to-service NullAuthenticatedSessionStrategy is preferred.
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
}



@Bean
@Override
@ConditionalOnMissingBean(HttpSessionManager.class)
protected HttpSessionManager httpSessionManager() {
    return new HttpSessionManager();
}

// This is RBAC.
@Override
protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    http
        .authorizeRequests()
        .antMatchers("/books").hasAnyRole("Member", "Librarian")
        .antMatchers("/manager/**").hasRole("Librarian")
        .anyRequest().permitAll();
}
}
