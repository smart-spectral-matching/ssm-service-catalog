package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BatsDatasetControllerIT {

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
     * Setup base url.
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
     * Returns full database uri given the Collection title.
     *
     * @param collectionTitle Title for the Collection
     * @return            Full URI for the Collection
    */
    private String getCollectionUri(final String collectionTitle) {
        return createUrl("/collections/" + collectionTitle);
    }

    /**
     * Returns uri given the Collection title and Dataset UUID.
     *
     * @param collectionTitle Title for the Collection the dataset belongs to
     * @param datasetUUID   UUID for the Dataset
     * @param format      Format return for dataset
     * @return            Full URI for the Dataset
    */
    private String getDatasetUri(
        final String collectionTitle,
        final String datasetUUID,
        final String format
    ) {
        String uri = getCollectionUri(collectionTitle) + "/datasets/" + datasetUUID;
        uri += "?format=" + format.toUpperCase(Locale.getDefault());
        return uri;
    }

    /**
     * Returns uri to full dataset given the Collection title and Dataset UUID.
     *
     * @param collectionTitle Title for the Collection the dataset belongs to
     * @param datasetUUID   UUID for the Dataset
     * @return            Full URI for the Dataset
    */
    private String getDatasetUriFull(
        final String collectionTitle,
        final String datasetUUID
    ) {
        return getDatasetUri(collectionTitle, datasetUUID, "full");
    }

    /**
     * Returns parital uri w/o base url given the Collection and Dataset UUID.
     *
     * @param collectionTitle Title for the Collection the dataset belongs to
     * @param datasetUUID   UUID for the Dataset
     * @return            Partial URI for the Dataset
    */
    private String getDatasetUriPartial(
        final String collectionTitle,
        final String datasetUUID
    ) {
        return "/collections/" + collectionTitle + "/datasets/" + datasetUUID;
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
     * Returns a JsonNode used for updating the simple JSON-LD dataset.
     *
     * @return JsonNode to use for updating the simple json-ld dataset
     */
    private JsonNode getSimpleUpdateNode() {
        ObjectNode updateNode = MAPPER.createObjectNode();
        updateNode.put("@id", "http://localhost/beatles/member/1");
        updateNode.put("homepage", "https://dbpedia.org/page/Ringo_Starr");
        updateNode.put("name", "Ringo Starr");
        updateNode.put("spouse", "https://dbpedia.org/page/Barbara_Bach");
        updateNode.put("birthDate", "1940-07-07");
        return updateNode;
    }

    /**
    *  Asserts we have matching URIs in @id part of each object in @graph array.
    *
    * @param resultGraph Result JsonNode to check the @graph array for
    * @param collectionTitle The title of the collection for the target dataset
    * @param datasetUUID   The UUID of the dataset for the target dataset
    */
    private void assertEqualIDsInGraphArrayNodes(
        final JsonNode resultGraph,
        final String collectionTitle,
        final String datasetUUID
    ) {
        // Check the URIs match in the @id part of each object in the @graph array
        ArrayNode resultArray = (ArrayNode) resultGraph;
        for (JsonNode jsonNode : resultArray) {
            String nodeID = jsonNode.get("@id").asText();

            // Skip the "metadata" node which contains
            // "created" and "modified"
            if (nodeID.contains(JsonUtils.METADATA_URI)) {
                continue;
            }

            String datasetPath = getDatasetUriPartial(collectionTitle, datasetUUID);
            String msg = "Asserting " + nodeID + " contains " + datasetPath;
            System.out.println(msg);

            Assertions.assertTrue(nodeID.contains(datasetPath));
            System.out.println("  - assertion true!\n");
        }
    }

    /**
     * Constructs an input JSON-LD for creating an example dataset.
     * Comes from "A Simple Example" at https://json-ld.org/
     * The JSON-LD retrieved after uploading is found in the
     * simpleOutputJSONLD() method
     *
     * @return JSOND-LD as string
    */
    private String simpleInputJSONLD() throws IOException {
        return getFileDataFromTestResources("simple.input.jsonld");
    }

    /**
     * Constructs the output JSON-LD we get back from the API from the one.
     * created in the simpleInputJSONLD() method.
     *
     * @return JSOND-LD as string
    */
    private String simpleOutputJSONLD() throws IOException {
        return getFileDataFromTestResources("simple.output.jsonld");
    }

    /**
     * Constructs an input JSON-LD for a SciData dataset.
     * Partial JSON-LD example from nmr.jsonld
     * Retrieved on 1/22/2021 from:
     *     https://github.com/stuchalk/scidata/blob/master/examples/nmr.jsonld
     * The JSON-LD retrieved after uploading is found in the
     * scidataPhOutputJSONLD() method
     *
     * @return JSOND-LD as string
    */
    private String scidataInputJSONLD() throws IOException {
        return getFileDataFromTestResources(
            "scidata_nmr_abbreviated.input.jsonld"
        );
    }

    /**
     * Constructs the output JSON-LD we get back from the API from the one
     * created in the scidataInputJSONLD() method.
     *
     * @param baseUri Base Uri to replace throughout the output JSON-LD
     * @return        JSOND-LD as string
    */
    private String scidataOutputJSONLD(final String baseUri)
    throws IOException {
        String jsonld = getFileDataFromTestResources(
            "scidata_nmr_abbreviated.output.jsonld"
        );
        String oldBaseUri = "https://stuchalk.github.io/scidata/examples/nmr/";
        String newJsonld = jsonld.replaceAll(
            oldBaseUri,
            baseUri
        );
        return newJsonld;
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
     * Helper function to create a collection we can add datasets to.
     *
     * @param title Title for the collection
     * @return Collection title
    */
    private String createCollection(final String title) throws Exception {
        ObjectNode titleObject = MAPPER.createObjectNode();
        titleObject.put("title", title);
        String collectionJson = MAPPER.writeValueAsString(titleObject);

        String jsonString = restTemplate.postForEntity(
            createUrl("/collections"),
            makeBody(MediaType.APPLICATION_JSON, collectionJson),
            String.class).getBody();

        String collectionTitle = MAPPER.readTree(jsonString)
                                    .get("title")
                                    .asText();
        return collectionTitle;
    }

    /**
     * Helper function to create a dataset to a given collection.
     *
     * @param collectionTitle The title of the collection to add the dataset
     * @param jsonld      JSON-LD for the Dataset to create
     * @return            Dataset UUID
    */
    private String createDataset(final String collectionTitle, final String jsonld)
    throws Exception {
        String jsonString = restTemplate.postForEntity(
            getCollectionUri(collectionTitle) + "/datasets",
            makeBody(MediaType.APPLICATION_JSON, jsonld),
            String.class).getBody();
        String datasetUUID  = MAPPER.readTree(jsonString).get("uuid").asText();
        return datasetUUID;
    }

    /**
     * Test to create a Dataset from a simple JSON-LD.
    */
    @Test
    public void testCreateSimpleFullDataset() throws Exception {
        String collectionTitle = createCollection("testCreateSimpleDataset");
        ResponseEntity<String> response = restTemplate.postForEntity(
                getCollectionUri(collectionTitle) + "/datasets",
                makeBody(MediaType.APPLICATION_JSON, simpleInputJSONLD()),
                String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Check the @context sections match
        Assertions.assertEquals(
            MAPPER.readTree(simpleOutputJSONLD()).get("@context"),
            MAPPER.readTree(response.getBody()).get("dataset").get("@context")
        );

        // Check that part of the @graph sections match
        // Certain fields ("created", "modified") cannot match
        String id = "http://localhost/beatles/member/1";
        Assertions.assertEquals(
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(simpleOutputJSONLD()).get("@graph")
            ),
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(response.getBody()).get("dataset").get("@graph")
            )
        );


    }

    /**
     * Test to create a Dataset from a SciData JSON-LD.
    */
    @Test
    public void testCreateSciDataFullDataset() throws Exception {
        String collectionTitle = createCollection("testCreateSciDataDataset");

        ResponseEntity<String> response = restTemplate.postForEntity(
                getCollectionUri(collectionTitle) + "/datasets",
                makeBody(MediaType.APPLICATION_JSON, scidataInputJSONLD()),
                String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        String datasetUUID = MAPPER.readTree(response.getBody())
                                 .get("uuid")
                                 .asText();
        String datasetUri = getDatasetUriFull(collectionTitle, datasetUUID);

        System.out.println("\n\n");
        System.out.println("Collection title: " + collectionTitle);
        System.out.println("Dataset Uuid: " + datasetUUID);
        System.out.println("Dataset Uri: " + datasetUri);
        System.out.println("\n\n");
        JsonNode targetGraph = MAPPER.readTree(scidataOutputJSONLD(datasetUri))
                                .get("@graph");
        JsonNode resultGraph = MAPPER.readTree(response.getBody())
                                .get("dataset")
                                .get("@graph");

        Assertions.assertEquals(targetGraph.size(), resultGraph.size());

        assertEqualIDsInGraphArrayNodes(resultGraph, collectionTitle, datasetUUID);
    }

    /**
     * Test to get a Dataset created from a simple JSON-LD.
    */
    @Test
    public void testGetSimpleFullDataset() throws Exception {
        String collectionTitle = createCollection("testGetSimpleFullDataset");
        String datasetUUID = createDataset(collectionTitle, simpleInputJSONLD());
        String datasetUri = getDatasetUriFull(collectionTitle, datasetUUID);

        ResponseEntity<String> response = restTemplate.getForEntity(
            datasetUri,
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // Check the @context sections match
        Assertions.assertEquals(
            MAPPER.readTree(simpleOutputJSONLD()).get("@context"),
            MAPPER.readTree(response.getBody()).get("dataset").get("@context")
        );

        // Check that part of the @graph sections match
        // Certain fields ("created", "modified") cannot match
        String id = "http://localhost/beatles/member/1";
        Assertions.assertEquals(
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(simpleOutputJSONLD()).get("@graph")
            ),
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(response.getBody()).get("dataset").get("@graph")
            )
        );

    }

    /**
     * Test to get a Dataset created from a SciData JSON-LD.
    */
    @Test
    public void testGetSciDataFullDataset() throws Exception {
        String collectionTitle = createCollection("testGetSciDataFullDataset");
        String datasetUUID = createDataset(collectionTitle, scidataInputJSONLD());
        String datasetUri = getDatasetUriFull(collectionTitle, datasetUUID);

        ResponseEntity<String> response = restTemplate.getForEntity(
            datasetUri,
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode targetGraph = MAPPER.readTree(scidataOutputJSONLD(datasetUri))
                                .get("@graph");
        JsonNode resultGraph = MAPPER.readTree(response.getBody())
                                .get("dataset")
                                .get("@graph");

        Assertions.assertEquals(targetGraph.size(), resultGraph.size());
    }

    /**
     * Test to get correct HTTP status if Dataset not found.
    */
    @Test
    public void testGetFullDatasetNotFound() throws Exception {
        String collectionTitle = createCollection("testGetDatasetNotFound");

        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getDatasetUriFull(collectionTitle, "1"),
                Void.class
            ).getStatusCode()
        );
    }

    /**
     * Test to update via replace for a Dataset using a simple JSON-LD.
    */
    @Test
    public void testUpdateSimpleFullDatasetReplace() throws Exception {
        String collectionTitle = createCollection("testUpdateSimpleDatasetReplace");
        String datasetUUID = createDataset(collectionTitle, simpleInputJSONLD());
        String datasetUri = getDatasetUriFull(collectionTitle, datasetUUID);

        // Create @graph node
        JsonNode updateNode = getSimpleUpdateNode();
        ArrayNode graphNode = MAPPER.createArrayNode();
        graphNode.add(updateNode);

        // Create @context node
        JsonNode contextNode = MAPPER.readTree(simpleOutputJSONLD()).get("@context");

        // Create JSON-LD to replace using @graph and @context nodes
        ObjectNode jsonld = MAPPER.createObjectNode();
        jsonld.set("@graph", graphNode);
        jsonld.set("@context", contextNode);
        String jsonldPayload = MAPPER.writeValueAsString(jsonld);

        // Send the update
        ResponseEntity<String> response = restTemplate.exchange(
            datasetUri,
            HttpMethod.PUT,
            makeBody(MediaType.APPLICATION_JSON, jsonldPayload),
            String.class
        );

        // Check the status code
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // Ensure the update modified the data
        String id = "http://localhost/beatles/member/1";
        Assertions.assertEquals(
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(jsonldPayload).get("@graph")
            ),
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(response.getBody()).get("dataset").get("@graph")
            )
        );
    }

    /**
     * Test to partial update for a Dataset using a simple JSON-LD.
    */
    @Test
    public void testUpdateSimpleFullDatasetPartial() throws Exception {
        String collectionTitle = createCollection("testUpdateSimpleDatasetPartial");
        String datasetUUID = createDataset(collectionTitle, simpleInputJSONLD());
        String datasetUri = getDatasetUriFull(collectionTitle, datasetUUID);

        // Create body for our update to the dataset
        JsonNode newNameNode = getSimpleUpdateNode();

        ArrayNode newGraphArray = MAPPER.createArrayNode();
        newGraphArray.add(newNameNode);

        ObjectNode newGraphNode = MAPPER.createObjectNode();
        newGraphNode.set("@graph", newGraphArray);

        String newName = MAPPER.writeValueAsString(newGraphNode);

        // Send the update
        ResponseEntity<String> response = restTemplate.exchange(
            datasetUri,
            HttpMethod.PATCH,
            makeBody(MediaType.APPLICATION_JSON, newName),
            String.class
        );


        // Check the status code
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // Merge payload with dataset for target we verify against
        JsonNode originalJson = MAPPER.readTree(simpleOutputJSONLD());
        JsonNode newNameJson = MAPPER.readTree(newName);
        JsonNode target = JsonUtils.merge(originalJson, newNameJson);

        // Ensure the update modified the data
        String id = "http://localhost/beatles/member/1";
        Assertions.assertEquals(
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) target.get("@graph")
            ),
            JsonUtils.getIdFromArrayNode(
                id,
                (ArrayNode) MAPPER.readTree(response.getBody()).get("dataset").get("@graph")
            )
        );
    }

    /**
     * Test to delete a Dataset using a simple JSON-LD.
    */
    @Test
    public void testDeleteSimpleFullDataset() throws Exception {
        String collectionTitle = createCollection("testDeleteSimpleDatasetl");
        String datasetUUID = createDataset(collectionTitle, simpleInputJSONLD());

        // Make sure dataset exists
        Assertions.assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                getDatasetUriFull(collectionTitle, datasetUUID),
                Void.class
            ).getStatusCode()
        );

        // Ensure we return correct code for delete
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                getDatasetUriFull(collectionTitle, datasetUUID),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );

        // Make sure dataset does not exists
        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getDatasetUriFull(collectionTitle, datasetUUID),
                Void.class
            ).getStatusCode()
        );
    }
}
