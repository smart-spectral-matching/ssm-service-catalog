package ssm.catalog.models;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CustomizedCollection {

    /**
     * This is the default name used as the base for all unnamed instances of
     * Collection.
     */
    public static final String DEFAULT_NAME = "unnamed-dataset";

    /**
     * Setup logger for CustomizedCollection.
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        CustomizedCollection.class
    );

    /**
     * The default host which holds the dataset.
     */
    private String host = "http://localhost";

    /**
     * The default port of the host which holds the dataset.
     */
    private int port = 3030;

    /**
     * The default name for a dataset.
     */
    private String name = DEFAULT_NAME;

    private String configureName() {
        if (this.name.equals(DEFAULT_NAME)) {
            this.name += "_" + UUID.randomUUID().toString();
        }
        return this.name;
    }

    /**
     * Set name of the data set.
     *
     * @param name
     */
    public void setName(final String name) {
        this.name = name.toLowerCase(new Locale("en"));
    }

    /**
     * Get name of the data set.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get host of the data set.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host of the data set.
     *
     * @param host the URI of the remote Fuseki host that hosts the data set
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Get port of the host of this data set.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set port of the host of this data set.
     *
     * @param port
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * Get host of the data set, including hostname, port, and set name.
     *
     * @return the full URI including all parts
     */
    public String getFullURI() {
        return getHost() + ":" + getPort() + "/" + getName();
    }

    /**
     * Creates a dataset with the given name.
     *
     * @throws Exception this exception is thrown if the data set cannot be created
     *                   for any reason.
     */
    public void create() throws Exception {
        // Configure name
        String dbName = configureName();

        // Per the spec, always use tdb2.
        String dbType = "tdb2";

        // Connect the HTTP client
        HttpClient client = HttpClientBuilder.create().build();
        String fusekiLocation = host + ":" + port + "/";
        String fusekiDataAPILoc = "$/datasets";
        HttpPost post = new HttpPost(fusekiLocation + fusekiDataAPILoc);

        // Add the database parameters into the form with UTF_8 encoding.
        List<NameValuePair> form = new ArrayList<NameValuePair>();
        form.add(new BasicNameValuePair("dbName", dbName));
        form.add(new BasicNameValuePair("dbType", dbType));
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(form, Consts.UTF_8);

        // Create the data set
        post.setEntity(formEntity);
        HttpResponse response = client.execute(post);
        LOGGER.debug(response.toString());
    }

    /**
     * Delete data set with the given name.
     *
     * @throws Exception
     */
    public void delete() throws Exception {
        // Connect the HTTP client
        HttpClient client = HttpClientBuilder.create().build();
        String fusekiLocation = host + ":" + port + "/";
        String fusekiDataAPILoc = "$/datasets/" + name;
        HttpDelete delete = new HttpDelete(fusekiLocation + fusekiDataAPILoc);

        // Delete the data set
        HttpResponse response = client.execute(delete);
        LOGGER.debug(response.toString());
    }

    /**
     * Update dataset with this version of the dataset.
     *
     * @param datasetName the name of the dataset that will be updated
     * @param dataset     the dataset that will be updated remotely
     */
    public void updateDataset(final String datasetName, final Model dataset) {

        RDFConnectionRemoteBuilder uploadConnBuilder = RDFConnectionFuseki.create()
                .destination(getFullURI() + "/data");

        // Open a connection to upload the ICE ontology.
        try (RDFConnectionFuseki uploadConn = (RDFConnectionFuseki) uploadConnBuilder.build()) {
            // Note that transactions must proceed with begin(), some operation(), and
            // commit().
            uploadConn.begin(ReadWrite.WRITE);
            uploadConn.put(datasetName, dataset);
            uploadConn.commit();
            LOGGER.debug("Committed dataset " + datasetName + " to data set" + getName());
        } catch (Exception e) {
            LOGGER.error("Unable to update dataset " + datasetName + " in data set " + getName()
                    + " on the remote Fuseki server.", e);
        }
    }

    /**
     * Delete dataset.
     *
     * @param datasetName the name of the dataset that will be deleted
     */
    public void deleteDataset(final String datasetName) {

        RDFConnectionRemoteBuilder uploadConnBuilder = RDFConnectionFuseki.create()
                .destination(getFullURI() + "/data");

        // Open a connection to upload the ICE ontology.
        try (RDFConnectionFuseki uploadConn = (RDFConnectionFuseki) uploadConnBuilder.build()) {
            // Note that transactions must proceed with begin(), some operation(), and
            // commit().
            uploadConn.begin(ReadWrite.WRITE);
            uploadConn.delete(datasetName);
            uploadConn.commit();
            LOGGER.debug("Deleted dataset " + datasetName + " from collection" + getName());
        } catch (Exception e) {
            LOGGER.error("Unable to delete dataset " + datasetName + " in collection " + getName()
                    + " on the remote Fuseki server.", e);
        }
    }

    /**
     * Get root dataset in the data set.
     *
     * @return the root dataset if the data set exists, otherwise null
     */
    public Model getRootDataset() {
        return getDataset(null);
    }

    /**
     * Get dataset with the given name.
     *
     * @param datasetName the name of the dataset that should be retrieved from the data
     *                  set. Note that like Jena, calling with an argument of
     *                  "default" or "null" will return the default graph/model.
     * @return the dataset if it exists in the collection, otherwise null
     */
    public Model getDataset(final String datasetName) {
        Model dataset = null;
        RDFConnectionRemoteBuilder getConnBuilder = RDFConnectionFuseki.create()
                .destination(getFullURI() + "/data");

        try (RDFConnectionFuseki getConn = (RDFConnectionFuseki) getConnBuilder.build()) {
            getConn.begin(ReadWrite.READ);
            dataset = getConn.fetch(datasetName);
            getConn.commit();
            LOGGER.debug("Retrieved dataset " + datasetName + " from data set" + getName());
        } catch (Exception e) {
            LOGGER.error("Unable to find dataset " + datasetName + " in data set " + getName(), e);
        }

        return dataset;
    }
}
