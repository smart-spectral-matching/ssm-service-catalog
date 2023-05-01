package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

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
            http.authorizeRequests(authorizeRequests -> authorizeRequests
                    .antMatchers("/logon").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(e -> e.permitAll())
            .logout(e -> e.logoutSuccessHandler(handler))
            .oauth2ResourceServer()
            .jwt();
        } else if (applicationConfig.getAuthorization().equals(AuthorizationType.NONE)) {
            http.authorizeRequests(authorizeRequests -> authorizeRequests.anyRequest().permitAll());
        }
    }

}
