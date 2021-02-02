package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Configuration
@ConfigurationProperties(prefix = "ssm_bats_rest_api_server")
/*
 * Represents the REST API server config.
*/
public class ServerConfig {

    /**
     * Hostname for REST API server.
    */
    private final String hostname;

    /**
     * Port for REST API server.
    */
    private final int port;

    /**
     * Constructor for ServerConfig.
     *
     * @param newHostname Hostname for REST API server
     * @param newPort        Port for REST API server
    */
    public ServerConfig(final String newHostname, final int newPort) {
        this.hostname = newHostname;
        this.port = newPort;
    }

    /**
     * Get host for REST API server set by properties.
     *
     * @return Hostname for the REST API server
    */
    public final String getHost() {
        return hostname;
    }

    /**
     * Get port for REST API server set by properties.
     *
     * @return Port number for the REST API server
    */
    public final int getPort() {
        return port;
    }

    /**
     * Get full hostname REST API server set by properties.
     *
     * @return Hostname + port for the REST API server
    */
    public final String getFullHost() {
        return hostname + ":" + String.valueOf(port);
    }
}
