package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.util.Locale;

import javax.servlet.ServletContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BatsDatasetControllerIT {
    /**
     * Object Mapper reused for all tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Web context.
     */
    @Autowired
    private WebApplicationContext webContext;

    /**
     * The mock agent used to make requests to endpoints.
     */
    private MockMvc mockMvc;

    /**
     * Server context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Application configuration from application.properties.
     */
    @Autowired
    private ApplicationConfig applicationConfig;

    /**
     * Non-static method run before each @Test. Define the MockMVC agent.
     */
    @BeforeAll
    public void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webContext)
            .apply(springSecurity())
            .build();
    }

    /**
     *
     * @return base Dataset URI for POSTing (with trailing slash)
     */
    private String getDatasetUri() {
        return servletContext.getContextPath() + "/datasets/";
    }

    /**
     * Returns string with dataset JSON for POST to create new dataset.
     *
     * @param title title for the new dataset
     * @return      JSON as string for new dataset, used for POST
    */
    private String getDatasetData(final String title) {
        ObjectNode dataset = MAPPER.createObjectNode();
        dataset.put("title", title);
        return dataset.toString();
    }

    /**
     * Utility function to create a Dataset.
     *
     * @param title Title for the dataset
     * @return Title of new Dataset
    */
    private String createDataset(final String title) throws Exception {
        String datasetJson = getDatasetData(title);

        String response = mockMvc.perform(post(getDatasetUri())
        .contentType(MediaType.APPLICATION_JSON)
        .content(datasetJson))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
        final String datasetTitle = MAPPER.readTree(response).get("title").asText();

        return datasetTitle;
    }

    /**
     * Test to get a Dataset.
    */
    @Test
    public void testGetDataset() throws Exception {
        final String title = "testGetDataset";
        final String titleLower = createDataset(title);

        // Test using the returned title from POST
        String responseLower = mockMvc.perform(
            get(getDatasetUri() + titleLower)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        final String datasetTitleLower = MAPPER.readTree(responseLower).get("title").asText();
        Assertions.assertTrue(datasetTitleLower.contains(titleLower));

        // Test case-insenstive
        String response = mockMvc.perform(
            get(getDatasetUri() + title)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

        final String datasetTitle = MAPPER.readTree(response).get("title").asText();
        Assertions.assertTrue(datasetTitle.contains(titleLower));
    }

    /**
     * Test to get correct HTTP status if Dataset not found.
    */
    @Test
    public void testGetDataSetNotFound() throws Exception {
        mockMvc.perform(
            get(getDatasetUri() + "pizza")
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isNotFound());

        mockMvc.perform(
            get(getDatasetUri() + "pizza/models")
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isNotFound());

        mockMvc.perform(
            get(getDatasetUri() + "pizza/models/uuids")
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isNotFound());
    }

    /**
     * Test to create a Dataset.
    */
    @Test
    public void testCreateDataSet() throws Exception {
        String title = "testCreateDataset-foo";
        String datasetJson = getDatasetData(title);

        System.out.println("\n\n");
        System.out.println("Dataset title: " + title);
        System.out.println("Dataset JSON body: " + datasetJson);
        System.out.println("Auth Type: " + applicationConfig.getAuthorization());
        System.out.println("\n\n");

        String response = mockMvc.perform(post(getDatasetUri())
        .contentType(MediaType.APPLICATION_JSON)
        .content(datasetJson))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

        String datasetTitle = MAPPER.readTree(response).get("title").asText();
        Assertions.assertTrue(response.contains("\"title\":"));
        Assertions.assertTrue(datasetTitle.contains(title.toLowerCase(new Locale("en"))));
    }

    /**
     * Test we return correct response for invalid Dataset title.
    */
    @Test
    public void testForInvalidDatasetTitle() throws Exception {
        String datasetJsonNumbers = getDatasetData("testForInvalidDatasetTitle-99");

        String response = mockMvc.perform(post(getDatasetUri())
        .contentType(MediaType.APPLICATION_JSON)
        .content(datasetJsonNumbers))
        .andExpect(status().isBadRequest())
        .andReturn().getResponse().getContentAsString();

        String datasetJsonHyphen = getDatasetData("testForInvalidDatasetTitle_foo");
        response = mockMvc.perform(post(getDatasetUri())
        .contentType(MediaType.APPLICATION_JSON)
        .content(datasetJsonHyphen))
        .andExpect(status().isBadRequest())
        .andReturn().getResponse().getContentAsString();
    }

    /**
     * Test we cannot re-create two Datasets with same title.
    */
    @Test
    public void testTwoDatasetsCannotHaveSameTitle() throws Exception {
        String title = "testTwoDatasetsCannotHaveSameTitle";
        createDataset(title);

        // Upper-case test
        String datasetJson = getDatasetData(title);
        String response = mockMvc.perform(post(getDatasetUri())
        .contentType(MediaType.APPLICATION_JSON)
        .content(datasetJson))
        .andExpect(status().isConflict())
        .andReturn().getResponse().getContentAsString();

        // Lower-case test
        datasetJson = getDatasetData(title.toLowerCase(new Locale("en")));
        response = mockMvc.perform(post(getDatasetUri())
        .contentType(MediaType.APPLICATION_JSON)
        .content(datasetJson))
        .andExpect(status().isConflict())
        .andReturn().getResponse().getContentAsString();
    }

    /**
     * Test to delete a Dataset.
    */
    @Test
    public void testDeleteDataSet() throws Exception {
        // Test using the title returned from POST
        String title = "testDeleteDataSet";
        String titleLowerCase = createDataset(title);
        mockMvc.perform(
            delete(getDatasetUri() + "/" + titleLowerCase)
        ).andExpect(status().isNoContent());

        // Test for case-insensitive
        createDataset(title);
        mockMvc.perform(
            delete(getDatasetUri() + "/" + title)
        ).andExpect(status().isNoContent());
    }
}
