package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.AuthorizationHandler;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;

/**
 * Utilities for authentication and authorization information.
 *
 * @author Robert Smith
 *
 */
public final class AuthorizationUtils {

    /**
     * Remove public constructor.
     */
    private AuthorizationUtils() {

    }

    /**
     * Get the currently logged in OIDC user's username, or the anonymous user if the user is not
     * logged in through OIDC.
     *
     * @return The user's name if Spring-boot security's user is an OIDC user. "ANONYMOUSE_USER"
     * otherwise.
     */
    public static String getUser() {

        // Get the current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If the user is an OIDC user, get the username
        if (authentication.getPrincipal() instanceof OidcUser) {

            OidcUser user = (OidcUser) authentication.getPrincipal();
            return user.getPreferredUsername();
        }

        // The user isn't logged in, return anonymous user
        return AuthorizationHandler.ANONYMOUS_USER;
    }

    /**
     * Check whether authorization is in use. Authorization is in use if the
     * ApplicationConfig an authorization server defined and if the current request
     * was kicked off by a user who is logged in, meaning that authentication is
     * also on and the current action was requested by a user.
     *
     * @param appConfig The configuration information from application.properties
     * @return True if an authorization server is defined and a user is currently
     *         logged in. False otherwise.
     */
    public static boolean isUsingAuthorization(final ApplicationConfig appConfig) {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        // Authorization is in use if there is a handler and a user
        if (authHandler != null && getUser() != null) {
                return true;
        }

        return false;
    }
}
