package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties
public class FusekiConfig {

    /**
     * Hostname for Fuseki server
     */
    @Value("${fuseki.hostname}")
    private String hostname;

    /**
     * Port for Fuseki server
     */
    @Value("${fuseki.port}")
    private int port;

    /**
     * Get host for Fuseki server set by properties
     */
    public String getHost() {
        return hostname;
    }

    /**
     * Get port for Fuseki server set by properties
     */
    public int getPort() {
        return port;
    }
} 
