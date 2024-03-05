package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

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
public class BatsCollectionControllerIT {

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
     * Returns string with collection JSON for POST to create new collection.
     *
     * @param title title for the new collection
     * @return      JSON as string for new collection, used for POST
    */
    private String getCollectionData(final String title) {
        ObjectNode collection = MAPPER.createObjectNode();
        collection.put("title", title);
        return collection.toString();
    }


    /**
     * Utility function to create a Collection.
     *
     * @param title Title for the collection
     * @return Title of new Collection
    */
    private String createCollection(final String title) throws Exception {
        String collectionJson = getCollectionData(title);
        String jsonString = restTemplate.postForEntity(
            createUrl("/s"),
            makeBody(MediaType.APPLICATION_JSON, collectionJson),
            String.class).getBody();

        String newTitle  = MAPPER.readTree(jsonString)
                                     .get("title")
                                     .asText();
        return newTitle;
    }

    /**
     * Test to get a Collection.
    */
    @Test
    public void testGetCollection() throws Exception {
        String title = "testGetCollection";
        String titleLower = createCollection(title);

        // Test using the returned title from POST
        ResponseEntity<String> responseLower = restTemplate.getForEntity(
            createUrl("/s/" + titleLower),
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
            createUrl("/s/" + title),
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
     * Test to get correct HTTP status if Collection not found.
    */
    @Test
    public void testGetCollectionNotFound() throws Exception {
        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                createUrl("/collection/pizza"),
                Void.class
            ).getStatusCode()
        );

        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                createUrl("/collection/pizza/datasets"),
                Void.class
            ).getStatusCode()
        );

        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                createUrl("/collections/pizza/datasets/uuids"),
                Void.class
            ).getStatusCode()
        );
    }

    /**
     * Test to create a Collection.
    */
    @Test
    public void testCreateCollection() throws Exception {
        String title = "testCreateCollection-foo";
        String collectionJson = getCollectionData(title);

        ResponseEntity<String> response = restTemplate.postForEntity(
            createUrl("/collections"),
            makeBody(MediaType.APPLICATION_JSON, collectionJson),
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
     * Test we return correct response for invalid Collection title.
    */
    @Test
    public void testForInvalidCollectionTitle() throws Exception {
        String collectionJsonNumbers = getCollectionData("testForInvalidCollectionTitle-99");
        Assertions.assertEquals(
            HttpStatus.BAD_REQUEST,
            restTemplate.postForEntity(
                createUrl("/collections"),
                makeBody(MediaType.APPLICATION_JSON, collectionJsonNumbers),
                String.class
            ).getStatusCode()
        );

        String collectionJsonHyphen = getCollectionData("testForInvalidCollectionTitle_foo");
        Assertions.assertEquals(
            HttpStatus.BAD_REQUEST,
            restTemplate.postForEntity(
                createUrl("/collections"),
                makeBody(MediaType.APPLICATION_JSON, collectionJsonHyphen),
                String.class
            ).getStatusCode()
        );
    }

    /**
     * Test we cannot re-create two collections with same title.
    */
    @Test
    public void testTwocollectionsCannotHaveSameTitle() throws Exception {
        String title = "testTwocollectionsCannotHaveSameTitle";
        createCollection(title);

        // Upper-case test
        String collectionJson = getCollectionData(title);
        Assertions.assertEquals(
            HttpStatus.CONFLICT,
            restTemplate.postForEntity(
                createUrl("/collections"),
                makeBody(MediaType.APPLICATION_JSON, collectionJson),
                String.class
            ).getStatusCode()
        );

        // Lower-case test
        collectionJson = getCollectionData(title.toLowerCase(new Locale("en")));
        Assertions.assertEquals(
            HttpStatus.CONFLICT,
            restTemplate.postForEntity(
                createUrl("/collections"),
                makeBody(MediaType.APPLICATION_JSON, collectionJson),
                String.class
            ).getStatusCode()
        );
    }

    /**
     * Test to delete a collection.
    */
    @Test
    public void testDeletecollection() throws Exception {
        // Test using the title returned from POST
        String title = "testDeletecollection";
        String titleLowerCase = createCollection(title);
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                createUrl("/collections/" + titleLowerCase),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );

        // Test for case-insensitive
        createCollection(title);
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                createUrl("/collections/" + title),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );
    }
}
