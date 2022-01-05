package gov.ornl.rse.datastreams.ssm_bats_rest_api.models;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;

import org.junit.jupiter.api.Test;

public class CustomizedBatsDataSetIT {
    /**
     * This is a utility operation for checking if data sets correctly created
     * themselves on the remote server.
     *
     * @param dataSet the dataset to check
     */
    private void checkDataSetCreationOnServer(final CustomizedBatsDataSet dataSet) {
        // Create the dataset
        try {
            dataSet.create();
        } catch (Exception e) {
            // Complain
            e.printStackTrace();
            fail();
        }

        // Grab the dataset directy from the server
        String name = dataSet.getName();
        String fusekiURI = dataSet.getHost() + ":" + dataSet.getPort() + "/" + name;
        String fusekiGetURI = fusekiURI + "/get";
        RDFConnectionRemoteBuilder getConnBuilder = RDFConnectionFuseki.create()
                                                                       .destination(fusekiGetURI);
        try (RDFConnectionFuseki getConn = (RDFConnectionFuseki) getConnBuilder.build()) {
            System.out.println("Pulling " + dataSet.getName());
            getConn.begin(ReadWrite.READ);
            Model model = getConn.fetch(null);
            getConn.commit();

            // The only real check that exists is whether or not the exception is caught.

        } catch (Exception e) {
            e.printStackTrace();
            fail("Data set not found!");
        }
    }

    /**
     * Test dataset create.
     */
    @Test
    public void testCreate() {

        // Create a default, empty data set with the default name
        CustomizedBatsDataSet dataSet = new CustomizedBatsDataSet();
        // Check the data set creation
        checkDataSetCreationOnServer(dataSet);

        // Configure the name and some other details of a dataset and test that
        // functionality
        CustomizedBatsDataSet dataSet2 = new CustomizedBatsDataSet();
        String uuidString = UUID.randomUUID().toString();
        String name = "dataSetTest" + "." + uuidString;
        dataSet2.setName(name);
        dataSet2.setHost("http://127.0.0.1");
        dataSet2.setPort(5);
        // Make sure these work OK
        assertEquals(name.toLowerCase(), dataSet2.getName());
        assertEquals("http://127.0.0.1", dataSet2.getHost());
        // Just check that the port is set properly since actually testing a port switch
        // is too onerous
        assertEquals(5, dataSet2.getPort());
        // Reset the port to avoid an error since it has been proven that it could be
        // stored correctly.
        dataSet2.setPort(3030);

        // Check creating the dataset on the server with its custom args
        checkDataSetCreationOnServer(dataSet2);

        return;
    }

    /**
     * Test dataset deletion.
     *
     * @throws Exception this exception is thrown from getJenaDataset since
     *                   we are unable to find the dataset after we delete it
     */
    @Test
    public void testDelete() throws Exception {
        // Create a default, empty data set with the default name
        CustomizedBatsDataSet dataSet = new CustomizedBatsDataSet();
        // Check the data set creation
        checkDataSetCreationOnServer(dataSet);

        // Delete the dataset
        dataSet.delete();

        // Get response code for dataset to determine if it exists
        URL url = new URL(
            dataSet.getFullURI()
            + "/$/datasets/"
            + dataSet.getName());

        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        int code = http.getResponseCode();
        int httpStatusNotFound = 404;
        assertEquals(code, httpStatusNotFound);
    }

    /**
     * This get of models for dataset.
     */
    @Test
    public void testModels() {
        // Create a new data set
        CustomizedBatsDataSet dataSet = new CustomizedBatsDataSet();
        checkDataSetCreationOnServer(dataSet);

        // Put something in it
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("testModelResource");
        Property property = model.createProperty("none", "g");
        resource.addProperty(property, "testProp");

        // Update the data set
        dataSet.updateModel("testModel", model);

        // Check the root/default model
        Model rootModel = dataSet.getRootModel();
        assertNotNull(rootModel);

        // Check the named model
        Model namedModel = dataSet.getModel("testModel");
        assertNotNull(namedModel);
        // Make sure that the model matches the original model by doing a difference and
        // checking the number of statements in the difference model.
        Model differenceModel = namedModel.difference(model);
        assertFalse(differenceModel.listStatements().hasNext());

        // Try putting the model a second time to make sure that it doesn't get
        // duplicated.
        dataSet.updateModel("testModel", model);
        // Make sure the number of triples didn't change with this update.
        Model namedModel2 = dataSet.getModel("testModel");
        Model differenceModel2 = namedModel2.difference(model);
        assertFalse(differenceModel2.listStatements().hasNext());

        // Delete model and make sure it doesn't exist
        dataSet.deleteModel("testModel");
        namedModel = dataSet.getModel("testModel");
        assertNull(namedModel);
    }

    /**
     * Test loading pre-existing data set.
     */
    @Test
    public void testJenaDataSetLoad() {

        // Create a new data set
        CustomizedBatsDataSet referenceDataSet = new CustomizedBatsDataSet();
        checkDataSetCreationOnServer(referenceDataSet);

        // Put something in it
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("testModelResource");
        Property property = model.createProperty("none", "h");
        resource.addProperty(property, "testProp");

        // Upload it to the server
        referenceDataSet.updateModel("testModel", model);

        // Load the contents from the server into a new, empty data set
        CustomizedBatsDataSet loadedSet = new CustomizedBatsDataSet();
        loadedSet.setHost(referenceDataSet.getHost());
        loadedSet.setPort(referenceDataSet.getPort());
        loadedSet.setName(referenceDataSet.getName());

        // Check something!
        assertEquals(referenceDataSet.getName(), loadedSet.getName());
    }
}
