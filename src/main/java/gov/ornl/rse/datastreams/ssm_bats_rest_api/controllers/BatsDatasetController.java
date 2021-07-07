package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Scanner;

import javax.validation.constraints.Pattern;

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
import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.DatasetUtils;

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
     * Valid regex for the BatsDataset title.
    */
    public static final String TITLE_REGEX = "^[A-za-z]+$";

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
     * Error message for invalid dataset title.
    */
    private static final String INVALID_TITLE_ERROR =
        "Invalid title format provided.";

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
        @RequestBody final BatsDataset batsDataset
    ) throws Exception {
        String title = batsDataset.getTitle();

        // Format check the dataset title
        boolean isTitleValid = title.matches(TITLE_REGEX);
        if (!isTitleValid) {
            LOGGER.error(INVALID_TITLE_ERROR);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Title " + title + " incorrectly formatted: "
                + TITLE_REGEX
            );
        }

        DataSet dataset = new DataSet();
        dataset.setName(title);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());
        dataset.create();
        LOGGER.info("Created datatset: " + title);
        return new BatsDataset(title);
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
            LOGGER.error(URL_ACCESS_ERROR, e);
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
        @Pattern(regexp = TITLE_REGEX) final String title)
        throws
            ResponseStatusException {
        DataSet dataset = new DataSet();
        dataset.setName(title);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());

        DatasetUtils.checkDataSetExists(dataset, fuseki(), LOGGER);

        return new BatsDataset(title);
    }

    /**
     * DELETE Dataset collection for given Dataset title.
     *
     * @param title Title of Dataset to delete
    */
    @RequestMapping(value = "/{title}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDataSet(@PathVariable("title")
        @Pattern(regexp = TITLE_REGEX) final String title)
        throws Exception {
        DataSet dataset = new DataSet();
        dataset.setName(title);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());
        dataset.delete();
        LOGGER.info("Deleted dataset: " + title);
    }
}
