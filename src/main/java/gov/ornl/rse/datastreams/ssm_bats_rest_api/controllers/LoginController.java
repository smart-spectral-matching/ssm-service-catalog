package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;

/**
 * Controller for a page to verify user logged in status and provide a url to
 * instructions for obtaining a token if not.
 *
 * @author Robert Smith
 *
 */
@RestController
@RequestMapping("/logon")
public class LoginController {

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Returns whether the user is logged in or the url to login with.
     *
     * @return A webpage
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity getLogin() {

        // Get the current user
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        // If the user is logged in the OIDC, give a success message.
        if (authentication.getPrincipal() instanceof OidcUser) {
            return new ResponseEntity("Logged in", HttpStatus.OK);
        }

        // If user isn't logged in with OIDC, give the url for the token page
        return new ResponseEntity(appConfig.getHost() + "/token", HttpStatus.UNAUTHORIZED);
    }
}
