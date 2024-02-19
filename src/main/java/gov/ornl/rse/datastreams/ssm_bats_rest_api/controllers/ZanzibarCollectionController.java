package gov.ornl.rse.datastreams.ssm_bats_rest_api.controllers;

import java.util.List;

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

import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.AuthorizationHandler;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.Permissions;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.Relationship;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization.Roles;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.utils.AuthorizationUtils;

/**
 * Controller for endpoints relating to authorization for Zanzibar collections.
 *
 * @author r8s
 *
 */
@RestController
@RequestMapping("/authorization/collections")
@Validated
public class ZanzibarCollectionController {

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Add something to a collection.
     *
     * @param uuid       The collection to add to.
     * @param objectUUID The object to add.
     * @throws ResponseStatusException In case of authorization problems.
     */
    @RequestMapping(value = "/collection/{uuid}/add/{objectUUID}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void addToCollection(@PathVariable("uuid") final String uuid,
            @PathVariable("objectUUID") final String objectUUID) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        // Add the object to the collection
        try {
            authHandler.addToCollection(user, objectUUID, uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks ASSOCIATE permissions on collection " + uuid
                            + " and/or object " + objectUUID);
        }
    }

    /**
     * Initialize a new collection.
     *
     * @param name The unique name of the collection.
     * @throws ResponseStatusException If the name already exists.
     */
    @RequestMapping(value = "/collection/{name}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void create(@PathVariable("name") final String name) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        // If there is already a collection of that name, return an error.
        if (authHandler.checkExists(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Name " + name + " already exists.");
        }

        // Create the new collection
        authHandler.initializeObject(name, user);
    }

    /**
     * Delete a collection. Only the literal collection itself is deleted. Members
     * of the collection will continue to exist.
     *
     * @param name UUID of the collection to delete.
     * @throws ResponseStatusException If user lacks DELETE on the collection.
     */
    @RequestMapping(value = "/collection/{name}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void delete(@PathVariable("name") final String name) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        // Delete the collection
        try {
            authHandler.deleteObject(user, name);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks DELETE permissions on collection " + name);
        }
    }

    /**
     * Get all permissions on a given collection.
     *
     * @param name The collection to get permissions on.
     * @return A list of every Zanzibar relationship for which the collection is an
     *         object.
     */
    @RequestMapping(value = "/collection/{name}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Relationship> get(@PathVariable("name") final String name) {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        return authHandler.getObjectPermissions(name);
    }

    /**
     * Get the uuids of all collections that exist.
     *
     * @return A list of every collection's uuid.
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<String> getAll() {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        return authHandler.getCollections();
    }

    /**
     * Grant a permission to the given user on the collection.
     *
     * @param uuid       The uuid of the collection.
     * @param user       The user to grant the permission to.
     * @param permission The permission to grant. Must match a value of the
     *                   Permissions enum.
     */
    @RequestMapping(value = "/collection/{uuid}/grant/{user}/{permission}", method =
            RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void grantPermission(@PathVariable("uuid") final String uuid,
            @PathVariable("user") final String user,
            @PathVariable("permission") final String permission) throws ResponseStatusException {

        String editor = AuthorizationUtils.getUser();
        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        try {
            authHandler.grantUserPermission(editor, user, Permissions.valueOf(permission), uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + editor + " lacks GRANT_* permissions on collection " + uuid);
        }
    }

    /**
     * Get all the contents of the given collection.
     *
     * @param name The name of the collection.
     * @return The UUIDs of each member of a given collection.
     */
    @RequestMapping(value = "/collection/{name}/contents", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<String> getContents(@PathVariable("name") final String name) {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        return authHandler.getCollectionContents(name);
    }

    /**
     * Remove something from a collection.
     *
     * @param uuid       The collection to remove from.
     * @param objectUUID The object to remove.
     * @throws ResponseStatusException In case of authorization problems.
     */
    @RequestMapping(value = "/collection/{uuid}/remove/{objectUUID}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void removeFromCollection(@PathVariable("uuid") final String uuid,
            @PathVariable("objectUUID") final String objectUUID) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        // Add the object to the collection
        try {
            authHandler.removeFromCollection(user, objectUUID, uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks ASSOCIATE permissions on collection " + uuid
                            + " and/or object " + objectUUID);
        }
    }

    /**
     * Revokes a permission from the given user on the collection.
     *
     * @param uuid       The uuid of the collection.
     * @param user       The user to revoke the permission from.
     * @param permission The permission to revoke. Must match a value of the
     *                   Permissions enum.
     */
    @RequestMapping(value = "/collection/{uuid}/revoke/{user}/{permission}", method =
            RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void revokePermission(@PathVariable("uuid") final String uuid,
            @PathVariable("user") final String user,
            @PathVariable("permission") final String permission) throws ResponseStatusException {

        String editor = AuthorizationUtils.getUser();
        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        try {
            authHandler.revokeUserPermission(editor, user, Permissions.valueOf(permission), uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + editor + " lacks GRANT_* permissions on collection " + uuid);
        }
    }

    /**
     * Make the give collection private by removing it from the special public
     * collection.
     *
     * @param uuid The uuid of the collection to make private
     * @throws ResponseStatusException If there is a permissions issue.
     */
    @RequestMapping(value = "/collection/{uuid}/private", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void setPrivate(@PathVariable("uuid") final String uuid) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        try {
            authHandler.makeNonPublic(user, uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks ASSOCIATE permission on collection " + uuid);
        }
    }

    /**
     * Make the give collection public by adding it to the special public
     * collection.
     *
     * @param uuid The uuid of the collection to make public
     * @throws ResponseStatusException If there is a permissions issue.
     */
    @RequestMapping(value = "/collection/{uuid}/public", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void setPublic(@PathVariable("uuid") final String uuid) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        try {
            authHandler.makePublic(user, uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks ASSOCIATE permission on collection " + uuid);
        }
    }

    /**
     * Set the given user to the given role on the given collection.
     *
     * @param uuid The uuid of the collection to grant the role on.
     * @param user The user whose role is to be edited.
     * @param role The role to set the user to. Must be either "NONE" to remove any
     *             existing role or a valid value of the Roles enum.
     * @throws ResponseStatusException If there are permissions issues.
     */
    @RequestMapping(value = "/collection/{uuid}/role/set/{user}/{role}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void setRole(@PathVariable("uuid") final String uuid,
            @PathVariable("user") final String user, @PathVariable("role") final String role)
            throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        Roles currRole = authHandler.getRole(user, uuid);
        String editor = AuthorizationUtils.getUser();

        try {
            if (currRole != null) {
                authHandler.removeRole(editor, user, currRole, uuid);
            }

            if (!"NONE".equals(role)) {
                authHandler.addRole(editor, user, Roles.valueOf(role), uuid);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks permission to set user " + user + " with role "
                            + (currRole != null ? currRole : "NONE") + " to new role " + role
                            + " on collection " + uuid);
        }
    }
}
