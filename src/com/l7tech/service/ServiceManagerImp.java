/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import cirrus.hibernate.HibernateException;
import cirrus.hibernate.Session;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import com.l7tech.service.resolution.ResolutionManager;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages PublishedService instances.  Note that this object has state, so it should be effectively a Singleton--only get one from the Locator!
 *
 * @author alex
 * @version $Revision$
 */
public class ServiceManagerImp extends HibernateEntityManager implements ServiceManager, TransactionListener {
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
        } catch (InterruptedException e) {
            throw new ObjectModelException("exception building cache", e);
        } catch (IOException e) {
            throw new ObjectModelException("exception building cache", e);
        }
    }

    public PublishedService findByPrimaryKey(long oid) throws FindException {
        try {
            return (PublishedService)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public long save(PublishedService service) throws SaveException {
        // 1. record the service
        PersistenceContext context = null;
        try {
            context = getContext();
            long oid = _manager.save(context, service );
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
            context.registerTransactionListener(this,
                            new TransactionCallbackData(TransactionCallbackData.SAVE, service));
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
        } catch (FindException e) {
            throw new UpdateException("could not get original service", e);
        }

        // check version
        if (original.getVersion() != service.getVersion()) {
            logger.severe("db service has version: " + original.getVersion() + ". requestor service has version: " + service.getVersion());
            throw new VersionException("the published service you are trying to update is no longer valid.");
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
            _manager.update(context, original);
            logger.info( "Updated service #" + service.getOid() );

        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new UpdateException( se.toString(), se );
        }

        // update cache after commit
        try {
            context.registerTransactionListener(this,
                            new TransactionCallbackData(TransactionCallbackData.UPDATE, service));
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
            _manager.delete(context, service );
            resolutionManager.deleteResolutionParameters(service.getOid());
            logger.info("Deleted service " + service.getOid());

        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }

        try {
            context.registerTransactionListener(this,
                            new TransactionCallbackData(TransactionCallbackData.DELETE, service));
        } catch (TransactionException e) {
            String msg = "could not register for transaction callback";
            logger.log(Level.WARNING, msg, e);
            throw new DeleteException(msg, e);
        }
    }

    public void postCommit(Object param) {
        TransactionCallbackData data = null;
        if (param != null && param instanceof TransactionCallbackData) {
            data = (TransactionCallbackData)param;
        } else {
            logger.warning("transaction callback data of wrong type or null");
        }
        switch (data.transactionType) {
            case TransactionCallbackData.DELETE:
            {
                try {
                    ServiceCache.getInstance().removeFromCache(data.service);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "error removing from cache after commit", e);
                }
                break;
            }
            case TransactionCallbackData.SAVE:
            case TransactionCallbackData.UPDATE:
            {
                try {
                    // get service. version property must be up-to-date
                    PublishedService svcnow = null;
                    try {
                        svcnow = findByPrimaryKey(data.service.getOid());
                    } catch (FindException e) {
                        svcnow = null;
                        logger.log(Level.WARNING, "could not get service back", e);
                    }
                    if (svcnow != null) {
                        ServiceCache.getInstance().cache(svcnow);
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "error updating cache after commit", e);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "error updating cache after commit", e);
                }
                break;
            }
        }
    }

    public void postRollback(Object data) {
        // do nothing (cache should not be updated if transaction is rolledback)
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

    private class TransactionCallbackData {
        public TransactionCallbackData(int type, PublishedService service) {
            this.transactionType = type;
            this.service = service;
        }
        public int transactionType;
        public PublishedService service;
        public static final int UPDATE = 1;
        public static final int SAVE = 2;
        public static final int DELETE = 3;
    }

    private static final Logger logger = LogManager.getInstance().getSystemLogger();
    private static final String F_VERSION = "version";
}
