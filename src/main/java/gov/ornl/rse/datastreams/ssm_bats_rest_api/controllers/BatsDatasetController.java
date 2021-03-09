package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
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
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig.Fuseki;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;

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
     * CREATE a new Dataset collection for Models.
     *
     * @return BatsDataset for newly created Dataset in Fuseki
    */
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public BatsDataset  createDataSet() throws Exception {
        String uuid = UUIDGenerator.generateUUID();
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());
        dataset.create();
        LOGGER.info("Created datatset: " + uuid);
        return new BatsDataset(uuid);
    }

    /**
     * READ A list of all dataset UUIDs.
     *
     * @return A JSON formatted list of every dataset's UUID.
     */
    @RequestMapping(value = "/uuids", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String getUUIDS() {

        //Read the Fuseki dataset list endpoint
        ObjectMapper mapper = new ObjectMapper();

        URL url = null;

        try {
            url = new URL(fuseki().getHostname() + ":"
                    + fuseki().getPort() + "/$/datasets");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Scanner scanner = null;

        try {
            scanner = new Scanner(url.openStream(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            JsonNode fusekiResponse = mapper.readTree(
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
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            LOGGER.error(READ_DATASETS_ERROR, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                READ_DATASETS_ERROR
            );
        }
    }

    /**
     * READ Dataset collection for given Dataset UUID.
     *
     * @param uuid UUID of Dataset to retrieve
     * @return BatsDataset for given Dataset UUID
    */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public BatsDataset  getDataSet(@PathVariable("uuid") final String uuid)
        throws
            ResponseStatusException {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());

        Dataset contents = dataset.getJenaDataset();
        if (contents == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "DataSet Not Found"
            );
        }
        LOGGER.info("Pulled dataset: " + uuid);
        return new BatsDataset(uuid);
    }

    /**
     * DELETE Dataset collection for given Dataset UUID.
     *
     * @param uuid UUID of Dataset to delete
    */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDataSet(@PathVariable("uuid") final String uuid)
        throws
            Exception {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(fuseki().getHostname());
        dataset.setPort(fuseki().getPort());
        dataset.delete();
        LOGGER.info("Deleted dataset: " + uuid);
    }
}
