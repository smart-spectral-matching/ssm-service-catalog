package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties
/*
 * Represents the REST API server config.
*/
public class ServerConfig {

    /**
     * Hostname for REST API server.
    */
    @Value("${server.hostname}")
    private String hostname;

    /**
     * Port for REST API server.
    */
    @Value("${server.port}")
    private int port;

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
