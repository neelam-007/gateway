/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.service;

import com.l7tech.common.message.Message;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.service.resolution.ResolutionManager;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.beans.BeansException;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Query;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages PublishedService instances.
 */
public class ServiceManagerImp extends HibernateEntityManager implements ServiceManager, ApplicationContextAware, ApplicationListener {
    private ServiceCache serviceCache;
    private ApplicationContext applicationContext;

    private final String HQL_FIND_VERSION_BY_OID =
            "SELECT " + getTableName() + "." + F_VERSION +
                    " FROM " + getTableName() +
                    " IN CLASS " + getImpClass().getName() +
                    " WHERE " + getTableName() + "." + F_OID + " = ?";

    private final String HQL_FIND_ALL_VERSIONS = 
            "SELECT " +
                    getTableName() + "." + F_OID + ", " +
                    getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
                " IN CLASS " + getImpClass().getName();

    public void setVisitorClassnames(String visitorClassnames) {
        String[] names = visitorClassnames.split(",\\s*");
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            try {
                Class.forName(name);
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING, "PolicyVisitor classname " + name + " could not be loaded.", e);
            }
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String resolveWsdlTarget(String url) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    public PublishedService findByPrimaryKey(long oid) throws FindException {
        return (PublishedService)findByPrimaryKey(getImpClass(), oid);
    }

    public long save(PublishedService service) throws SaveException {
        // 1. record the service
        long oid = ((Long)getHibernateTemplate().save(service)).longValue();
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
                            TarariLoader.updateSchemasToCard(applicationContext);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }
        });

        return service.getOid();
    }

    public int getCurrentPolicyVersion(long policyId) throws FindException {
        try {
            Session s = getSession();
            Query q = s.createQuery(HQL_FIND_VERSION_BY_OID);
            q.setLong(0, policyId);
            List results = q.list();
            if (results == null || results.isEmpty() || results.size() > 1) {
                throw new FindException("cannot get version for service " + Long.toString(policyId));
            }
            Integer i = (Integer)results.get(0);
            return i.intValue();
        } catch (HibernateException e) {
            throw new FindException("cannot get version", e);
        }
    }

    public void update(PublishedService service) throws UpdateException, VersionException {
        final PublishedService original;
        // check for original service
        try {
            original = findByPrimaryKey(service.getOid());
            if (original == null) {
                throw new UpdateException("Could not retrieve the service " + service.getName() + ".\n" +
                  "The service has been removed in the meantime.");
            }
        } catch (FindException e) {
            throw new UpdateException("could not get original service", e);
        }

        // check version
        if (original.getVersion() != service.getVersion()) {
            logger.severe("db service has version: " + original.getVersion() + ". requestor service has version: "
              + service.getVersion());
            throw new VersionException("The service '" + service.getName() + "' has been changed in the meantime by another user.");
        }

        // try recording resolution parameters
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "cannot update service. duplicate resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // update
        try {
            original.copyFrom(service);
        } catch (IOException e) {
            throw new UpdateException("could not copy published service", e);
        }
        getHibernateTemplate().update(original);
        logger.info("Updated service " + service.getName() + "  #" + service.getOid());


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
                            TarariLoader.updateSchemasToCard(applicationContext);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }
        });
    }

    public void delete(PublishedService service) throws DeleteException {
        getHibernateTemplate().delete(service);
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

    public ServerAssertion getServerPolicy(long serviceOid) throws FindException {
        try {
            return serviceCache.getServerPolicy(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing policy from cache", e);
        }
    }

    public PublishedService resolve(Message req) throws ServiceResolutionException {
        return serviceCache.resolve(req);
    }

    public ServiceStatistics getServiceStatistics(long serviceOid) throws FindException {
        try {
            return serviceCache.getServiceStatistics(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    public Collection getAllServiceStatistics() throws FindException {
        try {
            return serviceCache.getAllServiceStatistics();
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    /**
     * get the service versions as currently recorded in database
     *
     * @return a map whose keys is a Long with service id and values is an Integer with the service version
     * @throws FindException if the query fails for some reason
     */
    public Map getServiceVersions() throws FindException {
        Map output = new HashMap();

        try {
            Session s = getSession();
            Query q = s.createQuery(HQL_FIND_ALL_VERSIONS);
            List results = q.list();
            if (results == null || results.isEmpty()) {
                // logger.fine("no version info to return");
            } else {
                for (Iterator i = results.iterator(); i.hasNext();) {
                    Object[] toto = (Object[])i.next();
                    output.put(toto[0], toto[1]);
                }
            }
            return output;
        } catch (HibernateException e) {
            String msg = "error getting versions";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }


    public Class getImpClass() {
        return PublishedService.class;
    }

    public Class getInterfaceClass() {
        return PublishedService.class;
    }

    public String getTableName() {
        return "published_service";
    }

    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    public void setServiceCache(ServiceCache serviceCache) {
        this.serviceCache = serviceCache;
    }

    /**
     * Set the resolution manager. This is managed by Spring runtime.
     *
     * @param resolutionManager
     */
    public void setResolutionManager(ResolutionManager resolutionManager) {
        this.resolutionManager = resolutionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
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

    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if(SchemaEntry.class.isAssignableFrom(eie.getEntityClass())) {
                logger.info("Updating Tarari schemas.");
                try {
                    TarariLoader.updateSchemasToCard(applicationContext);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error updating schemas to tarari card " + ExceptionUtils.getMessage(e), e);
                }
            }
        }

    }

    private void initializeServiceCache() throws ObjectModelException {
        // build the cache if necessary
        try {
            if (serviceCache.size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.info("building service cache");
                Collection services = findAll();
                PublishedService service;
                for (Iterator i = services.iterator(); i.hasNext();) {
                    service = (PublishedService)i.next();
                    serviceCache.cache(service);
                }
                TarariLoader.compile();
                try {
                    TarariLoader.updateSchemasToCard(applicationContext);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "error uploading schemas to tarari card", e);
                }
            }
            // make sure the integrity check is running
            logger.info("initiate service cache version check process");
            serviceCache.initiateIntegrityCheckProcess();
        } catch (Exception e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }

    private ResolutionManager resolutionManager;
    private PlatformTransactionManager transactionManager; // required for TransactionTemplate
    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());
    private static final String F_VERSION = "version";
}
