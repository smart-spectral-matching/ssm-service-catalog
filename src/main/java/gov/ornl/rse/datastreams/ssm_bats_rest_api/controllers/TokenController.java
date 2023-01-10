package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Controller for displaying OIDC tokens.
 *
 * @author Robert Smith
 *
 */
@RestController
@RequestMapping("/token")
public class TokenController {

    /**
     * Oauth authentication service.
    */
    @Autowired(required = false)
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Displays the user's token and sample curl usage.
     *
     * @return A webpage
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity getToken() {

        // Get the current user
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        // If the user is an OIDC user, display the OIDC JWT tokens
        if (authentication.getPrincipal() instanceof OidcUser) {

            // The user information
            OidcUser user = (OidcUser) authentication.getPrincipal();

            // If the token has expired, delete it from the session and refresh so that
            // spring security will get a valid one
            if (user.getIdToken().getExpiresAt().isBefore(Instant.now())) {
                SecurityContextHolder.getContext().setAuthentication(null);
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", "/token");
                return new ResponseEntity<String>(headers, HttpStatus.FOUND);
            }

            // Get the JWT tokens
            String token = user.getIdToken().getTokenValue();
            String contents = "Token = " + token;
            OAuth2AuthorizedClient client =
                    authorizedClientService.loadAuthorizedClient("keycloak", user.getName());
            String refreshToken = client.getRefreshToken().getTokenValue();
            String accessToken = client.getAccessToken().getTokenValue();

            // Construct the return content with the tokens and usage instructions
            contents = contents + "\n Refresh Token = " + refreshToken + "\n Access Token = "
                + accessToken
                + "\n To manually use this token: \n curl -H \"Authorization: Bearer "
                + token + "\" "
                + ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
                + "/proflie";
            return new ResponseEntity(contents, HttpStatus.OK);

        } else if (authentication.getPrincipal() instanceof Jwt) {

            // If the user authenticated with a JWT, echo back the JWT
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return new ResponseEntity("\n JWT authenticated \n" + jwt.toString() + "\n",
                    HttpStatus.OK);
        }

        // If another authentication method was used, display what it was.
        return new ResponseEntity("Logged in with: "
                + authentication.getPrincipal().getClass().toString(),
                HttpStatus.OK);
    }
}
