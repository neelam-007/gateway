/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.*;
import com.l7tech.service.resolution.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.wsdl.WSDLException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import cirrus.hibernate.Session;
import cirrus.hibernate.HibernateException;

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

    private void validate( PublishedService candidateService ) throws WSDLException, DuplicateObjectException{
        // Make sure WSDL is valid
        candidateService.parsedWsdl();
        ServiceResolver resolver;
        Map services = _oidToServiceMap;

        // Check for duplicate services
        Map matchingServices = null;

        for (Iterator i = _resolvers.iterator(); i.hasNext(); ) {
            resolver = (ServiceResolver)i.next();
            matchingServices = resolver.matchingServices( candidateService, services );
            if ( matchingServices != null && matchingServices.size() > 0 ) {
                // One or more matched... Let's see if anyone else matches.
                services = matchingServices;
            }
        }

        if ( matchingServices != null && !matchingServices.isEmpty() )
            throw new DuplicateObjectException( "Duplicate service resolution parameters!" );
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
                classnames.append( " " );
                classnames.append( HttpUriResolver.class.getName() );
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
            validate( service );
            long oid = _manager.save( getContext(), service );
            putService( service );
            fireCreated( service );
            return oid;
        } catch ( SQLException se ) {
            _log.log( Level.SEVERE, se.toString(), se );
            throw new SaveException( se.toString(), se );
        } catch ( WSDLException we ) {
            SaveException se = new SaveException( "Missing or invalid WSDL", we );
            _log.log( Level.SEVERE, se.toString(), se );
            throw se;
        } catch ( DuplicateObjectException doe ) {
            SaveException se = new SaveException( "Duplicate service resolution parameters", doe );
            _log.log( Level.SEVERE, se.toString(), se );
            throw se;
        }
    }

    public int getCurrentPolicyVersion(long policyId) throws FindException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)getContext();
            Session s = context.getSession();
            List results = s.find( getFieldQuery(policyId, F_VERSION) );
            Integer i = (Integer)results.get(0);
            int res = i.intValue();
            return res;
        } catch (HibernateException e) {
            throw new FindException("cannot get version", e);
        } catch (SQLException e) {
            throw new FindException("cannot get version", e);
        }
    }

    // todo, throw version exception directly
    public void update(PublishedService service) throws UpdateException {
        try {
            // check if it's valid
            validate( service );

            // update
            _manager.update( getContext(), service );
            putService( service );
            fireUpdated( service );
        } catch ( SQLException se ) {
            _log.log( Level.SEVERE, se.toString(), se );
            throw new UpdateException( se.toString(), se );
        } catch ( WSDLException we ) {
            UpdateException ue = new UpdateException( "Missing or invalid WSDL", we );
            _log.log( Level.SEVERE, ue.toString(), ue );
            throw ue;
        } catch ( DuplicateObjectException doe ) {
            UpdateException ue = new UpdateException( "Duplicate service resolution parameters" );
            _log.log( Level.SEVERE, ue.toString(), ue );
            throw ue;
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

    private String getFieldQuery(long oid, String getfield ) {
        String alias = getTableName();
        StringBuffer sqlBuffer = new StringBuffer( "SELECT " );
        sqlBuffer.append( alias );
        sqlBuffer.append( "." );
        sqlBuffer.append( getfield );
        sqlBuffer.append( " FROM " );
        sqlBuffer.append( alias );
        sqlBuffer.append( " in class " );
        sqlBuffer.append( getImpClass().getName() );
        sqlBuffer.append( " WHERE " );
        sqlBuffer.append( alias );
        sqlBuffer.append( "." );
        sqlBuffer.append( F_OID );
        sqlBuffer.append( " = " );
        sqlBuffer.append( Long.toString(oid) );
        return sqlBuffer.toString();
    }

    private static final Logger _log = LogManager.getInstance().getSystemLogger();

    protected SortedSet _resolvers;

    protected transient Map _oidToServiceMap = new HashMap();
    protected transient List _serviceListeners = new ArrayList();

    private static final String F_VERSION = "version";
    private static final String F_OID = "oid";
}
