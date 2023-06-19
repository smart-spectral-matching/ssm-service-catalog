package gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * The default constructor.
     *
     * @param configuration Application-wide configuration object.
     */
    public ZanzibarPermissionsHandler(ApplicationConfig configuration) {
        config = configuration;
    }
    
    /**
     * Check whether or not the given subject has the requested permission.
     * 
     * @param subject The subject to check the permission for.
     * @param permission The permission to check for.
     * @param object The object to check if subject has permission for.
     * @return True if subject has the requested permission. False otherwise.
     */
    public boolean checkPermission(String subject, String permission, String object) {

        // Prefix for every permissions check API call
        String urlString = config.getZanzibarReadHost() + "/relation-tuples/check?namespace=ssm&subject_id=" + subject
                + "&relation=" + permission + "&object=" + object;

        ObjectMapper mapper = new ObjectMapper();

        // Get the permissions check for this subject.
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        // The API call reply as a string.
        String reply = "";

        // Read the results into the string
        String line = reader.readLine();

        while (line != null) {
            reply += line;
            line = reader.readLine();
        }

        // Parse the reply as json.
        JsonNode root = mapper.readTree(reply);

        // Check if the reply confirmed the action is allowed and return
        if (root.get("allowed").asBoolean()) {
            return true;
        }

        return false;
    }

    /**
     * Create a new permission.
     *
     * @param object The data to grant the permission over. Null if only a subject set will be used.
     * @param permission The Zanzibar relation representing the permission to grant. Null if only a subject set will be used.
     * @param subject The user to grant the permission to.
     * @param setObject The object definition for the subject set. Null if a subject set will not be used.
     * @param setPermission The relation definition for the subject set. Null if a subject set will not be used.
     */
    public void createPermission(String subject, String permission, String object, String setObject, String setPermission) {

        // Create the JSON representation of the new permission.
        String payload = "{ \"namespace\" : \"ssm\", ";

        if(object != null) {
            payload += "\"object\" : \"" + object + "\",";
        }

        if(permission != null) {
            payload += "\"relation\" : \"" + permission + "\",";
        }

        if(subject != null) {
            payload += "\"subject_id\" : \"" + subject + "\"";
        }

        if (setObject != null && setPermission != null) {
            payload += ", \"subject_set\" : { \"namespace\" : \"ssm\", " +
                "\"object\" : \"" + setObject + "\"," +
                "\relation\" : \"" + setPermission + "\" }";
        }

        payload += " }";

        // Send it to the API.
        HttpClient httpclient = HttpClients.createDefault();
        HttpPut request = new HttpPut(config.getZanzibarWriteHost() + "/admin/relation-tuples");
        request.setEntity(new StringEntity(payload));
        httpclient.execute(request);

    }

    /**
     * Delete an existing permission. 
     * 
     * @param object The data the permission is granted over. May be null.
     * @param permission The Zanzibar relation representing the permission granted. May be null.
     * @param subject The user the permission is granted to.
     * @param setObject The object definition for the subject set. May be null.
     * @param setPermission The relation definition for the subject set. May be null.
     */
    public void deletePermission(String subject, String permission, String object, String setObject, String setPermission) {
        
        // Setup a request
        HttpClient httpclient = HttpClients.createDefault();
        HttpDelete request = new HttpDelete(config.getZanzibarWriteHost() + "/admin/relation-tuples");
        URIBuilder builder = new URIBuilder(request.getURI()).addParameter("namespace", "ssm");
        
        // Set all specified parameters
        if(object != null) {
            builder.addParameter("object", object);
        }
        
        if(permission != null) {
            builder.addParameter("relation", permission);
        }
        
        if(subject != null) {
            builder.addParameter("subject_id", subject);
        }
        
        if (setObject != null && setPermission != null) {
            builder.addParameter("subject_set.namespace", "ssm").addParameter("subject_set.object", setObject).addParameter("subject_set.relation", setPermission);
        }
        
        // Send the request
        request.setURI(builder.build());
        httpclient.execute(request);
        
    }

    /**
     * Create a new list of the given objects whose UUIDs are stored in Zanzibar and
     * for which the user has the given permission.
     * 
     * @param subject     The user to check permissions for.
     * @param permission The permission to check for.
     * @param objects   List of UUID bearing objects to check permissions for.
     * @return A list of every item from subject such that Zanzibar confirms
     *         that user subject has the relation permission on that object.
     */
    public List<UniquelyIdentifiable> filter(String subject, String permission,
            List<UniquelyIdentifiable> objects) {

        // Prefix for every permissions check API call
        String urlPrefix = config.getZanzibarReadHost() + "/relation-tuples/check?namespace=ssm&subjec_id=" + subject
                + "&relation=" + permission + "&object=";

        ObjectMapper mapper = new ObjectMapper();

        // List of valid subjects to return.
        ArrayList<UniquelyIdentifiable> valid = new ArrayList<UniquelyIdentifiable>();

        // Check each subject's permissions.
        for (UniquelyIdentifiable object : objects) {
            try {
                
                // Get the permissions check for this subject.
                URL url = new URL(urlPrefix + object.getUUID());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                // The API call reply as a string.
                String reply = "";

                // Read the results into the string
                String line = reader.readLine();

                while (line != null) {
                    reply += line;
                    line = reader.readLine();
                }

                // Parse the reply as json.
                JsonNode root = mapper.readTree(reply);

                // Check if the reply confirmed the action is allowed and add to the list of valid subjects if so.
                if (root.get("allowed").asBoolean()) {
                    valid.add(object);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return valid;

    }
    
    /**
     * Get all permissions matching the given criteria. 
     * 
     * @param object The object the permission is granted over. May be null.
     * @param permission The Zanzibar relation representing the permission granted. May be null.
     * @param subject The user the permission is granted to.
     * @param setObject The object definition for the subject set. May be null.
     * @param setPermission The relation definition for the subject set. May be null.
     * @return A list of all Zanzibar relationships that meet the critera.
     */
    public List<Relationship> queryPermission(String subject, String permission, String object, String setObject, String setPermission) {
        
        // Setup a request
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet request = new HttpGet(config.getZanzibarWriteHost() + "/relation-tuples");
        URIBuilder builder = new URIBuilder(request.getURI()).addParameter("namespace", "ssm");
        
        // Set all specified parameters
        if(object != null) {
            builder.addParameter("object", object);
        }
        
        if(permission != null) {
            builder.addParameter("relation", permission);
        }
        
        if(subject != null) {
            builder.addParameter("subject_id", subject);
        }
        
        if (setObject != null && setPermission != null) {
            builder.addParameter("subject_set.namespace", "ssm").addParameter("subject_set.object", setObject).addParameter("subject_set.relation", setPermission);
        }
        
        // Send the request
        request.setURI(builder.build());
        String result = EntityUtils.toString(httpclient.execute(request).getEntity(), "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(result);
        
        // List of returned relationships
        ArrayList<Relationship> relationships = new ArrayList<Relationship>();
        
        // Convert each node into an object.
        for(JsonNode node : root.get("relation_tuples")) {
            relationships.add(mapper.convertValue(node, Relationship.class));
        }
        
        return relationships;
    }
}
