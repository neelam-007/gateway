/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.service.PublishedService;
import com.l7tech.service.MetricsBin;
import com.l7tech.identity.User;
import com.l7tech.cluster.ServiceUsage;
import org.springframework.transaction.TransactionStatus;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Manages PublishedService instances.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class ServiceManagerImp
        extends HibernateEntityManager<PublishedService, EntityHeader>
        implements ServiceManager
{
    private ServiceCache serviceCache;
    private RoleManager roleManager;

    @Transactional(propagation=SUPPORTS)
    public void setVisitorClassnames(String visitorClassnames) {
        String[] names = visitorClassnames.split(",\\s*");
        for (String name : names) {
            try {
                Class.forName(name);
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING, "PolicyVisitor classname " + name + " could not be loaded.", e);
            }
        }
    }

    @Transactional(propagation=SUPPORTS)
    public String resolveWsdlTarget(String url) throws RemoteException {
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
            String msg = "cannot save service. duplicate resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw e;
        } catch (UpdateException e) {
            String msg = "cannot save service's resolution parameters.";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }

        // 3. update cache on callback
        final long passedServiceId = service.getOid();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    // get service. version property must be up-to-date
                    PublishedService svcnow;
                    try {
                        svcnow = findByPrimaryKey(passedServiceId);
                    } catch (FindException e) {
                        svcnow = null;
                        logger.log(Level.WARNING, "could not get service back", e);
                    }
                    if (svcnow != null) {
                        try {
                            serviceCache.cache(svcnow);
                            TarariLoader.compile();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }
        });

        return service.getOid();
    }

    public void update(PublishedService service) throws UpdateException {
        // try recording resolution parameters
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "cannot update service. duplicate resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        super.update(service);

        // update cache after commit
        final long passedServiceId = service.getOid();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    // get service. version property must be up-to-date
                    PublishedService svcnow;
                    try {
                        svcnow = findByPrimaryKey(passedServiceId);
                    } catch (FindException e) {
                        svcnow = null;
                        logger.log(Level.WARNING, "could not get service back", e);
                    }
                    if (svcnow != null) {
                        try {
                            serviceCache.cache(svcnow);
                            TarariLoader.compile();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }
        });
    }

    public void delete(PublishedService service) throws DeleteException {
        super.delete(service);
        resolutionManager.deleteResolutionParameters(service.getOid());
        logger.info("Deleted service " + service.getName() + " #" + service.getOid());

        final PublishedService deletedService = service;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    try {
                        serviceCache.removeFromCache(deletedService);
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        });
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
    public void setServiceCache(ServiceCache serviceCache) {
        this.serviceCache = serviceCache;
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

    protected void initDao() throws Exception {
        if (serviceCache == null) {
            throw new IllegalArgumentException("Service Cache is required");
        }
        if (transactionManager == null) {
            throw new IllegalArgumentException("Transaction Manager is required");
        }
    }

    /**
     * this should be called within the boot process to initiate the service cache which in turn will
     * create server side policies
     */
    @Transactional(propagation=SUPPORTS)
    public void initiateServiceCache() {
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    initializeServiceCache();
                } catch (ObjectModelException e) {
                    throw new RuntimeException("Error intializing service cache", e);
                }
            }
        });
    }

    private void initializeServiceCache() throws ObjectModelException {
        // build the cache if necessary
        try {
            if (serviceCache.size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.info("building service cache");
                Collection<PublishedService> services = findAll();
                for (PublishedService service : services) {
                    try {
                        serviceCache.cache(service);
                    } catch (ServerPolicyException e) {
                        Assertion ass = e.getAssertion();

                        String ordinal = ass == null ? "" : "#" + Integer.toString(ass.getOrdinal());
                        String what = ass == null ? "<unknown>" : "(" + ass.getClass().getSimpleName() + ")";
                        String msg = "Disabling PublishedService #{0} ({1}); policy could not be compiled (assertion {2} {3})";
                        logger.log(Level.WARNING, MessageFormat.format(msg, service.getOid(), service.getName(), ordinal, what));
                        // We don't actually disable the service here -- only the admin should be doing that.
                        // Instead, we will let the service cache continue to monitor the situation
                    } catch (Exception e) {
                        LogRecord r = new LogRecord(Level.WARNING, "Disabling PublishedService #{0} ({1}); policy could not be compiled");
                        r.setParameters(new Object[] { service.getOid(), service.getName() });
                        r.setThrown(e);
                        logger.log(r);
                    }
                }
                TarariLoader.compile();
            }
            // make sure the integrity check is running
            logger.info("initiate service cache version check process");
            serviceCache.initiateIntegrityCheckProcess();
        } catch (Exception e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    private ResolutionManager resolutionManager;
    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());

    /**
     * Creates a new role for the specified PublishedService.
     *
     * @param service      the PublishedService that is in need of a Role.  Must not be null.
     * @throws com.l7tech.objectmodel.SaveException  if the new Role could not be saved
     */
    public void addManageServiceRole(PublishedService service) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        String name = "Manage " + service.getName() + " Service (#" + service.getOid() + ")";

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        // RUD this service
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.SERVICE, service.getId()); // Read this service
        newRole.addPermission(OperationType.UPDATE, com.l7tech.common.security.rbac.EntityType.SERVICE, service.getId()); // Update this service
        newRole.addPermission(OperationType.DELETE, com.l7tech.common.security.rbac.EntityType.SERVICE, service.getId()); // Delete this service

        // Read information generated by messages sent to this service
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.CLUSTER_INFO, null); // Prerequisite to viewing audits and metrics
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.METRICS_BIN, MetricsBin.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.AUDIT_MESSAGE, MessageSummaryAuditRecord.ATTR_SERVICE_OID, service.getId());
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.SERVICE_USAGE, ServiceUsage.ATTR_SERVICE_OID, service.getId());

        // Search identities (for adding IdentityAssertions)
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG, null);
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.USER, null);
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.GROUP, null);

        // Read JMS queues
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.JMS_CONNECTION, null);
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.JMS_ENDPOINT, null);

        if (currentUser != null) {
            // See if we should give the current user admin permission for this service
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.SERVICE, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.SERVICE, OperationType.UPDATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.SERVICE, OperationType.DELETE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.METRICS_BIN, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.AUDIT_MESSAGE, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.USER, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.GROUP, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.CLUSTER_INFO, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.SERVICE_USAGE, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.JMS_CONNECTION, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.JMS_ENDPOINT, OperationType.READ);
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
}
