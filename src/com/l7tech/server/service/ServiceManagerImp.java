/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service;

import com.l7tech.cluster.ServiceUsage;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import static com.l7tech.common.security.rbac.EntityType.*;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.service.ServiceAdmin;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Manages {@link PublishedService} instances.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class ServiceManagerImp
        extends HibernateEntityManager<PublishedService, EntityHeader>
        implements ServiceManager, ApplicationContextAware
{
    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());

    private ResolutionManager resolutionManager;
    private RoleManager roleManager;
    private ApplicationContext spring;

    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, ServiceAdmin.ROLE_NAME_TYPE_SUFFIX));

    @Transactional(propagation=SUPPORTS)
    public String resolveWsdlTarget(String url) {
        throw new UnsupportedOperationException();
    }

    public long save(PublishedService service) throws SaveException {
        // 1. record the service
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

        // 3. update cache on callback
        spring.publishEvent(new ServiceCacheEvent.Updated(service));
        return service.getOid();
    }

    public void update(PublishedService service) throws UpdateException {
        // try recording resolution parameters
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "cannot update service. duplicate resolution parameters";
            logger.log(Level.INFO, msg, e);
            throw new UpdateException(msg, e);
        }

        super.update(service);

        try {
            roleManager.renameEntitySpecificRole(SERVICE, service, replaceRoleName);
        } catch (FindException e) {
            throw new UpdateException("Couldn't find Role to rename", e);
        }

        // update cache after commit
        spring.publishEvent(new ServiceCacheEvent.Updated(service));
    }

    public void delete(PublishedService service) throws DeleteException {
        super.delete(service);
        resolutionManager.deleteResolutionParameters(service.getOid());
        logger.info("Deleted service " + service.getName() + " #" + service.getOid());
        spring.publishEvent(new ServiceCacheEvent.Deleted(service));
    }

    @Transactional(propagation=SUPPORTS)
    public Class getImpClass() {
        return PublishedService.class;
    }

    @Transactional(propagation=SUPPORTS)
    public Class getInterfaceClass() {
        return PublishedService.class;
    }

    @Transactional(propagation=SUPPORTS)
    public String getTableName() {
        return "published_service";
    }

    @Transactional(propagation=SUPPORTS)
    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    @Transactional(propagation=SUPPORTS)
    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    /**
     * Set the resolution manager. This is managed by Spring runtime.
     *
     * @param resolutionManager
     */
    @Transactional(propagation=SUPPORTS)
    public void setResolutionManager(ResolutionManager resolutionManager) {
        this.resolutionManager = resolutionManager;
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    /**
     * Creates a new role for the specified PublishedService.
     *
     * @param service      the PublishedService that is in need of a Role.  Must not be null.
     * @throws SaveException  if the new Role could not be saved
     */
    public void addManageServiceRole(PublishedService service) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        // truncate service name in the role name to avoid going beyond 128 limit
        String svcname = service.getName();
        // cutoff is arbitrarily set to 50
        svcname = HexUtils.truncStringMiddle(svcname, 50);
        String name = MessageFormat.format(ServiceAdmin.ROLE_NAME_PATTERN, svcname, service.getOid());

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        // RUD this service
        newRole.addPermission(READ, SERVICE, service.getId()); // Read this service
        newRole.addPermission(UPDATE, SERVICE, service.getId()); // Update this service
        newRole.addPermission(DELETE, SERVICE, service.getId()); // Delete this service

        // Read all information generated by messages sent to this service
        newRole.addPermission(READ, CLUSTER_INFO, null); // Prerequisite to viewing audits and metrics
        newRole.addPermission(READ, METRICS_BIN, MetricsBin.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(READ, AUDIT_MESSAGE, MessageSummaryAuditRecord.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(READ, SERVICE_USAGE, ServiceUsage.ATTR_SERVICE_OID, service.getId());

        // CRUD {@link SampleMessage}s for this Service
        newRole.addPermission(CREATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(READ, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(UPDATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(DELETE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_OID, service.getId());

        // Search all identities (for adding IdentityAssertions)
        newRole.addPermission(READ, ID_PROVIDER_CONFIG, null);
        newRole.addPermission(READ, USER, null);
        newRole.addPermission(READ, GROUP, null);

        // Read all JMS queues
        newRole.addPermission(READ, JMS_CONNECTION, null);
        newRole.addPermission(READ, JMS_ENDPOINT, null);

        newRole.setEntityType(SERVICE);
        newRole.setEntityOid(service.getOid());
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} service.");

        if (currentUser != null) {
            // See if we should give the current user admin permission for this service
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAnyEntityOfType(currentUser, READ, SERVICE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, SERVICE);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, SERVICE);
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

    @Transactional(propagation = Propagation.SUPPORTS)
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.spring = applicationContext;
    }
}
