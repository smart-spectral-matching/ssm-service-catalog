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
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsCollection;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.RdfModelWriter;

public final class DatasetSparql {
    /**
     * Setup logger for DatasetSqarql.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DatasetSparql.class
    );

    /**
     * Static methods only.
     */
    private DatasetSparql() { }

    /**
     * SPARQL query string for dataset summary.
     *
     * @param dataset Dataset URI to get dataset summary for
     * @return String for part of SPARQL query
     */
    private static String queryStringForDatasetSummary(final String dataset) {
        String query =
            "SELECT ?title ?scidata_url ?modified ?created "
            + "WHERE { "
            + "  GRAPH <" + dataset + "> { "
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
     * SPARQL query string for dataset summaries.
     *
     * @return String for part of the SPARQL query
     */
    private static String queryStringForDatasetSummaries() {
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
     * @param endpointUrl Collection url for SPARQL server
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

        //Prepare to execute the query against the given collection
        Query query = sparql.asQuery();
        return QueryExecution.service(endpointUrl).query(query).build();
    }

    /**
     * Get dataset summary for a named graph via a mode URI.
     *
     * @param endpointUrl SPARQL endpoint to query for the named graph
     * @param datasetUri    Dataset URI for the named graph to get the summary for
     * @return Map with a specific dataset summary for the named graph
     * @throws QueryException
     */
    public static Map<String, Object> getDatasetSummary(
        final String endpointUrl,
        final String datasetUri
     ) throws QueryException {

        // For some reason, uploading fails with http, getting fails with https
        String queryString = SparqlPrefix.queryPrefixesAll()
        .replace("https://purl.org", "http://purl.org")
            + queryStringForDatasetSummary(datasetUri);

        QueryExecution execution = prepareSparqlQuery(//NOPMD
        // pmd does not recognize that this is always being closed
            endpointUrl, queryString
        );

        Map<String, Object> datasetSummary = new LinkedHashMap<String, Object>();
        ResultSet datasetResults = execution.execSelect();
        try {
            while (datasetResults.hasNext()) {
                QuerySolution datasetSolution = datasetResults.next();
                datasetSummary = getDatasetSummaryFromQuery(datasetSolution);
            }
        } catch (QueryException ex) {
            throw ex;
        } finally {
            execution.close();
            datasetResults.close();
        }

        return datasetSummary;
    }

    /**
     * Submit SPARQL query for datasets and return result set.
     *
     * @param pageSize    Page size for the returned dataset result set
     * @param pageNumber  Page number to use for the returned dataset result set
     * @param endpointUrl SPARQL endpoint URL to use for issuing the query
     * @return SPARQL query execution for dataset summaries
     * @throws QueryException
     */
    public static QueryExecution queryDatasetSummariesWithPagination(
        final int pageSize,
        final int pageNumber,
        final String endpointUrl
    ) throws QueryException {

        // For some reason, uploading fails with http, getting fails with https
        String queryString =
            SparqlPrefix.queryPrefixesAll()
            .replace("https://purl.org", "http://purl.org")
            + queryStringForDatasetSummaries()
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
     * Gets full models from collection based on SPARQL query results.
     *
     * @param pageSize    Page size for the returned model result set
     * @param pageNumber  Page number to use for the returned model result set
     * @param endpointUrl SPARQL endpoint URL to use for issuing the query
     * @param collection     Apache Jena Collection the models belong to
     * @return            List of BatsModel for the full models
    */
    public static List<BatsDataset> getFullModels(
        final int pageSize,
        final int pageNumber,
        final String endpointUrl,
        final CustomizedBatsCollection collection
    ) throws QueryException {
        List<BatsDataset> body = new ArrayList<>();

        QueryExecution execution = DatasetSparql.queryDatasetSummariesWithPagination(//NOPMD
            pageSize,
            pageNumber,
            endpointUrl
        );

        // immediately return 200 if the query was not valid
        ResultSet modelResults  = execution.execSelect();
        try {
            while (modelResults.hasNext()) {
                QuerySolution solution = modelResults.next();
                RDFNode node = solution.get("?model");
                Model model = collection.getModel(node.toString());
                try {
                    body.add(
                        new BatsDataset(//NOPMD
                            node.toString(),
                            RdfModelWriter.getJsonldForModel(model)
                        )
                    );
                } catch (IOException e) {
                    LOGGER.error(
                        "Unable to parse JSONLD from model {} collection {}",
                        node.toString(),
                        collection.getName()
                    );
                }
            }
        } catch (QueryException ex) {
            throw ex;
        } finally {
            execution.close();
            modelResults.close();
        }

        return body;
    }

    /**
     * Gets dataset summaries from SPARQL query results.
     *
     * @param pageSize    Page size for the returned dataset result set
     * @param pageNumber  Page number to use for the returned dataset result set
     * @param endpointUrl SPARQL endpoint URL to use for issuing the query
     * @return             List of Maps for dataset data
    */
    public static List<Map<String, Object>> getDatasetSummaries(
        final int pageSize,
        final int pageNumber,
        final String endpointUrl
    ) throws QueryException {
        List<Map<String, Object>> body = new ArrayList<>();

        QueryExecution execution = DatasetSparql.queryDatasetSummariesWithPagination(//NOPMD
            pageSize,
            pageNumber,
            endpointUrl
        );

        // immediately return 200 if the query was not valid
        ResultSet datasetResults = execution.execSelect();
        try {
            while (datasetResults.hasNext()) {
                QuerySolution solution = datasetResults.next();
                Map<String, Object> map = getDatasetSummaryFromQuery(solution);

                // We do this outside of getDatasetSummaryFromQuery since for a named graph,
                // we dont get ?model back in the query solution
                String url = solution.get("?model").toString();
                String[] bits = url.split("/");
                String uuid = bits[bits.length - 1];
                map.put("uuid", uuid);
                body.add(map);
            }
        } catch (QueryException ex) {
            throw ex;
        } finally {
            execution.close();
            datasetResults.close();
        }
        return body;
    }

    /**
     * Get a dataset summary as a map from a single SPARQL query solution.
     *
     * @param solution Single SPARQL query solution from a ResultSet to create the map
     * @return Map of the dataset summary info
     */
    public static Map<String, Object> getDatasetSummaryFromQuery(
        final QuerySolution solution
    ) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("title", solution.get("?title").toString());
        map.put("url", solution.get("?scidata_url").toString());
        map.put("created", solution.get("?created").toString());
        map.put("modified", solution.get("?modified").toString());
        return map;
    }

    /**
     * SPARQL query to get the model uuids.
     *
     * @param endpointUrl SPARQL endpoint URL to issue query against
     * @return ArrayNode of the model uuids from query
     * @throws QueryException
     */
    public static ArrayNode getDatasetUuids(final String endpointUrl)
    throws QueryException {
        QueryExecution execution = prepareSparqlQuery(//NOPMD
        // pmd does not recognize that this is always being closed
            endpointUrl,
            "SELECT DISTINCT ?model {GRAPH ?model { ?x ?y ?z }}"
        );

        // Build JSON response; immediately return 200 if the query was not valid
        ArrayNode uuidArray = new ArrayNode(new JsonNodeFactory(false));
        ResultSet results  = execution.execSelect();
        try {
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                RDFNode node = solution.get("?model");
                uuidArray.add(node.toString());
            }
        } catch (QueryException ex) {
            throw ex;
        } finally {
            execution.close();
            results.close();
        }

        execution.close();
        results.close();
        return uuidArray;
    }

    /**
     * SPARQL query to count total number of models.
     *
     * @param endpointUrl SPARQL endpoint URL to issue query against
     * @return Total number of models via SPARQL query
     * @throws QueryException
     */
    public static int getDatasetCount(final String endpointUrl)
    throws QueryException {
        // SPARQL query for getting the dataset count
        String countAllQueryString =
            "SELECT (count(distinct ?model) as ?count) WHERE {"
            + "GRAPH ?model { ?x ?y ?z }}";
        QueryExecution countAllExecution = // NOPMD
            prepareSparqlQuery(endpointUrl, countAllQueryString);

        // Extracting out the dataset count from the result
        int totalResults = 0;
        ResultSet countResults = countAllExecution.execSelect();
        try {
            while (countResults.hasNext()) {
                QuerySolution countSolution = countResults.next();
                totalResults = Integer.parseInt(countSolution.get("?count")
                    .asLiteral()
                    .getLexicalForm());
            }
        } catch (QueryException ex) {
            throw ex;
        } finally {
            countResults.close();
            countAllExecution.close();
        }

        return totalResults;
    }
}
