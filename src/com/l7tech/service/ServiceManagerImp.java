/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.message.Request;
import com.l7tech.objectmodel.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.resolution.ResolutionManager;
import com.l7tech.service.resolution.ServiceResolutionException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages PublishedService instances.
 * Note that this object has state, so it should be effectively a Singleton--only get one from the Locator!
 *
 * @author alex
 * @version $Revision$
 */
public class ServiceManagerImp extends HibernateEntityManager implements ServiceManager {
    public String resolveWsdlTarget(String url) throws RemoteException {
        throw new UnsupportedOperationException();
    }

    public ServiceManagerImp() throws ObjectModelException {
        super();

        // build the cache if necessary
        try {
            if (ServiceCache.getInstance().size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.finest("building service cache");
                Collection services = findAll();
                PublishedService service;
                for (Iterator i = services.iterator(); i.hasNext();) {
                    service = (PublishedService)i.next();
                    ServiceCache.getInstance().cache(service);
                }
            }
            // make sure the integrity check is running
            ServiceCache.getInstance().initiateIntegrityCheckProcess();
        } catch (InterruptedException e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }

    public PublishedService findByPrimaryKey(long oid) throws FindException {
        try {
            return (PublishedService)PersistenceManager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public long save(PublishedService service) throws SaveException {
        // 1. record the service
        PersistenceContext context = null;
        try {
            context = getContext();
            long oid = PersistenceManager.save(context, service );
            logger.info( "Saved service #" + oid );
            service.setOid(oid);
        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new SaveException( se.toString(), se );
        }
        // 2. record resolution parameters
        ResolutionManager resolutionManager = new ResolutionManager();
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "cannot save service. duplicate resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        } catch (UpdateException e) {
            String msg = "cannot save service's resolution parameters.";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }

        // 3. update cache on callback
        try {
            final long passedServiceId = service.getOid();

            TransactionListener inlineListener = new TransactionListener() {
                public void postCommit() {
                    // get service. version property must be up-to-date
                    PublishedService svcnow = null;
                    try {
                        svcnow = findByPrimaryKey(passedServiceId);
                    } catch (FindException e) {
                        svcnow = null;
                        logger.log(Level.WARNING, "could not get service back", e);
                    }
                    if (svcnow != null) {
                        try {
                            ServiceCache.getInstance().cache(svcnow);
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, "could not update cache", e);
                        }
                    }
                }
                public void postRollback() {
                    // nothing
                }
            };
            context.registerTransactionListener(inlineListener);
        } catch (TransactionException e) {
            String msg = "could not register for transaction callback";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }

        return service.getOid();
    }

    public int getCurrentPolicyVersion(long policyId) throws FindException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)getContext();
            Session s = context.getSession();
            List results = s.find( getFieldQuery( new Long( policyId ).toString(), F_VERSION) );
            if (results == null || results.isEmpty()) {
                throw new FindException("cannot get version for service " + Long.toString(policyId));
            }
            Integer i = (Integer)results.get(0);
            int res = i.intValue();
            return res;
        } catch (HibernateException e) {
            throw new FindException("cannot get version", e);
        } catch (SQLException e) {
            throw new FindException("cannot get version", e);
        }
    }

    public void update(PublishedService service) throws UpdateException, VersionException {
        PublishedService original = null;
        // check for original service
        try {
            original = findByPrimaryKey(service.getOid());
            if (original == null) {
                throw new UpdateException("Could not retrieve the service "+service.getName()+ ".\n" +
                  "The service has been removed in the meantime.");
            }
        } catch (FindException e) {
            throw new UpdateException("could not get original service", e);
        }

        // check version
        if (original.getVersion() != service.getVersion()) {
            logger.severe("db service has version: " + original.getVersion() + ". requestor service has version: "
                          + service.getVersion());
            throw new VersionException("The service '"+service.getName()+"' has been changed in the meantime by another user.");
        }

        // try recording resolution parameters
        ResolutionManager resolutionManager = new ResolutionManager();
        try {
            resolutionManager.recordResolutionParameters(service);
        } catch (DuplicateObjectException e) {
            String msg = "cannot update service. duplicate resolution parameters";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }

        // update
        PersistenceContext context = null;
        try {
            try {
                original.copyFrom(service);
            } catch (IOException e) {
                throw new UpdateException("could not copy published service", e);
            }
            context = getContext();
            PersistenceManager.update(context, original);
            logger.info( "Updated service #" + service.getOid() );

        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new UpdateException( se.toString(), se );
        }

        // update cache after commit
        try {
            final long passedServiceId = service.getOid();
            TransactionListener inlineListener = new TransactionListener() {
                public void postCommit() {
                    // get service. version property must be up-to-date
                    PublishedService svcnow = null;
                    try {
                        svcnow = findByPrimaryKey(passedServiceId);
                    } catch (FindException e) {
                        svcnow = null;
                        logger.log(Level.WARNING, "could not get service back", e);
                    }
                    if (svcnow != null) {
                        try {
                            ServiceCache.getInstance().cache(svcnow);
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, "could not update cache", e);
                        }
                    }
                }
                public void postRollback() {
                    // nothing
                }
            };
            context.registerTransactionListener(inlineListener);
        } catch (TransactionException e) {
            String msg = "could not register for transaction callback";
            logger.log(Level.WARNING, msg, e);
            throw new UpdateException(msg, e);
        }
    }

    public void delete( PublishedService service ) throws DeleteException {
        ResolutionManager resolutionManager = new ResolutionManager();
        PersistenceContext context = null;
        try {
            context = getContext();
            PersistenceManager.delete(context, service );
            resolutionManager.deleteResolutionParameters(service.getOid());
            logger.info("Deleted service " + service.getOid());
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }

        try {
            final PublishedService deletedService = service;
            TransactionListener inlineListener = new TransactionListener() {
                public void postCommit() {
                    try {
                        ServiceCache.getInstance().removeFromCache(deletedService);
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "could not update cache", e);
                    }
                }
                public void postRollback() {
                    // nothing
                }
            };
            context.registerTransactionListener(inlineListener);
        } catch (TransactionException e) {
            String msg = "could not register for transaction callback";
            logger.log(Level.WARNING, msg, e);
            throw new DeleteException(msg, e);
        }
    }

    public ServerAssertion getServerPolicy(long serviceOid) throws FindException {
        try {
            return ServiceCache.getInstance().getServerPolicy(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing policy from cache", e);
        }
    }

    public PublishedService resolve(Request req) throws ServiceResolutionException {
        return ServiceCache.getInstance().resolve(req);
    }

    public ServiceStatistics getServiceStatistics(long serviceOid) throws FindException {
        try {
            return ServiceCache.getInstance().getServiceStatistics(serviceOid);
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    public Collection getAllServiceStatistics() throws FindException {
        try {
            return ServiceCache.getInstance().getAllServiceStatistics();
        } catch (InterruptedException e) {
            throw new FindException("error accessing statistics from cache", e);
        }
    }

    /**
     * get the service versions as currently recorded in database
     * @return a map whose keys is a Long with service id and values is an Integer with the service version
     * @throws FindException if the query fails for some reason
     */
    public Map getServiceVersions() throws FindException {
        String query = "SELECT " + getTableName() + "." + F_OID + ", " + getTableName() + "." + F_VERSION +
                       " FROM " + getTableName() + " in class " + getImpClass().getName();
        Map output = new HashMap();

        try {
            HibernatePersistenceContext context = (HibernatePersistenceContext)getContext();
            Session s = context.getSession();
            List results = s.find(query);
            if (results == null || results.isEmpty()) {
                // logger.fine("no version info to return");
            } else {
                for (Iterator i = results.iterator(); i.hasNext();) {
                    Object[] toto = (Object[])i.next();
                    output.put(toto[0], toto[1]);
                }
            }
            return output;
        } catch (SQLException e) {
            String msg = "error getting versions";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
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

    public void destroy() {
        ServiceCache.getInstance().destroy();
    }

    private static final Logger logger = Logger.getLogger(ServiceManagerImp.class.getName());
    private static final String F_VERSION = "version";
}
