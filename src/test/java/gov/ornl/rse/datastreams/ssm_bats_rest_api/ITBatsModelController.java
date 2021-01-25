package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.github.jsonldjava.utils.JsonUtils;

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
import org.springframework.test.context.junit4.SpringRunner;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ITBatsModelController {

	@Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
	private int port;

    private static final String BASE_URL = "http://localhost";

    private String baseUrl() {
        return BASE_URL + ":" + port;
    }

    private String getFileDataFromTestResources(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get("src", "test", "resources", filename)));
    }

    /*
     * Constructs an input JSON-LD for creating an example model
     * Comes from "A Simple Example" at https://json-ld.org/
     * The JSON-LD retrieved after uploading is found in the
     * simpleOutputJSONLD() method
     *
     * @return JSOND-LD as string
     */
    private String simpleInputJSONLD() throws IOException {
        return getFileDataFromTestResources("simple.input.jsonld");
    }

    /*
     * Constructs the output JSON-LD we get back from the API from the one
     * created in the simpleInputJSONLD() method.
     *
     * @return JSOND-LD as string
     */
    private String simpleOutputJSONLD() throws IOException {
        return getFileDataFromTestResources("simple.output.jsonld");
    }

    /*
     * Constructs an input JSON-LD for a SciData model
     * Partial JSON-LD example from nmr.jsonld
     * Retrieved on 1/22/2021 from:
     *     https://github.com/stuchalk/scidata/blob/master/examples/nmr.jsonld
     * The JSON-LD retrieved after uploading is found in the
     * scidataPhOutputJSONLD() method
     *
     * @return JSOND-LD as string
     */
    private String scidataInputJSONLD() throws IOException {
        return getFileDataFromTestResources("scidata_nmr_abbreviated.input.jsonld");
    }

    /*
     * Constructs the output JSON-LD we get back from the API from the one
     * created in the scidataInputJSONLD() method.
     *
     * @return JSOND-LD as string
     */
    private String scidataOutputJSONLD() throws IOException {
        return getFileDataFromTestResources("scidata_nmr_abbreviated.output.jsonld");
    }

    /*
     * Helper function to create HTTP body
     *
     * @param mediaType Content Type for the body data
     * @param body      Data to be posted
     * @return properly formatted body for post statement (with HTTP headers)
     */
    private HttpEntity<Object> makeBody(MediaType mediaType, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        return new HttpEntity<>(body, headers);
    }

    /*
     * Helper function to create a dataset we can add models to
     *
     * @return Dataset UUID
     */
    private String createDataset() throws Exception {
        String jsonString = restTemplate.postForEntity(
            baseUrl() + "/datasets",
            HttpEntity.EMPTY,
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String datasetUUID  = mapper.readTree(jsonString).get("uuid").textValue();
        return datasetUUID;
    }

    /*
     * Helper function to create a model to a given dataset
     *
     * @param  datasetUUID The UUID of the dataset to add the model
     * @return Model UUID
     */
    private String createModel(String datasetUUID, String jsonld) throws Exception {
        String jsonString = restTemplate.postForEntity(
            baseUrl() + "/datasets/" + datasetUUID + "/models",
            makeBody(MediaType.APPLICATION_JSON, jsonld),
            String.class).getBody();
        ObjectMapper mapper = new ObjectMapper();
        String modelUUID  = mapper.readTree(jsonString).get("uuid").textValue();
        return modelUUID;
    }

    // Tests

    @Test
    public void testCreateSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models",
                makeBody(MediaType.APPLICATION_JSON, simpleInputJSONLD()),
                String.class
            ).getStatusCode()
        );
    }

    @Test
    public void testCreateSciDataModel() throws Exception {
        String datasetUUID = createDataset();
        assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models",
                makeBody(MediaType.APPLICATION_JSON, scidataInputJSONLD()),
                String.class
            ).getStatusCode()
        );
    }

    @Test
    public void testGetSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());

        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                String.class
            ).getStatusCode()
        );

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                String.class
            );

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(
            mapper.readTree(simpleOutputJSONLD()),
            mapper.readTree(response.getBody()).get("model")
        );
    }

    @Test
    public void testGetSciDataModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, scidataInputJSONLD());

        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                String.class
            ).getStatusCode()
        );

        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                String.class
            );

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(
            mapper.readTree(scidataOutputJSONLD()),
            mapper.readTree(response.getBody()).get("model")
        );
    }


    @Test
    public void testGetModelNotFound() throws Exception {
        String datasetUUID = createDataset();

        assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/1",
                Void.class
            ).getStatusCode()
        );
    }

    @Test
    public void testUpdateSimpleModelReplace() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());

        // Create body for our update to the model
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newNameNode = mapper.createObjectNode();
        newNameNode.put("name", "Ringo Starr");
        String newName = mapper.writeValueAsString(newNameNode);

        // Merge payload with model for target we verify against
        JsonNode originalJson = mapper.readTree(simpleOutputJSONLD());
        JsonNode newNameJson = mapper.readTree(newName);
        JsonNode jsonldPayload = mapper.readerForUpdating(originalJson).readValue(newNameJson);

        // Send the update
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                HttpMethod.PATCH,
                makeBody(MediaType.APPLICATION_JSON, jsonldPayload),
                String.class);

        // Check the status code
        assertEquals(response.getStatusCode(), HttpStatus.OK);

        // Ensure the update modified the data
        assertEquals(jsonldPayload, mapper.readTree(response.getBody()).get("model"));
    }

    @Test
    public void testUpdateSimpleModelPartial() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());

        // Create body for our update to the model
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newNameNode = mapper.createObjectNode();
        newNameNode.put("name", "Ringo Starr");
        String newName = mapper.writeValueAsString(newNameNode);

        // Send the update
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                HttpMethod.PATCH,
                makeBody(MediaType.APPLICATION_JSON, newName),
                String.class);

        // Check the status code
        assertEquals(response.getStatusCode(), HttpStatus.OK);

        // Merge payload with model for target we verify against
        JsonNode originalJson = mapper.readTree(simpleOutputJSONLD());
        JsonNode newNameJson = mapper.readTree(newName);
        JsonNode target = mapper.readerForUpdating(originalJson).readValue(newNameJson);

        // Ensure the update modified the data
        assertEquals(target, mapper.readTree(response.getBody()).get("model"));
    }

    @Test
    public void testDeleteSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID, simpleInputJSONLD());

        // Make sure model exists
        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                Void.class
            ).getStatusCode()
        );

        // Ensure we return correct code for delete
        assertEquals(
            HttpStatus.NO_CONTENT,
            restTemplate.exchange(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
            ).getStatusCode()
        );

        // Make sure model does not exists
        assertEquals(
            HttpStatus.NOT_FOUND,
            restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                Void.class
            ).getStatusCode()
        );
    }
}
