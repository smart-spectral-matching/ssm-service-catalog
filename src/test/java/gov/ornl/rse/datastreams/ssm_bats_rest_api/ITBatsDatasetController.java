package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ITBatsDatasetController {

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
     * @return     Concatenated base url and path
    */
    private String createUrl(final String path) {
        return BASE_URL + ":" + port + path;
    }

    /**
     * Utility function to create a Dataset.
     *
     * @return UUID of new Dataset
    */
    private String createDataset() throws Exception {
        String jsonString = restTemplate.postForEntity(
            createUrl("/datasets"),
            "",
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String uuid  = mapper.readTree(jsonString).get("uuid").asText();
        return uuid;
    }

    /**
     * Test to get a Dataset.
    */
    @Test
    public void testGetDataset() throws Exception {
        String uuid = createDataset();

        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                createUrl("/datasets/" + uuid),
                String.class
            ).getStatusCode()
        );

        String json = restTemplate.getForEntity(
                createUrl("/datasets/" + uuid),
                String.class
            ).getBody();

        assertTrue(json.contains("\"uuid\":\"" + uuid + "\""));
    }

    /**
     * Test to get correct HTTP status if Dataset not found.
    */
    @Test
    public void testGetDataSetNotFound() throws Exception {
        assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                createUrl("/datasets/1"),
                Void.class
            ).getStatusCode()
        );
    }

    /**
     * Test to create a Dataset.
    */
    @Test
    public void testCreateDataSet() throws Exception {
        assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                createUrl("/datasets"),
                HttpEntity.EMPTY,
                String.class
            ).getStatusCode()
        );

        String json = restTemplate.postForEntity(
                createUrl("/datasets"),
                HttpEntity.EMPTY,
                String.class
            ).getBody();
        assertTrue(json.contains("\"uuid\":"));
    }

    /**
     * Test to delete a Dataset.
    */
    @Test
    public void testDeleteDataSet() throws Exception {
        String uuid = createDataset();
        assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                createUrl("/datasets/" + uuid),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );
    }
}
