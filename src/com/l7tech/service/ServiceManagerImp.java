/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.util.*;
import java.sql.SQLException;

import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ServiceManagerImp extends HibernateEntityManager implements ServiceManager {
    public String resolveWsdlTarget(String url) throws RemoteException {
        return null;
    }

    public PublishedService resolveService(Request request) throws ServiceResolutionException {
        return null;
    }

    public ServiceManagerImp() throws NamingException, ObjectModelException {
        super();

        Collection services = findAll();

        InitialContext ic = new InitialContext();
        String serviceResolvers = (String)ic.lookup( "java:comp/env/ServiceResolvers" );
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
                resolver.setServices( services );

                _resolvers.add( resolver );
            } catch ( ClassNotFoundException cnfe ) {
                log.error( cnfe );
            } catch ( InstantiationException ie ) {
                log.error( ie );
            } catch ( IllegalAccessException iae ) {
                log.error( iae );
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
            return _manager.save( getContext(), service );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update(PublishedService service) throws UpdateException {
        try {
            _manager.update( getContext(), service );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public void delete( PublishedService service ) throws DeleteException {
        try {
            _manager.delete( getContext(), service );
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

    private static final Category log = Category.getInstance(ServiceManagerImp.class);
    protected SortedSet _resolvers;
    protected transient Collection _allServices;
    //protected transient Map _paramNameMap = new HashMap();
    //protected transient Map _paramToServiceMap = new HashMap();
}
