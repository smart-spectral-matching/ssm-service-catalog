package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import org.json.JSONObject;

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

	@Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
	private int port;

    private static final String BASE_URL = "http://localhost";

    private String baseUrl() {
        return BASE_URL + ":" + port;
    }

    private String createDataset() throws Exception {
        String jsonString = restTemplate.postForEntity(
            baseUrl() + "/datasets",
            "",
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String uuid  = mapper.readTree(jsonString).get("uuid").textValue();
        return uuid;
    }

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
        assertTrue(json.contains("\"uri\""));
    }

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
