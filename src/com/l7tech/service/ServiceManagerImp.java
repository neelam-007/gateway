/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.*;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.service.resolution.ServiceResolver;
import com.l7tech.service.resolution.SoapActionResolver;
import com.l7tech.service.resolution.UrnResolver;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.wsdl.WSDLException;
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
        return null;
    }

    /**
     * Attempts to find a single PublishedService that can satisfy the request. If no matching PublishedService is found, or if multiple PublishedServices could satisfy the request, null is returned.
     * @param request
     * @return null if multiple or no PublishedServices could be found to satisfy the request.
     * @throws ServiceResolutionException
     */
    public PublishedService resolveService(Request request) throws ServiceResolutionException {
        Set services = new HashSet();
        Set resolvedServices = null;
        int size;
        services.addAll( serviceSet() );
        Iterator i = _resolvers.iterator();

        while ( i.hasNext() ) {
            ServiceResolver resolver = (ServiceResolver)i.next();
            resolvedServices = resolver.resolve( request, services );
            if ( resolvedServices != null ) {
                // If for some reason it's null, we'll just pass the same old set to the next one.
                size = resolvedServices.size();
                switch ( size ) {
                case 0:
                    // Didn't find anything... Go around to the next Resolver
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

    synchronized void putService( PublishedService service ) {
        _oidToServiceMap.put( new Long( service.getOid() ), service );
    }

    synchronized void removeService( PublishedService service ) {
        _oidToServiceMap.remove( new Long( service.getOid() ) );
    }

    synchronized Set serviceSet() {
        HashSet set = new HashSet();
        set.addAll( _oidToServiceMap.values() );
        return set;
    }

    private boolean validate( PublishedService candidateService ) {
        try {
            // Make sure WSDL is valid
            candidateService.parsedWsdl();
            ServiceResolver resolver;
            Map services = _oidToServiceMap;

            Map matchingServices = null;

            for (Iterator i = _resolvers.iterator(); i.hasNext(); ) {
                resolver = (ServiceResolver)i.next();
                matchingServices = resolver.matchingServices( candidateService, services );
                if ( matchingServices != null && matchingServices.size() > 0 ) {
                    // One or more matched... Let's see if anyone else matches.
                    services = matchingServices;
                }
            }

            return ( matchingServices == null || matchingServices.isEmpty() );
        } catch ( WSDLException we ) {
            _log.log( Level.SEVERE, we.toString(), we );
            return false;
        }
    }

    public ServiceManagerImp() throws ObjectModelException {
        super();

        synchronized( this ) {
            Collection services = findAll();

            PublishedService service;
            for (Iterator i = services.iterator(); i.hasNext();) {
                service = (PublishedService)i.next();
                putService( service );
            }

            String serviceResolvers = null;
            try {
                InitialContext ic = new InitialContext();
                serviceResolvers = (String)ic.lookup( "java:comp/env/ServiceResolvers" );
            } catch ( NamingException ne ) {
                _log.log( Level.INFO, ne.toString(), ne );
            }

            if ( serviceResolvers == null ) {
                StringBuffer classnames = new StringBuffer();
                classnames.append( UrnResolver.class.getName() );
                classnames.append( " " );
                classnames.append( SoapActionResolver.class.getName() );
                serviceResolvers = classnames.toString();
            }

            _resolvers = new TreeSet();
            StringTokenizer stok = new StringTokenizer( serviceResolvers );
            String className;
            Class resolverClass;
            ServiceResolver resolver;

            while ( stok.hasMoreTokens() ) {
                className = stok.nextToken();
                try {
                    resolverClass = Class.forName( className );
                    resolver = (ServiceResolver)resolverClass.newInstance();
                    resolver.setServices( serviceSet() );
                    addServiceListener( resolver );

                    _resolvers.add( resolver );
                } catch ( ClassNotFoundException cnfe ) {
                    _log.log( Level.SEVERE, cnfe.toString(), cnfe );
                } catch ( InstantiationException ie ) {
                    _log.log( Level.SEVERE, ie.toString(), ie );
                } catch ( IllegalAccessException iae ) {
                    _log.log( Level.SEVERE, iae.toString(), iae );
                }
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
            if ( !validate( service ) ) {
                SaveException se = new SaveException( "Duplicate service resolution parameters" );
                _log.log( Level.SEVERE, se.toString(), se );
                throw se;
            }
            long oid = _manager.save( getContext(), service );
            putService( service );
            fireCreated( service );
            return oid;
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update(PublishedService service) throws UpdateException {
        try {
            if ( !validate( service ) ) {
                UpdateException ue = new UpdateException( "Duplicate service resolution parameters" );
                _log.log( Level.SEVERE, ue.toString(), ue );
                throw ue;
            }
            _manager.update( getContext(), service );
            putService( service );
            fireUpdated( service );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public void delete( PublishedService service ) throws DeleteException {
        try {
            _manager.delete( getContext(), service );
            removeService( service );
            fireDeleted( service );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    synchronized void fireCreated( PublishedService service ) {
        for (Iterator i = _serviceListeners.iterator(); i.hasNext();) {
            ServiceListener listener = (ServiceListener) i.next();
            listener.serviceCreated( service );
        }
    }

    synchronized void fireUpdated( PublishedService service ) {
        for (Iterator i = _serviceListeners.iterator(); i.hasNext();) {
            ServiceListener listener = (ServiceListener)i.next();
            listener.serviceUpdated( service );
        }
    }

    synchronized void fireDeleted( PublishedService service ) {
        for (Iterator i = _serviceListeners.iterator(); i.hasNext();) {
            ServiceListener listener = (ServiceListener) i.next();
            listener.serviceDeleted( service );
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

    public synchronized void addServiceListener( ServiceListener listener ) {
        _serviceListeners.add( listener );
    }

    private static final Logger _log = LogManager.getInstance().getSystemLogger();

    protected SortedSet _resolvers;

    protected transient Map _oidToServiceMap = new HashMap();
    protected transient List _serviceListeners = new ArrayList();
}
