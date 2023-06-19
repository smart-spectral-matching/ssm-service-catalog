package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Utilities for authentication and authorization information.
 * 
 * @author Robert Smith
 *
 */
public final class AuthorizationUtils {

    /**
     * Get the currently logged in OIDC user's username, or null if the user is not logged in through OIDC.
     * 
     * @return The user's name if Spring-boot security's user is an OIDC user. Null otherwise.
     */
    public static String getUser() {
        
        // Get the current user
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        // If the user is an OIDC user, get the username
        if (authentication.getPrincipal() instanceof OidcUser) {

            OidcUser user = (OidcUser) authentication.getPrincipal();
            return user.getPreferredUsername();
        }
        
        // The user isn't logged in
        return null;
    }
}
