/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import static com.l7tech.objectmodel.EntityType.POLICY;
import static com.l7tech.objectmodel.EntityType.SERVICE_TEMPLATE;
import static com.l7tech.objectmodel.EntityType.*;
import static com.l7tech.gateway.common.security.rbac.OperationType.*;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.TextUtils;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Disjunction;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
    private static final boolean multipleGlobalPolicies = SyspropUtil.getBoolean( "com.l7tech.server.policy.multipleGlobalPolicies", false );

    private PolicyCache policyCache;
    private final RoleManager roleManager;
    private final PolicyAliasManager policyAliasManager;
    private final FolderManager folderManager;
    private final LicenseManager licenseManager;

    public PolicyManagerImpl(RoleManager roleManager, PolicyAliasManager policyAliasManager, FolderManager folderManager, LicenseManager licenseManager) {
        this.roleManager = roleManager;
        this.policyAliasManager = policyAliasManager;
        this.folderManager = folderManager;
        this.licenseManager = licenseManager;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public void setPolicyCache(PolicyCache policyCache) {
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

        if (currentUser != null) {
            // See if we should give the current user admin permission for this policy
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAnyEntityOfType(currentUser, READ, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, FOLDER);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, JDBC_CONNECTION);
            } catch (FindException e) {
                throw new SaveException("Coudln't get existing permissions", e);
            }

            if (!omnipotent) {
                logger.info("Assigning current User to new Role");
                newRole.addAssignedUser(currentUser);
            }
        }
        roleManager.save(newRole);
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
    public String getDefaultPolicyXml(PolicyType type, String internalTag) {
        if(type == PolicyType.INTERNAL){
            if( PolicyType.TAG_AUDIT_MESSAGE_FILTER.equals(internalTag)){
                return getAuditMessageFilterDefaultPolicy();
            } else if (PolicyType.TAG_AUDIT_VIEWER.equals(internalTag)){
                return getAuditViewerDefaultPolicy();
            }
        }
        return null;
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

    /**
     * Choose between the default xml and the fallback xml.
     * <p/>
     * Protected visibility for test cases.
     *
     * @param defaultXml        desired default xml. Validated to be valid.
     * @param fallbackXml       fallback xml, not validated.
     * @param policyInternalTag internal tag string used for logging warning.
     * @return defaultXml if it contains no unlicensed assertions and is valid policy XML, otherwise the fallbackXml.
     */
    protected String getDefaultXmlBasedOnLicense(final String defaultXml,
                                                 final String fallbackXml,
                                                 final String policyInternalTag) {
        try {
            final Assertion assertion = WspReader.getDefault().parsePermissively(
                    defaultXml, WspReader.INCLUDE_DISABLED);

            if (assertion instanceof CompositeAssertion) {
                CompositeAssertion root = (CompositeAssertion) assertion;
                final boolean defaultContainsUnlicensedAssertion = xmlContainsUnlicensedAssertion(root);
                if (defaultContainsUnlicensedAssertion) {
                    return fallbackXml;
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not parse default " + policyInternalTag + " policy xml.");
            //should not happen
            //fall through
        }

        return defaultXml;
    }

    private String getAuditMessageFilterDefaultPolicy(){
        //By using XML, which should always be backwards compatible, we don't need to add dependencies for
        //modular assertions
        //TODO Look up the Audit Viewer Private Key's cert and configure the encrypt XML element to use it.

        return getDefaultXmlBasedOnLicense(DEFAULT_AUDIT_MESSAGE_FILTER_POLICY_XML,
                FALLBACK_AUDIT_MESSAGE_FILTER_POLICY_XML, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
    }

    private String getAuditViewerDefaultPolicy(){
        return getDefaultXmlBasedOnLicense(DEFAULT_AUDIT_VIEWER_POLICY_XML,
                FALLBACK_AUDIT_VIEWER_POLICY_XML, PolicyType.TAG_AUDIT_VIEWER);

    }

    private boolean xmlContainsUnlicensedAssertion(CompositeAssertion parent){
        final List<Assertion> kids = parent.getChildren();
        if(kids.isEmpty()) return false;

        for (Assertion kid : kids) {
            if(kid instanceof CompositeAssertion){
                if(xmlContainsUnlicensedAssertion((CompositeAssertion) kid)){
                    return true;
                }
            }
            final String featureSetName = kid.getFeatureSetName();
            if(!licenseManager.isFeatureEnabled(featureSetName)){
                return true;
            }
        }

        return false;
    }

    private static final String AMF_COMMENT_FRAGMENT = 
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Add policy logic to scrub / protect the request or response messages before they are audited.\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Policy is invoked by the audit sub system post service and global policy processing.\"/>\n" +
            "        </L7p:CommentAssertion>\n";
    
    public static final String DEFAULT_AUDIT_MESSAGE_FILTER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AMF_COMMENT_FRAGMENT +            
            "        <L7p:EncodeDecode>\n" +
            "            <L7p:SourceVariableName stringValue=\"request.mainpart\"/>\n" +
            "            <L7p:TargetContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:TargetDataType variableDataType=\"message\"/>\n" +
            "            <L7p:TargetVariableName stringValue=\"request\"/>\n" +
            "            <L7p:TransformType transformType=\"BASE64_ENCODE\"/>\n" +
            "        </L7p:EncodeDecode>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PHNhdmVkbWVzc2FnZSB4bWxucz0iaHR0cDovL2xheWVyN3RlY2guY29tL25zL2F1ZGl0Ij4NCiR7cmVxdWVzdC5tYWlucGFydH0NCjwvc2F2ZWRtZXNzYWdlPg==\"/>\n" +
            "            <L7p:ContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:DataType variableDataType=\"message\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"request\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"Configure cert to use here. Should match the Audit Viewer Private Key if defined.\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:NonSoapEncryptElement>\n" +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "            <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                <L7p:Expression stringValue=\"//*\"/>\n" +
            "                <L7p:Namespaces mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"xenc\"/>\n" +
            "                        <L7p:value stringValue=\"http://www.w3.org/2001/04/xmlenc#\"/>\n" +
            "                    </L7p:entry>\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"ds\"/>\n" +
            "                        <L7p:value stringValue=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Namespaces>\n" +
            "            </L7p:XpathExpression>\n" +
            "        </L7p:NonSoapEncryptElement>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    public static final String FALLBACK_AUDIT_MESSAGE_FILTER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AMF_COMMENT_FRAGMENT +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final String AV_COMMENT_FRAGMENT =
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Add logic to transform audited messages and details.\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Policy is invoked from the audit viewer.\"/>\n" +
            "        </L7p:CommentAssertion>\n";

    public static final String DEFAULT_AUDIT_VIEWER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AV_COMMENT_FRAGMENT +
            "        <L7p:NonSoapDecryptElement/>\n" +
            "        <L7p:RequestXpathAssertion>\n" +
            "            <L7p:VariablePrefix stringValue=\"output\"/>\n" +
            "            <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                <L7p:Expression stringValue=\"/ns:savedmessage\"/>\n" +
            "                <L7p:Namespaces mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"ns\"/>\n" +
            "                        <L7p:value stringValue=\"http://layer7tech.com/ns/audit\"/>\n" +
            "                    </L7p:entry>\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"s\"/>\n" +
            "                        <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Namespaces>\n" +
            "            </L7p:XpathExpression>\n" +
            "        </L7p:RequestXpathAssertion>\n" +
            "        <L7p:EncodeDecode>\n" +
            "            <L7p:CharacterEncoding stringValueNull=\"null\"/>\n" +
            "            <L7p:SourceVariableName stringValue=\"output.result\"/>\n" +
            "            <L7p:TargetContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:TargetDataType variableDataType=\"message\"/>\n" +
            "            <L7p:TargetVariableName stringValue=\"request\"/>\n" +
            "            <L7p:TransformType transformType=\"BASE64_DECODE\"/>\n" +
            "        </L7p:EncodeDecode>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    public static final String FALLBACK_AUDIT_VIEWER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AV_COMMENT_FRAGMENT +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

}

