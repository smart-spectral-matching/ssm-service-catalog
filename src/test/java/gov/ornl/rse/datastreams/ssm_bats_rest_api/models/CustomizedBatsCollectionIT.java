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

public class CustomizedBatsCollectionIT {
    /**
     * This is a utility operation for checking if data sets correctly created
     * themselves on the remote server.
     *
     * @param collection the collection to check
     */
    private void checkCollectionCreationOnServer(final CustomizedBatsCollection collection) {
        // Create the collection
        try {
            collection.create();
        } catch (Exception e) {
            // Complain
            e.printStackTrace();
            fail();
        }

        // Grab the collection directy from the server
        String name = collection.getName();
        String fusekiURI = collection.getHost() + ":" + collection.getPort() + "/" + name;
        String fusekiGetURI = fusekiURI + "/get";
        RDFConnectionRemoteBuilder getConnBuilder = RDFConnectionFuseki.create()
                                                                       .destination(fusekiGetURI);
        try (RDFConnectionFuseki getConn = (RDFConnectionFuseki) getConnBuilder.build()) {
            System.out.println("Pulling " + collection.getName());
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
     * Test collection create.
     */
    @Test
    public void testCreate() {

        // Create a default, empty data set with the default name
        CustomizedBatsCollection collection = new CustomizedBatsCollection();
        // Check the data set creation
        checkCollectionCreationOnServer(collection);

        // Configure the name and some other details of a collection and test that
        // functionality
        CustomizedBatsCollection collection2 = new CustomizedBatsCollection();
        String uuidString = UUID.randomUUID().toString();
        String name = "collectionTest" + "." + uuidString;
        collection2.setName(name);
        collection2.setHost("http://127.0.0.1");
        collection2.setPort(5);
        // Make sure these work OK
        assertEquals(name.toLowerCase(), collection2.getName());
        assertEquals("http://127.0.0.1", collection2.getHost());
        // Just check that the port is set properly since actually testing a port switch
        // is too onerous
        assertEquals(5, collection2.getPort());
        // Reset the port to avoid an error since it has been proven that it could be
        // stored correctly.
        collection2.setPort(3030);

        // Check creating the collection on the server with its custom args
        checkCollectionCreationOnServer(collection2);

        return;
    }

    /**
     * Test collection deletion.
     *
     * @throws Exception this exception is thrown from getJenaCollection since
     *                   we are unable to find the collection after we delete it
     */
    @Test
    public void testDelete() throws Exception {
        // Create a default, empty data set with the default name
        CustomizedBatsCollection collection = new CustomizedBatsCollection();
        // Check the data set creation
        checkCollectionCreationOnServer(collection);

        // Delete the collection
        collection.delete();

        // Get response code for collection to determine if it exists
        URL url = new URL(
            collection.getFullURI()
            + "/$/collections/"
            + collection.getName());

        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        int code = http.getResponseCode();
        int httpStatusNotFound = 404;
        assertEquals(code, httpStatusNotFound);
    }

    /**
     * This get of models for collection.
     */
    @Test
    public void testModels() {
        // Create a new data set
        CustomizedBatsCollection collection = new CustomizedBatsCollection();
        checkCollectionCreationOnServer(collection);

        // Put something in it
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("testModelResource");
        Property property = model.createProperty("none", "g");
        resource.addProperty(property, "testProp");

        // Update the data set
        collection.updateModel("testModel", model);

        // Check the root/default model
        Model rootModel = collection.getRootModel();
        assertNotNull(rootModel);

        // Check the named model
        Model namedModel = collection.getModel("testModel");
        assertNotNull(namedModel);
        // Make sure that the model matches the original model by doing a difference and
        // checking the number of statements in the difference model.
        Model differenceModel = namedModel.difference(model);
        assertFalse(differenceModel.listStatements().hasNext());

        // Try putting the model a second time to make sure that it doesn't get
        // duplicated.
        collection.updateModel("testModel", model);
        // Make sure the number of triples didn't change with this update.
        Model namedModel2 = collection.getModel("testModel");
        Model differenceModel2 = namedModel2.difference(model);
        assertFalse(differenceModel2.listStatements().hasNext());

        // Delete model and make sure it doesn't exist
        collection.deleteDataset("testModel");
        namedModel = collection.getModel("testModel");
        assertNull(namedModel);
    }

    /**
     * Test loading pre-existing data set.
     */
    @Test
    public void testJenaCollectionLoad() {

        // Create a new data set
        CustomizedBatsCollection referenceCollection = new CustomizedBatsCollection();
        checkCollectionCreationOnServer(referenceCollection);

        // Put something in it
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("testModelResource");
        Property property = model.createProperty("none", "h");
        resource.addProperty(property, "testProp");

        // Upload it to the server
        referenceCollection.updateModel("testModel", model);

        // Load the contents from the server into a new, empty data set
        CustomizedBatsCollection loadedSet = new CustomizedBatsCollection();
        loadedSet.setHost(referenceCollection.getHost());
        loadedSet.setPort(referenceCollection.getPort());
        loadedSet.setName(referenceCollection.getName());

        // Check something!
        assertEquals(referenceCollection.getName(), loadedSet.getName());
    }
}
