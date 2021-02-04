package gov.ornl.rse.datastreams.ssm_bats_rest_api.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ServerTest {

    /**
     * Target port for test results.
    */
    private static final int PORT = 8888;

    /**
     * Target hostname for test results.
    */
    private static final String HOSTNAME = "localhost";

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
                context -> assertThat(context).hasSingleBean(Server.class));
    }

    @Test
    void hostnameSetByPropertyValue() {
        applicationContextRunner
            .withPropertyValues("apiserver.hostname=" + HOSTNAME)
            .run(
                context ->
                    assertThat(context.getBean(Server.class).getHostname())
                      .isEqualTo(HOSTNAME));
    }

    @Test
    void portSetByPropertyValue() {
        applicationContextRunner
            .withPropertyValues("apiserver.port=" + PORT)
            .run(
                context ->
                    assertThat(context.getBean(Server.class).getPort())
                      .isEqualTo(PORT));
    }
}