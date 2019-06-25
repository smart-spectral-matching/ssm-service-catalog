package gov.ornl.rse.datastreams.bats_microservice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ornl.rse.bats.DataSet;

@RestController
public class BatsController {
	private static final Logger logger = LoggerFactory.getLogger(BatsController.class);

	@PostMapping("/ingest")
	public void ingestDataset(@RequestBody String stringToParse) throws Exception{
		String modelName = "testModel";
		Model model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(stringToParse,"UTF-8"), null, "JSON-LD");
		DataSet dataset = new DataSet();
		dataset.setName(modelName);
		dataset.setHost("http://rse-nds-dev1.ornl.gov");
		dataset.create();
		System.out.println(dataset.getFullURI());
		try { 
            dataset.updateModel(modelName, model);
			logger.debug("Model uploaded!");
		} catch (Exception e) {
			logger.error("Unable to update model on the remote Fuseki server.", e);
		}

		try { 
            Model modelPulled = dataset.getModel(modelName);
			logger.debug("Model successfully accessed from Fuseki");
		} catch (Exception e) {
			logger.error("Unable to pull model from the remote Fuseki server.", e);
		}
	}   
}
