package gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsModel;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsDataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.RdfModelWriter;

public final class ModelSparql {
    /**
     * Setup logger for ModelSqarql.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        ModelSparql.class
    );

    /**
     * Static methods only.
     */
    private ModelSparql() { }

    /**
     * SPARQL query string for model summary.
     *
     * @param model Model URI to get model summary for
     * @return String for part of SPARQL query
     */
    private static String queryStringForModelSummary(final String model) {
        String query =
            "SELECT ?title ?scidata_url ?modified ?created "
            + "WHERE { "
            + "  GRAPH <" + model + "> { "
            + "    ?node1 dcterms:title ?_title . "
            + "    ?node2 dcterm:modified ?modified . "
            + "    ?node3 dcterm:created ?created . "
            + "    ?scidata_url rdf:type sdo:scidataFramework . "
            + "  } "
            + "  BIND( xml:string(?_title) as ?title)"
            + "}";
        return query;
    }

    /**
     * SPARQL query string for model summaries.
     *
     * @return String for part of the SPARQL query
     */
    private static String queryStringForModelSummaries() {
        String query =
        "SELECT ?model ?title ?scidata_url ?modified ?created "
        + "WHERE { "
        + "  GRAPH ?model { "
        + "    ?node1 dcterms:title ?_title . "
        + "    ?node2 dcterm:modified ?modified . "
        + "    ?node3 dcterm:created ?created . "
        + "    ?scidata_url rdf:type sdo:scidataFramework . "
        + "  } "
        + "  BIND( xml:string(?_title) as ?title)"
        + "}";
    return query;
    }

    /**
     * <p>
     * Prepare to a SPARQL query execution.
     * </p>
     * <p>
     * Note that the return value does not actually execute, as different
     * implementations may want to handle exceptions differently.
     * </p>
     *
     * @param endpointUrl Dataset url for SPARQL server
     * @param queryStr literal query to call
     * @return a prepared query, ready to be executed
     */
    public static QueryExecution prepareSparqlQuery(
        final String endpointUrl,
        final String queryStr
    ) {
        //SPARQL query to find all unique graphs
        ParameterizedSparqlString sparql = new ParameterizedSparqlString();
        sparql.append(queryStr);

        //Prepare to execute the query against the given dataset
        Query query = sparql.asQuery();
        return QueryExecutionFactory.sparqlService(endpointUrl, query);
    }

    /**
     * Get model summary for a named graph via a mode URI.
     *
     * @param endpointUrl SPARQL endpoint to query for the named graph
     * @param modelUri    Model URI for the named graph to get the summary for
     * @return Map with a specific model summary for the named graph
     * @throws QueryException
     */
    public static Map<String, Object> getModelSummary(
        final String endpointUrl,
        final String modelUri
     ) throws QueryException {
        String queryString = SparqlPrefix.queryPrefixesAll()
            + queryStringForModelSummary(modelUri);

        QueryExecution execution = prepareSparqlQuery(//NOPMD
        // pmd does not recognize that this is always being closed
            endpointUrl, queryString
        );

        ResultSet modelResults;
        try {
            modelResults = execution.execSelect();
        } catch (QueryException ex) {
            execution.close();
            throw ex;
        }

        Map<String, Object> modelSummary = new LinkedHashMap<String, Object>();
        while (modelResults.hasNext()) {
            QuerySolution modelSolution = modelResults.next();
            modelSummary = getModelSummaryFromQuery(modelSolution);
        }
        return modelSummary;
    }

    /**
     * Submit SPARQL query for models and return result set.
     *
     * @param pageSize    Page size for the returned model result set
     * @param pageNumber  Page number to use for the returned model result set
     * @param endpointUrl SPARQL endpoint URL to use for issuing the query
     * @return SPARQL query execution for model summaries
     * @throws QueryException
     */
    public static QueryExecution queryModelSummariesWithPagination(
        final int pageSize,
        final int pageNumber,
        final String endpointUrl
    ) throws QueryException {
        String queryString =
            SparqlPrefix.queryPrefixesAll()
            + queryStringForModelSummaries()
            + "ORDER BY DESC(?modified) "
            + "OFFSET " + (pageNumber * pageSize - pageSize) + " "
            + "LIMIT " + pageSize;

        QueryExecution execution = prepareSparqlQuery(//NOPMD
        // pmd does not recognize that this is always being closed
            endpointUrl, queryString
        );
        return execution;
    }

    /**
     * Gets full models from dataset based on SPARQL query results.
     *
     * @param pageSize    Page size for the returned model result set
     * @param pageNumber  Page number to use for the returned model result set
     * @param endpointUrl SPARQL endpoint URL to use for issuing the query
     * @param dataset     Apache Jena Dataset the models belong to
     * @return            List of BatsModel for the full models
    */
    public static List<BatsModel> getFullModels(
        final int pageSize,
        final int pageNumber,
        final String endpointUrl,
        final CustomizedBatsDataSet dataset
    ) {
        List<BatsModel> body = new ArrayList<>();

        QueryExecution execution = ModelSparql.queryModelSummariesWithPagination(//NOPMD
            pageSize,
            pageNumber,
            endpointUrl
        );

        // immediately return 200 if the query was not valid
        ResultSet modelResults;
        try {
            modelResults = execution.execSelect();
        } catch (QueryException ex) {
            execution.close();
            throw ex;
        }

        while (modelResults.hasNext()) {
            QuerySolution solution = modelResults.next();
            RDFNode node = solution.get("?model");
            Model model = dataset.getModel(node.toString());
            try {
                body.add(
                    new BatsModel(//NOPMD
                        node.toString(),
                        RdfModelWriter.getJsonldForModel(model)
                    )
                );
            } catch (IOException e) {
                LOGGER.error(
                    "Unable to parse JSONLD from model {} dataset {}",
                    node.toString(),
                    dataset.getName()
                );
            }
        }
        execution.close();
        return body;
    }

    /**
     * Gets model summaries from SPARQL query results.
     *
     * @param pageSize    Page size for the returned model result set
     * @param pageNumber  Page number to use for the returned model result set
     * @param endpointUrl SPARQL endpoint URL to use for issuing the query
     * @return             List of Maps for model data
    */
    public static List<Map<String, Object>> getModelSummaries(
        final int pageSize,
        final int pageNumber,
        final String endpointUrl
    ) {
        List<Map<String, Object>> body = new ArrayList<>();

        QueryExecution execution = ModelSparql.queryModelSummariesWithPagination(//NOPMD
            pageSize,
            pageNumber,
            endpointUrl
        );

        // immediately return 200 if the query was not valid
        ResultSet modelResults;
        try {
            modelResults = execution.execSelect();
        } catch (QueryException ex) {
            execution.close();
            throw ex;
        }

        while (modelResults.hasNext()) {
            QuerySolution solution = modelResults.next();
            Map<String, Object> map = getModelSummaryFromQuery(solution);
            body.add(map);
        }
        execution.close();
        return body;
    }

    /**
     * Get a model summary as a map from a single SPARQL query solution.
     *
     * @param solution Single SPARQL query solution from a ResultSet to create the map
     * @return Map of the model summary info
     */
    public static Map<String, Object> getModelSummaryFromQuery(
        final QuerySolution solution
    ) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("title", solution.get("?title").toString());
        map.put("url", solution.get("?scidata_url").toString());
        map.put("created", solution.get("?created").toString());
        map.put("modified", solution.get("?modified").toString());

        String url = solution.get("?model").toString();
        String[] bits = url.split("/");
        String uuid = bits[bits.length - 1];
        map.put("uuid", uuid);

        return map;
    }

    /**
     * SPARQL query to get the model uuids.
     *
     * @param endpointUrl SPARQL endpoint URL to issue query against
     * @return ArrayNode of the model uuids from query
     * @throws QueryException
     */
    public static ArrayNode getModelUuids(final String endpointUrl)
    throws QueryException {
        QueryExecution execution = prepareSparqlQuery(//NOPMD
        // pmd does not recognize that this is always being closed
            endpointUrl,
            "SELECT DISTINCT ?model {GRAPH ?model { ?x ?y ?z }}"
        );

        // immediately return 200 if the query was not valid
        ResultSet results;
        try {
            results = execution.execSelect();
        } catch (QueryException ex) {
            execution.close();
            throw ex;
        }
        execution.close();

        //The JSON response being built
        ArrayNode uuidArray = new ArrayNode(new JsonNodeFactory(false));

        //Add each found model to the response
        while (results.hasNext()) {
            QuerySolution solution = results.next();
            RDFNode node = solution.get("?model");
            uuidArray.add(node.toString());
        }
        return uuidArray;
    }

    /**
     * SPARQL query to count total number of models.
     *
     * @param endpointUrl SPARQL endpoint URL to issue query against
     * @return Total number of models via SPARQL query
     * @throws QueryException
     */
    public static int getModelCount(final String endpointUrl)
    throws QueryException {
        // SPARQL query for getting the model count
        String countAllQueryString =
            "SELECT (count(distinct ?model) as ?count) WHERE {"
            + "GRAPH ?model { ?x ?y ?z }}";
        QueryExecution countAllExecution = // NOPMD
            prepareSparqlQuery(endpointUrl, countAllQueryString);

        ResultSet countResults;
        try {
            countResults = countAllExecution.execSelect();
        } catch (QueryException ex) {
            countAllExecution.close();
            throw ex;
        }
        countAllExecution.close();

        // Extracting out the model count from the result
        int totalResults = 0;
        while (countResults.hasNext()) {
            QuerySolution countSolution = countResults.next();
            totalResults = Integer.parseInt(countSolution.get("?count")
                .asLiteral()
                .getLexicalForm());
        }

        return totalResults;
    }
}
