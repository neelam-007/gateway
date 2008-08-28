/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.EntityType;
import static com.l7tech.gateway.common.security.rbac.EntityType.ANY;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.policy.PolicyManager;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor = Throwable.class)
public class RoleManagerImpl
        extends HibernateEntityManager<Role, EntityHeader>
        implements RoleManager
{
    private static final Logger logger = Logger.getLogger(RoleManagerImpl.class.getName());

    private PolicyManager policyManager;

    public void setPolicyManager( PolicyManager policyManager){
        this.policyManager = policyManager;
    }

    public Class<Role> getImpClass() {
        return Role.class;
    }

    public Class<Role> getInterfaceClass() {
        return Role.class;
    }

    public String getTableName() {
        return "rbac_role";
    }

    @Transactional(readOnly=true)
    public Collection<Role> getAssignedRoles(final User user) throws FindException {
        //noinspection unchecked
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                //Get the User's directly assigned Role's
                Set<Role> roles = new HashSet<Role>();
                Criteria userAssignmentQuery = session.createCriteria(RoleAssignment.class);
                userAssignmentQuery.add(Restrictions.eq("identityId", user.getId()));
                userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                userAssignmentQuery.add(Restrictions.eq("entityType", EntityType.USER.getName()));
                List uras = userAssignmentQuery.list();
                //(hibernate results aren't generic)
                //noinspection ForLoopReplaceableByForEach
                for (Iterator i = uras.iterator(); i.hasNext();) {
                    RoleAssignment ra = (RoleAssignment) i.next();
                    roles.add(ra.getRole());
                }

                //Now get the Roles the user can access via it's group membership
                Set<IdentityHeader> iHeaders = JaasUtils.getCurrentUserGroupInfo();
                List<String> groupNames = new ArrayList<String>();
                for(IdentityHeader iH: iHeaders){
                    groupNames.add(iH.getStrId());                
                }
                if(groupNames.size() == 0) return roles;
                
                Criteria groupQuery = session.createCriteria(RoleAssignment.class);
                groupQuery.add(Restrictions.in("identityId", groupNames));
                groupQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                groupQuery.add(Restrictions.eq("entityType", EntityType.GROUP.getName()));
                List gList = groupQuery.list();

                for (Object aGList : gList) {
                    RoleAssignment ra = (RoleAssignment) aGList;
                    roles.add(ra.getRole());
                }

                return roles;
            }
        });
    }

    @Transactional(readOnly=true)
    public boolean isPermittedForEntitiesOfTypes(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final Set<EntityType> requiredTypes)
            throws FindException
    {
        if (authenticatedUser == null) throw new IllegalArgumentException();
        if (requiredTypes == null || requiredTypes.isEmpty()) throw new IllegalArgumentException();
        if (requiredOperation == null || !OperationType.ALL_CRUD.contains(requiredOperation)) throw new IllegalArgumentException();

        final Map<EntityType, Boolean> permittedTypes = new HashMap<EntityType, Boolean>();

        for (Role role : getAssignedRoles(authenticatedUser)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != requiredOperation) continue; // This permission is for a different operation
                com.l7tech.gateway.common.security.rbac.EntityType ptype = perm.getEntityType();

                if (ptype == ANY) return true; // Permitted against all types
                permittedTypes.put(ptype, true); // Permitted for this type
            }
        }

        if (permittedTypes.isEmpty()) return false; // Not permitted on any type

        for (EntityType requiredType : requiredTypes) {
            Boolean permittedType = permittedTypes.get(requiredType);
            if (permittedType == null) return false; // Required type is not permitted
        }

        return true;
    }


    @Transactional(readOnly=true)
    public boolean isPermittedForAnyEntityOfType(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final EntityType requiredType)
            throws FindException
    {
        if (authenticatedUser == null || requiredType == null) throw new NullPointerException();
        logger.log(Level.FINE, "Checking for permission to {0} any {1}", new Object[] { requiredOperation.getName(), requiredType.getName()});
        for (Role role : getAssignedRoles(authenticatedUser)) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getScope() != null && !perm.getScope().isEmpty()) continue; // This permission is too restrictive
                if (perm.getOperation() != requiredOperation) continue; // This permission is for a different operation
                com.l7tech.gateway.common.security.rbac.EntityType ptype = perm.getEntityType();
                if (ptype == ANY || ptype == requiredType) return true;
            }
        }
        return false;
    }

    // TODO check whether any of the assigned users is internal?
    @Override
    public void update(Role role) throws UpdateException {
        if (role.getOid() == Role.ADMIN_ROLE_OID && role.getRoleAssignments().isEmpty())
            throw new UpdateException(RoleManager.ADMIN_REQUIRED);

        // Merge in OIDs for any known user assignments (See bug 4176)
        boolean needsOidMerge = false;
        for ( RoleAssignment ura : role.getRoleAssignments() ) {
            if ( ura.getOid() == RoleAssignment.DEFAULT_OID ) {
                needsOidMerge = true;
                break;
            }
        }

        if ( needsOidMerge ) {
            try {
                Role persistedRole = findByPrimaryKey(role.getOid());
                Set<RoleAssignment> previousAssignments = persistedRole.getRoleAssignments();

                for ( RoleAssignment ura : role.getRoleAssignments() ) {
                    if ( ura.getOid() == RoleAssignment.DEFAULT_OID ) {
                        ura.setOid(getOidForAssignment(previousAssignments, ura));
                    }
                }
            } catch (FindException fe) {
                // fail on update below
                logger.log(Level.FINE, "Find error when merging assignments for role", ExceptionUtils.getDebugException(fe));
            }
        }

        super.update(role);
    }

    @Transactional(readOnly=true)
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException {
        if (user == null || entity == null || operation == null) throw new NullPointerException();
        if (operation == OperationType.OTHER && otherOperationName == null) throw new IllegalArgumentException("otherOperationName must be specified when operation == OTHER");
        logger.log(Level.FINE, "Checking for permission to {0} {1} #{2}", new Object[] { operation.getName(), entity.getClass().getSimpleName(), entity.getId()});

        Collection<Role> assignedRoles = getAssignedRoles(user);
        for (Role role : assignedRoles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.matches(entity) && perm.getOperation() == operation) {
                    if (operation != OperationType.OTHER && operation != OperationType.NONE) {
                        return true;
                    } else {
                        if (otherOperationName.equals(perm.getOtherOperationName())) return true;
                    }
                }
            }
        }
        return false;
    }


    public Role findEntitySpecificRole(PermissionMatchCallback callback) throws FindException {
        for (Role role : findAll()) {
            boolean match = !role.getPermissions().isEmpty();
            for (Permission perm : role.getPermissions()) {
                match = match && callback.matches(perm);
                if (!match) break;
            }

            if (match) return role;
        }

        return null;
    }

    public Role findEntitySpecificRole(final EntityType etype, final long entityId) throws FindException {
        return (Role) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Role.class);
                crit.add(Restrictions.eq("entityTypeName", etype.name()));
                crit.add(Restrictions.eq("entityOid", entityId));
                return crit.uniqueResult();
            }
        });
    }

    public void deleteEntitySpecificRole(PermissionMatchCallback callback) throws DeleteException {
        try {
            Role role = findEntitySpecificRole(callback);
            if (role == null) return;
            logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
            delete(role);
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }
    }

    public void deleteEntitySpecificRole(EntityType etype, final long entityOid) throws DeleteException {
        try {
            Role role = findEntitySpecificRole(etype, entityOid);
            if (role == null) return;
            logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
            delete(role);
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }
    }

    public void renameEntitySpecificRole(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
        Role role = findEntitySpecificRole(entityType, entity.getOid());
        if (role == null) {
            logger.warning(MessageFormat.format("No entity-specific role was found for {0} ''{1}'' (#{2})", entity.getName(), entityType.getName(), entity.getOid()));
            return;
        }
        String name = role.getName();
        Matcher matcher = replacePattern.matcher(name);
        String newName = matcher.replaceAll(entity.getName().replace("\\", "\\\\").replace("$", "\\$"));
        if (!newName.equals(name)) {
            logger.info(MessageFormat.format("Updating ''{0}'' Role with new name: ''{1}''", role.getName(), newName));
            role.setName(newName);
            update(role);
        }
    }

    private long getOidForAssignment(Set<RoleAssignment> roleAssignments, RoleAssignment assignment) {
        long oid = RoleAssignment.DEFAULT_OID;

        for ( RoleAssignment ura : roleAssignments) {
            if ( ura.getProviderId()==assignment.getProviderId() &&
                 ura.getIdentityId().equals(assignment.getIdentityId())  ) {
                oid = ura.getOid();
                break;
            }
        }

        return oid;
    }

    private boolean isPermitted(Collection<Role> assignedRoles, Entity entity, OperationType operation, String otherOperationName) {
        for (Role role : assignedRoles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.matches(entity) && perm.getOperation() == operation) {
                    if (operation != OperationType.OTHER && operation != OperationType.NONE) {
                        return true;
                    } else {
                        if (otherOperationName.equals(perm.getOtherOperationName())) return true;
                    }
                }
            }
        }
        return false;
    }

    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser,
                                                                       OperationType requiredOperation,
                                                                       Iterable<T> headers,
                                                                       EntityFinder entityFinder)
            throws FindException
    {
        if (authenticatedUser == null) throw new IllegalArgumentException();
        if (requiredOperation == null || !OperationType.ALL_CRUD.contains(requiredOperation)) throw new IllegalArgumentException();

        // If we already have blanket permission for this type, just return the original collection
        // however as the SSM now shows services and policies together, blanket only applies if the user has it on
        // all entites which can be shown in the tree
        if (isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, EntityType.SERVICE)
                && isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, EntityType.POLICY)) return headers;

        // Do this outside the loop so performance isn't appalling
        final Collection<Role> userRoles = getAssignedRoles(authenticatedUser);
        if (userRoles.isEmpty()) return Collections.emptyList();

        final List<T> result = new LinkedList<T>();
        for (final T header : headers) {
            final Entity entity;
            try {
                if(header.getType() == com.l7tech.objectmodel.EntityType.POLICY) {
                    entity = policyManager==null ?  null : policyManager.findByGuid(header.getStrId());
                }else{
                    entity = entityFinder.find(header);
                }
                if (entity == null) continue;
            } catch (FindException e) {
                logger.log(Level.WARNING, MessageFormat.format("Unable to find entity for header: {0}; skipping", header), e);
                continue;
            }

            //check for alias
            if(header.isAlias()){
                if(!(entity instanceof Aliasable)){
                    //As T is an OrganizationHeader this implies that any entity found implements Alisable
                    throw new IllegalStateException("Any organizable entity must be alisable");
                }
                Aliasable a = (Aliasable) entity;
                a.setAlias(true);
            }

            if (isPermitted(userRoles, entity, requiredOperation, null))
                result.add(header);
        }

        return result;
    }
}
