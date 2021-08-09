package gov.ornl.rse.datastreams.ssm_bats_rest_api;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public final class AbbreviatedJson {
    /**
     * Constructor set to private since this is a utility class.
    */
    private AbbreviatedJson() { }

    /**
     * Class ObjectMapper.
    */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .disable(MapperFeature.IGNORE_MERGE_FOR_UNMERGEABLE);

    /**
     * SciData Ontology string for RDF frame filtering.
    */
    private static final String SDO = "https://stuchalk.github.io/scidata/ontology/scidata.owl";

    /**
     * Traverse the json node.
     *
     * @param node
     */
    private static void traverse(final JsonNode node) {
        if (node.getNodeType() == JsonNodeType.ARRAY) {
            traverseArray(node);
        } else if (node.getNodeType() == JsonNodeType.OBJECT) {
            traverseObject(node);
        }
    }

    /**
     * Traverse an object json node.
     *
     * @param node
     */
    private static void traverseObject(final JsonNode node) {
        node.fieldNames().forEachRemaining((String fieldName) -> {
            JsonNode childNode = node.get(fieldName);
            printNode(childNode, fieldName);
            traverse(childNode);
        });
    }

    /**
     * Travsere an array json node.
     *
     * @param node
     */
    private static void traverseArray(final JsonNode node) {
        for (JsonNode jsonArrayNode : node) {
            printNode(jsonArrayNode, "arrayElement");
            traverse(jsonArrayNode);
        }
    }

    /**
     * Pretty print the json node.
     *
     * @param node
     * @param keyName
     */
    private static void printNode(
        final JsonNode node,
        final String keyName
    ) {
        System.out.printf("|-- KEY:%s%n", keyName);
        Object value = null;
        if (node.isTextual()) {
            value = node.textValue();
        } else if (node.isNumber()) {
            value = node.numberValue();
        }

        if (keyName.equals("@value")) {
            System.out.printf("|-- %s=%s type=%s%n",
                "", keyName, value, node.getNodeType());
        }
    }

    /**
     * Get frame filtered JSON-LD for a model.
     *
     * @param model
     * @param frame
     * @return Frame filtered JSON
     */
    private static String getFramedJsonLd(final Model model, final String frame)  {
        DatasetGraph g = DatasetFactory.wrap(model).asDatasetGraph();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        ctx.setFrame(frame);
        return RdfModelWriter.write2String(g, RDFFormat.JSONLD_FRAME_PRETTY, ctx);
    }

    /**
     * Returns RDF model as abbreviated JSON.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      Abbreviated JSON for the Model provided
    */
    public static String getYAxis(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + "#dependent";
        String typeFrame = "{\"@type\" : \"" + typeFilter + "\"}";

        String fullJson = getFramedJsonLd(model, typeFrame);

        JsonNode graphNode = MAPPER.readTree(fullJson).get("@graph");
        traverse(graphNode);
        return fullJson;
    }
}
