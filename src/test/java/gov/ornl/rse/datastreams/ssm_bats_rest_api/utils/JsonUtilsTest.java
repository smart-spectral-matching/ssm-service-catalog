package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtilsTest {

    /**
     * Object Mapper reused for all tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Test to extract object node from array node using @id.
    */
    @Test
    public void testGetIdFromArrayNode() throws Exception {
        // Create the first node of the array
        String firstId = "object/id/1";
        ObjectNode firstNode = MAPPER.createObjectNode();
        firstNode.put("@id", firstId);
        firstNode.put("value", "First Object Value");

        // Create the second node of the array
        String secondId = "object/id/2";
        ObjectNode secondNode = MAPPER.createObjectNode();
        secondNode.put("@id", secondId);
        secondNode.put("value", "Second Object Value");

        // Create the array using first and second nodes
        ArrayNode arrayNode = MAPPER.createArrayNode();
        arrayNode.add(firstNode);
        arrayNode.add(secondNode);

        // Test each node
        Assertions.assertEquals(
            firstNode,
            JsonUtils.getIdFromArrayNode(firstId, arrayNode)
        );

        Assertions.assertEquals(
            secondNode,
            JsonUtils.getIdFromArrayNode(secondId, arrayNode)
        );
    }

}