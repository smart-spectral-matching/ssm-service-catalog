package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
public class ITBatsModelControllerMVC {

    /**
     * Object mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The mock agent used to make requests to endpoints.
     */
    private MockMvc mockMvc;

    /**
     * Web context.
     */
    @Autowired
    private WebApplicationContext webContext;

    /**
     * Server context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Non-static method run before each @Test. Define the MockMVC agent.
     */
    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    /**
     *
     * @return base Dataset URI for POSTing (with trailing slash)
     */
    private String getDatasetUri() {
        return servletContext.getContextPath() + "/datasets/";
    }

    /**
     *
     * @param datasetUUID dataset UUID part of the URL
     * @return base Moder URI for POSTing (with trailing slash)
     */
    private String getModelUri(final String datasetUUID) {
        return getDatasetUri() + datasetUUID + "/models/";
    }

    /**
     * Returns string for a file located in test/resources.
     *
     * @param filename Filename to load from test/resources as string
     * @return         File data as string
    */
    private String getFileDataFromTestResources(final String filename)
    throws IOException {
        return new String(
            Files.readAllBytes(Paths.get("src", "test", "resources", filename))
        );
    }



    /**
     *
     * Full suite test:
     *
     * 1. Test that "created" and "modified" are added when POSTing model.
     * 2. Test that if the user tries to POST JSON with "created" and "modified" properties,
     *    they are ignored.
     * 3. Test that "created" IS NOT updated, but "modified" IS updated
     *    when making a PUT or PATCH request. If the user includes these properties
     *    in their request, the user's values should be ignored.
     *
     * @throws Exception
     */
    @Test
    public void testTimestampChanges() throws Exception {
        // create dataset and make model URI
        String response = mockMvc.perform(post(getDatasetUri()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        final String datasetUUID = MAPPER.readTree(response).path("uuid").asText();
        final String modelUri = getModelUri(datasetUUID);

        // create model with POST
        String sampleJson = getFileDataFromTestResources("simple.input.jsonld");
        response = mockMvc.perform(post(modelUri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sampleJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        // get the created timestamp from the response
        final LocalDateTime createdTime = LocalDateTime.parse(
            MAPPER.readTree(response)
                .get("model")
                .get("@graph")
                .get(1)
                .get("created")
            .asText()
            .replace(' ', 'T')
            );
        // get the updated timestamp from the response
        LocalDateTime updatedTime = LocalDateTime.parse(
            MAPPER.readTree(response)
                .get("model")
                .get("@graph")
                .get(1)
                .get("modified")
            .asText()
            .replace(' ', 'T')
            );
        assertEquals(createdTime, updatedTime);

        // PUT - do not submit timestamps

        // PUT - submit timestamps

        // PATCH - do not submit timestamps

        // PATCH - submit timestamps
    }

}
