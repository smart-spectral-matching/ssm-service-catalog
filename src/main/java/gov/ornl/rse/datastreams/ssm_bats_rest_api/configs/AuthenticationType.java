package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

/**
 * The type of authentication the API will apply.
 *
 * @author Robert Smith
 *
 */
public enum AuthenticationType {

    /**
     * NONE- No authentication required.
     * KEYCLOAK- OIDC authentication through Keycloak.
    */
    NONE, KEYCLOAK
}
