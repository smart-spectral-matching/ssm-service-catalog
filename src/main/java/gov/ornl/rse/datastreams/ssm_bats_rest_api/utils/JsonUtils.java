package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.util.Iterator;

import org.apache.jena.vocabulary.DCTerms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utilities for working with Json.
 */
public final class JsonUtils {

    /**
     * The "created" string as used in DCTerms.
     */
    public static final String CREATED = DCTerms.created.getLocalName();
    /**
     * The "modified" string as used in DCTerms.
     */
    public static final String MODIFIED = DCTerms.modified.getLocalName();
    /**
     * URI used for the special metadata field. Can really be anything, as long as it's consistent.
     */
    public static final String METADATA_URI = "http://purl.org/dc/terms/";

    private JsonUtils() {

    }

    /**
     *
     * Recursively search a JsonNode for field names, and remove them.
     *
     * Please note that this traverses the ENTIRE JSON tree, and should therefore be executed
     * on user input before merging the user input with additional data.
     *
     * @param root root JsonNode.
     * @return the exact same JsonNode, with "created" or "modified" fields removed.
     */
    public static JsonNode clearTimestamps(final JsonNode root) {
        if (root.isObject()) {
            // look for field names to clear out
            Iterator<String> itr = root.fieldNames();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                if (fieldName.equals(CREATED) || fieldName.equals(MODIFIED)) {
                    itr.remove();
                    ((ObjectNode) root).remove(fieldName);
                } else if (root.get(fieldName).isArray() || root.get(fieldName).isObject()) {
                    clearTimestamps(root.get(fieldName));
                }
            }
        } else if (root.isArray()) {
            for (final JsonNode node: root) {
                clearTimestamps(node);
            }
        }
        return root;
    }

    /**
     * Merge two JsonNodes together.
     *
     * @param mainNode
     * @param updateNode
     * @return the JsonNode result of merging mainNode and updateNode
     */
    public static JsonNode merge(final JsonNode mainNode,
        final JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode
            if (valueToBeUpdated != null
                && valueToBeUpdated.isArray()
                && updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated,
                    // if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
            // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }

    /**
     * Get object of array node matching @id using given id arg.
     *
     * @param id        ID used to match @id in each element object
     * @param arrayNode Array to iterate over to find id
     * @return          JsonNode for matching object, otherwise null
     */
    public static JsonNode getIdFromArrayNode(
        final String id,
        final ArrayNode arrayNode
    ) {
        for (JsonNode jsonNode : arrayNode) {
            // If the @id is not on the node, just continue to next node
            if (!jsonNode.has("@id")) {
                continue;
            }

            // Get @id and compare to our input ID, return object if match
            String nodeId = jsonNode.get("@id").asText();
            if (nodeId.equals(id)) {
                return jsonNode;
            }
        }

        // Return null if no @id found in array
        return null;
    }
}
