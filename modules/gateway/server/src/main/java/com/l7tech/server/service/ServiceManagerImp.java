/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import static com.l7tech.objectmodel.EntityType.*;
import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.policy.Policy;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.StaleObjectStateException;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Manages {@link PublishedService} instances.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class ServiceManagerImp
        extends FolderSupportHibernateEntityManager<PublishedService, ServiceHeader>
        implements ServiceManager, ApplicationContextAware
{
    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());

    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, ServiceAdmin.ROLE_NAME_TYPE_SUFFIX));

    private final ResolutionManager resolutionManager;
    private final RoleManager roleManager;

    private ApplicationContext spring;
    private final ServiceAliasManager serviceAliasManager;


    public ServiceManagerImp(ResolutionManager resolutionManager, RoleManager roleManager, ServiceAliasManager serviceAliasManager) {
        this.roleManager = roleManager;
        this.resolutionManager = resolutionManager;
        this.serviceAliasManager = serviceAliasManager;
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
    public long save(final PublishedService service) throws SaveException {
        // 1. record the service (no policy)
        final Policy policy = service.getPolicy();
        service.setPolicy( null );
        long oid = super.save(service);
        logger.info("Saved service #" + oid);
        service.setOid(oid);

        // 2. record resolution parameters
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "Error saving service. Duplicate resolution parameters";
            logger.info(msg + " " + e.getMessage());
            logger.log(Level.FINE, msg, e);
            throw e;
        } catch (UpdateException e) {
            String msg = "cannot save service's resolution parameters.";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }

        // 3. Service now has correct oid so update the associated policy with the right name
        try {
            updatePolicyName( service, policy );
            Object key = getHibernateTemplate().save( policy );
            if (!(key instanceof Long))
                throw new SaveException("Primary key was a " + key.getClass().getName() + ", not a Long");
            policy.setOid( (Long)key );
            service.setPolicy( policy );
            getHibernateTemplate().update( service );
        } catch (RuntimeException e) {
            String msg = "Error updating policy name after saving service.";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }

        // 4. update cache on callback
        spring.publishEvent(new ServiceCacheEvent.Updated(service));
        return service.getOid();
    }

    @Override
    public void update(PublishedService service) throws UpdateException {
        // try recording resolution parameters
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "cannot update service. duplicate resolution parameters";
            logger.log(Level.INFO, msg, e);
            throw new UpdateException(msg, e);
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
        spring.publishEvent(new ServiceCacheEvent.Updated(service));
    }

    @Override
    public void updateFolder( final long entityId, final Folder folder ) throws UpdateException {
        setParentFolderForEntity( entityId, folder );
    }

    @Override
    public void updateFolder( final PublishedService entity, final Folder folder ) throws UpdateException {
        if ( entity == null ) throw new UpdateException("Service is required but missing.");
        setParentFolderForEntity( entity.getOid(), folder );
    }

    @Override
    public void delete( final long oid ) throws DeleteException, FindException {
        findAndDelete(oid);
    }

    @Override
    public void delete(PublishedService service) throws DeleteException {
        super.delete(service);

        resolutionManager.deleteResolutionParameters(service.getOid());
        logger.info("Deleted service " + service.getName() + " #" + service.getOid());
        spring.publishEvent(new ServiceCacheEvent.Deleted(service));
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
        addManageServiceRole(service);
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
        String name = MessageFormat.format(ServiceAdmin.ROLE_NAME_PATTERN, svcname, service.getOid());

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
        newRole.addAttributePermission(READ, METRICS_BIN, MetricsBin.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(READ, AUDIT_MESSAGE, MessageSummaryAuditRecord.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(READ, SERVICE_USAGE, ServiceUsage.ATTR_SERVICE_OID, service.getId());

        // CRUD {@link SampleMessage}s for this Service
        newRole.addAttributePermission(CREATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(READ, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(UPDATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(DELETE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());

        // Search all identities (for adding IdentityAssertions)
        newRole.addEntityPermission(READ, ID_PROVIDER_CONFIG, null);
        newRole.addEntityPermission(READ, USER, null);
        newRole.addEntityPermission(READ, GROUP, null);

        // Read all JMS queues
        newRole.addEntityPermission(READ, JMS_CONNECTION, null);
        newRole.addEntityPermission(READ, JMS_ENDPOINT, null);

        // Read this service's folder ancestry
        newRole.addEntityFolderAncestryPermission(EntityType.SERVICE, service.getId());

        // Set role as entity-specific
        newRole.setEntityType(SERVICE);
        newRole.setEntityOid(service.getOid());
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} service.");

        //Read all UDDIRegistries
        newRole.addEntityPermission(READ, UDDI_REGISTRY, null);

        //add attribute predicate to allow crud on a uddi_proxied_service if one gets created
        newRole.addAttributePermission(CREATE, UDDI_PROXIED_SERVICE, UDDIProxiedService.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(READ, UDDI_PROXIED_SERVICE, UDDIProxiedService.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(UPDATE, UDDI_PROXIED_SERVICE, UDDIProxiedService.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(DELETE, UDDI_PROXIED_SERVICE, UDDIProxiedService.ATTR_SERVICE_OID, service.getId());

        //add attribute predicate to allow crud on a uddi_service_control if one gets created
        newRole.addAttributePermission(CREATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(READ, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(UPDATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, service.getId());
        newRole.addAttributePermission(DELETE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, service.getId());


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
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, CREATE, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, READ, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, SAMPLE_MESSAGE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, SAMPLE_MESSAGE);
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
    @Transactional(propagation = Propagation.SUPPORTS)
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.spring = applicationContext;
    }

    private void updatePolicyName( final PublishedService service, final Policy policy ) {
        if ( policy != null ) {
            String policyName = service.generatePolicyName();
            policy.setName( policyName );
        }
    }
}
