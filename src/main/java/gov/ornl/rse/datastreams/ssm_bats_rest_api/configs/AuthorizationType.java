package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

/**
 * The type of authorization the API will apply.
 *
 * @author Robert Smith
 *
 */
public enum AuthorizationType {

    /**
     * NONE- No authorization required.
     * KEYCLOAK- OIDC authentication through Keycloak.
    */
    NONE, KEYCLOAK
}
