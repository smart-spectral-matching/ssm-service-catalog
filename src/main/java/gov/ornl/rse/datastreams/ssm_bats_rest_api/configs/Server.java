package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "apiserver")
@ConstructorBinding
@AllArgsConstructor
/*
 * Represents the REST API server config.
*/
public class Server {
    /**
     * Hostname for REST API server.
     *
     * @return Hostname for the REST API server
    */
    @Getter private final String hostname;

    /**
     * Port for REST API server.
     *
     * @return Port number for the REST API server
    */
    @Getter private final int port;

    /**
     * Get full hostname REST API server set by properties.
     *
     * @return Hostname + port for the REST API server
    */
    public final String getFullHost() {
        return hostname + ":" + String.valueOf(port);
    }
}
