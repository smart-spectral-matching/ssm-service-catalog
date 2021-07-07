package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ITBatsDatasetController {

    /**
     * Object Mapper reused for all tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Java servlet context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Setup test rest template.
    */
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Setup local server port for testing.
    */
    @LocalServerPort
    private int port;

    /**
     * Constant base url.
    */
    private static final String BASE_URL = "http://localhost";

    /**
     * Create url using base url.
     *
     * @param path Path of URL to append to base url
     * @return     Concatenated base url, servlet root context, and path
    */
    private String createUrl(final String path) {
        return BASE_URL + ":" + port
          + servletContext.getContextPath() + path;
    }

    /**
     * Helper function to create HTTP body.
     *
     * @param mediaType Content Type for the body data
     * @param body      Data to be posted
     * @return properly formatted body for post statement (with HTTP headers)
    */
    private HttpEntity<Object> makeBody(
        final MediaType mediaType,
        final Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        return new HttpEntity<>(body, headers);
    }

    /**
     * Utility function to create a Dataset.
     *
     * @param title Title for the dataset
     * @return Title of new Dataset
    */
    private String createDataset(final String title) throws Exception {
        ObjectNode titleObject = MAPPER.createObjectNode();
        titleObject.put("title", title);
        String datasetJson = MAPPER.writeValueAsString(titleObject);

        String jsonString = restTemplate.postForEntity(
            createUrl("/datasets"),
            makeBody(MediaType.APPLICATION_JSON, datasetJson),
            String.class).getBody();

        String datasetTitle  = MAPPER.readTree(jsonString)
                                     .get("title")
                                     .asText();
        return datasetTitle;
    }

    /**
     * Test to get a Dataset.
    */
    @Test
    public void testGetDataset() throws Exception {
        String title = createDataset("testGetDataset");

        Assertions.assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                createUrl("/datasets/" + title),
                String.class
            ).getStatusCode()
        );

        String json = restTemplate.getForEntity(
                createUrl("/datasets/" + title),
                String.class
            ).getBody();

        Assertions.assertTrue(json.contains("\"title\":\"" + title + "\""));
    }

    /**
     * Test to get correct HTTP status if Dataset not found.
    */
    @Test
    public void testGetDataSetNotFound() throws Exception {
        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                createUrl("/datasets/pizza"),
                Void.class
            ).getStatusCode()
        );
    }

    /**
     * Test to create a Dataset.
    */
    @Test
    public void testCreateDataSet() throws Exception {
        ObjectNode titleObject = MAPPER.createObjectNode();
        titleObject.put("title", "testCreateDataset");
        String datasetJson = MAPPER.writeValueAsString(titleObject);

        Assertions.assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                createUrl("/datasets"),
                makeBody(MediaType.APPLICATION_JSON, datasetJson),
                String.class
            ).getStatusCode()
        );

        String json = restTemplate.postForEntity(
                createUrl("/datasets"),
                makeBody(MediaType.APPLICATION_JSON, datasetJson),
                String.class
            ).getBody();
        Assertions.assertTrue(json.contains("\"title\":"));
    }

    /**
     * Test to delete a Dataset.
    */
    @Test
    public void testDeleteDataSet() throws Exception {
        String title = createDataset("testDeleteDataSet");
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                createUrl("/datasets/" + title),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );
    }
}
