package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dogs")
//@RequestMapping("/datasets/{dataset_uuid}/models")
public class BatsModelsController {
    /*
    // GET
    @RequestMapping(value = "/{uuid}", method = RequestMethod.GET)
    public String getDataSetById(@PathVariable("uuid") String uuid) {
        String jsonld = new String();
        try {
            DataSet dataset = new DataSet();
            dataset.setName(uuid);
            dataset.setHost(hostname);
            Model model = dataset.getModel(uuid);
            jsonld = RdfModelWriter.model2jsonld(model);

        } catch (Exception e) {
            logger.error("Unable to get model on the remote Fuseki server.", e);
        }
        return jsonld;
    }

    // POST
    @RequestMapping(value = "", method = RequestMethod.POST)
    public String createDataset(@RequestBody String jsonPayload) throws Exception {
        // JSON -> Tree
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeNode = mapper.readTree(jsonPayload);

        // add stuff to tree here
        String uuid = UUIDGenerator.generateUUID();

        // Tree -> JSON -> Jena Model
        StringReader reader = new StringReader(treeNode.toString());
        Model model = ModelFactory.createDefaultModel().read(reader, null, "JSON-LD");

        // Jena Model -> BATS DataSet
        DataSet dataset = new DataSet();
        dataset.setName(uuid);
        dataset.setHost(hostname);
        dataset.create();
        try {
            dataset.updateModel(uuid, model);
            logger.debug("Model uploaded!");
        } catch (Exception e) {
            logger.error("Unable to update model on the remote Fuseki server.", e);
        }
        return uuid;
    }
    */
}