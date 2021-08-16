package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
public class ITBatsModelController {

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
     * Returns full database uri given the Dataset title.
     *
     * @param datasetTitle Title for the Dataset
     * @return            Full URI for the Dataset
    */
    private String getDatasetUri(final String datasetTitle) {
        return createUrl("/datasets/" + datasetTitle);
    }

    /**
     * Returns uri given the Dataset title and Model UUID.
     *
     * @param datasetTitle Title for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @param full        Boolean flag if we want a full model
     * @return            Full URI for the Model
    */
    private String getModelUri(
        final String datasetTitle,
        final String modelUUID,
        final boolean full
    ) {
        String uri = getDatasetUri(datasetTitle) + "/models/" + modelUUID;
        if (full) {
            uri += "?full=true";
        }
        return uri;
    }

    /**
     * Returns uri to full model given the Dataset title and Model UUID.
     *
     * @param datasetTitle Title for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return            Full URI for the Model
    */
    private String getModelUriFull(
        final String datasetTitle,
        final String modelUUID
    ) {
        return getModelUri(datasetTitle, modelUUID, true);
    }

    /**
     * Returns uri to abbrviated model given the Dataset title and Model UUID.
     *
     * @param datasetTitle Title for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return            Full URI for the Model
    */
    private String getModelUriAbbreviated(
        final String datasetTitle,
        final String modelUUID
    ) {
        return getModelUri(datasetTitle, modelUUID, false);
    }

    /**
     * Returns parital uri w/o base url given the Dataset and Model UUID.
     *
     * @param datasetTitle Title for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return            Partial URI for the Model
    */
    private String getModelUriPartial(
        final String datasetTitle,
        final String modelUUID
    ) {
        return "/datasets/" + datasetTitle + "/models/" + modelUUID;
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
     * Returns a JsonNode used for updating the simple JSON-LD model.
     *
     * @return JsonNode to use for updating the simple json-ld model
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
    * @param datasetTitle The title of the dataset for the target model
    * @param modelUUID   The UUID of the model for the target model
    */
    private void assertEqualIDsInGraphArrayNodes(
        final JsonNode resultGraph,
        final String datasetTitle,
        final String modelUUID
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

            String modelPath = getModelUriPartial(datasetTitle, modelUUID);
            String msg = "Asserting " + nodeID + " contains " + modelPath;
            System.out.println(msg);

            Assertions.assertTrue(nodeID.contains(modelPath));
            System.out.println("  - assertion true!\n");
        }
    }

    /**
     * Constructs an input JSON-LD for creating an example model.
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
     * Constructs an input JSON-LD for a SciData model.
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
     * Helper function to create a dataset we can add models to.
     *
     * @param title Title for the dataset
     * @return Dataset title
    */
    private String createDataset(final String title) throws Exception {
        ObjectNode titleObject = MAPPER.createObjectNode();
        titleObject.put("title", title);
        String datasetJson = MAPPER.writeValueAsString(titleObject);

        String jsonString = restTemplate.postForEntity(
            createUrl("/datasets"),
            makeBody(MediaType.APPLICATION_JSON, datasetJson),
            String.class).getBody();

        String datasetTitle = MAPPER.readTree(jsonString)
                                    .get("title")
                                    .asText();
        return datasetTitle;
    }

    /**
     * Helper function to create a model to a given dataset.
     *
     * @param datasetTitle The title of the dataset to add the model
     * @param jsonld      JSON-LD for the Model to create
     * @return            Model UUID
    */
    private String createModel(final String datasetTitle, final String jsonld)
    throws Exception {
        String jsonString = restTemplate.postForEntity(
            getDatasetUri(datasetTitle) + "/models",
            makeBody(MediaType.APPLICATION_JSON, jsonld),
            String.class).getBody();
        String modelUUID  = MAPPER.readTree(jsonString).get("uuid").asText();
        return modelUUID;
    }

    /**
     * Test to create a Model from a simple JSON-LD.
    */
    @Test
    public void testCreateSimpleFullModel() throws Exception {
        String datasetTitle = createDataset("testCreateSimpleModel");
        ResponseEntity<String> response = restTemplate.postForEntity(
                getDatasetUri(datasetTitle) + "/models",
                makeBody(MediaType.APPLICATION_JSON, simpleInputJSONLD()),
                String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Check the @context sections match
        Assertions.assertEquals(
            MAPPER.readTree(simpleOutputJSONLD()).get("@context"),
            MAPPER.readTree(response.getBody()).get("model").get("@context")
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
                (ArrayNode) MAPPER.readTree(response.getBody()).get("model").get("@graph")
            )
        );


    }

    /**
     * Test to create a Model from a SciData JSON-LD.
    */
    @Test
    public void testCreateSciDataFullModel() throws Exception {
        String datasetTitle = createDataset("testCreateSciDataModel");

        ResponseEntity<String> response = restTemplate.postForEntity(
                getDatasetUri(datasetTitle) + "/models",
                makeBody(MediaType.APPLICATION_JSON, scidataInputJSONLD()),
                String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        String modelUUID = MAPPER.readTree(response.getBody())
                                 .get("uuid")
                                 .asText();
        String modelUri = getModelUriFull(datasetTitle, modelUUID);

        System.out.println("\n\n");
        System.out.println("Dataset title: " + datasetTitle);
        System.out.println("Model Uuid: " + modelUUID);
        System.out.println("Model Uri: " + modelUri);
        System.out.println("\n\n");
        JsonNode targetGraph = MAPPER.readTree(scidataOutputJSONLD(modelUri))
                                .get("@graph");
        JsonNode resultGraph = MAPPER.readTree(response.getBody())
                                .get("model")
                                .get("@graph");

        Assertions.assertEquals(targetGraph.size(), resultGraph.size());

        assertEqualIDsInGraphArrayNodes(resultGraph, datasetTitle, modelUUID);
    }

    /**
     * Test to get a Model created from a simple JSON-LD.
    */
    @Test
    public void testGetSimpleFullModel() throws Exception {
        String datasetTitle = createDataset("testGetSimpleModel");
        String modelUUID = createModel(datasetTitle, simpleInputJSONLD());
        String modelUri = getModelUriFull(datasetTitle, modelUUID);

        ResponseEntity<String> response = restTemplate.getForEntity(
            modelUri,
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // Check the @context sections match
        Assertions.assertEquals(
            MAPPER.readTree(simpleOutputJSONLD()).get("@context"),
            MAPPER.readTree(response.getBody()).get("model").get("@context")
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
                (ArrayNode) MAPPER.readTree(response.getBody()).get("model").get("@graph")
            )
        );

    }

    /**
     * Test to get a Model created from a SciData JSON-LD.
    */
    @Test
    public void testGetSciDataFullModel() throws Exception {
        String datasetTitle = createDataset("testGetSciDataModel");
        String modelUUID = createModel(datasetTitle, scidataInputJSONLD());
        String modelUri = getModelUriFull(datasetTitle, modelUUID);

        ResponseEntity<String> response = restTemplate.getForEntity(
            modelUri,
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode targetGraph = MAPPER.readTree(scidataOutputJSONLD(modelUri))
                                .get("@graph");
        JsonNode resultGraph = MAPPER.readTree(response.getBody())
                                .get("model")
                                .get("@graph");

        Assertions.assertEquals(targetGraph.size(), resultGraph.size());
    }

    /**
     * Test to get correct HTTP status if Model not found.
    */
    @Test
    public void testGetFullModelNotFound() throws Exception {
        String datasetTitle = createDataset("testGetModelNotFound");

        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getModelUriFull(datasetTitle, "1"),
                Void.class
            ).getStatusCode()
        );
    }

    /**
     * Test to update via replace for a Model using a simple JSON-LD.
    */
    @Test
    public void testUpdateSimpleFullModelReplace() throws Exception {
        String datasetTitle = createDataset("testUpdateSimpleModelReplace");
        String modelUUID = createModel(datasetTitle, simpleInputJSONLD());
        String modelUri = getModelUriFull(datasetTitle, modelUUID);

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
            modelUri,
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
                (ArrayNode) MAPPER.readTree(response.getBody()).get("model").get("@graph")
            )
        );
    }

    /**
     * Test to partial update for a Model using a simple JSON-LD.
    */
    @Test
    public void testUpdateSimpleFullModelPartial() throws Exception {
        String datasetTitle = createDataset("testUpdateSimpleModelPartial");
        String modelUUID = createModel(datasetTitle, simpleInputJSONLD());
        String modelUri = getModelUriFull(datasetTitle, modelUUID);

        // Create body for our update to the model
        JsonNode newNameNode = getSimpleUpdateNode();

        ArrayNode newGraphArray = MAPPER.createArrayNode();
        newGraphArray.add(newNameNode);

        ObjectNode newGraphNode = MAPPER.createObjectNode();
        newGraphNode.set("@graph", newGraphArray);

        String newName = MAPPER.writeValueAsString(newGraphNode);

        // Send the update
        ResponseEntity<String> response = restTemplate.exchange(
            modelUri,
            HttpMethod.PATCH,
            makeBody(MediaType.APPLICATION_JSON, newName),
            String.class
        );


        // Check the status code
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // Merge payload with model for target we verify against
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
                (ArrayNode) MAPPER.readTree(response.getBody()).get("model").get("@graph")
            )
        );
    }

    /**
     * Test to delete a Model using a simple JSON-LD.
    */
    @Test
    public void testDeleteSimpleFullModel() throws Exception {
        String datasetTitle = createDataset("testDeleteSimpleModel");
        String modelUUID = createModel(datasetTitle, simpleInputJSONLD());

        // Make sure model exists
        Assertions.assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                getModelUriFull(datasetTitle, modelUUID),
                Void.class
            ).getStatusCode()
        );

        // Ensure we return correct code for delete
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                getModelUriFull(datasetTitle, modelUUID),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );

        // Make sure model does not exists
        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getModelUriFull(datasetTitle, modelUUID),
                Void.class
            ).getStatusCode()
        );
    }
}
