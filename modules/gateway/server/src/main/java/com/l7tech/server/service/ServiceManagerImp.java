package com.l7tech.server.service;

import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;
import org.hibernate.StaleObjectStateException;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import javax.wsdl.WSDLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.*;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * Manages {@link PublishedService} instances.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class ServiceManagerImp
        extends FolderSupportHibernateEntityManager<PublishedService, ServiceHeader>
        implements ServiceManager
{
    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());

    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, ServiceAdmin.ROLE_NAME_TYPE_SUFFIX));
    private final RoleManager roleManager;

    private final ServiceAliasManager serviceAliasManager;

    private final Config config;

    static final String AUTO_CREATE_ROLE_PROPERTY = "rbac.autoRole.manageService.autoCreate";


    public ServiceManagerImp(RoleManager roleManager, ServiceAliasManager serviceAliasManager, @NotNull final Config config) {
        this.roleManager = roleManager;
        this.serviceAliasManager = serviceAliasManager;
        this.config = config;
    }

    @Override
    protected ServiceHeader newHeader(PublishedService entity) {
        return new ServiceHeader( entity );   
    }

    @Override
    @Transactional(propagation=SUPPORTS)
    public String resolveWsdlTarget(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders(boolean includeAliases) throws FindException {
        Collection<ServiceHeader> origHeaders = super.findAllHeaders();
        if(!includeAliases) return origHeaders;

        return serviceAliasManager.expandEntityWithAliases(origHeaders);
    }

    @Override
    public Collection<PublishedService> findByRoutingUri(String routingUri) throws FindException {
        return findByPropertyMaybeNull("routingUri", routingUri);
    }

    @Override
    public Collection<ServiceHeader> findHeaders(int offset, int windowSize, Map<String,String> filters) throws FindException {
        Map<String,String> serviceFilters = filters;
        String defaultFilter = filters.get(DEFAULT_SEARCH_NAME);
        if (defaultFilter != null && ! defaultFilter.isEmpty()) {
            serviceFilters = new HashMap<String, String>(filters);
            serviceFilters.put("name", defaultFilter);
            serviceFilters.put("routingUri", defaultFilter);
        }
        serviceFilters.remove(DEFAULT_SEARCH_NAME);
        return doFindHeaders( offset, windowSize, serviceFilters, true ); // disjunction
    }

    @Override
    public Goid save(final PublishedService service) throws SaveException {
        // 1. record the service (no policy)
        final Policy policy = service.getPolicy();
        service.setPolicy( null );
        Goid goid = super.save(service);
        logger.info("Saved service #" + goid);
        service.setGoid(goid);

        // 2. check wsdl
        if ( service.isSoap() ) {
            try {
                service.parsedWsdl();
            } catch ( WSDLException e ) {
                throw new SaveException( "Error accessing WSDL for service:" + ExceptionUtils.getMessage(e) );
            }
        }

        // 3. Service now has correct oid so update the associated policy with the right name
        try {
            updatePolicyName( service, policy );
            Object key = getHibernateTemplate().save( policy );
            if (!(key instanceof Goid))
                throw new SaveException("Primary key was a " + key.getClass().getName() + ", not a Goid");
            policy.setGoid( (Goid)key );
            service.setPolicy( policy );
            getHibernateTemplate().update( service );
        } catch (RuntimeException e) {
            String msg = "Error updating policy name after saving service.";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }

        // 4. update cache on callback
        applicationContext.publishEvent(new ServiceCacheEvent.Updated(service));
        return service.getGoid();
    }

    @Override
    public void update(PublishedService service) throws UpdateException {
        // check wsdl
        if ( service.isSoap() ) {
            try {
                service.parsedWsdl();
            } catch ( WSDLException e ) {
                throw new UpdateException( "Error accessing WSDL for service:" + ExceptionUtils.getMessage(e) );
            }
        }

        updatePolicyName(service, service.getPolicy());
        try {
            super.update(service);
        } catch (UpdateException ue) {
            if (ExceptionUtils.causedBy(ue, StaleObjectStateException.class)) {
                throw new StaleUpdateException("version mismatch", ue);
            } else {
                throw ue;
            }
        }

        try {
            roleManager.renameEntitySpecificRoles(SERVICE, service, replaceRoleName);
        } catch (FindException e) {
            throw new UpdateException("Couldn't find Role to rename", e);
        }

        // update cache after commit
        applicationContext.publishEvent(new ServiceCacheEvent.Updated(service));
    }

    @Override
    public void updateFolder( final Goid entityId, final Folder folder ) throws UpdateException {
        setParentFolderForEntity( entityId, folder );
    }

    @Override
    public void updateFolder( final PublishedService entity, final Folder folder ) throws UpdateException {
        if ( entity == null ) throw new UpdateException("Service is required but missing.");
        setParentFolderForEntity( entity.getGoid(), folder );
    }

    @Override
    public void delete( final Goid goid ) throws DeleteException, FindException {
        findAndDelete(goid);
    }

    @Override
    public void delete(PublishedService service) throws DeleteException {
        super.delete(service);

        logger.info("Deleted service " + service.getName() + " #" + service.getGoid());
        applicationContext.publishEvent(new ServiceCacheEvent.Deleted(service));
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public Class<PublishedService> getImpClass() {
        return PublishedService.class;
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public Class<PublishedService> getInterfaceClass() {
        return PublishedService.class;
    }

    @Override
    @Transactional(propagation=SUPPORTS)
    public String getTableName() {
        return "published_service";
    }

    @Transactional(propagation=SUPPORTS)
    @Override
    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public void createRoles( final PublishedService service ) throws SaveException {
        if (config.getBooleanProperty(AUTO_CREATE_ROLE_PROPERTY, true)) {
            addManageServiceRole(service);
        }
    }

    @Override
    public void updateRoles( final PublishedService entity ) throws UpdateException {
        // handled in update method
    }

    /**
     * Creates a new role for the specified PublishedService.
     *
     * @param service      the PublishedService that is in need of a Role.  Must not be null.
     * @throws SaveException  if the new Role could not be saved
     */
    @Override
    public void addManageServiceRole(PublishedService service) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        // truncate service name in the role name to avoid going beyond 128 limit
        String svcname = service.getName();
        // cutoff is arbitrarily set to 50
        svcname = TextUtils.truncStringMiddle(svcname, 50);
        String name = MessageFormat.format(ServiceAdmin.ROLE_NAME_PATTERN, svcname, service.getGoid());

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        // RUD this service
        newRole.addEntityPermission(READ, SERVICE, service.getId()); // Read this service
        newRole.addEntityPermission(UPDATE, SERVICE, service.getId()); // Update this service
        newRole.addEntityPermission(DELETE, SERVICE, service.getId()); // Delete this service

        // Read/Update the policy for this service (don't need to be able to delete it; the policy's lifecycle is tied to the service's)
        newRole.addEntityPermission(READ,   POLICY, service.getPolicy().getId());
        newRole.addEntityPermission(UPDATE, POLICY, service.getPolicy().getId());

        // Read all information generated by messages sent to this service
        newRole.addEntityPermission(READ, CLUSTER_INFO, null); // Prerequisite to viewing audits and metrics
        newRole.addAttributePermission(READ, METRICS_BIN, MetricsBin.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(READ, AUDIT_MESSAGE, MessageSummaryAuditRecord.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(READ, SERVICE_USAGE, ServiceUsage.ATTR_SERVICE_GOID, service.getId());

        // CRUD {@link SampleMessage}s for this Service
        newRole.addAttributePermission(CREATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(READ, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(UPDATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(DELETE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, service.getId());

        // Search all identities (for adding IdentityAssertions)
        newRole.addEntityPermission(READ, ID_PROVIDER_CONFIG, null);
        newRole.addEntityPermission(READ, USER, null);
        newRole.addEntityPermission(READ, GROUP, null);

        // Read all JMS queues
        newRole.addEntityPermission(READ, JMS_CONNECTION, null);
        newRole.addEntityPermission(READ, JMS_ENDPOINT, null);

        // Read all JDBC Connections
        newRole.addEntityPermission(READ, JDBC_CONNECTION, null);

        // Read all HTTP Configurations
        newRole.addEntityPermission(READ, HTTP_CONFIGURATION, null);

        // Read this service's folder ancestry
        newRole.addEntityFolderAncestryPermission(EntityType.SERVICE, service.getGoid());

        // Set role as entity-specific
        newRole.setEntityType(SERVICE);
        newRole.setEntityGoid(service.getGoid());
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} service.");

        //Read all UDDIRegistries
        newRole.addEntityPermission(READ, UDDI_REGISTRY, null);

        //add attribute predicate to allow crud on a uddi_proxied_service if one gets created
        newRole.addAttributePermission(CREATE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(READ, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(UPDATE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(DELETE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, service.getId());

        //add attribute predicate to allow crud on a uddi_service_control if one gets created
        newRole.addAttributePermission(CREATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(READ, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(UPDATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, service.getId());
        newRole.addAttributePermission(DELETE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, service.getId());

        //use encapsulated assertions in a policy
        newRole.addEntityPermission(READ, ENCAPSULATED_ASSERTION, null);

        // use all assertions in a policy
        newRole.addEntityPermission(READ, ASSERTION_ACCESS, null);

        if (currentUser != null) {
            // See if we should give the current user admin permission for this service
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAnyEntityOfType(currentUser, READ, SERVICE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, SERVICE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, SERVICE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, METRICS_BIN);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, AUDIT_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, ID_PROVIDER_CONFIG);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, USER);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, GROUP);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, CLUSTER_INFO);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, SERVICE_USAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, JMS_CONNECTION);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, JMS_ENDPOINT);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, JDBC_CONNECTION);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, CREATE, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, UDDI_REGISTRY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, ENCAPSULATED_ASSERTION);
            } catch (FindException e) {
                throw new SaveException("Coudln't get existing permissions", e);
            }

            if (!omnipotent && shouldAutoAssignToNewRole()) {
                logger.info("Assigning current User to new Role");
                newRole.addAssignedUser(currentUser);
            }
        }
        roleManager.save(newRole);
    }

    private boolean shouldAutoAssignToNewRole() {
        return ConfigFactory.getBooleanProperty("rbac.autoRole.manageService.autoAssign", true);
    }

    private void updatePolicyName( final PublishedService service, final Policy policy ) {
        if ( policy != null ) {
            String policyName = service.generatePolicyName();
            policy.setName( policyName );
        }
    }
}
