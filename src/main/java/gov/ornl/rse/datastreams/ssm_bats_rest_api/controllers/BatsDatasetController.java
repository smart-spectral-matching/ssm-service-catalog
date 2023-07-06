package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Scanner;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.apache.jena.query.QueryException;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ConfigUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.CustomizedBatsDataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.repositories.DocumentRepository;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DatasetUtils;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.sparql.ModelSparql;

@RestController
@RequestMapping("/datasets")
@Validated
public class BatsDatasetController {

    /**
     * Setup logger for BatsDatasetController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        BatsDatasetController.class
    );

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Document store repository for model documents.
     */
    @Autowired
    private DocumentRepository repository;

    /**
     * Configuration utilities.
    */
    @Autowired
    private ConfigUtils configUtils;

    /**
     * Dataset utilities.
    */
    @Autowired
    private DatasetUtils datasetUtils;


    /**
     * Class ObjectMapper.
    */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @return the Fuseki configuration.
     */
    private Fuseki fuseki() {
        return appConfig.getFuseki();
    }

    /**
     * Error message for reading datasets.
    */
    private static final String READ_DATASETS_ERROR =
        "Unable to read dataset(s) on the remote Fuseki server.";

    /**
     * Error message for malformed URL.
    */
    private static final String BAD_URL_ERROR =
        "Error forming URL to Fuseki dataset asset";

    /**
     * Error message for URL and connection IO issues to Fuseki.
    */
    private static final String URL_ACCESS_ERROR =
        "Fuseki URL / connection access error for dataset";

    /**
     * CREATE a new Dataset collection for Models.
     *
     * @param batsDataset JSON body for creating a new Dataset
     * @return BatsDataset for newly created Dataset in Fuseki
    */
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public BatsDataset  createDataSet(
        @Valid @RequestBody final BatsDataset batsDataset
    ) throws ResponseStatusException, Exception {
        String title = batsDataset.getTitle();

        // Setup the database connection
        CustomizedBatsDataSet dataset = datasetUtils.initDatasetConnection(title);

        // Check that a Dataset with this same title doesn't already exist
        DatasetUtils.DataSetQueryStatus code = datasetUtils.doesDataSetExist(dataset);

        if (code == DatasetUtils.DataSetQueryStatus.EXISTS) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Title " + dataset.getName() + " already exists!"
            );
        }

        // Create the dataset
        dataset.create();
        LOGGER.info("Created dataset: " + dataset.getName());
        return new BatsDataset(dataset.getName());
    }

    /**
     * READ A list of all dataset titles.
     *
     * @return A JSON formatted list of every dataset's title.
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getTitles() {

        //Read the Fuseki dataset list endpoint

        URL url = null;

        try {
            url = new URL(fuseki().getHostname()
                    + ":"
                    + fuseki().getPort()
                    + "/$/datasets");
        } catch (MalformedURLException e) {
            LOGGER.error(BAD_URL_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                BAD_URL_ERROR
            );
        }

        Scanner scanner = null;

        try {
            scanner = new Scanner(url.openStream(), "UTF-8");
        } catch (IOException e) {
            LOGGER.error(URL_ACCESS_ERROR + ": " + url, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_DATASETS_ERROR
            );
        }

        try {
            JsonNode fusekiResponse = MAPPER.readTree(
                    scanner.useDelimiter("\\A").next());

            //Get the node containing the list of datasets
            ArrayNode datasetsNode = (ArrayNode) fusekiResponse.get("datasets");
            Iterator<JsonNode> datasetIterator = datasetsNode.elements();

            //The JSON response being built
            ArrayNode response = new ArrayNode(new JsonNodeFactory(false));

            //Read out only the name field of each dataset and add it to the
            //response
            while (datasetIterator.hasNext()) {
                response.add(
                    datasetIterator.next()
                                   .get("ds.name")
                                   .asText()
                                   .replaceAll("/", "")
                );
            }

            //Return the JSON representation
            return MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_DATASETS_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_DATASETS_ERROR
            );
        } finally {
            scanner.close();
        }
    }

    /**
     * READ Dataset collection for given Dataset title.
     *
     * @param title Title of Dataset to retrieve
     * @return BatsDataset for given Dataset title
    */
    @RequestMapping(value = "/{title}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsDataset getDataSet(@PathVariable("title")
        @Pattern(regexp = BatsDataset.TITLE_REGEX) final String title)
        throws
            ResponseStatusException {
        CustomizedBatsDataSet dataset = datasetUtils.getDataset(title);
        return new BatsDataset(dataset.getName());
    }

    /**
     * DELETE Dataset collection for given Dataset title.
     *
     * @param title Title of Dataset to delete
    */
    @RequestMapping(value = "/{title}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDataSet(@PathVariable("title")
        @Pattern(regexp = BatsDataset.TITLE_REGEX) final String title)
        throws Exception {
        CustomizedBatsDataSet dataset = datasetUtils.getDataset(title);

        // Get the Model UUID list for the dataset
        String endpointUrl = fuseki().getHostname() + ":" + fuseki().getPort() + "/" + title;
        ArrayNode uuidArray = MAPPER.createArrayNode();
        try {
            uuidArray = ModelSparql.getModelUuids(endpointUrl);
        } catch (QueryException ex) {
            LOGGER.info("No models to delete for datset.");
        }

        // Loop to delete the models from dataset
        for (JsonNode modelUuidNode: uuidArray) {
            String modelUUID = modelUuidNode.asText();
            String uuid = modelUUID.substring(modelUUID.lastIndexOf('/') + 1);
            String modelUri = configUtils.getModelUri(title, uuid);

            // Get model for rollback
            Model model = dataset.getModel(modelUri);

            // Delete model from graph database
            LOGGER.info("Deleting model: " + uuid + " from graph database");
            try {
                dataset.deleteModel(modelUri);
            } catch (Exception ex) {
                String message = "Unable to delete model: " + uuid + " from graph database.";
                LOGGER.error(message);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }

            // Delete model from document store (rollback graph database if fails)
            LOGGER.info("Deleting model: " + uuid + " from document store");
            try {
                repository.delete(repository.findById(uuid).get());
            } catch (Exception ex) {
                String message = "Unable to delete model: " + uuid + " from document store.";
                LOGGER.error(message);
                dataset.updateModel(modelUri, model);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }

        }


        // Delete dataset collection from graph database
        dataset.delete();
        LOGGER.info("Deleted dataset: " + dataset.getName());
    }
}
