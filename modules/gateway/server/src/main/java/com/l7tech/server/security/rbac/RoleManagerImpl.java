/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
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
public class RoleManagerImpl extends HibernateEntityManager<Role, EntityHeader> implements RoleManager, RbacServices {
    private static final Logger logger = Logger.getLogger(RoleManagerImpl.class.getName());

    private RbacServices rbacServices;

    private static RoleManagerIdentitySource groupProvider = new RoleManagerIdentitySource(){
        @Override
        public void validateRoleAssignments() throws UpdateException {}
        @Override
        public Set<IdentityHeader> getGroups(User user) throws FindException {
            return Collections.emptySet();
        }
    };

    public static void setIdentitySource(RoleManagerIdentitySource groupProvider) {
        RoleManagerImpl.groupProvider = groupProvider;
    }

    @Override
    public Class<Role> getImpClass() {
        return Role.class;
    }

    @Override
    public Class<Role> getInterfaceClass() {
        return Role.class;
    }

    @Override
    public String getTableName() {
        return "rbac_role";
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<Role> getAssignedRoles(final User user) throws FindException {
        final Set<IdentityHeader> groupHeaders = groupProvider.getGroups(user);

        //noinspection unchecked
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
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
                List<String> groupNames = new ArrayList<String>();
                for( IdentityHeader groupHeader : groupHeaders ){
                    if ( groupHeader != null && groupHeader.getProviderOid()==user.getProviderId() ) {
                        groupNames.add( groupHeader.getStrId() );
                    }
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

    @Override
    @Transactional(readOnly=true)
    public boolean isPermittedForEntitiesOfTypes(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final Set<EntityType> requiredTypes)
            throws FindException
    {
        return rbacServices.isPermittedForEntitiesOfTypes(authenticatedUser, requiredOperation, requiredTypes);
    }


    @Override
    @Transactional(readOnly=true)
    public boolean isPermittedForAnyEntityOfType(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final EntityType requiredType)
            throws FindException
    {
        return rbacServices.isPermittedForAnyEntityOfType(authenticatedUser, requiredOperation, requiredType);
    }

    @Override
    public void update(Role role) throws UpdateException {
        // Quick pre-check for admin role assignment
        if (role.getTag() == Role.Tag.ADMIN && role.getRoleAssignments().isEmpty())
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
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.FINE, "Find error when merging assignments for role", ExceptionUtils.getDebugException(fe));
            }
        }

        super.update(role);

        // full role check (will ensure there is a non-expired user in group, etc)
        validateRoleAssignments();
    }

    @Override
    @Transactional(readOnly=true)
    public void validateRoleAssignments() throws UpdateException {
        try {
            groupProvider.validateRoleAssignments();
        } catch ( UpdateException ue ) {
            throw new UpdateException(ADMIN_REQUIRED, ue);            
        }
    }

    @Override
    @Transactional(readOnly=true)
    public boolean isPermittedForEntity(User user, Entity entity, OperationType operation, String otherOperationName) throws FindException {
        return rbacServices.isPermittedForEntity(user, entity, operation, otherOperationName);
    }

    @Override
    @Transactional(readOnly=true)
    public Role findByTag(final Role.Tag tag) throws FindException {
        if ( tag == null ) throw new IllegalArgumentException("tag must not be null");

        Role role = null;
        Collection<Role> roles = this.findByPropertyMaybeNull("tag", tag);
        if ( roles.size() > 1 ) {
            throw new  FindException("Found multiple matching roles for tag '"+tag+"'.");
        } else if ( !roles.isEmpty() ) {
            role = roles.iterator().next();    
        }
        return role;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    @Transactional(readOnly=true)
    public Collection<Role> findEntitySpecificRoles(final EntityType etype, final long entityId) throws FindException {
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Role.class);
                crit.add(Restrictions.eq("entityTypeName", etype.name()));
                crit.add(Restrictions.eq("entityOid", entityId));
                return crit.list();
            }
        });
    }

    @Override
    public void deleteEntitySpecificRoles(EntityType etype, final long entityOid) throws DeleteException {
        try {
            Collection<Role> roles = findEntitySpecificRoles(etype, entityOid);
            if (roles == null) return;
            for ( Role role : roles ) {
                logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
                delete(role);
            }
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }
    }

    @Override
    public void renameEntitySpecificRoles(EntityType entityType, NamedEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
        Collection<Role> roles = findEntitySpecificRoles(entityType, entity.getOid());
        if (roles == null) {
            logger.warning(MessageFormat.format("No entity-specific role was found for {0} ''{1}'' (#{2})", entity.getName(), entityType.getName(), entity.getOid()));
            return;
        }
        for ( Role role : roles ) {
            String name = role.getName();
            Matcher matcher = replacePattern.matcher(name);
            String newName = matcher.replaceAll(entity.getName().replace("\\", "\\\\").replace("$", "\\$"));
            if (!newName.equals(name)) {
                logger.info(MessageFormat.format("Updating ''{0}'' Role with new name: ''{1}''", role.getName(), newName));
                role.setName(newName);
                update(role);
            }
        }
    }

    @Transactional(readOnly=true)
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


    @Override
    @Transactional(readOnly=true)
    public <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser,
                                                                       OperationType requiredOperation,
                                                                       Iterable<T> headers,
                                                                       EntityFinder entityFinder)
            throws FindException
    {
        return rbacServices.filterPermittedHeaders(authenticatedUser, requiredOperation, headers, entityFinder);
    }

    @Override
    @SuppressWarnings({ "ThrowInsideCatchBlockWhichIgnoresCaughtException" })
    public void deleteRoleAssignmentsForUser(final User user) throws DeleteException {
        try {
            Collection<Role> roles = getAssignedRoles(user);
            for (Role role : roles) {
                role.removeAssignedUser(user);
                update(role);
            }
        } catch (FindException fe) {
            logger.log(Level.INFO, "Failed to find assigned roles for user '" + user.getLogin() + "' for provider " + user.getProviderId());
            throw new DeleteException("Failed to delete role assignment for user '" + user.getLogin() +"'", fe.getCause());
        } catch (UpdateException ue) {
            logger.log(Level.INFO, "Failed to update role for assigned roles deletion");
            throw new DeleteException("Failed to delete role assignment for user '" + user.getLogin() +"'", ue.getCause());
        }
    }

    public void setRbacServices(RbacServicesImpl rbacServices) {
        this.rbacServices = rbacServices;
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();
        if (rbacServices == null) throw new IllegalStateException("rbacServices component is missing");
    }
}
