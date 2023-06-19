package gov.ornl.rse.datastreams.ssm_bats_rest_api.authorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import gov.ornl.rse.datastreams.ssm_bats_rest_api.configs.ApplicationConfig;
import gov.ornl.rse.datastreams.ssm_bats_rest_api.models.UniquelyIdentifiable;

/**
 * Handler for managing authorization activities.
 *
 * @author Robert Smith
 *
 */
public class AuthorizationHandler {

    /**
     * Fake object for relations to grant universal dataset administration permissions.
     */
    private static String administrateDatasetPermission = "DATASETS ADMINISTRATION";

    /**
     * Fake object for relations to grant universal machine learning model administration permissions.
     */
    private static String administrateMachineLearningModelPermission = "MACHINE LEARNING MODEL ADMINISTRATION";

    /**
     * The user representing what permissions unauthenticated users should have.
     */
    private static String anonymousUser = "ANONYMOUS USER";

    /**
     * Object representing the collection of items which are readable/writable by all authenticated users.
     */
    private static String publicCollection = "PUBLIC COLLECTION";

    /**
     * Handler for Zanzibar API calls.
     */
    private ZanzibarPermissionsHandler handler;

    /**
     * Map from roles to the permissions granted by that role.
     */
    private HashMap<Roles,ArrayList<Permissions>> rolesToPermissions;

    /**
     * The default constructor.
     *
     * @param configuration Application level configuration information from application.properties.
     */
    public AuthorizationHandler(ApplicationConfig configuration) {
        handler = new ZanzibarPermissionsHandler(configuration);

        // Create a map from each Role to its associated Permissions
        rolesToPermissions = new HashMap<Roles,ArrayList<Permissions>>();
        ArrayList<Permissions> collaboratorPerms = new ArrayList<Permissions>();
        collaboratorPerms.add(Permissions.READ);
        rolesToPermissions.put(Roles.COLLABORATOR, collaboratorPerms);
        ArrayList<Permissions> maintainerPerms = new ArrayList<Permissions>();
        maintainerPerms.add(Permissions.ASSOCIATE);
        maintainerPerms.add(Permissions.CREATE);
        maintainerPerms.add(Permissions.READ);
        maintainerPerms.add(Permissions.UPDATE);
        maintainerPerms.add(Permissions.GRANT_ASSOCIATE);
        maintainerPerms.add(Permissions.GRANT_CREATE);
        maintainerPerms.add(Permissions.GRANT_READ);
        maintainerPerms.add(Permissions.GRANT_UPDATE);
        rolesToPermissions.put(Roles.MAINTAINER, maintainerPerms);
        ArrayList<Permissions> memberPerms = new ArrayList<Permissions>();
        memberPerms.add(Permissions.READ);
        memberPerms.add(Permissions.UPDATE);
        rolesToPermissions.put(Roles.MEMBER, memberPerms);
        ArrayList<Permissions> ownerPerms = new ArrayList<Permissions>();
        ownerPerms.add(Permissions.ASSOCIATE);
        ownerPerms.add(Permissions.CREATE);
        ownerPerms.add(Permissions.READ);
        ownerPerms.add(Permissions.UPDATE);
        ownerPerms.add(Permissions.DELETE);
        ownerPerms.add(Permissions.GRANT_ASSOCIATE);
        ownerPerms.add(Permissions.GRANT_CREATE);
        ownerPerms.add(Permissions.GRANT_READ);
        ownerPerms.add(Permissions.GRANT_UPDATE);
        ownerPerms.add(Permissions.GRANT_DELETE);
        rolesToPermissions.put(Roles.MAINTAINER, maintainerPerms);

    }

    /**
     * Give the user a role on an object, removing all other roles. Action will fail if editorUsername is not null and the editor lacks GRANT_X permissions for each non-Grant type permission held by the new role or if the new role is a higher than the editor's.
     *
     * @param editorUsername Username for the user who is performing the action. Null to skill authorization.
     * @param username The user's username.
     * @param role The role.
     * @param objectUUID The object or collection to grant the role on.
     * @throws Exception When editorUsername is not authorized to set username to role.
     */
    public void addRole(String editorUsername, String username, Roles role, String objectUUID) throws Exception {

        // If the change is not associated with a user, there is no need to check permissions.
        if(editorUsername != null) {

            // The Role held by the user requesting the role change.
            Roles editorRole = null;

            // The current Role held by the user whose Role is to be changed
            Roles userRole = null;

            // Get the editor's role.
            if(handler.checkPermission(editorUsername, Roles.COLLABORATOR.toString(), objectUUID)) {
                editorRole = Roles.COLLABORATOR;
            } else if (handler.checkPermission(editorUsername, Roles.MEMBER.toString(), objectUUID)) {
                editorRole = Roles.MEMBER;
            } else if (handler.checkPermission(editorUsername, Roles.MAINTAINER.toString(), objectUUID)) {
                editorRole = Roles.MAINTAINER;
            } else if (handler.checkPermission(editorUsername, Roles.OWNER.toString(), objectUUID)) {
                editorRole = Roles.OWNER;
            }

            // Get the user's role.
            if(handler.checkPermission(username, Roles.COLLABORATOR.toString(), objectUUID)) {
                userRole = Roles.COLLABORATOR;
            } else if (handler.checkPermission(username, Roles.MEMBER.toString(), objectUUID)) {
                userRole = Roles.MEMBER;
            } else if (handler.checkPermission(username, Roles.MAINTAINER.toString(), objectUUID)) {
                userRole = Roles.MAINTAINER;
            } else if (handler.checkPermission(username, Roles.OWNER.toString(), objectUUID)) {
                userRole = Roles.OWNER;
            }

            // Do not let anyone modify the role of a user whose own role is higher than theirs.
            if (editorRole == null ||
                    (Roles.COLLABORATOR.equals(editorRole) && (Roles.MEMBER.equals(userRole) || Roles.MAINTAINER.equals(userRole)) || Roles.OWNER.equals(userRole)) ||
                    (Roles.MEMBER.equals(editorRole) && (Roles.MAINTAINER.equals(userRole)) || Roles.OWNER.equals(userRole)) ||
                    (Roles.MAINTAINER.equals(editorRole) && Roles.OWNER.equals(userRole))) {
                throw new Exception("Users cannot promote/demote users with higher roles.");
            }

            // Check that the granter has an appropriate GRANT permission for each permission held by the new role.
            for(Permissions permission : rolesToPermissions.get(role)) {
                if(Permissions.ASSOCIATE.equals(permission)) {
                    if (!checkPermission(editorUsername, Permissions.GRANT_ASSOCIATE, objectUUID)) {
                        throw new Exception("User " + editorUsername + " does not have permission to grant ASSOCIATE which is held by Role " + role.toString());
                    }
                } else if(Permissions.CREATE.equals(permission)) {
                    if (!checkPermission(editorUsername, Permissions.GRANT_CREATE, objectUUID)) {
                        throw new Exception("User " + editorUsername + " does not have permission to grant CREATE which is held by Role " + role.toString());
                    }
                } else if(Permissions.READ.equals(permission)) {
                    if (!checkPermission(editorUsername, Permissions.GRANT_READ, objectUUID)) {
                        throw new Exception("User " + editorUsername + " does not have permission to grant READ which is held by Role " + role.toString());
                    }
                } else if(Permissions.UPDATE.equals(permission)) {
                    if (!checkPermission(editorUsername, Permissions.GRANT_UPDATE, objectUUID)) {
                        throw new Exception("User " + editorUsername + " does not have permission to grant UPDATE which is held by Role " + role.toString());
                    }
                } else if(Permissions.DELETE.equals(permission)) {
                    if (!checkPermission(editorUsername, Permissions.GRANT_DELETE, objectUUID)) {
                        throw new Exception("User " + editorUsername + " does not have permission to grant DELETE which is held by Role " + role.toString());
                    }
                }
            }
        }

        // Delete any existing role.
        for(Roles currRole : Roles.values()) {
            handler.deletePermission(username, currRole.toString(), objectUUID, null, null);
        }

        // Add the new role.
        handler.createPermission(username, role.toString(), objectUUID, null, null);
    }

    /**
     * Add an object to a group. Fails if editorUsername is set but the user does not have appropriate permissions.
     *
     * @param editorUsername User who is requesting the change. Optional and skips permission checking if not used.
     * @param objectUUID UUID for the object to add to the group.
     * @param groupUUID UUID for the group to add the object to.
     * @exception throws Exception If editorUsername does not have permission to perform the operation.
     */
    public void addToCollection(String editorUsername, String objectUUID, String groupUUID) throws Exception {

        // Skip permissions checking if user is not set.
        if (editorUsername != null) {

            // Check that the user has permission
            if (!checkPermission(editorUsername, Permissions.ASSOCIATE, objectUUID)) {
                throw new Exception("User " + editorUsername + " does not have permission to ASSOCIATE on object " + objectUUID);
            }

            if (!checkPermission(editorUsername, Permissions.UPDATE, groupUUID)) {
                throw new Exception("User " + editorUsername + " does not have permission to UPDATE on object " + groupUUID);
            }
        }

        // Mirror all permissions from the group to the object.
        for (Permissions permission : Permissions.values()) {
            handler.createPermission(null, permission.toString(), objectUUID, groupUUID, permission.toString());
        }

        // Mirror all roles from the collection to the object
        for (Roles role : Roles.values()) {
            handler.createPermission(null, role.toString(), objectUUID, groupUUID, role.toString());
        }
    }

    /**
     * Check whether the given user has permission to create models.
     *
     * @param username Username to check the permission for.
     * @return True if the user has the requested permission. False otherwise.
     */
    public boolean checkDatasetCreationPermission(String username) {
        return handler.checkPermission(username, Permissions.CREATE.toString(), administrateDatasetPermission);
    }

    /**
     * Check whether the given user has permission to create machine learning models.
     *
     * @param username Username to check the permission for.
     * @return True if the user has the requested permission. False otherwise.
     */
    public boolean checkMachineLearningModelPermission(String username) {
        return handler.checkPermission(username, Permissions.CREATE.toString(), administrateMachineLearningModelPermission);
    }

    /**
     * Check whether the given user has the given permission on the given object.
     *
     * @param username Username to check the permission for.
     * @param permission Permission to check if username has over uuid.
     * @param uuid UUID for the object to check permissions for.
     * @return True if the user has the requested permission directly or indirectly. False otherwise.
     */
    public boolean checkPermission(String username, Permissions permission, String uuid) {
        return handler.checkPermission(username, permission.toString(), uuid);
    }

    /**
     * Check whether the given user has the given role on the given object.
     *
     * @param username Username to check the role for.
     * @param role Role to check if username has over uuid.
     * @param uuid UUID for the object to check role for.
     * @return True if the user has the requested role.
     */
    public boolean checkRole(String username, Roles role, String uuid) {
        return handler.checkPermission(username, role.toString(), uuid);
    }

    /**
     * Create a new user.
     *
     * @param username The user to create
     */
    public void createUser(String username) {

        // The only universal permissions are read and update on the public collection, so add those.
        handler.createPermission(username, Permissions.READ.toString(), publicCollection, null, null);
        handler.createPermission(username, Permissions.UPDATE.toString(), publicCollection, null, null);
    }

    /**
     * Delete all permissions relating to the given object. Fails if the user does not have DELETE on the object.
     *
     * @param username The user requesting to perform the action. Optional.
     * @param uuid The object whose permissions are being deleted.
     * @throws Exception If username is specified but user lacks DELETE on the object.
     */
    public void deleteObject(String username, String uuid) throws Exception {

        // If the user is specified, check for permission to delete the object.
        if(username != null && !handler.checkPermission(username, Permissions.DELETE.toString(), uuid)) {
            throw new Exception("User " + username + " lacks DELETE on " + uuid);
        }

        // Get all relations involving the object
        List<Relationship> tuples = handler.queryPermission(null, null, uuid, null, null);
        tuples.addAll(handler.queryPermission(null, null, null, uuid, null));

        //Delete each relationship
        for(Relationship r : tuples) {

            // Get the subject set fields, if any
            String subjectSetSubject = null;
            String subjectSetPermission = null;

            if(r.getSubjectSet() != null) {
                subjectSetSubject = r.getSubjectSet().getSubjectId();
                subjectSetPermission = r.getSubjectSet().getRelation();
            }

            handler.deletePermission(r.getSubjectId(), r.getRelation(), r.getObject(), subjectSetSubject, subjectSetPermission);
        }

    }

    /**
     * Delete the given user.
     *
     * @param username The user to delete.
     */
    public void deleteUser(String username) {

        // Get all relationships for this user.
        List<Relationship> relationships = handler.queryPermission(username, null, null, null, null);

        // Delete each one.
        for(Relationship relationship : relationships) {
            handler.deletePermission(relationship.getSubjectId(), relationship.getRelation(), relationship.getObject(),
                    relationship.getSubjectSet().getObject(), relationship.getSubjectSet().getRelation());
        }
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
        return handler.filter(subject, permission, objects);
    }

    /**
     * Get a list of all collections.
     *
     * @return Every object in the database such that READ on the object implies READ on some other object.
     */
    public ArrayList<String> getCollections(){

        HashSet<String> collections = new HashSet<String>();

        // Get all tuples in the database where an object inherits READ from some other object through a subject set.
        List<Relationship> relationships = handler.queryPermission(null, Permissions.READ.toString(), null, null, Permissions.READ.toString());
        for(Relationship r : relationships) {
            collections.add(r.getSubjectSet().getSubjectId());
        }

        return new ArrayList<String>(collections);

    }

    /**
     * Grant the user permission to create new datasets.
     *
     * @param username The user to grant the permission to.
     */
    public void grantDatasetCreationPermission(String username) {
        handler.createPermission(username, Permissions.CREATE.toString(), administrateDatasetPermission, null, null);
    }

    /**
     * Grant the user permission to create new machine learning models.
     *
     * @param username The user to grant the permission to.
     */
    public void grantMachineLearningModelCreationPermission(String username) {
        handler.createPermission(username, Permissions.CREATE.toString(), administrateMachineLearningModelPermission, null, null);
    }

    /**
     * Grant a permission on an object or collection to everyone, even unauthenticated users. Fails if eidtorUsername is specified and user does not have the appropriate GRANT_* permission.
     *
     * @param editorUsername The user who is attempting to perform the action. Optional.
     * @param permission The permission to grant.
     * @param uuid The UUID for the object or collection to grant permission to.
     * @throws Exception If editorUsername is non-null and the user does not have the appropriate permission.
     */
    public void grantUnauthenticatedPermission(String editorUsername, Permissions permission, String uuid) throws Exception {
        grantUserPermission(editorUsername, anonymousUser, permission, uuid);
    }

    /**
     * Grant a permission on an object or collection to all people with a role.
     *
     * @param editorUsername The user who is attempting to perform the action.
     * @param role The role to grant permissions to.
     * @param permission The permission to grant.
     * @param uuid The UUID  for the object or collection to grant permission to.
     * @throws Exception If the user does not have authorization to grant the permission.
     */
    public void grantRolePermission(String editorUsername, Roles role, Permissions permission, String uuid) throws Exception {

        // Throw a security exception if the user doesn't have the correct GRANT permission
        if(Permissions.ASSOCIATE.equals(permission) || Permissions.GRANT_ASSOCIATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_ASSOCIATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_ASSOCIATE on object " + uuid);
            }
        } else if(Permissions.CREATE.equals(permission) || Permissions.GRANT_CREATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_CREATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_CREATE on object " + uuid);
            }
        } else if(Permissions.READ.equals(permission) || Permissions.GRANT_READ.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_READ, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_READ on object " + uuid);
            }
        } else if(Permissions.UPDATE.equals(permission) || Permissions.GRANT_UPDATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_UPDATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_UPDATE on object " + uuid);
            }
        } else if(Permissions.DELETE.equals(permission) || Permissions.GRANT_DELETE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_DELETE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_DELETE on object " + uuid);
            }
        }

        handler.createPermission(null, permission.toString(), uuid, uuid, role.toString());
    }

    /**
     * Grant a permission on an object or collection to a user.
     *
     * @param username The user to grant permissions to.
     * @param permission The permission to grant.
     * @param uuid The UUID for the object or collection to grant permission to.
     * @throws Exception If the user does not have authorization to grant the permission.
     */
    public void grantUserPermission(String editorUsername, String username, Permissions permission, String uuid) throws Exception {

        // Throw a security exception if the user doesn't have the correct GRANT permission
        if(Permissions.ASSOCIATE.equals(permission) || Permissions.GRANT_ASSOCIATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_ASSOCIATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_ASSOCIATE on object " + uuid);
            }
        } else if(Permissions.CREATE.equals(permission) || Permissions.GRANT_CREATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_CREATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_CREATE on object " + uuid);
            }
        } else if(Permissions.READ.equals(permission) || Permissions.GRANT_READ.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_READ, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_READ on object " + uuid);
            }
        } else if(Permissions.UPDATE.equals(permission) || Permissions.GRANT_UPDATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_UPDATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_UPDATE on object " + uuid);
            }
        } else if(Permissions.DELETE.equals(permission) || Permissions.GRANT_DELETE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_DELETE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_DELETE on object " + uuid);
            }
        }

        handler.createPermission(username, permission.toString(), uuid, null, null);
    }

    /**
     * Setup all default permissions (ones inherited from Roles) for an object and assign it an owner.
     *
     * @param uuid The uuid for the object or collection to initialize
     * @param owner The user who will be the object's owner. Optional.
     */
    public void initializeObject(String uuid, String owner) {

        // Add every role's permissions to the object
        for(Roles role : rolesToPermissions.keySet()) {
            for(Permissions permission : rolesToPermissions.get(role)) {
                handler.createPermission(null, permission.toString(), uuid, uuid, role.toString());
            }
        }

        // If specified, assign a user as the owner.
        if (owner != null) {
            try {
                addRole(null, owner, Roles.OWNER, uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove an object or collection from the public collection. Fails if editorUsername is specified but user does not have UPDATE permission on uuid.
     *
     * @param eidtorUsername The user attempting to make the change. Optional.
     * @param uuid The uuid for the object or collection to remove from the public collection.
     * @throws Exception If editorName is non-null but user does not have appropriate permissions.
     */
    public void makeNonPublic(String editorUsername, String uuid) throws Exception {
        removeFromCollection(editorUsername, uuid, publicCollection);
    }

    /**
     * Add an object or collection to the public collection. Fails if editorUsername is specified but the user does not have UPDATE permission on uuid.
     *
     * @param editorUsername The user attempting to make the change. Optional.
     * @param uuid The UUID for the object or collection to add to the public collection.
     * @throws Exception If editorUsername is non-null but does not have required permissions on uuid.
     */
    public void makePublic(String editorUsername, String uuid) throws Exception {
        addToCollection(editorUsername, uuid, publicCollection);
    }

    /**
     * Remove an object or collection from a collection.
     *
     * @param editorUsername The user who is attempting to make the change.
     * @param objectUUID The UUID for the object or collection to remove.
     * @param groupUUID The UUID for the collection to remove from.
     * @throws Exception If editorUsername is non-null but user does not have appropriate permissions.
     */
    public void removeFromCollection(String editorUsername, String objectUUID, String groupUUID) throws Exception {

        // Skip permissions checking if user is not set.
        if (editorUsername != null) {

            // Check that the user has permission
            if (!checkPermission(editorUsername, Permissions.ASSOCIATE, objectUUID)) {
                throw new Exception("User " + editorUsername + " does not have permission to ASSOCIATE on object " + objectUUID);
            }

            if (!checkPermission(editorUsername, Permissions.UPDATE, groupUUID)) {
                throw new Exception("User " + editorUsername + " does not have permission to UPDATE on object " + groupUUID);
            }
        }

        // Remove each permission from the group to the object
        for(Permissions permission : Permissions.values()) {
            handler.deletePermission(null, permission.toString(), objectUUID, groupUUID, permission.toString());
        }

        // Remove each role from the group to the object
        for(Roles role : Roles.values()) {
            handler.deletePermission(null, role.toString(), objectUUID, groupUUID, role.toString());
        }
    }

    /**
     * Remove a role from a user. Fails if editorUsername is set but user lacks the appropriate permissions.
     *
     * @param editorUsername The user requesting the change. Optional.
     * @param username The user to remove the role from.
     * @param role The role to remove.
     * @param objectUUID The object or collection to remove the role for.
     * @throws Exception If editorUsername is non-null but is a lower ranked Role than the user.
     */
    public void removeRole(String editorUsername, String username, Roles role, String objectUUID) throws Exception {

        // If the change is not associated with a user, there is no need to check permissions.
        if(editorUsername != null) {

            // The Role held by the user requesting the role change.
            Roles editorRole = null;

            // The current Role held by the user whose Role is to be changed
            Roles userRole = null;

            // Get the editor's role.
            if(handler.checkPermission(editorUsername, Roles.COLLABORATOR.toString(), objectUUID)) {
                editorRole = Roles.COLLABORATOR;
            } else if (handler.checkPermission(editorUsername, Roles.MEMBER.toString(), objectUUID)) {
                editorRole = Roles.MEMBER;
            } else if (handler.checkPermission(editorUsername, Roles.MAINTAINER.toString(), objectUUID)) {
                editorRole = Roles.MAINTAINER;
            } else if (handler.checkPermission(editorUsername, Roles.OWNER.toString(), objectUUID)) {
                editorRole = Roles.OWNER;
            }

            // Get the user's role.
            if(handler.checkPermission(username, Roles.COLLABORATOR.toString(), objectUUID)) {
                userRole = Roles.COLLABORATOR;
            } else if (handler.checkPermission(username, Roles.MEMBER.toString(), objectUUID)) {
                userRole = Roles.MEMBER;
            } else if (handler.checkPermission(username, Roles.MAINTAINER.toString(), objectUUID)) {
                userRole = Roles.MAINTAINER;
            } else if (handler.checkPermission(username, Roles.OWNER.toString(), objectUUID)) {
                userRole = Roles.OWNER;
            }

            // Do not let anyone remove a user from a higher ranked role than their own.
            if (editorRole == null ||
                    (Roles.COLLABORATOR.equals(editorRole) && (Roles.MEMBER.equals(userRole) || Roles.MAINTAINER.equals(userRole)) || Roles.OWNER.equals(userRole)) ||
                    (Roles.MEMBER.equals(editorRole) && (Roles.MAINTAINER.equals(userRole)) || Roles.OWNER.equals(userRole)) ||
                    (Roles.MAINTAINER.equals(editorRole) && Roles.OWNER.equals(userRole))) {
                throw new Exception("Users cannot demote users with higher roles.");
            }
        }

        handler.deletePermission(username, role.toString(), objectUUID, null, null);
    }

    /**
     * Remove a user's permission to create datasets.
     *
     * @param username The user to remove the permission from.
     */
    public void revokeDatasetCreationPermissions(String username) {
        handler.deletePermission(username, Permissions.CREATE.toString(), administrateDatasetPermission, null, null);
    }

    /**
     * Remove a user's permission to create machine learning models.
     *
     * @param username The user to remove the permission from.
     */
    public void revokeMachineLearningModelCreationPermissions(String username) {
        handler.deletePermission(username, Permissions.CREATE.toString(), administrateMachineLearningModelPermission, null, null);
    }

    /**
     * Remove a role's permission on an object or collection. Fails if editorUsername is specified but user does not have appropriate permissions.
     *
     * @param editorUsername The user requesting the change. Optional.
     * @param role The role to remove the permission from.
     * @param permission The permission to remove.
     * @param uuid The object or collection to remove the permission for.
     */
    public void revokeRolePermission(String editorUsername, String role, Permissions permission, String uuid) throws Exception {

        // Throw a security exception if the user doesn't have the correct GRANT permission
        if(Permissions.ASSOCIATE.equals(permission) || Permissions.GRANT_ASSOCIATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_ASSOCIATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_ASSOCIATE on object " + uuid);
            }
        } else if(Permissions.CREATE.equals(permission) || Permissions.GRANT_CREATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_CREATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_CREATE on object " + uuid);
            }
        } else if(Permissions.READ.equals(permission) || Permissions.GRANT_READ.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_READ, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_READ on object " + uuid);
            }
        } else if(Permissions.UPDATE.equals(permission) || Permissions.GRANT_UPDATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_UPDATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_UPDATE on object " + uuid);
            }
        } else if(Permissions.DELETE.equals(permission) || Permissions.GRANT_DELETE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_DELETE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_DELETE on object " + uuid);
            }
        }

        handler.deletePermission(null, permission.toString(), uuid, role, uuid);
    }

    /**
     * Remove a permission for un-authenticated users on an object or collection. Fails if editorUsername is specified but user does not have the appropriate GRANT_* permission.
     *
     * @param editorUsername The user requesting the change. Optional.
     * @param permission The permission to remove.
     * @param uuid The object or collection to remove the permission from.
     * @throws Exception If editorUsername is non-null but user does not have appropriate permissions.
     */
    public void revokeUnauthenticatedPermission(String editorUsername, Permissions permission, String uuid) throws Exception {
        revokeUserPermission(editorUsername, anonymousUser, permission, uuid);
    }

    /**
     * Remove a user's permission from an object or collection. Fails if editorUsername is specified but user does not have appropriate permission GRANT_* permission.
     *
     * @param editorUsername The user request the change. Optional.
     * @param username The user to remove the permission from.
     * @param permission The permission to be removed.
     * @param uuid The object or collection to remove the permission for.
     * @throws Exception If editorUsername is non-null and user does not have appropriate permissions.
     */
    public void revokeUserPermission(String editorUsername, String username, Permissions permission, String uuid) throws Exception {

        // Throw a security exception if the user doesn't have the correct GRANT permission
        if(Permissions.ASSOCIATE.equals(permission) || Permissions.GRANT_ASSOCIATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_ASSOCIATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_ASSOCIATE on object " + uuid);
            }
        } else if(Permissions.CREATE.equals(permission) || Permissions.GRANT_CREATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_CREATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_CREATE on object " + uuid);
            }
        } else if(Permissions.READ.equals(permission) || Permissions.GRANT_READ.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_READ, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_READ on object " + uuid);
            }
        } else if(Permissions.UPDATE.equals(permission) || Permissions.GRANT_UPDATE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_UPDATE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_UPDATE on object " + uuid);
            }
        } else if(Permissions.DELETE.equals(permission) || Permissions.GRANT_DELETE.equals(permission)) {
            if(!checkPermission(editorUsername, Permissions.GRANT_DELETE, uuid)) {
                throw new Exception("User " + editorUsername + " lacks GRANT_DELETE on object " + uuid);
            }
        }

        handler.deletePermission(username, permission.toString(), uuid, null, null);
    }

}
