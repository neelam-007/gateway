/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.*;
import cirrus.hibernate.Transaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.*;
import java.sql.SQLException;
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
        //String dsUrl = props.getProperty(DATASOURCE_URL_PROPERTY);
        String sessionFactoryUrl = props.getProperty( SESSIONFACTORY_URL_PROPERTY );
        //if ( dsUrl == null || dsUrl.length() == 0 ) throw new RuntimeException( "Couldn't find property " + DATASOURCE_URL_PROPERTY + "!" );

        //_dataSource = (DataSource)_initialContext.lookup(dsUrl);

        try {
            Hibernate.configure();
            //_dataStore = Hibernate.createDatastore().storeResource( DEFAULT_HIBERNATE_RESOURCEPATH, getClass().getClassLoader() );
            _sessionFactory = (SessionFactory)_initialContext.lookup( sessionFactoryUrl );
        } catch ( HibernateException he ) {
            he.printStackTrace();
            throw new SQLException( he.toString() );
        }
    }

    public PersistenceContext doGetContext() throws SQLException {
        return new HibernatePersistenceContext( getSession() );
    }

    private Session getSession() throws SQLException {
        if ( _sessionFactory == null ) throw new IllegalStateException("HibernatePersistenceManager must be initialized before calling getSession()!");
        return _sessionFactory.openSession();
    }

    //private DataSource _dataSource;
    //private Datastore _dataStore;
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
        ContextHolder h = getContextHolder( context );
        Session s = h._session;
        try {
            Transaction txn = s.beginTransaction();
            h._context.setTransaction( txn );
        } catch ( HibernateException he ) {
            throw new TransactionException( he.toString(), he );
        }
    }

    void doCommitTransaction( PersistenceContext context ) throws TransactionException {
        ContextHolder h = getContextHolder( context );
        Transaction txn = h._context.getTransaction();
        if ( txn == null ) throw new IllegalStateException( "No transaction is active!");
        try {
            txn.commit();
        } catch ( HibernateException he ) {
            throw new TransactionException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new TransactionException( se.toString(), se );
        }
    }

    void doRollbackTransaction( PersistenceContext context ) throws TransactionException {
        ContextHolder h = getContextHolder( context );
        Transaction txn = h._context.getTransaction();
        if ( txn == null ) throw new IllegalStateException( "No transaction is active!");
        try {
            txn.rollback();
        } catch ( HibernateException he ) {
            throw new TransactionException( he.toString(), he );
        }
    }

    List doFind( PersistenceContext context, String query, Object param, Class paramClass) {
        ContextHolder h = getContextHolder( context );
        return null;
    }

    List doFind( PersistenceContext context, String query, Object[] params, Class[] paramClasses) {
        ContextHolder h = getContextHolder( context );
        return null;
    }

    Entity doFindByPrimaryKey( PersistenceContext context, Class clazz, long oid) throws FindException {
        ContextHolder h = getContextHolder( context );
        return null;
    }

    Entity doFindByPrimaryKey( PersistenceContext context, Class clazz, long oid, boolean forUpdate) throws FindException {
        ContextHolder h = getContextHolder( context );
        return null;
    }

    long doSave( PersistenceContext context, Entity obj) throws SaveException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;

        try {
            Object key = s.save( obj );
            if ( key.getClass().isAssignableFrom(Long.TYPE) ) {
                return ((Long)key).longValue();
            } else {
                throw new RuntimeException( "Primary key is not a long!");
            }
        } catch ( HibernateException he ) {
            throw new SaveException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    void doDelete( PersistenceContext context, Entity obj) {
        ContextHolder h = getContextHolder( context );
    }


}
