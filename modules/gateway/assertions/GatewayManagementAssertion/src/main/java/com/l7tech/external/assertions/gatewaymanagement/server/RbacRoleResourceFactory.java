package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.api.impl.RemoveAssignmentsContext;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Eithers;
import com.l7tech.util.Functions;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static com.l7tech.util.Eithers.*;

/**
 * Resource factory for Rbac roles.
 * <p/>
 * The rbac role factory also manages other related rbac entities. Permissions, assignments and predicates.
 * <p/>
 * Roles have full CRUD support, and provide two additional operations. Add assignments and remove assignments. These
 * two additional operations are needed because gateway managed roles can only be manipulated by adding or removing
 * assignments
 */
@ResourceFactory.ResourceType(type = RbacRoleMO.class)
public class RbacRoleResourceFactory extends EntityManagerResourceFactory<RbacRoleMO, Role, EntityHeader> {

    private FolderManager folderManager;
    private SecurityZoneManager securityZoneManager;
    private EntityFinder entityFinder;
    private IdentityProviderFactory identityProviderFactory;

    private static final Object EmptyReturn = new Object();

    public RbacRoleResourceFactory(final RbacServices services,
                                   final SecurityFilter securityFilter,
                                   final PlatformTransactionManager transactionManager,
                                   final RoleManager roleManager,
                                   final FolderManager folderManager,
                                   final SecurityZoneManager securityZoneManager,
                                   final EntityFinder entityFinder,
                                   final IdentityProviderFactory identityProviderFactory) {
        super(false, true, services, securityFilter, transactionManager, roleManager);
        this.folderManager = folderManager;
        this.securityZoneManager = securityZoneManager;
        this.entityFinder = entityFinder;
        this.identityProviderFactory = identityProviderFactory;
    }

    /**
     * This will add assignments to the specified role.
     *
     * @param selectorMap           The selector map to use to select the role
     * @param addAssignmentsContext The assignments to add to the role
     * @throws ResourceNotFoundException
     * @throws InvalidResourceException
     */
    @ResourceMethod(name = "AddAssignments", selectors = true, resource = true)
    public void addAssignments(final Map<String, String> selectorMap, final AddAssignmentsContext addAssignmentsContext) throws ResourceNotFoundException, InvalidResourceException {
        extract2(transactional(new TransactionalCallback<Eithers.E2<ResourceNotFoundException, InvalidResourceException, Object>>() {
            @Override
            public Eithers.E2<ResourceNotFoundException, InvalidResourceException, Object> execute() throws ObjectModelException {
                try {
                    //Find the role
                    final Role role = selectEntity(selectorMap);
                    checkPermitted(OperationType.UPDATE, null, role);
                    List<RbacRoleAssignmentMO> assignmentMOs = addAssignmentsContext.getAssignments();
                    Role tempRole = new Role();
                    //Convert the RbacRoleAssignmentMO's to RoleAssignment's these are added to the temp role
                    assignmentsFromResource(tempRole, assignmentMOs);
                    // validate that the assignments refer to existing users and groups
                    validateAssignmentsExist(tempRole.getRoleAssignments());
                    //Update the role and add all the assignments
                    updateAssignments(role, tempRole.getRoleAssignments(), false);
                    return right2(EmptyReturn);
                } catch (ResourceNotFoundException e) {
                    return left2_1(e);
                } catch (InvalidResourceException e) {
                    return left2_2(e);
                }
            }
        }, false));
    }

    /**
     * This will remove a list of assignments from the specified role.
     *
     * @param selectorMap              The selector map to use to select the role
     * @param removeAssignmentsContext The list of assignment id's to remove
     * @throws ResourceNotFoundException
     * @throws InvalidResourceException
     */
    @ResourceMethod(name = "RemoveAssignments", selectors = true, resource = true)
    public void removeAssignments(final Map<String, String> selectorMap, final RemoveAssignmentsContext removeAssignmentsContext) throws ResourceNotFoundException, InvalidResourceException {
        extract2(transactional(new TransactionalCallback<Eithers.E2<ResourceNotFoundException, InvalidResourceException, Object>>() {
            @Override
            public Eithers.E2<ResourceNotFoundException, InvalidResourceException, Object> execute() throws ObjectModelException {
                try {
                    //find the role to update
                    final Role role = selectEntity(selectorMap);
                    checkPermitted(OperationType.UPDATE, null, role);
                    HashSet<RoleAssignment> assignmentsToRemove = new HashSet<>(removeAssignmentsContext.getAssignmentIds().size());
                    //find all the assignments to Remove
                    for (RoleAssignment assignment : Collections.unmodifiableSet(role.getRoleAssignments())) {
                        if (removeAssignmentsContext.getAssignmentIds().contains(assignment.getId())) {
                            assignmentsToRemove.add(assignment);
                            removeAssignmentsContext.getAssignmentIds().remove(assignment.getId());
                        }
                    }
                    //if there are assignments that do not belong to this role return an error
                    if (!removeAssignmentsContext.getAssignmentIds().isEmpty()) {
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Assignment Ids do not belong to role: " + role.getDescriptiveName() + " assignment ids: " + removeAssignmentsContext.getAssignmentIds().toString());
                    }
                    //remove all the assignments from the role.
                    for (RoleAssignment assignment : assignmentsToRemove) {
                        role.getRoleAssignments().remove(assignment);
                    }
                    return right2(EmptyReturn);
                } catch (ResourceNotFoundException e) {
                    return left2_1(e);
                } catch (InvalidResourceException e) {
                    return left2_2(e);
                }
            }
        }, false));
    }

    /**
     * Returns the role as a Managed Object
     *
     * @param role the role to convert
     * @return The managed object representing the role
     */
    @Override
    protected RbacRoleMO asResource(final Role role) {
        RbacRoleMO rbacRoleMO = ManagedObjectFactory.createRbacRoleMO();

        rbacRoleMO.setName(role.getName());
        rbacRoleMO.setDescription(role.getDescription());
        rbacRoleMO.setUserCreated(role.isUserCreated());
        rbacRoleMO.setAssignments(assignmentsAsResource(role.getRoleAssignments()));
        rbacRoleMO.setPermissions(permissionsAsResource(role.getPermissions()));
        return rbacRoleMO;
    }

    private List<RbacRolePermissionMO> permissionsAsResource(Set<Permission> permissions) {
        ArrayList<RbacRolePermissionMO> rbacRolePermissionMOs = new ArrayList<>(permissions.size());
        for (Permission rolePermission : permissions) {
            RbacRolePermissionMO rbacRolePermissionMO = ManagedObjectFactory.createRbacRolePermissionMO();
            rbacRolePermissionMO.setId(rolePermission.getId());
            rbacRolePermissionMO.setVersion(rolePermission.getVersion());
            rbacRolePermissionMO.setEntityType(rolePermission.getEntityType().name());
            rbacRolePermissionMO.setOtherOperationName(rolePermission.getOtherOperationName());
            rbacRolePermissionMO.setOperation(rolePermission.getOperation() == null ? null : RbacRolePermissionMO.OperationType.valueOf(rolePermission.getOperation().name()));
            rbacRolePermissionMO.setScope(predicateAsResource(rolePermission.getScope()));
            rbacRolePermissionMOs.add(rbacRolePermissionMO);
        }
        return rbacRolePermissionMOs;
    }

    private List<RbacRolePredicateMO> predicateAsResource(Set<ScopePredicate> predicates) {
        ArrayList<RbacRolePredicateMO> rbacRolePredicateMOs = new ArrayList<>(predicates.size());
        for (ScopePredicate scopePredicate : predicates) {
            RbacRolePredicateMO rbacRolePredicateMO = ManagedObjectFactory.createRbacRolePredicateMO();
            if (scopePredicate instanceof AttributePredicate) {
                AttributePredicate attributePredicate = (AttributePredicate) scopePredicate;
                rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.AttributePredicate);
                rbacRolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("attribute", attributePredicate.getAttribute())
                        .put("value", attributePredicate.getValue())
                        .put("mode", attributePredicate.getMode()).map());
            } else if (scopePredicate instanceof EntityFolderAncestryPredicate) {
                EntityFolderAncestryPredicate entityFolderAncestryPredicate = (EntityFolderAncestryPredicate) scopePredicate;
                rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.EntityFolderAncestryPredicate);
                rbacRolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("entityId", entityFolderAncestryPredicate.getEntityId())
                        .put("entityType", entityFolderAncestryPredicate.getEntityType().name()).map());
            } else if (scopePredicate instanceof FolderPredicate) {
                FolderPredicate folderPredicate = (FolderPredicate) scopePredicate;
                rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.FolderPredicate);
                rbacRolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("folderId", folderPredicate.getFolder().getId())
                        .put("transitive", String.valueOf(folderPredicate.isTransitive())).map());
            } else if (scopePredicate instanceof ObjectIdentityPredicate) {
                ObjectIdentityPredicate objectIdentityPredicate = (ObjectIdentityPredicate) scopePredicate;
                rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.ObjectIdentityPredicate);
                rbacRolePredicateMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                        .put("entityId", objectIdentityPredicate.getTargetEntityId()).map());
            } else if (scopePredicate instanceof SecurityZonePredicate) {
                SecurityZonePredicate securityZonePredicate = (SecurityZonePredicate) scopePredicate;
                rbacRolePredicateMO.setType(RbacRolePredicateMO.Type.SecurityZonePredicate);
                rbacRolePredicateMO.setProperties(securityZonePredicate.getRequiredZone() == null ? null : CollectionUtils.MapBuilder.<String, String>builder()
                        .put("securityZoneId", securityZonePredicate.getRequiredZone().getId()).map());
            }
            rbacRolePredicateMOs.add(rbacRolePredicateMO);
        }
        return rbacRolePredicateMOs;
    }

    private List<RbacRoleAssignmentMO> assignmentsAsResource(Set<RoleAssignment> roleAssignments) {
        ArrayList<RbacRoleAssignmentMO> rbacRoleAssignmentMOs = new ArrayList<>(roleAssignments.size());
        for (RoleAssignment roleAssignment : roleAssignments) {
            RbacRoleAssignmentMO rbacRoleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
            rbacRoleAssignmentMO.setId(roleAssignment.getId());
            rbacRoleAssignmentMO.setIdentityId(roleAssignment.getIdentityId());
            rbacRoleAssignmentMO.setProviderId(roleAssignment.getProviderId().toString());
            rbacRoleAssignmentMO.setEntityType(roleAssignment.getEntityType());
            rbacRoleAssignmentMOs.add(rbacRoleAssignmentMO);
        }
        return rbacRoleAssignmentMOs;
    }

    /**
     * Creates a role from a role managaed object
     *
     * @param resource The resource representation
     * @return The Role created from the resource
     * @throws InvalidResourceException
     */
    @Override
    protected Role fromResource(final Object resource) throws InvalidResourceException {
        if (!(resource instanceof RbacRoleMO))
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected rbac role");

        final RbacRoleMO rbacRoleMO = (RbacRoleMO) resource;

        final Role role = new Role();
        role.setName(rbacRoleMO.getName());
        role.setDescription(rbacRoleMO.getDescription());
        // Note do not use the userCreated flag from the resource. Roles created using wsman will automatically have this flag set to true.
        try {
            assignmentsFromResource(role, rbacRoleMO.getAssignments());
            permissionsFromResource(role, rbacRoleMO);
        } catch (FindException e) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Could not find entity: " + e.getMessage());
        }

        return role;
    }

    private void permissionsFromResource(Role role, RbacRoleMO rbacRoleMO) throws InvalidResourceException, FindException {
        for (RbacRolePermissionMO rbacRolePermissionMO : rbacRoleMO.getPermissions()) {
            Permission permission = new Permission(role, OperationType.valueOf(rbacRolePermissionMO.getOperation().name()), EntityType.valueOf(rbacRolePermissionMO.getEntityType()));
            permission.setOtherOperationName(rbacRolePermissionMO.getOtherOperationName());
            HashSet<ScopePredicate> scopeSet = new HashSet<>(rbacRolePermissionMO.getScope().size());
            for (RbacRolePredicateMO predicateMO : rbacRolePermissionMO.getScope()) {
                final ScopePredicate scopePredicate;
                switch (predicateMO.getType()) {
                    case AttributePredicate:
                        scopePredicate = new AttributePredicate(permission, predicateMO.getProperties().get("attribute"), predicateMO.getProperties().get("value"));
                        ((AttributePredicate) scopePredicate).setMode(predicateMO.getProperties().get("mode"));
                        break;
                    case EntityFolderAncestryPredicate:
                        scopePredicate = new EntityFolderAncestryPredicate(permission, EntityType.valueOf(predicateMO.getProperties().get("entityType")), predicateMO.getProperties().get("entityId"));
                        break;
                    case FolderPredicate:
                        final Folder folder = folderManager.findByPrimaryKey(Goid.parseGoid(predicateMO.getProperties().get("folderId")));
                        if (folder == null) {
                            throw new FindException("Cannot find folder with id: " + predicateMO.getProperties().get("folderId"));
                        }
                        scopePredicate = new FolderPredicate(permission, folder, Boolean.valueOf(predicateMO.getProperties().get("transitive")));
                        break;
                    case ObjectIdentityPredicate:
                        scopePredicate = new ObjectIdentityPredicate(permission, predicateMO.getProperties().get("entityId"));
                        break;
                    case SecurityZonePredicate:
                        final SecurityZone securityZone;
                        if (predicateMO.getProperties().get("securityZoneId") != null) {
                            securityZone = securityZoneManager.findByPrimaryKey(Goid.parseGoid(predicateMO.getProperties().get("securityZoneId")));
                            if (securityZone == null) {
                                throw new FindException("Cannot find security zone with id: " + predicateMO.getProperties().get("securityZoneId"));
                            }
                        } else {
                            securityZone = null;
                        }
                        scopePredicate = new SecurityZonePredicate(permission, securityZone);
                        break;
                    default:
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "Unknown role predicate type: " + predicateMO.getType().name());
                }
                scopeSet.add(scopePredicate);
            }
            permission.setScope(scopeSet);
            role.getPermissions().add(permission);
        }
    }

    private void assignmentsFromResource(Role role, List<RbacRoleAssignmentMO> rbacRoleAssignmentMOs) throws InvalidResourceException, FindException {
        for (RbacRoleAssignmentMO rbacRoleAssignmentMO : rbacRoleAssignmentMOs) {
            if ("User".equals(rbacRoleAssignmentMO.getEntityType())) {
                UserBean user = new UserBean();
                user.setProviderId(Goid.parseGoid(rbacRoleAssignmentMO.getProviderId()));
                if (rbacRoleAssignmentMO.getIdentityId() != null) {
                    user.setUniqueIdentifier(rbacRoleAssignmentMO.getIdentityId());
                } else if (rbacRoleAssignmentMO.getIdentityName() != null) {
                    user.setUniqueIdentifier(findIdentityIdByName(Goid.parseGoid(rbacRoleAssignmentMO.getProviderId()), rbacRoleAssignmentMO.getEntityType(), rbacRoleAssignmentMO.getIdentityName()));
                } else {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "Must specify either role assignment identity id or identity name");
                }
                role.addAssignedUser(user);
            } else if ("Group".equals(rbacRoleAssignmentMO.getEntityType())) {
                GroupBean group = new GroupBean();
                group.setProviderId(Goid.parseGoid(rbacRoleAssignmentMO.getProviderId()));
                if (rbacRoleAssignmentMO.getIdentityId() != null) {
                    group.setUniqueIdentifier(rbacRoleAssignmentMO.getIdentityId());
                } else if (rbacRoleAssignmentMO.getIdentityName() != null) {
                    group.setUniqueIdentifier(findIdentityIdByName(Goid.parseGoid(rbacRoleAssignmentMO.getProviderId()), rbacRoleAssignmentMO.getEntityType(), rbacRoleAssignmentMO.getIdentityName()));
                } else {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "Must specify either role assignment identity id or identity name");
                }
                role.addAssignedGroup(group);
            } else {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Unknown role assignment entity type: " + rbacRoleAssignmentMO.getEntityType());
            }
        }
    }

    @Override
    protected void updateEntity(final Role oldEntity, final Role newEntity) throws InvalidResourceException {
        //Note: Do not update the user created flag on the old role. If the old role is not user created updated will not be allowed. See beforeUpdateEntity()
        oldEntity.setName(newEntity.getName());
        oldEntity.setDescription(newEntity.getDescription());
        updateAssignments(oldEntity, newEntity.getRoleAssignments(), true);


        //remove existing assignments
        oldEntity.getPermissions().clear();
        //add new assignments
        for (Permission newPermission : newEntity.getPermissions()) {
            Set<ScopePredicate> newScope = newPermission.getScope();
            if (newScope != null && !newScope.isEmpty()) {
                Permission permission = new Permission(oldEntity, newPermission.getOperation(), newPermission.getEntityType());
                permission.setOtherOperationName(newPermission.getOtherOperationName());
                HashSet<ScopePredicate> scopeSet = new HashSet<>(newScope.size());
                for (ScopePredicate scopePredicate : newScope) {
                    final ScopePredicate predicate;
                    if (scopePredicate instanceof AttributePredicate) {
                        AttributePredicate attributePredicate = (AttributePredicate) scopePredicate;
                        predicate = new AttributePredicate(permission, attributePredicate.getAttribute(), attributePredicate.getValue());
                        ((AttributePredicate) predicate).setMode(attributePredicate.getMode());
                    } else if (scopePredicate instanceof EntityFolderAncestryPredicate) {
                        EntityFolderAncestryPredicate entityFolderAncestryPredicate = (EntityFolderAncestryPredicate) scopePredicate;
                        predicate = new EntityFolderAncestryPredicate(permission, entityFolderAncestryPredicate.getEntityType(), entityFolderAncestryPredicate.getEntityId());
                    } else if (scopePredicate instanceof ObjectIdentityPredicate) {
                        ObjectIdentityPredicate objectIdentityPredicate = (ObjectIdentityPredicate) scopePredicate;
                        predicate = new ObjectIdentityPredicate(permission, objectIdentityPredicate.getTargetEntityId());
                    } else if (scopePredicate instanceof FolderPredicate) {
                        FolderPredicate folderPredicate = (FolderPredicate) scopePredicate;
                        predicate = new FolderPredicate(permission, folderPredicate.getFolder(), folderPredicate.isTransitive());
                    } else if (scopePredicate instanceof SecurityZonePredicate) {
                        SecurityZonePredicate securityZonePredicate = (SecurityZonePredicate) scopePredicate;
                        predicate = new SecurityZonePredicate(permission, securityZonePredicate.getRequiredZone());
                    } else {
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "Unknown Role permission predicate type: " + scopePredicate.getClass().getName());
                    }
                    scopeSet.add(predicate);
                }
                permission.setScope(scopeSet);
                oldEntity.getPermissions().add(permission);
            } else {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "Role permission must supply predicates.");
            }
        }
    }

    private void updateAssignments(Role oldEntity, Set<RoleAssignment> newRoleAssignments, boolean removeOthers) throws InvalidResourceException {
        if (removeOthers) {
            //Note: cannot delete all existing assignments and replace them with the new assignment. It will cause hibernate errors if the same assignments are re-added.
            //Instead remove any assignments not in the new role. And add all assignments not already in the existing role.
            //remove assignments
            HashSet<RoleAssignment> assignmentsToRemove = new HashSet<>();
            for (RoleAssignment oldAssignment : oldEntity.getRoleAssignments()) {
                if (!containsAssignment(newRoleAssignments, oldAssignment)) {
                    assignmentsToRemove.add(oldAssignment);
                    oldEntity.getRoleAssignments().remove(oldAssignment);
                }
            }
            for (RoleAssignment assignment : assignmentsToRemove) {
                oldEntity.getRoleAssignments().remove(assignment);
            }
        }
        //add new assignments
        for (RoleAssignment newAssignment : newRoleAssignments) {
            if (!containsAssignment(oldEntity.getRoleAssignments(), newAssignment)) {
                if ("User".equals(newAssignment.getEntityType())) {
                    UserBean user = new UserBean();
                    user.setProviderId(newAssignment.getProviderId());
                    user.setUniqueIdentifier(newAssignment.getIdentityId());
                    oldEntity.addAssignedUser(user);
                } else if ("Group".equals(newAssignment.getEntityType())) {
                    GroupBean group = new GroupBean();
                    group.setProviderId(newAssignment.getProviderId());
                    group.setUniqueIdentifier(newAssignment.getIdentityId());
                    oldEntity.addAssignedGroup(group);
                } else {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Unknown role assignment entity type: " + newAssignment.getEntityType());
                }
            }
        }
    }

    /**
     * This will search through the set of assignments to see if it contains the given assignment. In this case
     * assignments are equal if their identityId, providerId, and entityTypes's are the same
     *
     * @param roleAssignments The set of assignments to search through
     * @param assignment The assignment to find
     * @return True iff the roleAssignments contatins the assignment to find.
     */
    private boolean containsAssignment(Set<RoleAssignment> roleAssignments, final RoleAssignment assignment) {
        return Functions.exists(roleAssignments, new Functions.Unary<Boolean, RoleAssignment>() {
            @Override
            public Boolean call(RoleAssignment roleAssignment) {
                return roleAssignment.getIdentityId().equals(assignment.getIdentityId()) &&
                        Goid.equals(roleAssignment.getProviderId(), assignment.getProviderId()) &&
                        roleAssignment.getEntityType().equals(assignment.getEntityType());
            }
        });
    }

    /**
     * Enforce that only userCreated roles can be deleted.
     */
    @Override
    protected void beforeDeleteEntity(final EntityBag<Role> roleEntityBag) throws ObjectModelException {
        if (!roleEntityBag.getEntity().isUserCreated()) {
            throw new ConstraintViolationException("Cannot delete gateway managed role.");
        }
    }

    /**
     * Enforce that only user created roles can be updated. And validate that all referenced entities exist.
     */
    @Override
    protected void beforeUpdateEntity(final EntityBag<Role> roleEntityBag) throws ObjectModelException {
        if (!roleEntityBag.getEntity().isUserCreated()) {
            throw new ConstraintViolationException("Cannot update gateway managed role.");
        }
        validateReferencedEntitiesExist(roleEntityBag.getEntity());
    }

    /**
     * Set the userCreated flag to true. And validate that all referenced entities exist.
     */
    @Override
    protected void beforeCreateEntity(final EntityBag<Role> roleEntityBag) throws ObjectModelException {
        //folder the user created flag to be true.
        roleEntityBag.getEntity().setUserCreated(true);
        validateReferencedEntitiesExist(roleEntityBag.getEntity());
    }

    /**
     * Checks the rol to verify that all referenced entities exist.
     * @param role The role to check
     * @throws FindException
     * @throws ConstraintViolationException
     */
    private void validateReferencedEntitiesExist(Role role) throws FindException, ConstraintViolationException {
        if (role.getEntityGoid() != null && role.getEntityType() != null) {
            EntityHeader header = entityFinder.findHeader(role.getEntityType(), role.getEntityGoid());
            if (header == null) {
                throw new FindException("Could not find role referenced entity. Id: " + role.getEntityGoid() + " type: " + role.getEntityType());
            }
        }

        //check assignments
        validateAssignmentsExist(role.getRoleAssignments());
        //check permissions
        validatePermissionsExist(role.getPermissions());
    }

    private void validatePermissionsExist(Set<Permission> permissions) throws ConstraintViolationException, FindException {
        for (Permission permission : permissions) {
            for (ScopePredicate scopePredicate : permission.getScope()) {
                if (scopePredicate instanceof AttributePredicate) {
                    //nothing to check here. It may be possible to check id, but it is unneeded.
                } else if (scopePredicate instanceof EntityFolderAncestryPredicate) {
                    EntityFolderAncestryPredicate entityFolderAncestryPredicate = (EntityFolderAncestryPredicate) scopePredicate;
                    EntityHeader header = entityFinder.findHeader(entityFolderAncestryPredicate.getEntityType(), entityFolderAncestryPredicate.getEntityId());
                    if (header == null) {
                        throw new FindException("Could not find EntityFolderAncestryPredicate referenced entity. Id: " + entityFolderAncestryPredicate.getEntityId() + " type: " + entityFolderAncestryPredicate.getEntityType());
                    }
                } else if (scopePredicate instanceof FolderPredicate) {
                    //nothing to check here. folder will already have been found.
                } else if (scopePredicate instanceof ObjectIdentityPredicate) {
                    ObjectIdentityPredicate objectIdentityPredicate = (ObjectIdentityPredicate) scopePredicate;
                    EntityHeader header = entityFinder.findHeader(permission.getEntityType(), objectIdentityPredicate.getTargetEntityId());
                    if (header == null) {
                        throw new FindException("Could not find ObjectIdentityPredicate referenced entity. Id: " + objectIdentityPredicate.getTargetEntityId() + " type: " + permission.getEntityType());
                    }
                } else if (scopePredicate instanceof SecurityZonePredicate) {
                    //nothing to check here. security zone will already have been found.
                } else {
                    throw new ConstraintViolationException("Unknown Role permission predicate type: " + scopePredicate.getClass().getName());
                }
            }
        }
    }

    private void validateAssignmentsExist(Set<RoleAssignment> roleAssignments) throws FindException {
        for (RoleAssignment assignment : roleAssignments) {
            IdentityProvider provider = identityProviderFactory.getProvider(assignment.getProviderId());
            if (provider == null) {
                throw new FindException("Could not find identity provider. Id: " + assignment.getProviderId());
            }
            final Identity identity;
            if ("User".equals(assignment.getEntityType())) {
                identity = provider.getUserManager().findByPrimaryKey(assignment.getIdentityId());
            } else if ("Group".equals(assignment.getEntityType())) {
                identity = provider.getGroupManager().findByPrimaryKey(assignment.getIdentityId());
            } else {
                throw new UnsupportedEntityTypeException("Unknown role assignment entity type: " + assignment.getEntityType());
            }
            if (identity == null) {
                throw new FindException("Could not find identity. Identity provider id: " + assignment.getProviderId() + " Id: " + assignment.getIdentityId());
            }
        }
    }

    private String findIdentityIdByName(Goid providerId, String entityType, String identityName) throws FindException {
        IdentityProvider provider = identityProviderFactory.getProvider(providerId);
        if (provider == null) {
            throw new FindException("Could not find identity provider. Id: " + providerId);
        }
        final Identity identity;
        if ("User".equals(entityType)) {
            identity = provider.getUserManager().findByLogin(identityName);
        } else if ("Group".equals(entityType)) {
            identity = provider.getGroupManager().findByName(identityName);
        } else {
            throw new UnsupportedEntityTypeException("Unknown role assignment entity type: " + entityType);
        }
        if (identity == null) {
            throw new FindException("Could not find identity. Identity provider id: " + providerId + " Name: " + identityName + " Type: " + entityType);
        }
        return identity.getId();
    }
}
