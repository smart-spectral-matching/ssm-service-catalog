package gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.SSMBatsRestApiApplication;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.UniquelyIdentifiable;

/**
 * Handler for all API calls to Zanzibar.
 *
 * @author Robert Smith
 *
 */
public class ZanzibarPermissionsHandler {

    /**
     * Configuration information from application.properties.
     */
    private ApplicationConfig config;

    /**
     * logger.
     */
    private static final Logger LOG = LoggerFactory
        .getLogger(SSMBatsRestApiApplication.class);

    /**
     * The default constructor.
     *
     * @param configuration Application-wide configuration object.
     */
    public ZanzibarPermissionsHandler(final ApplicationConfig configuration) {
        config = configuration;
    }

    /**
     * Check whether or not the given subject has the requested permission.
     *
     * @param subject    The subject to check the permission for.
     * @param permission The permission to check for.
     * @param object     The object to check if subject has permission for.
     * @return True if subject has the requested permission. False otherwise.
     */
    public boolean checkPermission(final String subject, final String permission,
            final String object) {

        // Prefix for every permissions check API call
        String urlString = config.getZanzibarReadHost()
                + "/relation-tuples/check?namespace=ssm&subject_id=" + subject + "&relation="
                + permission + "&object=" + object;

        ObjectMapper mapper = new ObjectMapper();

        // Get the permissions check for this subject.
        try {
            URL url = new URL(urlString);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // The API call reply as a string.
            StringBuilder reply = new StringBuilder("");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                // Read the results into the string
                String line = reader.readLine();

                while (line != null) {
                    reply.append(line);
                    line = reader.readLine();
                }
            }

            // Parse the reply as json.
            JsonNode root = mapper.readTree(reply.toString());

            // Check if the reply confirmed the action is allowed and return
            if (root.get("allowed").asBoolean()) {
                return true;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Download an object from the Fuseki API.
     *
     * @param urlPrefix    The Fuseki server's url.
     * @param identifiable The object to download.
     * @return A string with the contents of identifiable as downloaded from Fuseki.
     */
    private String downloadUUID(final String urlPrefix, final UniquelyIdentifiable identifiable) {

        // Get the permissions check for this subject.
        try {
            URL url = new URL(urlPrefix + identifiable.getUUID());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {

                // The API call reply as a string.
                StringBuilder reply = new StringBuilder("");

                // Read the results into the string
                String line = reader.readLine();

                while (line != null) {
                    reply.append(line);
                    line = reader.readLine();
                }

                return reply.toString();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // In case of error, return an empty string.
        return "";
    }

    /**
     * Add parameters to the given Zanzibar URI.
     *
     * @param uri           The URI to edit.
     * @param object        The data to grant the permission over. Null if only a
     *                      subject set will be used.
     * @param permission    The Zanzibar relation representing the permission to
     *                      grant. Null if only a subject set will be used.
     * @param subject       The user to grant the permission to.
     * @param setObject     The object definition for the subject set. Null if a
     *                      subject set will not be used.
     * @param setPermission The relation definition for the subject set. Null if a
     *                      subject set will not be used.
     * @param pageToken     The token for the next page of query results.
     * @return The URI with the specified parameters added to it.
     */
    private URI createRequestURI(final URI uri, final String subject, final String permission,
            final String object, final String setObject, final String setPermission, final String
            pageToken) {

        URIBuilder builder = new URIBuilder(uri).addParameter("namespace", "ssm");

        // Set all specified parameters
        if (object != null) {
            builder.addParameter("object", object);
        }

        if (permission != null) {
            builder.addParameter("relation", permission);
        }

        if (subject != null && !subject.isEmpty()) {
            builder.addParameter("subject_id", subject);
        }

        if (setObject != null && !setObject.isEmpty() && setPermission != null
                && !setPermission.isEmpty()) {
            builder.addParameter("subject_set.namespace", "ssm")
                    .addParameter("subject_set.object", setObject)
                    .addParameter("subject_set.relation", setPermission);
        }

        if (pageToken != null) {
            builder.addParameter("page_token", pageToken);
        }

        try {
            return builder.build();
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a new permission.
     *
     * @param object        The data to grant the permission over. Null if only a
     *                      subject set will be used.
     * @param permission    The Zanzibar relation representing the permission to
     *                      grant. Null if only a subject set will be used.
     * @param subject       The user to grant the permission to.
     * @param setObject     The object definition for the subject set. Null if a
     *                      subject set will not be used.
     * @param setPermission The relation definition for the subject set. Null if a
     *                      subject set will not be used.
     */
    public void createPermission(final String subject, final String permission, final String object,
            final String setObject, final String setPermission) {

        // Create the JSON representation of the new permission.
        StringBuilder payload = new StringBuilder(256);
        payload.append("{ \"namespace\" : \"ssm\", ");

        if (object != null) {
            payload.append("\"object\" : \"" + object + "\",");
        }

        if (permission != null) {
            payload.append("\"relation\" : \"" + permission + "\"");
        }

        if (subject != null) {
            payload.append(", \"subject_id\" : \"" + subject + "\"");
        }

        if (setObject != null && setPermission != null) {
            payload.append(", \"subject_set\" : { \"namespace\" : \"ssm\", \"object\" : \""
                    + setObject + "\"," + "\"relation\" : \"" + setPermission + "\" }");
        }
        LOG.info(payload.toString());
        payload.append(" }");

        // Send it to the API.
        HttpClient httpclient = HttpClients.createDefault();
        HttpPut request = new HttpPut(config.getZanzibarWriteHost() + "/admin/relation-tuples");
        try {
            request.setEntity(new StringEntity(payload.toString()));
            httpclient.execute(request);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Delete an existing permission.
     *
     * @param object        The data the permission is granted over. May be null.
     * @param permission    The Zanzibar relation representing the permission
     *                      granted. May be null.
     * @param subject       The user the permission is granted to.
     * @param setObject     The object definition for the subject set. May be null.
     * @param setPermission The relation definition for the subject set. May be
     *                      null.
     */
    public void deletePermission(final String subject, final String permission, final String object,
            final String setObject, final String setPermission) {

        // Setup a request
        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete request = new HttpDelete(
                config.getZanzibarWriteHost() + "/admin/relation-tuples");
        LOG.info(createRequestURI(request.getURI(), subject, permission, object, setObject,
                setPermission, null).toString());
        request.setURI(createRequestURI(request.getURI(), subject, permission, object, setObject,
                setPermission, null));

        // Send the request
        try {
            httpclient.execute(request);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Create a new list of the given objects whose UUIDs are stored in Zanzibar and
     * for which the user has the given permission.
     *
     * @param subject    The user to check permissions for.
     * @param permission The permission to check for.
     * @param objects    List of UUID bearing objects to check permissions for.
     * @return A list of every item from subject such that Zanzibar confirms that
     *         user subject has the relation permission on that object.
     */
    public List<UniquelyIdentifiable> filter(final String subject, final String permission,
            final List<UniquelyIdentifiable> objects) {

        // Prefix for every permissions check API call
        String urlPrefix = config.getZanzibarReadHost()
                + "/relation-tuples/check?namespace=ssm&subjec_id=" + subject + "&relation="
                + permission + "&object=";

        ObjectMapper mapper = new ObjectMapper();

        // List of valid subjects to return.
        ArrayList<UniquelyIdentifiable> valid = new ArrayList<UniquelyIdentifiable>();

        // Check each subject's permissions.
        for (UniquelyIdentifiable object : objects) {

            String reply = downloadUUID(urlPrefix, object);

            try {
            // Parse the reply as json.
            JsonNode root = mapper.readTree(reply);

            // Check if the reply confirmed the action is allowed and add to the list of
            // valid subjects if so.
            if (root.get("allowed").asBoolean()) {
                valid.add(object);
            }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return valid;

    }

    /**
     * Get all permissions matching the given criteria.
     *
     * @param object        The object the permission is granted over. May be null.
     * @param permission    The Zanzibar relation representing the permission
     *                      granted. May be null.
     * @param subject       The user the permission is granted to.
     * @param setObject     The object definition for the subject set. May be null.
     * @param setPermission The relation definition for the subject set. May be
     *                      null.
     * @return A list of all Zanzibar relationships that meet the critera.
     */
    public List<Relationship> queryPermission(final String subject, final String permission,
            final String object, final String setObject, final String setPermission) {

        // Setup a request
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet request = new HttpGet(config.getZanzibarReadHost() + "/relation-tuples");
        request.setURI(createRequestURI(request.getURI(), subject, permission, object, setObject,
                setPermission, null));

        // Send the request
        try {
            String result = EntityUtils.toString(httpclient.execute(request).getEntity(), "UTF-8");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);

            // List of returned relationships
            ArrayList<Relationship> relationships = new ArrayList<Relationship>();

            // Convert each node into an object.
            for (JsonNode node : root.get("relation_tuples")) {
                relationships.add(mapper.convertValue(node, Relationship.class));
            }

            // Token for the next page of results
            String token = root.get("next_page_token").asText();

            // Keep looping until no next token is given
            while (!token.isEmpty()) {
                request.setURI(createRequestURI(request.getURI(), subject, permission, object,
                        setObject, setPermission, token));
                result = EntityUtils.toString(httpclient.execute(request).getEntity(), "UTF-8");
                root = mapper.readTree(result);

                // Convert each node into an object.
                for (JsonNode node : root.get("relation_tuples")) {
                    relationships.add(mapper.convertValue(node, Relationship.class));
                }

                token = root.get("next_page_token").asText();
            }

            return relationships;
        } catch (ParseException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new ArrayList<Relationship>();
        }

    }
}
