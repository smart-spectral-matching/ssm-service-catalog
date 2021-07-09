package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.util.Locale;

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
import org.springframework.http.ResponseEntity;

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
        String title = "testGetDataset";
        String titleLower = createDataset(title);

        // Test using the returned title from POST
        ResponseEntity<String> responseLower = restTemplate.getForEntity(
            createUrl("/datasets/" + titleLower),
            String.class
        );
        Assertions.assertEquals(
            HttpStatus.OK,
            responseLower.getStatusCode()
        );
        String json = responseLower.getBody();
        Assertions.assertTrue(json.contains("\"title\":\"" + titleLower + "\""));

        // Test case-insenstive
        ResponseEntity<String> response = restTemplate.getForEntity(
            createUrl("/datasets/" + title),
            String.class
        );
        Assertions.assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        json = response.getBody();
        Assertions.assertTrue(json.contains("\"title\":\"" + titleLower + "\""));
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
        String title = "testCreateDataset-foo";
        String datasetJson = getDatasetData(title);

        ResponseEntity<String> response = restTemplate.postForEntity(
            createUrl("/datasets"),
            makeBody(MediaType.APPLICATION_JSON, datasetJson),
            String.class
        );
        Assertions.assertEquals(
            HttpStatus.CREATED,
            response.getStatusCode()
        );

        String json = response.getBody();
        Assertions.assertTrue(json.contains("\"title\":"));
        Assertions.assertTrue(json.contains(title.toLowerCase(new Locale("en"))));
    }

    /**
     * Test we return correct response for invalid Dataset title.
    */
    @Test
    public void testForInvalidDatasetTitle() throws Exception {
        String datasetJsonNumbers = getDatasetData("testForInvalidDatasetTitle-99");
        Assertions.assertEquals(
            HttpStatus.BAD_REQUEST,
            restTemplate.postForEntity(
                createUrl("/datasets"),
                makeBody(MediaType.APPLICATION_JSON, datasetJsonNumbers),
                String.class
            ).getStatusCode()
        );

        String datasetJsonHyphen = getDatasetData("testForInvalidDatasetTitle_foo");
        Assertions.assertEquals(
            HttpStatus.BAD_REQUEST,
            restTemplate.postForEntity(
                createUrl("/datasets"),
                makeBody(MediaType.APPLICATION_JSON, datasetJsonHyphen),
                String.class
            ).getStatusCode()
        );
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
        Assertions.assertEquals(
            HttpStatus.CONFLICT,
            restTemplate.postForEntity(
                createUrl("/datasets"),
                makeBody(MediaType.APPLICATION_JSON, datasetJson),
                String.class
            ).getStatusCode()
        );

        // Lower-case test
        datasetJson = getDatasetData(title.toLowerCase(new Locale("en")));
        Assertions.assertEquals(
            HttpStatus.CONFLICT,
            restTemplate.postForEntity(
                createUrl("/datasets"),
                makeBody(MediaType.APPLICATION_JSON, datasetJson),
                String.class
            ).getStatusCode()
        );
    }

    /**
     * Test to delete a Dataset.
    */
    @Test
    public void testDeleteDataSet() throws Exception {
        // Test using the title returned from POST
        String title = "testDeleteDataSet";
        String titleLowerCase = createDataset(title);
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                createUrl("/datasets/" + titleLowerCase),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );

        // Test for case-insensitive
        createDataset(title);
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
