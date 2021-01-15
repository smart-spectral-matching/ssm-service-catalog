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

    /*
     * Constructs an input JSON-LD for creating an example model
     * Comes from "A Simple Example" at https://json-ld.org/
     * The JSON-LD retrieved after uploading is found in the
     * simpleOutputJSONLD() method
     *
     * @return JSOND-LD as string
     */
    private String simpleInputJSONLD() throws JsonProcessingException {
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
     * created in the simpleInputJSONLD() method.
     *
     * @return JSOND-LD as string
     */
    private String simpleOutputJSONLD() throws JsonProcessingException {
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
     * Constructs an input JSON-LD for a model with a complex '@context'
     * Comes from "Environment Linked Features" JSON-LD
     * of the OpenGeoSpatial ELFIE project
     * Retrieved on 1/15/2021 from:
     *     https://opengeospatial.github.io/ELFIE/json-ld/elf.jsonld
     * The JSON-LD retrieved after uploading is found in the
     * complexContextOutputJSONLD() method
     *
     * @return JSOND-LD as string
     */
    private String complexContextInputJSONLD() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode jsonld = mapper.createObjectNode();
        ObjectNode context = mapper.createObjectNode();
        context.put("schema", "http://schema.org/");
        context.put("skos", "https://www.w3.org/TR/skos-reference/");
        context.put("gsp", "http://www.opengis.net/ont/geosparql#");
        context.put("description", "schema:description");
        context.put("geo", "schema:geo");
        context.put("hasGeometry", "gsp:hasGeometry");
        context.put("asWKT", "gsp:asWKT");
        context.put("name", "schema:name");
        context.put("sameAs", "schema:sameAs");
        context.put("related", "skos:related");

        ObjectNode contextImage = mapper.createObjectNode();
        contextImage.put("@id", "schema:image");
        contextImage.put("@type", "@id");
        context.set("image", contextImage);

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
    private String createSimpleModel(String datasetUUID) throws Exception {
        String jsonString = restTemplate.postForEntity(
            baseUrl() + "/datasets/" + datasetUUID + "/models",
            makeBody(MediaType.APPLICATION_JSON, simpleInputJSONLD()),
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
    public void testCreateComplexContextModel() throws Exception {
        String datasetUUID = createDataset();
        assertEquals(
            HttpStatus.CREATED,
            restTemplate.postForEntity(
                baseUrl() + "/datasets/" + datasetUUID + "/models",
                makeBody(MediaType.APPLICATION_JSON, complexContextInputJSONLD()),
                String.class
            ).getStatusCode()
        );
    }

    @Test
    public void testGetSimpleModel() throws Exception {
        String datasetUUID = createDataset();
        String modelUUID = createSimpleModel(datasetUUID);

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
        String modelUUID = createSimpleModel(datasetUUID);

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
        String modelUUID = createSimpleModel(datasetUUID);

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
        String modelUUID = createSimpleModel(datasetUUID);

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
