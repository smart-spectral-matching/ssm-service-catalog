package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    }

    /**
     * The authorization type. Valid values are "none" and "keycloak".
     */
    private String authorization;

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
