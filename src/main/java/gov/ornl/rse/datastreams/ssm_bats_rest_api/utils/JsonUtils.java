package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.util.Iterator;

import org.apache.jena.vocabulary.DCTerms;

import com.fasterxml.jackson.databind.JsonNode;
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

}
