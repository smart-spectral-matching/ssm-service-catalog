package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.apache.jena.query.Dataset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.UUIDGenerator;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.FusekiConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.BatsDataset;

@RestController
@RequestMapping("/datasets")
public class BatsDatasetController {

    /**
     * Setup logger for BatsDatasetController.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        BatsDatasetController.class
    );

    /**
     * Setup Fuseki config.
    */
    @Autowired
    private FusekiConfig fusekiConfig;

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
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());
        dataset.create();
        LOGGER.info("Created datatset: " + uuid);
        return new BatsDataset(uuid);
    }

    /**
     * READ Dataset collection for given Dataset UUID.
     *
     * @param uuid UUID of Dataset to retrieve
     * @return BatsDataset for given Dataset UUID
    */
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    @ResponseBody
    public BatsDataset  getDataSet(@PathVariable("uuid") final String uuid)
        throws
            ResponseStatusException {
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());

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
        dataset.setHost(fusekiConfig.getHost());
        dataset.setPort(fusekiConfig.getPort());
        dataset.delete();
        LOGGER.info("Deleted dataset: " + uuid);
    }
}
