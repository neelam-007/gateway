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
        try {
            long oid = _manager.save( getContext(), service );
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
        try {
            try {
                original.copyFrom(service);
            } catch (IOException e) {
                throw new UpdateException("could not copy published service", e);
            }
            _manager.update(getContext(), original);
            logger.info( "Updated service #" + service.getOid() );
        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new UpdateException( se.toString(), se );
        }
    }

    public void delete( PublishedService service ) throws DeleteException {
        ResolutionManager resolutionManager = new ResolutionManager();
        try {
            _manager.delete( getContext(), service );
            resolutionManager.deleteResolutionParameters(service.getOid());
            logger.info( "Deleted service " + service.getOid() );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
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

    private static final Logger logger = LogManager.getInstance().getSystemLogger();

    private static final String F_VERSION = "version";
}
