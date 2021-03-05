package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ServerTest {

    /**
     * Target host for test results.
    */
    private static final String HOST = "localhost:8888";

    /**
     * Application context runner.
    */
    private ApplicationContextRunner applicationContextRunner;


    @BeforeEach
    void setup() {
        applicationContextRunner =
                new ApplicationContextRunner()
                        .withConfiguration(
                            AutoConfigurations.of(ServerConfig.class));
    }

    @Test
    void serverBeanExists() {
        applicationContextRunner
            .run(
                context -> Assertions.assertThat(context)
                                     .hasSingleBean(Server.class));
    }

    @Test
    void hostnameSetByPropertyValue() {
        applicationContextRunner
            .withPropertyValues("apiserver.host=" + HOST)
            .run(
                context ->
                    Assertions.assertThat(
                            context.getBean(Server.class).getHost()
                        ).isEqualTo(HOST));
    }

}
