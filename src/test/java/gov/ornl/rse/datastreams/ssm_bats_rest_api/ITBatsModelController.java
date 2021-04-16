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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ITBatsModelController {

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
    private static final String BASE_URI = "http://localhost";

    /**
     * Returns full base url w/ port.
     *
     * @return Base URI:port + servlet root context as string
    */
    private String baseUri() {
        return BASE_URI + ":" + port + servletContext.getContextPath();
    }

    /**
     * Returns full database uri given the Dataset UUID.
     *
     * @param datasetUUID UUID for the Dataset
     * @return            Full URI for the Dataset
    */
    private String getDatasetUri(final String datasetUUID) {
        return baseUri() + "/datasets/" + datasetUUID;
    }

    /**
     * Returns full database uri given the Dataset and Model UUID.
     *
     * @param datasetUUID UUID for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return            Full URI for the Model
    */
    private String getModelUri(
        final String datasetUUID,
        final String modelUUID
    ) {
        return getDatasetUri(datasetUUID) + "/models/" + modelUUID;
    }

    /**
     * Returns parital uri w/o base url given the Dataset and Model UUID.
     *
     * @param datasetUUID UUID for the Dataset the model belongs to
     * @param modelUUID   UUID for the Model
     * @return            Partial URI for the Model
    */
    private String getModelUriPartial(
        final String datasetUUID,
        final String modelUUID
    ) {
        return "/datasets/" + datasetUUID + "/models/" + modelUUID;
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
     * @return Dataset UUID
    */
    private String createDataset() throws Exception {
        String jsonString = restTemplate.postForEntity(
            baseUri() + "/datasets",
            HttpEntity.EMPTY,
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String datasetUUID  = mapper.readTree(jsonString)
                                    .get("uuid")
                                    .asText();
        return datasetUUID;
    }

    /**
     * Helper function to create a model to a given dataset.
     *
     * @param datasetUUID The UUID of the dataset to add the model
     * @param jsonld      JSON-LD for the Model to create
     * @return            Model UUID
    */
    private String createModel(final String datasetUUID, final String jsonld)
    throws Exception {
        String jsonString = restTemplate.postForEntity(
            getDatasetUri(datasetUUID) + "/models",
            makeBody(MediaType.APPLICATION_JSON, jsonld),
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String modelUUID  = mapper.readTree(jsonString).get("uuid").asText();
        return modelUUID;
    }

    /**
     * Test to create a Model from a simple JSON-LD.
    */
    @Test
    public void testCreateSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        ResponseEntity<String> response = restTemplate.postForEntity(
                getDatasetUri(datasetUUID) + "/models",
                makeBody(MediaType.APPLICATION_JSON, simpleInputJSONLD()),
                String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        Assertions.assertEquals(
            mapper.readTree(simpleOutputJSONLD()),
            mapper.readTree(response.getBody()).get("model")
        );
    }

    /**
     * Test to create a Model from a SciData JSON-LD.
    */
    @Test
    public void testCreateSciDataModel() throws Exception {
        String datasetUUID = createDataset();

        ResponseEntity<String> response = restTemplate.postForEntity(
                getDatasetUri(datasetUUID) + "/models",
                makeBody(MediaType.APPLICATION_JSON, scidataInputJSONLD()),
                String.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        String modelUUID = mapper.readTree(response.getBody())
                                 .get("uuid")
                                 .asText();
        String modelUri = getModelUri(datasetUUID, modelUUID);

        System.out.println("\n\n");
        System.out.println("Dataset Uuid: " + datasetUUID);
        System.out.println("Model Uuid: " + modelUUID);
        System.out.println("Model Uri: " + modelUri);
        System.out.println("\n\n");
        JsonNode targetGraph = mapper.readTree(scidataOutputJSONLD(modelUri))
                                .get("@graph");
        JsonNode resultGraph = mapper.readTree(response.getBody())
                                .get("model")
                                .get("@graph");

        Assertions.assertEquals(targetGraph.size(), resultGraph.size());

        ArrayNode resultArray = (ArrayNode) resultGraph;
        for (JsonNode jsonNode : resultArray) {
            String nodeID = jsonNode.get("@id").asText();
            String msg = "Asserting " + nodeID + " contains " + modelUri;
            System.out.println(msg);
            String modelPath = getModelUriPartial(datasetUUID, modelUUID);
            Assertions.assertTrue(nodeID.contains(modelPath));
            System.out.println("  - assertion true!\n");
        }
    }

    /**
     * Test to get a Model created from a simple JSON-LD.
    */
    @Test
    public void testGetSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());
        String modelUri = getModelUri(datasetUUID, modelUUID);

        ResponseEntity<String> response = restTemplate.getForEntity(
            modelUri,
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        ObjectMapper mapper = new ObjectMapper();
        Assertions.assertEquals(
            mapper.readTree(simpleOutputJSONLD()),
            mapper.readTree(response.getBody()).get("model")
        );
    }

    /**
     * Test to get a Model created from a SciData JSON-LD.
    */
    @Test
    public void testGetSciDataModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, scidataInputJSONLD());
        String modelUri = getModelUri(datasetUUID, modelUUID);

        ResponseEntity<String> response = restTemplate.getForEntity(
            modelUri,
            String.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        ObjectMapper mapper = new ObjectMapper();

        JsonNode targetGraph = mapper.readTree(scidataOutputJSONLD(modelUri))
                                .get("@graph");
        JsonNode resultGraph = mapper.readTree(response.getBody())
                                .get("model")
                                .get("@graph");

        Assertions.assertEquals(targetGraph.size(), resultGraph.size());
    }

    /**
     * Test to get correct HTTP status if Model not found.
    */
    @Test
    public void testGetModelNotFound() throws Exception {
        String datasetUUID = createDataset();

        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getModelUri(datasetUUID, "1"),
                Void.class
            ).getStatusCode()
        );
    }

    /**
     * Test to update via replace for a Model using a simple JSON-LD.
    */
    @Test
    public void testUpdateSimpleModelReplace() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());
        String modelUri = getModelUri(datasetUUID, modelUUID);

        // Create body for our update to the model
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newNameNode = mapper.createObjectNode();
        newNameNode.put("name", "Ringo Starr");
        String newName = mapper.writeValueAsString(newNameNode);

        // Merge payload with model for target we verify against
        JsonNode originalJson = mapper.readTree(simpleOutputJSONLD());
        JsonNode newNameJson = mapper.readTree(newName);
        JsonNode jsonldPayload = mapper.readerForUpdating(originalJson)
                                       .readValue(newNameJson);

        // Send the update
        ResponseEntity<String> response = restTemplate.exchange(
            modelUri,
            HttpMethod.PATCH,
            makeBody(MediaType.APPLICATION_JSON, jsonldPayload),
            String.class
        );

        // Check the status code
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        // Ensure the update modified the data
        Assertions.assertEquals(
            jsonldPayload,
            mapper.readTree(response.getBody()).get("model")
        );
    }

    /**
     * Test to partial update for a Model using a simple JSON-LD.
    */
    @Test
    public void testUpdateSimpleModelPartial() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());
        String modelUri = getModelUri(datasetUUID, modelUUID);

        // Create body for our update to the model
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newNameNode = mapper.createObjectNode();
        newNameNode.put("name", "Ringo Starr");
        String newName = mapper.writeValueAsString(newNameNode);

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
        JsonNode originalJson = mapper.readTree(simpleOutputJSONLD());
        JsonNode newNameJson = mapper.readTree(newName);
        JsonNode target = mapper.readerForUpdating(originalJson)
                                .readValue(newNameJson);

        // Ensure the update modified the data
        Assertions.assertEquals(
            target,
            mapper.readTree(response.getBody()).get("model")
        );
    }

    /**
     * Test to delete a Model using a simple JSON-LD.
    */
    @Test
    public void testDeleteSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());

        // Make sure model exists
        Assertions.assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                getModelUri(datasetUUID, modelUUID),
                Void.class
            ).getStatusCode()
        );

        // Ensure we return correct code for delete
        Assertions.assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                getModelUri(datasetUUID, modelUUID),
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );

        // Make sure model does not exists
        Assertions.assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                getModelUri(datasetUUID, modelUUID),
                Void.class
            ).getStatusCode()
        );
    }
}
