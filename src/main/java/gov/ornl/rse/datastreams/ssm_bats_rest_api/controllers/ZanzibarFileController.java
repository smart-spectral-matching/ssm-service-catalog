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
 * Controller for endpoints relating to authorization for Zanzibar objects.
 *
 * @author r8s
 *
 */
@RestController
@RequestMapping("/authorization/files")
@Validated
public class ZanzibarFileController {

    /**
     * Setup Application config.
     */
    @Autowired
    private ApplicationConfig appConfig;

    /**
     * Initialize a new file.
     *
     * @param name The unique name of the file.
     * @throws ResponseStatusException If the name already exists.
     */
    @RequestMapping(value = "/file/{name}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void create(@PathVariable("name") final String name) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        // If there is already a file of that name, return an error.
        if (authHandler.checkExists(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Name " + name + " already exists.");
        }

        // Create the new file
        authHandler.initializeObject(name, user);
    }

    /**
     * Delete a file.
     *
     * @param name UUID of the file to delete.
     * @throws ResponseStatusException If user lacks DELETE on the collection.
     */
    @RequestMapping(value = "/file/{name}", method = RequestMethod.DELETE)
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
                    "User " + user + " lacks DELETE permissions on file " + name);
        }
    }

    /**
     * Get all permissions on a given file.
     *
     * @param name The file to get permissions on.
     * @return A list of every Zanzibar relationship for which the file is an
     *         object.
     */
    @RequestMapping(value = "/file/{name}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Relationship> get(@PathVariable("name") final String name) {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        return authHandler.getObjectPermissions(name);
    }

    /**
     * Grant a permission to the given user on the file.
     *
     * @param uuid       The uuid of the file.
     * @param user       The user to grant the permission to.
     * @param permission The permission to grant. Must match a value of the
     *                   Permissions enum.
     */
    @RequestMapping(value = "/file/{uuid}/grant/{user}/{permission}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void grantPermission(@PathVariable("uuid") final String uuid,
            @PathVariable("user") final String user,
            @PathVariable("permission") final String permission) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String editor = AuthorizationUtils.getUser();

        try {
            authHandler.grantUserPermission(editor, user, Permissions.valueOf(permission), uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + editor + " lacks GRANT_* permissions on file " + uuid);
        }
    }

    /**
     * Revokes a permission from the given user on the file.
     *
     * @param uuid       The uuid of the file.
     * @param user       The user to revoke the permission from.
     * @param permission The permission to revoke. Must match a value of the
     *                   Permissions enum.
     */
    @RequestMapping(value = "/file/{uuid}/revoke/{user}/{permission}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void revokePermission(@PathVariable("uuid") final String uuid,
            @PathVariable("user") final String user,
            @PathVariable("permission") final String permission) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String editor = AuthorizationUtils.getUser();

        try {
            authHandler.revokeUserPermission(editor, user, Permissions.valueOf(permission), uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + editor + " lacks GRANT_* permissions on file " + uuid);
        }
    }

    /**
     * Make the given file private by removing it from the special public collection.
     *
     * @param uuid The uuid of the file to make private
     * @throws ResponseStatusException If there is a permissions issue.
     */
    @RequestMapping(value = "/file/{uuid}/private", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void setPrivate(@PathVariable("uuid") final String uuid) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        try {
            authHandler.makeNonPublic(user, uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks ASSOCIATE permission on file " + uuid);
        }
    }

    /**
     * Make the given file public by adding it to the special public collection.
     *
     * @param uuid The uuid of the file to make public
     * @throws ResponseStatusException If there is a permissions issue.
     */
    @RequestMapping(value = "/file/{uuid}/public", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void setPublic(@PathVariable("uuid") final String uuid) throws ResponseStatusException {

        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();
        String user = AuthorizationUtils.getUser();

        try {
            authHandler.makePublic(user, uuid);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "User " + user + " lacks ASSOCIATE permission on file " + uuid);
        }
    }

    /**
     * Set the given user to the given role on the given file.
     *
     * @param uuid The uuid of the file to grant the role on.
     * @param user The user whose role is to be edited.
     * @param role The role to set the user to. Must be either "NONE" to remove any
     *             existing role or a valid value of the Roles enum.
     * @throws ResponseStatusException If there are permissions issues.
     */
    @RequestMapping(value = "/file/{uuid}/role/set/{user}/{role}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void setRole(@PathVariable("uuid") final String uuid,
            @PathVariable("user") final String user, @PathVariable("role") final String role)
            throws ResponseStatusException {

        String editor = AuthorizationUtils.getUser();
        AuthorizationHandler authHandler = appConfig.getAuthorizationHandler();

        Roles currRole = authHandler.getRole(user, uuid);

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
