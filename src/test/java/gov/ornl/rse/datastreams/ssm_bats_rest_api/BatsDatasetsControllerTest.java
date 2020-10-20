package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
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
        System.out.println("jsonString: " + jsonString);
        String uuid  = mapper.readTree(jsonString).get("UUID").textValue();
        System.out.println("---");
        System.out.println("UUID:  " + uuid);
        System.out.println("---");
        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                BASE_URL + ":" + port + "/datasets/" + uuid,
                Void.class
            ).getStatusCode()
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
}