package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
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
     * Setup base url.
    */
    private static final String BASE_URL = "http://localhost";

    /**
     * Returns full base url w/ port.
     *
     * @return Base URL:port as string
    */
    private String baseUrl() {
        return BASE_URL + ":" + port;
    }

    /**
     * Utility function to create a Dataset.
     *
     * @return UUID of new Dataset
    */
    private String createDataset() throws Exception {
        String jsonString = restTemplate.postForEntity(
            baseUrl() + "/datasets",
            "",
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String uuid  = mapper.readTree(jsonString).get("uuid").textValue();
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
                baseUrl() + "/datasets/" + uuid,
                String.class
            ).getStatusCode()
        );

        String json = restTemplate.getForEntity(
                baseUrl() + "/datasets/" + uuid,
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
                baseUrl() + "/datasets/1",
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
                baseUrl() + "/datasets",
                HttpEntity.EMPTY,
                String.class
            ).getStatusCode()
        );

        String json = restTemplate.postForEntity(
                baseUrl() + "/datasets",
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
                baseUrl() + "/datasets/" + uuid,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );
    }
}
