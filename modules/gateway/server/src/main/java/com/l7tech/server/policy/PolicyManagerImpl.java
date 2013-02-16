package com.l7tech.server.policy;

import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.*;
import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * @author alex
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class PolicyManagerImpl extends FolderSupportHibernateEntityManager<Policy, PolicyHeader> implements PolicyManager {
    private static final Logger logger = Logger.getLogger(PolicyManagerImpl.class.getName());
    
    String ROLE_NAME_TYPE_SUFFIX = "Policy";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, PolicyAdmin.ROLE_NAME_TYPE_SUFFIX));

    /**
     * True if multiple global policy fragments of each tag are permitted, False for unique tags for global policy fragments.
     */
    private static final boolean multipleGlobalPolicies = ConfigFactory.getBooleanProperty( "com.l7tech.server.policy.multipleGlobalPolicies", false );

    private final PolicyCache policyCache;
    private final RoleManager roleManager;
    private final PolicyAliasManager policyAliasManager;
    private final FolderManager folderManager;

    public PolicyManagerImpl( final RoleManager roleManager,
                              final PolicyAliasManager policyAliasManager,
                              final FolderManager folderManager,
                              final PolicyCache policyCache ) {
        this.roleManager = roleManager;
        this.policyAliasManager = policyAliasManager;
        this.folderManager = folderManager;
        this.policyCache = policyCache;
    }

    @Override
    public Policy findByHeader(final EntityHeader header) throws FindException {
        if ( header instanceof GuidEntityHeader && ((GuidEntityHeader)header).getGuid() != null ) {
            return findByGuid( ((GuidEntityHeader)header).getGuid() );
        } else {
            return super.findByHeader( header );
        }
    }

    @Override
    public Policy findByGuid(final String guid) throws FindException {
        try {
            //noinspection unchecked
            return (Policy)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("guid", guid));
                    return crit.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }

    @Override
    public Collection<PolicyHeader> findHeadersWithTypes(final Set<PolicyType> types) throws FindException{
        return this.findHeadersWithTypes(types, false);
    }

    @Override
    public Collection<PolicyHeader> findHeadersWithTypes(final Set<PolicyType> types, boolean includeAliases)
            throws FindException{

        //noinspection unchecked
        List<Policy> policies = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            @Override
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Policy.class);
                if (types != null && !types.isEmpty()) {
                    Disjunction dis = Restrictions.disjunction(); // This is an "OR" :)
                    for (PolicyType type : types) {
                        dis.add(Restrictions.eq("type", type));
                    }
                    crit.add(dis);
                }
                return crit.list();
            }
        });
        List<PolicyHeader> origHeaders = new ArrayList<PolicyHeader>(policies.size());
        for (Policy policy : policies) {
            origHeaders.add(newHeader(policy));
        }

        if(!includeAliases) return origHeaders;

        //Modify results for any aliases that may exist
        Collection<PolicyAlias> allAliases = policyAliasManager.findAll();

        Map<Long, Set<PolicyAlias>> policyIdToAllItsAliases = new HashMap<Long, Set<PolicyAlias>>();
        for(PolicyAlias pa: allAliases){
            Long origServiceId = pa.getEntityOid();
            if(!policyIdToAllItsAliases.containsKey(origServiceId)){
                Set<PolicyAlias> aliasSet = new HashSet<PolicyAlias>();
                policyIdToAllItsAliases.put(origServiceId, aliasSet);
            }
            policyIdToAllItsAliases.get(origServiceId).add(pa);
        }

        Collection<PolicyHeader> returnHeaders = new ArrayList<PolicyHeader>();
        for(PolicyHeader ph: origHeaders){
            Long serviceId = ph.getOid();
            returnHeaders.add(ph);
            if(policyIdToAllItsAliases.containsKey(serviceId)){
                Set<PolicyAlias> aliases = policyIdToAllItsAliases.get(serviceId);
                for(PolicyAlias pa: aliases){
                    PolicyHeader newSH = new PolicyHeader(ph);
                    newSH.setAliasOid(pa.getOidAsLong());
                    newSH.setFolderOid(pa.getFolder().getOid());
                    returnHeaders.add(newSH);
                }
            }
        }
        return returnHeaders;
    }

    @Override
    public Collection<PolicyHeader> findHeaders(int offset, int windowSize, Map<String,String> filters) throws FindException {
        Map<String,String> policyFilters = filters;
        String defaultFilter = filters.get(DEFAULT_SEARCH_NAME);
        if (defaultFilter != null && ! defaultFilter.isEmpty()) {
            policyFilters = new HashMap<String, String>(filters);
            policyFilters.put("name", defaultFilter);
        }
        policyFilters.remove(DEFAULT_SEARCH_NAME);
        return doFindHeaders( offset, windowSize, policyFilters, true ); // disjunction
    }

    @Override
    public long save(final Policy policy) throws SaveException {
        long oid;

        try {
            policyCache.validate(policy);
        } catch ( CircularPolicyException e ) {
            throw new SaveException("Couldn't save Policy: " + ExceptionUtils.getMessage(e), e);
        }

        try {
            //if the policy doesn't contain a folder location, we'll default it to be placed under the root folder
            if (policy.getFolder() == null) {
                Folder rootFolder = folderManager.findRootFolder();
                policy.setFolder(rootFolder);
            }
        } catch (FindException fe) {
            throw new SaveException("Couldn't save policy under root folder.");
        }

        oid = super.save(policy);

        return oid;
    }

    @Override
    public void update(final Policy policy) throws UpdateException {
        try {
            policyCache.validate(policy);
        } catch ( CircularPolicyException e ) {
            throw new UpdateException("Couldn't update Policy: " + ExceptionUtils.getMessage(e), e);
        }

        if ( policy.getType() != PolicyType.PRIVATE_SERVICE ) {
            try {
                roleManager.renameEntitySpecificRoles(POLICY, policy, replaceRoleName);
            } catch (FindException e) {
                throw new UpdateException("Couldn't find Role to rename", e);
            }
        }

        super.update(policy);
    }

    @Override
    public void updateFolder( final long entityId, final Folder folder ) throws UpdateException {
        setParentFolderForEntity( entityId, folder );
    }

    @Override
    public void updateFolder( final Policy entity, final Folder folder ) throws UpdateException {
        if ( entity == null ) throw new UpdateException("Policy is required but missing.");
        setParentFolderForEntity( entity.getOid(), folder );
    }

    @Override
    public void delete( long oid ) throws DeleteException, FindException {
        findAndDelete(oid);
    }

    @Override
    public void delete( Policy policy) throws DeleteException {
        try {
            if ( policy != null )
                policyCache.validateRemove( policy.getOid() );
        } catch (PolicyDeletionForbiddenException e) {
            throw new DeleteException("Couldn't delete Policy: " + ExceptionUtils.getMessage(e), e);
        }

        super.delete(policy);
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<PolicyHeader> findHeadersByType(final PolicyType type) throws FindException {
        return findHeadersWithTypes(EnumSet.of(type));
    }

    @Override
    public void createRoles( final Policy policy ) throws SaveException {
        addManagePolicyRole( policy );    
    }

    @Override
    public void updateRoles( final Policy entity ) throws UpdateException {
        //handled in update method
    }

    @Override
    public void deleteRoles( final long policyOid ) throws DeleteException {
        roleManager.deleteEntitySpecificRoles(EntityType.POLICY, policyOid);        
    }

    @Override
    public void addManagePolicyRole(Policy policy) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        // truncate policy name in the role name to avoid going beyond 128 limit
        String pname = policy.getName();
        // cutoff is arbitrarily set to 50
        pname = TextUtils.truncStringMiddle(pname, 50);
        String name = MessageFormat.format(PolicyAdmin.ROLE_NAME_PATTERN, pname, policy.getOid());

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} policy.");        
        newRole.setEntityType(POLICY);
        newRole.setEntityOid(policy.getOid());

        // RUD this policy
        newRole.addEntityPermission(READ, POLICY, policy.getId()); // Read this policy
        newRole.addEntityPermission(UPDATE, POLICY, policy.getId()); // Update this policy
        newRole.addEntityPermission(DELETE, POLICY, policy.getId()); // Delete this policy
        newRole.addEntityPermission(READ, SERVICE_TEMPLATE, null);

        // Read all JDBC Connections
        newRole.addEntityPermission(READ, JDBC_CONNECTION, null);

        // Read all HTTP Configurations
        newRole.addEntityPermission(READ, HTTP_CONFIGURATION, null);

        // Read this policy's folder ancestry
        newRole.addEntityFolderAncestryPermission(POLICY, policy.getId());

        // Use encapsulated assertion within a policy
        newRole.addEntityPermission(READ, ENCAPSULATED_ASSERTION, null);

        if (currentUser != null) {
            // See if we should give the current user admin permission for this policy
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAnyEntityOfType(currentUser, READ, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, FOLDER);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, JDBC_CONNECTION);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, ENCAPSULATED_ASSERTION);
            } catch (FindException e) {
                throw new SaveException("Couldn't get existing permissions", e);
            }

            if (!omnipotent && shouldAutoAssignToNewRole()) {
                logger.info("Assigning current User to new Role");
                newRole.addAssignedUser(currentUser);
            }
        }
        roleManager.save(newRole);
    }

    private boolean shouldAutoAssignToNewRole() {
        return ConfigFactory.getBooleanProperty("rbac.autoRole.managePolicy.autoAssign", true);
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getImpClass() {
        return Policy.class;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getInterfaceClass() {
        return Policy.class;
    }

    @Override
    @Transactional(propagation=Propagation.SUPPORTS)
    public String getTableName() {
        return "policy";
    }

    @Override
    protected PolicyHeader newHeader( final Policy entity ) {
        return new PolicyHeader( entity );
    }

    @Override
    protected void doFindHeaderCriteria( final Criteria criteria ) {
        criteria.add(Restrictions.eq("type", PolicyType.INCLUDE_FRAGMENT));
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final Policy entity ) {
        List<Map<String,Object>> constraints = new ArrayList<Map<String,Object>>();
        Map<String,Object> nameMap = new HashMap<String, Object>();
        nameMap.put("name", entity.getName());
        constraints.add( nameMap );

        if ( entity.getType() == PolicyType.GLOBAL_FRAGMENT && !multipleGlobalPolicies ) {
            Map<String,Object> globalTagMap = new HashMap<String, Object>();
            globalTagMap.put("type", PolicyType.GLOBAL_FRAGMENT);
            if (entity.getInternalTag()!=null)
                globalTagMap.put("internalTag", entity.getInternalTag());
            constraints.add( globalTagMap );                
        }

        if ( entity.getType() == PolicyType.INTERNAL ) {
            String internalTag = entity.getInternalTag();
            if(PolicyType.getAuditMessageFilterTags().contains(internalTag)){
                Map<String,Object> auditTagMap = new HashMap<String, Object>();
                auditTagMap.put("type", PolicyType.INTERNAL);
                auditTagMap.put("internalTag", internalTag);
                constraints.add( auditTagMap );
            }
        }

        return constraints;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }
}

