package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


/**
 * Security configuration for OIDC sign in.
 *
 * @author Robert Smith
 *
 */
@EnableWebSecurity
@Configuration
public class OIDCLoginSecurityConfig extends WebSecurityConfigurerAdapter {

    /**
     * Application configuration from application.properties.
     */
    @Autowired
    private ApplicationConfig applicationConfig;

    /**
     * Spring-boot client registry.
     */
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Creates the filter chain.
     *
     * @param http SpringBoot security object
     * @throws Exception
     * */
    @Override
    public void configure(final HttpSecurity http) throws Exception {
        // Do nothing if not using keycloak type authorization
        if (applicationConfig.getAuthorization().equals(AuthorizationType.KEYCLOAK)) {

            // Create a logout handler
            OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
            handler.setPostLogoutRedirectUri(applicationConfig.getHost() + "token/");

            // Configure the request matcher to secure all pages except logon
            http.cors(Customizer.withDefaults())
                .authorizeRequests(authorizeRequests -> authorizeRequests
                .antMatchers("/logon").permitAll()
                .anyRequest().authenticated())
                .oauth2Login(e -> e.permitAll())
                .logout(e -> e.logoutSuccessHandler(handler))
                .oauth2ResourceServer()
                .jwt();
        } else if (applicationConfig.getAuthorization().equals(AuthorizationType.NONE)) {

            // Permit all requests, no authN/Z
            http.cors(Customizer.withDefaults())
                .authorizeHttpRequests((authz) -> authz
                .anyRequest()
                .permitAll()
            )
            .csrf()
            .disable();
        }
    }

    /**
     * Configure CORS for service.
     * @return source the CORS configuration source.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
