/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import cirrus.hibernate.HibernateException;
import cirrus.hibernate.Session;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.service.resolution.ServiceResolver;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
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

    /**
     * Attempts to find a single PublishedService that can satisfy the request. If no matching PublishedService is found, or if multiple PublishedServices could satisfy the request, null is returned.
     * @param request
     * @return null if multiple or no PublishedServices could be found to satisfy the request.
     * @throws ServiceResolutionException
     */
    public PublishedService resolveService( Request request ) throws ServiceResolutionException {
        Set services = clonedServiceSet();
        if ( services == null || services.isEmpty() ) return null;

        Set resolvedServices = null;
        int size;
        Iterator i = _resolvers.iterator();

        while ( i.hasNext() ) {
            ServiceResolver resolver = (ServiceResolver)i.next();
            resolvedServices = resolver.resolve( request, services );
            if ( resolvedServices != null ) {
                // If for some reason it's null, we'll just pass the same old set to the next one.
                size = resolvedServices.size();
                switch ( size ) {
                case 0:
                    // Didn't find anything... Go around to the next Resolver with the same subset
                    break;
                case 1:
                    // Found one service--done!
                    return (PublishedService)resolvedServices.iterator().next();
                default:
                    // Found more than one Service... Pass the subset to the next Resolver.
                    services = resolvedServices;
                    break;
                }
            }
        }

        return null;
    }

    /**
     * @param service
     */
    private void putCachedService( PublishedService service ) {
        _oidToServiceMap.put( new Long( service.getOid() ), service );
    }

    /**
     * @param service
     */
    private void removeCachedService( PublishedService service ) {
        _oidToServiceMap.remove( new Long( service.getOid() ) );
    }

    /**
     * Acquires the read lock.
     * @return
     */
    private Set clonedServiceSet() {
        Sync read = _rwlock.readLock();
        HashSet set = new HashSet();
        try {
            read.acquire();
            set.addAll( _oidToServiceMap.values() );
            return set;
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring read lock!" );
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if ( read != null ) read.release();
        }
    }

    public Map serviceMap() {
        return Collections.unmodifiableMap( _oidToServiceMap );
    }

    public Iterator allServices() {
        return Collections.unmodifiableSet( clonedServiceSet() ).iterator();
    }

    private void validate( PublishedService candidateService ) throws WSDLException, DuplicateObjectException{
        // Make sure WSDL is valid
        candidateService.parsedWsdl();
        ServiceResolver resolver;
        Map services = _oidToServiceMap;

        // Check for duplicate services
        for (Iterator i = _resolvers.iterator(); i.hasNext(); ) {
            resolver = (ServiceResolver)i.next();
            services = resolver.matchingServices( candidateService, services );
            if ( services == null || services .size() == 0 ) {
                // At least one resolver doesn't think it's a duplicate--that's good enough!
                return;
            }
        }

        throw new DuplicateObjectException( "Duplicate service resolution parameters!" );
    }

    public ServiceManagerImp() throws ObjectModelException {
        super();
        _resolvers = new TreeSet();

        // Load & cache all services
        Collection services = findAll();

        PublishedService service;
        for (Iterator i = services.iterator(); i.hasNext();) {
            service = (PublishedService)i.next();
            putCachedService( service );
        }

        // Initialize service resolvers
        String serviceResolvers = ServerConfig.getInstance().getServiceResolvers();

        StringTokenizer stok = new StringTokenizer( serviceResolvers );
        String className;
        Class resolverClass;
        ServiceResolver resolver;
        Set serviceSet = clonedServiceSet();

        while ( stok.hasMoreTokens() ) {
            className = stok.nextToken();
            try {
                resolverClass = Class.forName( className );
                resolver = (ServiceResolver)resolverClass.newInstance();
                resolver.setServices( serviceSet );
                addServiceListener( resolver );

                _resolvers.add( resolver );
            } catch ( ClassNotFoundException cnfe ) {
                logger.log( Level.SEVERE, cnfe.toString(), cnfe );
            } catch ( InstantiationException ie ) {
                logger.log( Level.SEVERE, ie.toString(), ie );
            } catch ( IllegalAccessException iae ) {
                logger.log( Level.SEVERE, iae.toString(), iae );
            }
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
        try {
            validate( service );
            long oid = _manager.save( getContext(), service );
            logger.info( "Saved service #" + oid );
            return oid;
        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new SaveException( se.toString(), se );
        } catch ( WSDLException we ) {
            SaveException se = new SaveException( "Missing or invalid WSDL", we );
            logger.log( Level.SEVERE, se.toString(), se );
            throw se;
        } catch ( DuplicateObjectException doe ) {
            SaveException se = new SaveException( "Duplicate service resolution parameters", doe );
            logger.log( Level.SEVERE, se.toString(), se );
            throw se;
        }
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
        try {
            original = findByPrimaryKey(service.getOid());
        } catch (FindException e) {
            throw new UpdateException("could not get original service", e);
        }

        try {
            // check if it's valid
            validate( service );

            // check version
            if (original.getVersion() != service.getVersion()) {
                logger.severe("db service has version: " + original.getVersion() + ". requestor service has version: " + service.getVersion());
                throw new VersionException("the published service you are trying to update is no longer valid.");
            }

            // copy back into hibernate object
            try {
                original.copyFrom(service);
            } catch (IOException e) {
                throw new UpdateException("could not copy published service", e);
            }

            // update
            _manager.update(getContext(), original);

            logger.info( "Updated service #" + service.getOid() );
        } catch ( SQLException se ) {
            logger.log( Level.SEVERE, se.toString(), se );
            throw new UpdateException( se.toString(), se );
        } catch ( WSDLException we ) {
            UpdateException ue = new UpdateException( "Missing or invalid WSDL", we );
            logger.log( Level.SEVERE, ue.toString(), ue );
            throw ue;
        } catch ( DuplicateObjectException doe ) {
            UpdateException ue = new UpdateException( "Duplicate service resolution parameters" );
            logger.log( Level.SEVERE, ue.toString(), ue );
            throw ue;
        }
    }

    public void delete( PublishedService service ) throws DeleteException {
        try {
            _manager.delete( getContext(), service );
            logger.info( "Deleted service " + service.getOid() );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public void postCreate( PublishedService service ) {
        Sync write = _rwlock.writeLock();
        try {
            write.acquire();
            putCachedService( service );
            for (Iterator i = _serviceListeners.iterator(); i.hasNext();) {
                ServiceListener listener = (ServiceListener) i.next();
                listener.serviceCreated( service );
            }
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            if ( write != null ) write.release();
        }
    }

    public void postUpdate( PublishedService service ) {
        Sync write = _rwlock.writeLock();
        try {
            write.acquire();
            removeCachedService( service );
            putCachedService( service );
            for (Iterator i = _serviceListeners.iterator(); i.hasNext();) {
                ServiceListener listener = (ServiceListener)i.next();
                listener.serviceUpdated( service );
            }
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            if ( write != null ) write.release();
        }
    }

    public void postDelete( PublishedService service ) {
        Sync write = _rwlock.writeLock();
        try {
            write.acquire();
            removeCachedService( service );
            for (Iterator i = _serviceListeners.iterator(); i.hasNext();) {
                ServiceListener listener = (ServiceListener) i.next();
                listener.serviceDeleted( service );
            }
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            if ( write != null ) write.release();
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

    public void addServiceListener( ServiceListener listener ) {
        Sync write = _rwlock.writeLock();
        try {
            write.acquire();
            _serviceListeners.add( listener );
        } catch ( InterruptedException ie ) {
            logger.fine( "Interrupted while acquiring write lock!" );
            Thread.currentThread().interrupt();
        } finally {
            if ( write != null ) write.release();
        }
    }

    private ReadWriteLock _rwlock = new WriterPreferenceReadWriteLock();

    private static final Logger logger = LogManager.getInstance().getSystemLogger();

    protected final SortedSet _resolvers;

    protected transient Map _oidToServiceMap = new HashMap();
    protected transient List _serviceListeners = new ArrayList();

    private static final String F_VERSION = "version";
}
