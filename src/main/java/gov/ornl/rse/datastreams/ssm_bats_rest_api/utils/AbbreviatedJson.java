package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.core.DatasetGraph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql.ModelSparql;

public final class AbbreviatedJson {
    /**
     * Splitter for JSON-LD map keys of the form 'url#label'.
     */
    private static final String HASH_SPLITTER = "#";

    /**
     * The key for the values in the frame-filtered JSON-LD maps.
     */
    private static final String VALUE_KEY = "@value";

    /**
     * The key for a list section in the frame-filtered JSON-LD maps.
     */
    private static final String LIST_KEY = "@list";
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
     * Dublin Core Ontology string for RDF frame filtering - HTTP.
     */
    private static final String DCTERM = "http://purl.org/dc/terms/";

    /**
     * Dublin Core Ontology string for RDF frame filtering - HTTPS.
     */
    private static final String DCTERMS = "https://purl.org/dc/terms/";

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
     * Create an array list from a list section of the JSON-LD map.
     *
     * @param listObject Object with list in JSON-LD format to create output list from
     * @return List of values extracted from inputList
     */
    private static ArrayList<Object> extractJsonLdArray(
        final Object listObject
    ) {
        ArrayList<Object> output = new ArrayList<>();

        ArrayList<Map<String, Object>> listOfMaps = (ArrayList<Map<String, Object>>) listObject;

        for (Object entry: listOfMaps) {
            if (Map.class.isInstance(entry)) {
                Map<String, Object> map = (Map<String, Object>) entry;
                if (map.containsKey(VALUE_KEY)) {
                    Object value = map.get(VALUE_KEY);
                    try {
                        Double doubleValue = Double.parseDouble((String) value);
                        output.add(doubleValue);
                    } catch (NumberFormatException e) {
                        output.add(value);
                    }
                } else {
                    Object subMap = getMapValue(map);
                    if (subMap != null) {
                        output.add(subMap);
                    }
                }
            } else if (String.class.isInstance(entry)) {
                output.add((String) entry);
            } else {
                throw new ClassCastException(
                    "Unable to cast following entry from JsonLd array: " + entry
                );
            }
        }

        return output;
    }

    /**
     * Get a label from a map key that is in JSON-LD formats.
     *
     * @param key JSON-LD formatted key to extract the label
     * @return    Label string extracted from input key
     */
    private static String getJsonLdLabel(final String key) {
        String label = "";

        // Split JSON-LD format 'url#<label>'
        if (key.split(HASH_SPLITTER).length == 2) {
            label = key.split(HASH_SPLITTER)[1];

        // Split JSON-LD dcterms field format 'http://purl.org/dc/terms/<label>'
        } else if (key.split(DCTERM).length == 2) {
            label = key.split(DCTERM)[1];

        // Split JSON-LD dcterms field format 'https://purl.org/dc/terms/<label>'
        } else if (key.split(DCTERMS).length == 2) {
            label = key.split(DCTERMS)[1];
        }

        return label;
    }

    /**
     * Gets the object from a map value of a key-value pair.
     *
     * @param value Map from value of a key-value pair
     * @return Object for the value
     */
    private static Object getMapValue(final Map<String, Object> value) {
        if (value.containsKey(VALUE_KEY)) {
            return value.get(VALUE_KEY);
        }

        if (value.containsKey(LIST_KEY)) {
            return extractJsonLdArray(value.get(LIST_KEY));
        }

        Map<String, Object> tempMap = extractJsonLdMap(value);
        if (!tempMap.isEmpty()) {
            return tempMap;
        }
        return null;
    }

    /**
     * Gets value of map entry from JSON-LD.
     *
     * @param entry Map entry from the JSON-LD map
     * @return Value object extracted from the map entry
     */
    private static Object getValueFromMapEntry(
        final Map.Entry<String, Object> entry
    ) {
        Object value = entry.getValue();
        // If a basic key-value, where value is string, add and return before we get to map
        if (value instanceof String || value instanceof Integer) {
            return value;
        }

        // Return early if not an instance of a map
        if (value instanceof Map) {
            return getMapValue((Map<String, Object>) value);
        }

        if (value instanceof ArrayList) {
            return extractJsonLdArray(value);
        }
        return null;
    }

    /**
     * Extract values into an output map from JSON-LD inputMap.
     *
     * @param inputMap Map to extract values from; in JSON-LD format
     * @return Output map with extract keys and values from inputMap
     */
    private static Map<String, Object> extractJsonLdMap(
        final Map<String, Object> inputMap
    ) {
        Map<String, Object> output = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
            String label = getJsonLdLabel(entry.getKey());

            if (label.isEmpty()) {
                continue;
            }

            Object value = getValueFromMapEntry(entry);
            if (value != null) {
                output.put(label, value);
            }
        }
        return output;
    }

    /**
     * Transform from JsonNode in JSON-LD format to the abbreviated JSON format Map.
     *
     * @param node Input JsonNode
     * @return     Map element for the abbrievated JSON
     */
    private static Map<String, Object> transformNodeToMap(final JsonNode node) {
        TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() { };
        Map<String, Object> map = MAPPER.convertValue(node, mapType);
        Map<String, Object> output = extractJsonLdMap(map);
        return output;
    }

    /**
     * Returns a frame filtered JSON-LD.
     *
     * @param model Apache Jena Model to apply the filter to
     * @param typeFilter The frame filter to apply to the model
     * @return Abbreviated Json after frame filtering a JSON-LD model
     */
    private static List<Map<String, Object>> getTypedFrameFilter(
        final Model model,
        final String typeFilter
    ) throws JsonProcessingException {
        String typeFrame = "{\"@type\" : \"" + typeFilter + "\"}";
        String fullJson = getFramedJsonLd(model, typeFrame);
        JsonNode graphNode = MAPPER.readTree(fullJson).get("@graph");

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for (JsonNode node : graphNode) {
            Map<String, Object> map = transformNodeToMap(node);
            list.add(map);
        }
        return list;
    }

    /**
     * Returns property for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      Property for abbreviated JSON for the Model provided
    */
    public static String getProperty(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + HASH_SPLITTER + "scientificData";
        List<Map<String, Object>> scientificDataList = getTypedFrameFilter(model, typeFilter);

        String output = "";
        String propertyKey = "property";

        if (scientificDataList.size() == 0) {
            return output;
        }

        if (!(scientificDataList.get(0).containsKey(propertyKey))) {
            return output;
        }

        output = (String) scientificDataList.get(0).get(propertyKey);
        return output;
    }

    /**
     * Returns description for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      Description for abbreviated JSON for the Model provided
    */
    public static String getDescription(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + HASH_SPLITTER + "scidataFramework";
        List<Map<String, Object>> scidataFrameworkList = getTypedFrameFilter(model, typeFilter);

        String output = "";
        String descriptionKey = "description";

        if (scidataFrameworkList.size() == 0) {
            return output;
        }

        if (!(scidataFrameworkList.get(0).containsKey(descriptionKey))) {
            return output;
        }

        output = (String) scidataFrameworkList.get(0).get(descriptionKey);
        return output;
    }

    /**
     * Returns sources for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      Sources for abbreviated JSON for the Model provided
    */
    public static List<Map<String, Object>> getSources(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = DCTERM + "source";
        return getTypedFrameFilter(model, typeFilter);
    }

    /**
     * Returns methodology for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      Methodology for abbreviated JSON for the Model provided
    */
    public static Map<String, Object> getMethodology(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + HASH_SPLITTER + "methodology";
        List<Map<String, Object>> methodologyList = getTypedFrameFilter(model, typeFilter);

        if (methodologyList.size() == 0) {
            return Collections.emptyMap();
        }

        return methodologyList.get(0);
    }

    /**
     * Returns system facets for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @return      System facets for abbreviated JSON for the Model provided
    */
    public static List<Map<String, Object>> getFacets(final Model model)
    throws
        JsonProcessingException {
        String typeFilter = SDO + HASH_SPLITTER + "system";
        List<Map<String, Object>> systemList = getTypedFrameFilter(model, typeFilter);

        if (systemList.size() == 0) {
            return Collections.emptyList();
        }

        Map<String, Object> hasFacetsMap = systemList.get(0);

        // Get the sub list for "hasSystemFacet", which has the actual facet values
        List<Map<String, Object>> facetsList = new ArrayList<Map<String, Object>>();
        if (hasFacetsMap.get("hasSystemFacet") instanceof List) {
            facetsList = (List<Map<String, Object>>) hasFacetsMap.get("hasSystemFacet");
        }

        // Construct the output facets list of maps from the "hasSystemFacet" list
        List<Map<String, Object>> output = new ArrayList<Map<String, Object>>();

        Map<String, Object> tempMap;
        for (Map<String, Object> facetMap: facetsList) {
            tempMap = new HashMap<String, Object>(); //NOPMD
            for (Map.Entry<String, Object> entry : facetMap.entrySet()) {
                Object value = getValueFromMapEntry(entry);
                tempMap.put(entry.getKey(), value);
            }
            output.add(tempMap);
        }
        return output;
    }

    /**
     * Returns x-axis for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @param element Element to pull from returned list
     * @return      X-axis for abbreviated JSON for the Model provided
    */
    public static Map<String, Object> getXAxis(final Model model, final int element)
    throws
        JsonProcessingException {
        String typeFilter = SDO + HASH_SPLITTER + "independent";
        List<Map<String, Object>> independentList = getTypedFrameFilter(model, typeFilter);

        if (independentList.size() <= element) {
            return Collections.emptyMap();
        }

        return independentList.get(element);
    }

    /**
     * Returns y-axis for the abbreviated JSON format.
     *
     * @param model Apache Jena Model to return as JSON-LD
     * @param element Element to pull from returned list
     * @return      Y-axis for abbreviated JSON for the Model provided
    */
    public static Map<String, Object> getYAxis(final Model model, final int element)
    throws
        JsonProcessingException {
        String typeFilter = SDO + HASH_SPLITTER + "dependent";
        List<Map<String, Object>> dependentList = getTypedFrameFilter(model, typeFilter);

        if (dependentList.size() <= element) {
            return Collections.emptyMap();
        }

        return dependentList.get(element);
    }

    /**
     * Returns the abbreviated json for the model.
     * @param endpointUrl Endpoint for issuing SPARQL queries
     * @param modelUri    Model URI to issue SPARQL queries for
     * @param model       Input Jena Model to do frame filtering on for abbreviated json info
     * @return Abbreviated JSON for the Model
     * @throws JsonProcessingException
     */
    public static String getJson(
        final String endpointUrl,
        final Model model,
        final String modelUri
    ) throws JsonProcessingException {
        Map<String, Object> map = ModelSparql.getModelSummary(endpointUrl, modelUri);
        map.put("full", map.get("url") + "?full=true");

        String[] bits = modelUri.split("/");
        String uuid = bits[bits.length - 1];
        map.put("uuid", uuid);

        List<Map<String, Object>> dataseries = new ArrayList<Map<String, Object>>();
        Map<String, Object> ds1 = new HashMap<>();
        ds1.put("x-axis", getXAxis(model, 0));
        ds1.put("y-axis", getYAxis(model, 0));
        dataseries.add(ds1);

        Map<String, Object> system = new HashMap<>();
        system.put("facets", getFacets(model));

        Map<String, Object> scidata = new HashMap<>();
        scidata.put("property", getProperty(model));
        scidata.put("description", getDescription(model));
        scidata.put("sources", getSources(model));

        scidata.put("methodology", getMethodology(model));
        scidata.put("system", system);
        scidata.put("dataseries", dataseries);

        map.put("scidata", scidata);

        return MAPPER.writeValueAsString(map);
    }
}
