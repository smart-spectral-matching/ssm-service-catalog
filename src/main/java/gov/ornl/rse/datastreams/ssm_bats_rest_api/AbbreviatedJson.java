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

            System.out.println("Object: " + fieldName);
            if (fieldName.equals("@value")) {
                System.out.println("  value: " + fieldName);
            }

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
            System.out.println("Array: " + jsonArrayNode);
            traverse(jsonArrayNode);
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
     * Returns a frame filtered JSON-LD.
     *
     * @param model Apache Jena Model to apply the filter to
     * @param typeFilter The frame filter to apply to the model
     * @return Abbreviated Json after frame filtering a JSON-LD model
     */
    private static String getTypedFrameFilter(
        final Model model,
        final String typeFilter
    ) throws JsonProcessingException {
        String typeFrame = "{\"@type\" : \"" + typeFilter + "\"}";
        String fullJson = getFramedJsonLd(model, typeFrame);
        JsonNode graphNode = MAPPER.readTree(fullJson).get("@graph");
        traverse(graphNode);
        return fullJson;
    }

    /**
     * Returns x-axis for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      X-axis for abbreviated JSON for the Model provided
    */
    public static String getXAxis(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + "#independent";
        return getTypedFrameFilter(model, typeFilter);
    }

    /**
     * Returns y-axis for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      Y-axis for abbreviated JSON for the Model provided
    */
    public static String getYAxis(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + "#dependent";
        return getTypedFrameFilter(model, typeFilter);
    }
}
