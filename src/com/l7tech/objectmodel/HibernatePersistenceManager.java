/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.*;
import cirrus.hibernate.Transaction;
import cirrus.hibernate.type.Type;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.io.*;

/**
 * @author alex
 * @version $Revision$
 */
public class HibernatePersistenceManager extends PersistenceManager {
    public static String DATASOURCE_URL_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.datasourceurl";
    public static String SESSIONFACTORY_URL_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.sessionfactoryurl";
    public static String DEFAULT_PROPERTIES_RESOURCEPATH = "com/l7tech/objectmodel/hibernatepersistence.properties";
    public static String PROPERTIES_RESOURCEPATH_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.properties.resourcepath";
    public static String DEFAULT_HIBERNATE_RESOURCEPATH = "com/l7tech/objectmodel/hibernate.cfg.xml";

    public static void initialize() throws IOException, SQLException, NamingException {
        HibernatePersistenceManager me = new HibernatePersistenceManager();
        PersistenceManager.setInstance( me );
    }

    private HibernatePersistenceManager() throws IOException, SQLException, NamingException {
        _initialContext = new InitialContext();

        Properties props = new Properties();

        String resourcePath = System.getProperty( PROPERTIES_RESOURCEPATH_PROPERTY );
        if ( resourcePath == null || resourcePath.length() == 0 )
            resourcePath = DEFAULT_PROPERTIES_RESOURCEPATH;

        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        props.load( is );
        String sessionFactoryUrl = props.getProperty( SESSIONFACTORY_URL_PROPERTY );

        try {
            Hibernate.configure();
            _sessionFactory = (SessionFactory)_initialContext.lookup( sessionFactoryUrl );
        } catch ( HibernateException he ) {
            he.printStackTrace();
            throw new SQLException( he.toString() );
        }
    }

    public PersistenceContext doGetContext() throws SQLException {
        try {
            return new HibernatePersistenceContext( getSession() );
        } catch ( HibernateException he ) {
            throw new SQLException( he.toString() );
        }
    }

    private Session getSession() throws HibernateException, SQLException {
        if ( _sessionFactory == null ) throw new IllegalStateException("HibernatePersistenceManager must be initialized before calling getSession()!");
        Session session = _sessionFactory.openSession();
        //Connection conn = session.connection();
        //conn.setAutoCommit(false);
        return session;
    }

    private SessionFactory _sessionFactory;
    private InitialContext _initialContext;

    class ContextHolder {
        HibernatePersistenceContext _context;
        Session _session;
    }

    private ContextHolder getContextHolder( PersistenceContext context ) {
        if ( context == null || !(context instanceof HibernatePersistenceContext) )
            throw new IllegalArgumentException( "Invalid context passed to a HibernatePersistenceManager method!");
        ContextHolder holder = new ContextHolder();
        holder._context = (HibernatePersistenceContext)context;
        holder._session = holder._context.getSession();

        return holder;
    }

    void doBeginTransaction( PersistenceContext context ) throws TransactionException {
        context.beginTransaction();
    }

    void doCommitTransaction( PersistenceContext context ) throws TransactionException {
        context.commitTransaction();
    }

    void doRollbackTransaction( PersistenceContext context ) throws TransactionException {
        context.rollbackTransaction();
    }

    List doFind( PersistenceContext context, String query ) throws FindException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;
        try {
            return s.find( query );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    List doFind( PersistenceContext context, String query, Object param, Class paramClass) throws FindException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;
        try {
            return s.find( query, param, Hibernate.serializable(paramClass) );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    List doFind( PersistenceContext context, String query, Object[] params, Class[] paramClasses) throws FindException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;
        Type[] types = new Type[ paramClasses.length ];
        for ( int i = 0; i < paramClasses.length; i++ )
            types[i] = Hibernate.serializable(paramClasses[i]);

        try {
            return s.find( query, params, types );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    Entity doFindByPrimaryKey( PersistenceContext context, Class clazz, long oid) throws FindException {
        return doFindByPrimaryKey( context, clazz, oid, false );
    }

    Entity doFindByPrimaryKey( PersistenceContext context, Class clazz, long oid, boolean forUpdate) throws FindException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;
        try {
            Object o = s.load( clazz, new Long(oid), forUpdate ? LockMode.WRITE : LockMode.READ );
            // TODO: Is it OK to throw a ClassCastException here?
            return (Entity)o;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        }
    }

    long doSave( PersistenceContext context, Entity obj) throws SaveException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;

        try {
            Object key = s.save( obj );
            if ( key instanceof Long ) {
                return ((Long)key).longValue();
            } else {
                throw new RuntimeException( "Generated primary key is not a long!");
            }
        } catch ( HibernateException he ) {
            throw new SaveException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    void doDelete( PersistenceContext context, Entity obj) throws DeleteException {
        ContextHolder h = getContextHolder( context );
        try {
            h._session.delete( obj );
        } catch ( HibernateException he ) {
            throw new DeleteException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }


}
