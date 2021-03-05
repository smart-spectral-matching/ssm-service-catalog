package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * All configuration derived from application properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
public class ApplicationConfig {

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
     * Nested Fuseki configuration.
     */
    private final Fuseki fuseki = new Fuseki();

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

}
