package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    /*
     * Constructs an input JSON-LD for creating an example model
     * Comes from "A Simple Example" at https://json-ld.org/
     * The JSON-LD retrieved after uploading is found in the
     * exampleOutputJSONLD() method
     *
     * @return JSOND-LD as string
     */
    private String exampleInputJSONLD() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode jsonld = mapper.createObjectNode();
        jsonld.put("@context", "https://json-ld.org/contexts/person.jsonld");
        jsonld.put("@id", "http://dbpedia.org/resource/John_Lennon");
        jsonld.put("name", "John Lennon");
        jsonld.put("born", "1940-10-09");
        jsonld.put("spouse", "http://dbpedia.org/resource/Cynthia_Lennon");
        return mapper.writeValueAsString(jsonld);
    }

    /*
     * Constructs the output JSON-LD we get back from the API from the one
     * created in the exampleInputJSONLD() method.
     *
     * @return JSOND-LD as string
     */
    private String exampleOutputJSONLD() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create output JSON-LD
        ObjectNode jsonld = mapper.createObjectNode();
        jsonld.put("@id", "http://dbpedia.org/resource/John_Lennon");
        jsonld.put("birthDate", "1940-10-09");
        jsonld.put("spouse", "http://dbpedia.org/resource/Cynthia_Lennon");
        jsonld.put("name", "John Lennon");

        // Create @context entry to JSON-LD
        ObjectNode context = mapper.createObjectNode();
        context.put("xsd", "http://www.w3.org/2001/XMLSchema#");

        ObjectNode contextBirthDate = mapper.createObjectNode();
        contextBirthDate.put("@id", "http://schema.org/birthDate");
        contextBirthDate.put("@type", "http://www.w3.org/2001/XMLSchema#date");
        context.set("birthDate", contextBirthDate);

        ObjectNode contextSpouse = mapper.createObjectNode();
        contextSpouse.put("@id", "http://schema.org/spouse");
        contextSpouse.put("@type", "@id");
        context.set("spouse", contextSpouse);

        ObjectNode contextName = mapper.createObjectNode();
        contextName.put("@id", "http://xmlns.com/foaf/0.1/name");
        context.set("name", contextName);

        jsonld.set("@context", context);

        return mapper.writeValueAsString(jsonld);
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
    private String createModel(String datasetUUID) throws Exception {
        String modelUUID= restTemplate.postForEntity(
            baseUrl() + "/datasets/" + datasetUUID + "/models",
            makeBody(MediaType.APPLICATION_JSON, exampleInputJSONLD()),
            String.class).getBody();
        return modelUUID;
    }

    // Tests

    @Test
    public void testCreateModel() throws Exception {
        String datasetUUID = createDataset();
        assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models",
                makeBody(MediaType.APPLICATION_JSON, exampleInputJSONLD()),
                String.class
            ).getStatusCode()
        );
    }

    @Test
    public void testGetModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID);

        assertEquals(
            HttpStatus.OK,
            restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                String.class
            ).getStatusCode()
        );

        String jsonld = restTemplate.getForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models/" + modelUUID,
                String.class
            ).getBody();

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(
            mapper.readTree(exampleOutputJSONLD()),
            mapper.readTree(jsonld)
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
    public void testUpdateModelReplace() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID);

        // Create body for our update to the model
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode newNameNode = mapper.createObjectNode();
        newNameNode.put("name", "Ringo Starr");
        String newName = mapper.writeValueAsString(newNameNode);

        // Merge payload with model for target we verify against
        JsonNode originalJson = mapper.readTree(exampleOutputJSONLD());
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
        assertEquals(jsonldPayload, mapper.readTree(response.getBody()));
    }

    @Test
    public void testUpdateModelPartial() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID);

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
        JsonNode originalJson = mapper.readTree(exampleOutputJSONLD());
        JsonNode newNameJson = mapper.readTree(newName);
        JsonNode target = mapper.readerForUpdating(originalJson).readValue(newNameJson);

        // Ensure the update modified the data
        assertEquals(target, mapper.readTree(response.getBody()));
    }

    @Test
    public void testDeleteModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createModel(datasetUUID);

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
