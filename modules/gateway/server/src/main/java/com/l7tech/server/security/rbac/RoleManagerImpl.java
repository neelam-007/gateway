package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
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
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(RoleManagerImpl.class.getName());
    private static final String IDENTITY_ID = "identityId";
    private static final String PROVIDER_ID = "providerId";
    private static final String ENTITY_TYPE = "entityType";

    private RbacServices rbacServices;

    private static final String HQL_FIND_ALL_SECURITY_ZONE_PREDICATES_REFERENCING_SECURITY_ZONE_ID =
        "from rbac_predicate_security_zone" +
            " in class " + SecurityZonePredicate.class.getName() +
            " where rbac_predicate_security_zone.requiredZone.goid = ?";

    private static final String HQL_FIND_ALL_FOLDER_PREDICATES_REFERENCING_FOLDER_ID =
        "from rbac_predicate_folder" +
            " in class " + FolderPredicate.class.getName() +
            " where rbac_predicate_folder.folder.goid = ?";

    // Finds scopes with this entity OID for ALL entity types; result will need to be further filtered
    // based on the owning Permission's entityType.
    private static final String HQL_FIND_ALL_OBJECT_IDENTITY_PREDICATES_REFERENCING_ENTITY_ID =
        "from rbac_predicate_oid" +
            " in class " + ObjectIdentityPredicate.class.getName() +
            " where rbac_predicate_oid.targetEntityId = ?";

    private static final String HQL_FIND_ALL_FOLDER_ANCESTRY_PREDICATES_REFERENCING_ENTITY_ID_AND_TYPE =
        "from rbac_predicate_entityfolder" +
            " in class " + EntityFolderAncestryPredicate.class.getName() +
            " where rbac_predicate_entityfolder.entityId = ?" +
            " and rbac_predicate_entityfolder.entityType = ?";

    private static RoleManagerIdentitySource groupProvider = new RoleManagerIdentitySource(){
        @Override
        public void validateRoleAssignments() throws UpdateException {}
        @Override
        public Set<IdentityHeader> getGroups(User user, boolean skipAccountValidation) throws FindException {
            return Collections.emptySet();
        }

        @Override
        public Set<IdentityHeader> getGroups(@NotNull final Group group) throws FindException {
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
        return getAssignedRoles0(user, false ,false);
    }

    @Override
    public Collection<Role> getAssignedRoles(@NotNull final Group group) throws FindException {
        final Collection<Role> roles = new ArrayList<>(getDirectlyAssignedRolesForGroup(group.getProviderId(), group.getId(), false));
        final Set<IdentityHeader> groups = groupProvider.getGroups(group);
        for (final IdentityHeader header : groups) {
            roles.addAll(getDirectlyAssignedRolesForGroup(header.getProviderGoid(), header.getStrId(), true));
        }
        return roles;
    }

    @Override
    public Collection<Role> getAssignedRoles( final User user,
                                              final boolean skipAccountValidation,
                                              final boolean throwOnGroupDisabled ) throws FindException {
        return getAssignedRoles0(user, skipAccountValidation, throwOnGroupDisabled);
    }

    @Override
    public Collection<Pair<Goid, String>> getExplicitRoleAssignments(){
        //noinspection unchecked
        return (Collection<Pair<Goid, String>>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                final List list = session.createQuery("select distinct r.identityId, r.providerId from " +
                        "com.l7tech.gateway.common.security.rbac.RoleAssignment r where r.entityType = 'User'").list();

                final Collection<Pair<Goid, String>> roleAssignedUsers = new ArrayList<Pair<Goid, String>>();
                for (Object o : list) {
                    if(o instanceof Object[]){
                        Object [] result = (Object[]) o;
                        if(result.length != 2) throw new IllegalStateException("Incorrect number of columns found.");

                        final Pair<Goid, String> pair = new Pair<Goid, String>(Goid.parseGoid(result[1].toString()), result[0].toString());
                        roleAssignedUsers.add(pair);
                    }
                }
                return roleAssignedUsers;
            }
        });
    }

    private Collection<Role> getDirectlyAssignedRolesForGroup(final Goid providerId, final String groupId, final boolean inherited) throws FindException {
       try {
           //noinspection unchecked
           return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
               @Override
               protected Collection<Role> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                   final Set<Role> roles = new HashSet<>();
                   final Criteria criteria = session.createCriteria(RoleAssignment.class);
                   criteria.add(Restrictions.eq(IDENTITY_ID, groupId));
                   criteria.add(Restrictions.eq(PROVIDER_ID, providerId));
                   criteria.add(Restrictions.eq(ENTITY_TYPE, EntityType.GROUP.getName()));
                   final List<RoleAssignment> roleAssignments = (List<RoleAssignment>) criteria.list();
                   for (final RoleAssignment assignment : roleAssignments) {
                       assignment.setInherited(inherited);
                       roles.add(assignment.getRole());
                   }
                   return roles;
               }
           });
       } catch (final Exception e) {
           throw new FindException("Error retrieving roles for group", e);
       }
   }

    private Collection<Role> getAssignedRoles0( final User user,
                                                final boolean skipAccountValidation,
                                                final boolean throwOnGroupDisabled ) throws FindException {
        final Set<IdentityHeader> groupHeaders = groupProvider.getGroups(user, skipAccountValidation);

        final Either<String,Collection<Role>> result =
                getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Either<String,Collection<Role>>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            public Either<String,Collection<Role>> doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                //Get the User's directly assigned Role's
                final Set<Role> roles = new HashSet<Role>();
                final Criteria userAssignmentQuery = session.createCriteria(RoleAssignment.class);
                userAssignmentQuery.add(Restrictions.eq("identityId", user.getId()));
                userAssignmentQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                userAssignmentQuery.add(Restrictions.eq("entityType", EntityType.USER.getName()));
                final List<RoleAssignment> uras = (List<RoleAssignment>) userAssignmentQuery.list();
                for ( final RoleAssignment ra : uras ) {
                    roles.add( ra.getRole() );
                }

                //Now get the Roles the user can access via it's group membership
                final List<String> groupIds = new ArrayList<String>();
                for( final IdentityHeader groupHeader : groupHeaders ){
                    if ( groupHeader != null && groupHeader.getProviderGoid().equals(user.getProviderId())) {
                        groupIds.add( groupHeader.getStrId() );
                    }
                }
                if(groupIds.size() == 0) return Either.<String,Collection<Role>>right( roles );

                final Set<String> disabledGroupsWithRoles = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                final Criteria groupQuery = session.createCriteria(RoleAssignment.class);
                groupQuery.add(Restrictions.in("identityId", groupIds));
                groupQuery.add(Restrictions.eq("providerId", user.getProviderId()));
                groupQuery.add(Restrictions.eq("entityType", EntityType.GROUP.getName()));
                final List<RoleAssignment> gList = (List<RoleAssignment>) groupQuery.list();
                for ( final RoleAssignment ra : gList) {
                    ra.setInherited(true);
                    IdentityHeader header = null;
                    for ( final IdentityHeader groupHeader : groupHeaders ) {
                        if ( ra.getIdentityId()!=null && ra.getIdentityId().equals( groupHeader.getStrId() )  ) {
                            header = groupHeader;
                        }
                    }
                    if ( header != null ) {
                        if ( header.isEnabled() ) {
                            roles.add(ra.getRole());
                        } else {
                            disabledGroupsWithRoles.add( header.getName() );
                        }
                    }
                }

                if ( throwOnGroupDisabled && roles.isEmpty() && !disabledGroupsWithRoles.isEmpty() ) {
                    return Either.left( "No permissions due to disabled groups " + disabledGroupsWithRoles );
                } else {
                    return Either.right( (Collection<Role>)roles );
                }
            }
        });

        return result.either( new Functions.UnaryThrows<Collection<Role>,String,FindException>(){
            @Override
            public Collection<Role> call( final String message ) throws FindException {
                throw new DisabledGroupRolesException( message );
            }
        }, new Functions.UnaryThrows<Collection<Role>,Collection<Role>,FindException>(){
            @Override
            public Collection<Role> call( final Collection<Role> roles ) throws FindException {
                return roles;
            }
        } );
    }

    private Collection<Role> getAssignedGroup(final Group group) throws FindException {

        //noinspection unchecked
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Set<Role> roles = new HashSet<Role>();

                // get roles from groups
                Criteria groupQuery = session.createCriteria(RoleAssignment.class);
                groupQuery.add(Restrictions.eq("identityId", group.getId()));
                groupQuery.add(Restrictions.eq("providerId", group.getProviderId()));
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
    @Transactional(readOnly=true)
    public boolean isPermittedForSomeEntityOfType(final User authenticatedUser,
                                                 final OperationType requiredOperation,
                                                 final EntityType requiredType)
            throws FindException
    {
        return rbacServices.isPermittedForSomeEntityOfType(authenticatedUser, requiredOperation, requiredType);
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
            throw new RuleViolationUpdateException(ADMIN_REQUIRED, ue);
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
    @Deprecated
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

    @SuppressWarnings({"unchecked"})
    @Override
    @Transactional(readOnly=true)
    public Collection<Role> findEntitySpecificRoles(final EntityType etype, final Goid entityId) throws FindException {
        return (Collection<Role>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Role.class);
                crit.add(Restrictions.eq("entityTypeName", etype.name()));
                crit.add(Restrictions.eq("entityGoid", entityId));
                return crit.list();
            }
        });
    }

    @Override
    @Deprecated
    public void deleteEntitySpecificRoles(EntityType etype, final long entityOid) throws DeleteException {
        try {
            Collection<Role> roles = findEntitySpecificRoles(etype, entityOid);
            if (roles == null) return;
            for ( Role role : roles ) {
                logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
                delete(role);
            }

            deleteEntitySpecificPermissions(new HashSet<Role>(roles), etype, entityOid);
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }

    }

    @Override
    public void deleteEntitySpecificRoles(EntityType etype, final Goid entityGoid) throws DeleteException {
        try {
            Collection<Role> roles = findEntitySpecificRoles(etype, entityGoid);
            if (roles == null) return;
            for ( Role role : roles ) {
                logger.info("Deleting obsolete Role #" + role.getOid() + " (" + role.getName() + ")");
                delete(role);
            }

            deleteEntitySpecificPermissions(new HashSet<Role>(roles), etype, entityGoid);
        } catch (FindException e) {
            throw new DeleteException("Couldn't find Roles for this Entity", e);
        }

    }

    // Scan all roles for permissions with scopes that will never again match if the specified entity is deleted, and remove
    // the affected permissions from their owning roles (leaving the roles behind, possibly with no permissions;
    // note that auto-created roles will have been already deleted by this point so only custom roles may be left in this state.)
    @Deprecated
    private void deleteEntitySpecificPermissions(Set<Role> rolesAlreadyDeleted, EntityType etype, final long entityOid) throws DeleteException {
        Set<Permission> permissionsToDelete = new HashSet<Permission>();

        permissionsToDelete.addAll(findObjectIdentityPredicatePermissionsForEntity(etype, entityOid));
        permissionsToDelete.addAll(findEntityFolderAncestryPredicatePermissionsForEntity(etype, entityOid));
        if (EntityType.FOLDER.equals(etype)) {
            throw new UnsupportedOperationException("Folder entities are no longer supported here");
        }
        if (EntityType.SECURITY_ZONE.equals(etype)) {
            throw new UnsupportedOperationException("SecurityZone entities are no longer supported here");
        }

        Set<Role> rolesToUpdate = new HashSet<Role>();
        for (Permission permission : permissionsToDelete) {
            final Role role = permission.getRole();
            if (!rolesAlreadyDeleted.contains(role)) {
                role.getPermissions().remove(permission);
                rolesToUpdate.add(role);
            }
        }

        for (Role role : rolesToUpdate) {
            try {
                logger.info("Removing obsolete permissions from Role #" + role.getOid() + " (" + role.getName() + ")");
                update(role);
            } catch (UpdateException e) {
                logger.log(Level.SEVERE, "Unable to remove obsolete permissions from Role #" + role.getOid() + " (" + role.getName() + "): " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    // Scan all roles for permissions with scopes that will never again match if the specified entity is deleted, and remove
    // the affected permissions from their owning roles (leaving the roles behind, possibly with no permissions;
    // note that auto-created roles will have been already deleted by this point so only custom roles may be left in this state.)
    private void deleteEntitySpecificPermissions(Set<Role> rolesAlreadyDeleted, EntityType etype, final Goid entityGoid) throws DeleteException {
        Set<Permission> permissionsToDelete = new HashSet<Permission>();

        permissionsToDelete.addAll(findObjectIdentityPredicatePermissionsForEntity(etype, entityGoid));
        permissionsToDelete.addAll(findEntityFolderAncestryPredicatePermissionsForEntity(etype, entityGoid));
        if (EntityType.FOLDER.equals(etype)) {
            permissionsToDelete.addAll(findFolderPredicatePermissionsForFolder(entityGoid));
        }
        if (EntityType.SECURITY_ZONE.equals(etype)) {
            permissionsToDelete.addAll(findSecurityZonePredicatePermissionsForSecurityZone(entityGoid));
        }

        Set<Role> rolesToUpdate = new HashSet<Role>();
        for (Permission permission : permissionsToDelete) {
            final Role role = permission.getRole();
            if (!rolesAlreadyDeleted.contains(role)) {
                role.getPermissions().remove(permission);
                rolesToUpdate.add(role);
            }
        }

        for (Role role : rolesToUpdate) {
            try {
                logger.info("Removing obsolete permissions from Role #" + role.getOid() + " (" + role.getName() + ")");
                update(role);
            } catch (UpdateException e) {
                logger.log(Level.SEVERE, "Unable to remove obsolete permissions from Role #" + role.getOid() + " (" + role.getName() + "): " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    @Deprecated
    private Set<Permission> findObjectIdentityPredicatePermissionsForEntity(EntityType etype, long entityOid) {
        Set<Permission> ret = new HashSet<Permission>();

        List predicates = getHibernateTemplate().find(HQL_FIND_ALL_OBJECT_IDENTITY_PREDICATES_REFERENCING_ENTITY_ID, String.valueOf(entityOid));
        for (Object predicate : predicates) {
            if (!(predicate instanceof ObjectIdentityPredicate))
                throw new HibernateException("Got unexpected return value type of " + predicate.getClass() + " while finding object identity predicates by entity oid");

            ObjectIdentityPredicate oip = (ObjectIdentityPredicate) predicate;
            if (etype.equals(oip.getPermission().getEntityType())) {
                ret.add(oip.getPermission());
            }
        }

        return ret;
    }

    private Set<Permission> findObjectIdentityPredicatePermissionsForEntity(EntityType etype, Goid entityGoid) {
        Set<Permission> ret = new HashSet<Permission>();

        List predicates = getHibernateTemplate().find(HQL_FIND_ALL_OBJECT_IDENTITY_PREDICATES_REFERENCING_ENTITY_ID, entityGoid.toHexString());
        for (Object predicate : predicates) {
            if (!(predicate instanceof ObjectIdentityPredicate))
                throw new HibernateException("Got unexpected return value type of " + predicate.getClass() + " while finding object identity predicates by entity goid");

            ObjectIdentityPredicate oip = (ObjectIdentityPredicate) predicate;
            if (etype.equals(oip.getPermission().getEntityType())) {
                ret.add(oip.getPermission());
            }
        }

        return ret;
    }

    @Deprecated
    private Set<Permission> findEntityFolderAncestryPredicatePermissionsForEntity(EntityType etype, long entityOid) {
        Set<Permission> ret = new HashSet<Permission>();

        List predicates = getHibernateTemplate().find(HQL_FIND_ALL_FOLDER_ANCESTRY_PREDICATES_REFERENCING_ENTITY_ID_AND_TYPE, String.valueOf(entityOid), etype);
        for (Object predicate : predicates) {
            if (!(predicate instanceof EntityFolderAncestryPredicate))
                throw new HibernateException("Got unexpected return value type of " + predicate.getClass() + " while finding entity folder ancestry predicates by entity oid and type");

            EntityFolderAncestryPredicate efap = (EntityFolderAncestryPredicate) predicate;
            ret.add(efap.getPermission());
        }

        return ret;
    }

    private Set<Permission> findEntityFolderAncestryPredicatePermissionsForEntity(EntityType etype, Goid entityGoid) {
        Set<Permission> ret = new HashSet<Permission>();

        List predicates = getHibernateTemplate().find(HQL_FIND_ALL_FOLDER_ANCESTRY_PREDICATES_REFERENCING_ENTITY_ID_AND_TYPE, entityGoid.toHexString(), etype);
        for (Object predicate : predicates) {
            if (!(predicate instanceof EntityFolderAncestryPredicate))
                throw new HibernateException("Got unexpected return value type of " + predicate.getClass() + " while finding entity folder ancestry predicates by entity goid and type");

            EntityFolderAncestryPredicate efap = (EntityFolderAncestryPredicate) predicate;
            ret.add(efap.getPermission());
        }

        return ret;
    }

    private Set<Permission> findFolderPredicatePermissionsForFolder(Goid folderGoid) {
        Set<Permission> ret = new HashSet<Permission>();

        List predicates = getHibernateTemplate().find(HQL_FIND_ALL_FOLDER_PREDICATES_REFERENCING_FOLDER_ID, folderGoid);
        for (Object predicate : predicates) {
            if (!(predicate instanceof FolderPredicate))
                throw new HibernateException("Got unexpected return value type of " + predicate.getClass() + " while finding folder predicates by folder oid");

            FolderPredicate fp = (FolderPredicate) predicate;
            ret.add(fp.getPermission());
        }

        return ret;
    }

    private Set<Permission> findSecurityZonePredicatePermissionsForSecurityZone(Goid securtiyZoneGoid) {
        Set<Permission> ret = new HashSet<Permission>();

        List predicates = getHibernateTemplate().find(HQL_FIND_ALL_SECURITY_ZONE_PREDICATES_REFERENCING_SECURITY_ZONE_ID, securtiyZoneGoid);
        for (Object predicate : predicates) {
            if (!(predicate instanceof ScopePredicate))
                throw new HibernateException("Got unexpected return value type of " + predicate.getClass() + " while finding folder predicates by security zone oid");

            ScopePredicate sp = (ScopePredicate) predicate;
            ret.add(sp.getPermission());
        }

        return ret;
    }

    @Override
    @Deprecated
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

    @Override
    public void renameEntitySpecificRoles(EntityType entityType, NamedGoidEntityImp entity, Pattern replacePattern) throws FindException, UpdateException {
        Collection<Role> roles = findEntitySpecificRoles(entityType, entity.getGoid());
        if (roles == null) {
            logger.warning(MessageFormat.format("No entity-specific role was found for {0} ''{1}'' (#{2})", entity.getName(), entityType.getName(), entity.getGoid()));
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
            if ( ura.getProviderId().equals(assignment.getProviderId()) &&
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
    public boolean isAdministrativeUser(@NotNull Pair<Goid, String> providerAndUserId, @NotNull User user) throws FindException {
        return rbacServices.isAdministrativeUser(providerAndUserId, user);
    }

    @Override
    public Collection<Role> getAssignedRoles(@NotNull Pair<Goid, String> providerAndUserId, @NotNull User user) throws FindException {
        return rbacServices.getAssignedRoles(providerAndUserId, user);
    }

    @Override
    @SuppressWarnings({ "ThrowInsideCatchBlockWhichIgnoresCaughtException" })
    public void deleteRoleAssignmentsForUser(final User user) throws DeleteException {
        try {
            Collection<Role> roles = getAssignedRoles(user, true, false);
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

    @Override
    @SuppressWarnings({ "ThrowInsideCatchBlockWhichIgnoresCaughtException" })
    public void deleteRoleAssignmentsForGroup(final Group group) throws DeleteException {
        try {
            Collection<Role> roles = getAssignedGroup(group);
            for (Role role : roles) {
                role.removeAssignedGroup(group);
                update(role);
            }
        } catch (FindException fe) {
            logger.log(Level.INFO, "Failed to find assigned roles for group '" + group.getName() + "' for provider " + group.getProviderId());
            throw new DeleteException("Failed to delete role assignment for group '" + group.getName() +"'", fe.getCause());
        } catch (UpdateException ue) {
            logger.log(Level.INFO, "Failed to update role for assigned roles deletion");
            throw new DeleteException("Failed to delete role assignment for group '" + group.getName() +"'", ue.getCause());
        }
    }

    public void setRbacServices(RbacServices rbacServices) {
        this.rbacServices = rbacServices;
    }

    @Override
    protected void initDao() throws Exception {
        super.initDao();
        if (rbacServices == null) throw new IllegalStateException("rbacServices component is missing");
    }
}
