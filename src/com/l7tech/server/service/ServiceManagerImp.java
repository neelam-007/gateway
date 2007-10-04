/*
 * Copyright (C) 2003-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.server.service;

import com.l7tech.cluster.ServiceUsage;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import static com.l7tech.common.security.rbac.EntityType.*;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.service.ServiceAdmin;
import org.springframework.transaction.TransactionStatus;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Manages PublishedService instances.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class ServiceManagerImp
        extends HibernateEntityManager<PublishedService, EntityHeader>
        implements ServiceManager
{
    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());

    private ResolutionManager resolutionManager;
    private ServiceCache serviceCache;
    private RoleManager roleManager;

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

                        if (logger.isLoggable(Level.WARNING)) {
                            String ordinal = ass == null ? "" : "#" + Integer.toString(ass.getOrdinal());
                            String what = ass == null ? "<unknown>" : "(" + ass.getClass().getSimpleName() + ")";
                            String msg = "Disabling PublishedService #{0} ({1}); policy could not be compiled (assertion {2} {3})";
                            String servUri = service.getRoutingUri();
                            String servName = service.getName();
                            String logServName = servUri == null ? servName : (servName + ": " + servUri);
                            logger.log(Level.WARNING, MessageFormat.format(msg, service.getOid(), logServName, ordinal, what));
                        }
                        // We don't actually disable the service here -- only the admin should be doing that.
                        // Instead, we will let the service cache continue to monitor the situation
                    } catch (Exception e) {
                        LogRecord r = new LogRecord(Level.WARNING, "Disabling PublishedService #{0} ({1}); policy could not be compiled");
                        String servUri = service.getRoutingUri();
                        String servName = service.getName();
                        String logServName = servUri == null ? servName : (servName + ": " + servUri);
                        r.setParameters(new Object[] { service.getOid(), logServName});
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
                omnipotent = roleManager.isPermittedForAllEntities(currentUser, SERVICE, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SERVICE, UPDATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SERVICE, DELETE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, METRICS_BIN, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, AUDIT_MESSAGE, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, ID_PROVIDER_CONFIG, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, USER, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, GROUP, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, CLUSTER_INFO, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SERVICE_USAGE, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, JMS_CONNECTION, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, JMS_ENDPOINT, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SAMPLE_MESSAGE, CREATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SAMPLE_MESSAGE, READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SAMPLE_MESSAGE, UPDATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, SAMPLE_MESSAGE, DELETE);
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
