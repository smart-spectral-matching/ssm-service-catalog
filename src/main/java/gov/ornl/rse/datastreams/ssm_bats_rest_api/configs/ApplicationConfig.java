package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.AuthorizationHandler;

/**
 * All configuration derived from application properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public class ApplicationConfig {

    /**
     * Configuration properties for File Converter service.
     */
    public static class FileConverter {
        /**
         * URI for the file converter service.
         */
        private String uri;

        /**
         * @return File converter server URI
         */
        public String getURI() {
            return uri;
        }

        /**
         * Set the file converter server uri.
         *
         * @param newUri
         */
        void setURI(final String newUri) {
            this.uri = newUri;
        }
    }

    /**
     * Configuration properties relating to Fuseki.
     */
    public static class Fuseki {
        /**
         * Hostname for Fuseki server.
         */
        private String hostname;
        /**
         * Fuseki server's port.
         */
        private Integer port;

        /**
         * @return Fuseki server hostname
         */
        public String getHostname() {
            return hostname;
        }

        /**
         * Set the Fuseki server hostname.
         * Called internally by Spring, should not be
         * used directly.
         *
         * @param hostname
         */
        void setHostname(final String hostname) {
            this.hostname = hostname;
        }

        /**
         * @return Fuseki server port
         */
        public Integer getPort() {
            return port;
        }

        /**
         * Set the Fuseki server hostname.
         * Called internally by Spring, should not be
         * used directly.
         *
         * @param port
         */
        void setPort(final Integer port) {
            this.port = port;
        }

        /**
         * Get the Fuseki server URI.
         *
         * @return URI of Fuseki
         */
        public String getURI() {
            return this.hostname + ":" + this.port;
        }
    }

    /**
     * The authorization type. Valid values are "none" and "keycloak".
     */
    private String authorization;

    /**
     * Handler for authorization API calls.
     */
    private AuthorizationHandler authorizationHandler;

    /**
     * Nested Fuseki configuration.
     */
    private final Fuseki fuseki = new Fuseki();

    /**
     * The JSON-LD -> SSM JSON converion type.
     * Valid values are "embedded" and "fileconverterservice".
     *
     */
    private String jsonConversion;

    /**
     * Nested file converter service configuration.
     */
    private final FileConverter fileConverter = new FileConverter();

    /**
     * <p>
     * Hostname + port of the REST API server. Examples:
     * </p>
     *
     * <ul>
     * <li>http://localhost:8080</li>
     * <li>https://ssm.ornl.gov</li>
     * </ul>
     */
    private String host;

    /**
     * URL for the Zanzibar read API.
     */
    private String zanzibarReadHost;

    /**
     * URL for the Zanzibar write API.
     */
    private String zanzibarWriteHost;

    /**
     * Getter for the Authorization type.
     *
     * @return The authorization type the API will use.
     */
    public AuthorizationType getAuthorization() {
        return EnumUtils.getEnumIgnoreCase(AuthorizationType.class, authorization);
    }

    /**
     * Setter for the authorization type.
     *
     * @param authorization
     */
    void setAuthorization(final String authorization) {
        this.authorization = authorization;
    }

    /**
     * Getter for the JsonConversion type.
     *
     * @return The authorization type the API will use.
     */
    public JsonConversionType getJsonConversion() {
        return EnumUtils.getEnumIgnoreCase(JsonConversionType.class, jsonConversion);
    }

    /**
     * Setter for the json conversion type.
     *
     * @param jsonConversion
     */
    void setJsonConversion(final String jsonConversion) {
        this.jsonConversion = jsonConversion;
    }

    /**
     * @return host for the REST API server
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the fully qualified host of the REST API server. Called internally by
     * Spring, should not be used directly.
     *
     * @param host
     */
    void setHost(final String host) {
        this.host = host;
    }

    /**
     * Getter for the ZanzibarReadHost.
     *
     * @return The ZanzibarReadHost
     */
    public String getZanzibarReadHost() {
        return zanzibarReadHost;
    }

    /**
     * Setter for the ZanzibarReadHost.
     *
     * @param zanzibarReadHost the new host
     */
    public void setZanzibarReadHost(final String zanzibarReadHost) {
        this.zanzibarReadHost = zanzibarReadHost;
    }

    /**
     * Getter for the ZanzibarWriteHost.
     *
     * @return The ZanzibarWriteHost
     */
    public String getZanzibarWriteHost() {
        return zanzibarWriteHost;
    }

    /**
     * Setter for the ZanzibarWriteHost.
     *
     * @param zanzibarWriteHost the new host
     */
    public void setZanzibarWriteHost(final String zanzibarWriteHost) {
        this.zanzibarWriteHost = zanzibarWriteHost;
    }

    /**
     * Returns the handler for authorization API calls, or null if authorization is not configured.
     *
     * @return An AuthorizationHandler if authorization is defined, or null if it is not.
     */
    public AuthorizationHandler getAuthorizationHandler() {

        // If the handler doesn't exist but the server address was given, create it.
        if (authorizationHandler == null && zanzibarReadHost != null && zanzibarWriteHost != null) {
            authorizationHandler = new AuthorizationHandler(this);
        }

        return authorizationHandler;

    }

    /**
     * @return nested Fuseki config
     */
    public Fuseki getFuseki() {
        return fuseki;
    }

    /**
     * @return nested File Converter service
     */
    public FileConverter getFileConverter() {
        return fileConverter;
    }
}
