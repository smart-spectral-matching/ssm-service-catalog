package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import lombok.AllArgsConstructor;

/**
 * Represents the REST API server config.
 */
@ConfigurationProperties(prefix = "apiserver")
@ConstructorBinding
@AllArgsConstructor
public class Server {

    /**
     * <p>
     * Hostname + port of the REST api server. Examples:
     * </p>
     *
     * <ul>
     * <li>http://localhost:8080</li>
     * <li>https://ssm.ornl.gov</li>
     * </ul>
    */
    private String host;

    /**
     * @return host for the REST API server
     */
    public String getHost() {
        return host;
    }
}
