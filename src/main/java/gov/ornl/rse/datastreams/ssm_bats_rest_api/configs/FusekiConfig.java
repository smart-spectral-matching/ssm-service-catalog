package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Fuseki.class)
public class FusekiConfig { }

