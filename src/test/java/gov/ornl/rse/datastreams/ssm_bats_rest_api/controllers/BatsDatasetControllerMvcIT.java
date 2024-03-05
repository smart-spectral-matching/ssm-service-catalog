package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import javax.servlet.ServletContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.JsonUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
@AutoConfigureMockMvc
public class BatsDatasetControllerMvcIT {

    /**
     * Object mapper.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Factory for creating new JSON objects.
     */
    private static final JsonNodeFactory FACTORY = new JsonNodeFactory(false);

    /**
     * The mock agent used to make requests to endpoints.
     */
    private MockMvc mockMvc;

    /**
     * Web context.
     */
    @Autowired
    private WebApplicationContext webContext;

    /**
     * Server context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * This class is used to store information from the POST request
     * (which can remain agnostic about specific data fields)
     * so they can be used in any PUT/PATCH functions
     * (which cannot remain agnostic about specific data fields)
     *
     * All fields are immutable.
     */
    private static final class TestData {
        /**
         * Made-up "far past" timestamp as String, for JSON.
         */
        private final String dummyTimeStr;
        /**
         * Made-up "far past" timestamp, for comparison.
         */
        private final LocalDateTime dummyTime;
        /**
         * Created timestamp from initial POST request.
         */
        private final LocalDateTime createdTime;
        /**
         * URL to the created Dataset. Includes the UUID.
         */
        private final String datasetUri;

        /**
         *
         * @param dummyTimeStr Made-up "far past" timestamp as String, for JSON.
         * @param dummyTime Made-up "far past" timestamp, for comparison.
         * @param createdTime Created timestamp from initial POST request.
         * @param datasetUri URL to the created Dataset. Includes the UUID.
         */
        private TestData(final String dummyTimeStr, final LocalDateTime dummyTime,
            final LocalDateTime createdTime, final String datasetUri) {
                this.dummyTimeStr = dummyTimeStr;
                this.dummyTime = dummyTime;
                this.createdTime = createdTime;
                this.datasetUri = datasetUri;
        }
    }

    /**
     * Non-static method run before each @Test. Define the MockMVC agent.
     */
    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
    }

    /**
     *
     * @return base Collection URI for POSTing (with trailing slash)
     */
    private String getCollectionUri() {
        return servletContext.getContextPath() + "/collections/";
    }

    /**
     *
     * @param collectionTitle collection title part of the URL
     * @return base Moder URI for POSTing (with trailing slash)
     */
    private String getDatasetUri(final String collectionTitle) {
        return getCollectionUri() + collectionTitle + "/datasets/";
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
     *
     * @param json Json node (directly from response)
     * @return the JSON node with the metadata. This will always have an @id
     *     with our metadata URI.
     * @throws Exception
     */
    private JsonNode getMetadataNode(final JsonNode json) throws Exception {
        Iterator<JsonNode> graphIter = json
            .get("dataset")
            .get("@graph")
            .elements();
        Iterable<JsonNode> iterable = () -> graphIter;
        return StreamSupport.stream(iterable.spliterator(), false)
            .filter(node -> node.get("@id").textValue().equals(JsonUtils.METADATA_URI))
            .findFirst()
            .get();
    }

    /**
     *
     * The base function for creating a dataset for all of the timestamp tests.
     *
     * This function can afford to be agnostic about which sample data file you read in.
     *
     * @param userIncludesTimestamps true to test timestamp submission,
     *      false to test "normal" submission
     * @param collectionTitle Title of the collection to create
     * @param jsonld name of jsonld file in src/test/resources
     * @return relevant test data for the update method, as a TestData object
     * @throws Exception
     */
    private TestData timestampTestBasePost(
        final boolean userIncludesTimestamps,
        final String collectionTitle,
        final String jsonld
    ) throws Exception {
        // String representation of timestamp - use any value in the far past here
        final String dummyTimestampStr = "1945-07-09 11:02:00";
        // timestamp
        final LocalDateTime dummyTimestamp = LocalDateTime.parse(
            dummyTimestampStr.replace(' ', 'T'));

        // create collection and make dataset URI
        String collectionJson = getCollectionData(collectionTitle);
        String response = mockMvc.perform(post(getCollectionUri())
            .contentType(MediaType.APPLICATION_JSON)
            .content(collectionJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        final String title = MAPPER.readTree(response).get("title").asText();
        final String datasetUri = getDatasetUri(title);

        // create dataset with POST
        String sampleJson = getFileDataFromTestResources(jsonld);
        if (userIncludesTimestamps) {
            sampleJson = MAPPER.readValue(sampleJson, ObjectNode.class)
                .put("created", dummyTimestampStr)
                .put("modified", dummyTimestampStr)
                .toString();
        }
        response = mockMvc.perform(post(datasetUri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sampleJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(response);
        final String datasetUpdateUri = datasetUri + json.get("uuid").asText();

        // get last element of @graph node, this is the metadata node we want
        json = getMetadataNode(json);
        // get the created timestamp from the response
        final LocalDateTime createdTime = LocalDateTime.parse(
            json
            .get("created")
            .asText()
            .replace(' ', 'T')
        );
        // get the updated timestamp from the response
        LocalDateTime modifiedTime = LocalDateTime.parse(
            json
            .get("modified")
            .asText()
            .replace(' ', 'T')
        );
        // createdTime and updatedTime should always be the same after a POST
        assertEquals(createdTime, modifiedTime);
        // today should be after the "far past" time we defined
        assertTrue(createdTime.isAfter(dummyTimestamp));

        return new TestData(dummyTimestampStr, dummyTimestamp, createdTime, datasetUpdateUri);
    }

    /**
     *
     * Main method for testing updates to the Scidata-formatted file.
     *
     * If you want to test a file with a different format, you'll need a new method.
     *
     * @param userIncludesTimestamps true to test timestamp submission,
     *      false to test "normal" submission
     * @param jsonld name of jsonld file in src/test/resources
     * @param data TestData object containing relevant data from generic POST request
     * @throws Exception
     */
    private void timestampTestScidataUpdate(final boolean userIncludesTimestamps,
        final String jsonld, final TestData data) throws Exception {
        /////////// update dataset with PUT ////////////////
        final String updateKey = "publisher";
        String updateValue = "Norton I, Emperor of the United States";

        String sampleJson = getFileDataFromTestResources(jsonld);
        ObjectNode node = MAPPER.readValue(sampleJson, ObjectNode.class);
        ((ObjectNode) node.get("@graph")).put(updateKey, updateValue);
        if (userIncludesTimestamps) {
            node.put("created", data.dummyTimeStr)
                .put("modified", data.dummyTimeStr);
        }
        sampleJson = node.toString();

        // wait a second before making the request
        Thread.sleep(1000);

        String response = mockMvc.perform(put(data.datasetUri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sampleJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(response);

        // get last element of @graph node, this is the metadata node we want
        json = getMetadataNode(json);
        // get the created timestamp from the response
        LocalDateTime createdTime = LocalDateTime.parse(
            json
            .get("created")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: created time is exactly the same from the POST request
        assertTrue(createdTime.equals(data.createdTime));
        assertTrue(createdTime.isAfter(data.dummyTime));
        // get the updated timestamp from the response
        LocalDateTime modifiedTime = LocalDateTime.parse(
            json
            .get("modified")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: modified time is after the POST request timestamp
        assertTrue(modifiedTime.isAfter(data.createdTime));

        ///////////// update dataset with PATCH ////////////////
        updateValue = "Screaming Lord Sutch";

        sampleJson = getFileDataFromTestResources(jsonld);
        node = MAPPER.createObjectNode();
        node.set("@graph", FACTORY.objectNode());
        ((ObjectNode) node.get("@graph")).put(updateKey, updateValue);
        if (userIncludesTimestamps) {
            node.put("created", data.dummyTimeStr)
                .put("modified", data.dummyTimeStr);
        }
        sampleJson = node.toString();

        // wait a second before making the request
        Thread.sleep(1000);

        response = mockMvc.perform(patch(data.datasetUri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sampleJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        json = MAPPER.readTree(response);

        // get last element of @graph node, this is the metadata node we want
        json = getMetadataNode(json);
        // get the created timestamp from the response
        createdTime = LocalDateTime.parse(
            json
            .get("created")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: the created timestamp still has not changed
        assertTrue(createdTime.equals(data.createdTime));
        assertTrue(createdTime.isAfter(data.dummyTime));
        // get the updated timestamp from the response
        LocalDateTime modifiedTime2 = LocalDateTime.parse(
            json
            .get("modified")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: the newest modified timestamp is after the last modified timestamp
        assertTrue(modifiedTime2.isAfter(modifiedTime));
    }

    /**
     *
     * Main method for testing updates to the simple file.
     *
     * If you want to test a file with a different format, you'll need a new method.
     *
     * @param userIncludesTimestamps true to test timestamp submission,
     *      false to test "normal" submission
     * @param jsonld name of jsonld file in src/test/resources
     * @param data TestData object containing relevant data from generic POST request
     * @throws Exception
     */
    private void timestampTestSimpleUpdate(final boolean userIncludesTimestamps,
        final String jsonld, final TestData data) throws Exception {
        /////////// update dataset with PUT ////////////////
        final String updateKey = "name";
        String updateValue = "Norton I, Emperor of the United States";

        String sampleJson = getFileDataFromTestResources(jsonld);
        ObjectNode node = MAPPER.readValue(sampleJson, ObjectNode.class);
        // the file does not have an @graph, so we don't have to add it
        node.put(updateKey, updateValue);
        if (userIncludesTimestamps) {
            node.put("created", data.dummyTimeStr)
                .put("modified", data.dummyTimeStr);
        }
        sampleJson = node.toString();

        // wait a second before making the request
        Thread.sleep(1000);

        String response = mockMvc.perform(put(data.datasetUri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sampleJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = MAPPER.readTree(response);

        // get last element of @graph node, this is the metadata node we want
        json = getMetadataNode(json);
        // get the created timestamp from the response
        LocalDateTime createdTime = LocalDateTime.parse(
            json
            .get("created")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: created time is exactly the same from the POST request
        assertTrue(createdTime.equals(data.createdTime));
        assertTrue(createdTime.isAfter(data.dummyTime));
        // get the updated timestamp from the response
        LocalDateTime modifiedTime = LocalDateTime.parse(
            json
            .get("modified")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: modified time is after the POST request timestamp
        assertTrue(modifiedTime.isAfter(data.createdTime));

        ///////////// update dataset with PATCH ////////////////
        updateValue = "Screaming Lord Sutch";

        sampleJson = getFileDataFromTestResources(jsonld);
        node = MAPPER.createObjectNode();
        node.put(updateKey, updateValue);
        if (userIncludesTimestamps) {
            node.put("created", data.dummyTimeStr)
                .put("modified", data.dummyTimeStr);
        }
        sampleJson = node.toString();

        // wait a second before making the request
        Thread.sleep(1000);

        response = mockMvc.perform(patch(data.datasetUri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(sampleJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        json = MAPPER.readTree(response);

        // get last element of @graph node, this is the metadata node we want
        json = getMetadataNode(json);
        // get the created timestamp from the response
        createdTime = LocalDateTime.parse(
            json
            .get("created")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: the created timestamp still has not changed
        assertTrue(createdTime.equals(data.createdTime));
        assertTrue(createdTime.isAfter(data.dummyTime));
        // get the updated timestamp from the response
        LocalDateTime modifiedTime2 = LocalDateTime.parse(
            json
            .get("modified")
            .asText()
            .replace(' ', 'T')
        );
        // ASSERT: the newest modified timestamp is after the last modified timestamp
        assertTrue(modifiedTime2.isAfter(modifiedTime));
    }

    /**
     *
     * Full suite test:
     *
     * 1. Test that if the user tries to POST JSON with "created" and "modified" properties,
     *    they are ignored in favor of the API's handling.
     * 2. Test that "created" IS NOT updated, but "modified" IS updated
     *    when making a PUT or PATCH request. If the user includes these properties
     *    in their request, the user's values should be ignored.
     * 3. Uses the SciData JSON-LD input file
     *
     * @throws Exception
     */
    @Test
    public void testTimestampFromUserScidata() throws Exception {
        final boolean userIncludesTimestamps = true;
        final String jsonld = "scidata_nmr_abbreviated.input.jsonld";
        final TestData data = timestampTestBasePost(
            userIncludesTimestamps,
            "testTimestampFromUserScidata",
            jsonld
        );
        timestampTestScidataUpdate(userIncludesTimestamps, jsonld, data);
    }

    /**
     *
     * Full suite test:
     *
     * 1. Test that "created" and "modified" are added to response when POSTing dataset,
     *    even when they are not included by the user.
     * 2. Test that "created" IS NOT updated, but "modified" IS updated
     *    when making a PUT or PATCH request. This should happen automatically
     * 3. Uses the SciData JSON-LD input file
     *
     * @throws Exception
     */
    @Test
    public void testTimestampNotProvidedScidata() throws Exception {
        final boolean userIncludesTimestamps = false;
        final String jsonld = "scidata_nmr_abbreviated.input.jsonld";
        final TestData data = timestampTestBasePost(
            userIncludesTimestamps,
            "testTimestampNotProvidedScidata",
            jsonld
        );
        timestampTestScidataUpdate(userIncludesTimestamps, jsonld, data);
    }

     /**
     *
     * Full suite test:
     *
     * 1. Test that if the user tries to POST JSON with "created" and "modified" properties,
     *    they are ignored in favor of the API's handling.
     * 2. Test that "created" IS NOT updated, but "modified" IS updated
     *    when making a PUT or PATCH request. If the user includes these properties
     *    in their request, the user's values should be ignored.
     * 3. Uses the simple format JSON-LD input file
     *
     * @throws Exception
     */
    @Test
    public void testTimestampFromUserSimple() throws Exception {
        final boolean userIncludesTimestamps = true;
        final String jsonld = "simple.input.jsonld";
        final TestData data = timestampTestBasePost(
            userIncludesTimestamps,
            "testTimestampFromUserSimple",
            jsonld
        );
        timestampTestSimpleUpdate(userIncludesTimestamps, jsonld, data);
    }

    /**
     *
     * Full suite test:
     *
     * 1. Test that "created" and "modified" are added to response when POSTing dataset,
     *    even when they are not included by the user.
     * 2. Test that "created" IS NOT updated, but "modified" IS updated
     *    when making a PUT or PATCH request. This should happen automatically
     * 3. Uses the simple format JSON-LD input file
     *
     * @throws Exception
     */
    @Test
    public void testTimestampNotProvidedSimple() throws Exception {
        final boolean userIncludesTimestamps = false;
        final String jsonld = "simple.input.jsonld";
        final TestData data = timestampTestBasePost(
            userIncludesTimestamps,
            "testTimestampNotProvidedSimple",
            jsonld
        );
        timestampTestSimpleUpdate(userIncludesTimestamps, jsonld, data);
    }

}
