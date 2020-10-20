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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BatsDatasetsControllerTest {

	@Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
	private int port;

    private static final String BASE_URL = "http://localhost";

    private String getUrl() {
        return BASE_URL + ":" + port;
    }

    private String createDataset() {
        String uuid = restTemplate.postForEntity(
            getUrl() + "/datasets",
            "",
            String.class).getBody();
        return uuid;
    }

    @Test
    public void testGetDataset() throws Exception {
        String jsonString = createDataset();
        ObjectMapper mapper = new ObjectMapper();
        String uuid  = mapper.readTree(jsonString).get("UUID").textValue();

        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                BASE_URL + ":" + port + "/datasets/" + uuid,
                String.class
            ).getStatusCode()
        );

        JSONObject uuidJsonObject = new JSONObject();
        uuidJsonObject.put("UUID", uuid);
        assertEquals(
            uuidJsonObject.toString(),
            restTemplate.getForEntity(
                BASE_URL + ":" + port + "/datasets/" + uuid,
                String.class
            ).getBody()
        );

    }

    @Test
    public void testGetDatasetNotFound() throws Exception {
        assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getUrl() + "/datasets/1",
                Void.class
            ).getStatusCode()
        );
    }

    @Test
    public void testCreateDataset() throws Exception {
        HttpEntity<String> entity = null;
        assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                getUrl() + "/datasets",
                entity,
                String.class
            ).getStatusCode()
        );
        
        String json = restTemplate.postForEntity(
                getUrl() + "/datasets",
                entity,
                String.class
            ).getBody();
        assertTrue(json.contains("\"UUID\":"));
    }
}