/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.*;
import cirrus.hibernate.type.Type;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.io.*;

/**
 * @author alex
 * @version $Revision$
 */
public class HibernatePersistenceManager extends PersistenceManager {
    public static final String DATASOURCE_URL_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.datasourceurl";
    public static final String SESSIONFACTORY_URL_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.sessionfactoryurl";
    public static final String DEFAULT_PROPERTIES_RESOURCEPATH = "com/l7tech/objectmodel/hibernatepersistence.properties";
    public static final String PROPERTIES_RESOURCEPATH_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.properties.resourcepath";
//    public static final String DEFAULT_HIBERNATE_RESOURCEPATH = "hibernate.cfg.xml";
    public static final String DEFAULT_HIBERNATE_RESOURCEPATH = "SSG.hbm.xml";
    public static final String HIBERNATE_RESOURCEPATH_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.hibernateconfigxml";

    public static void initialize() throws IOException, SQLException {
        HibernatePersistenceManager me = new HibernatePersistenceManager();
        PersistenceManager.setInstance( me );
    }

    private HibernatePersistenceManager() throws IOException, SQLException {
        //_initialContext = new InitialContext();

        Properties props = new Properties();

        String resourcePath = System.getProperty( PROPERTIES_RESOURCEPATH_PROPERTY );
        if ( resourcePath == null || resourcePath.length() == 0 )
            resourcePath = DEFAULT_PROPERTIES_RESOURCEPATH;

        String hibernateXmlResourcePath = System.getProperty( HIBERNATE_RESOURCEPATH_PROPERTY );
        if ( hibernateXmlResourcePath == null || hibernateXmlResourcePath.length() == 0 )
            hibernateXmlResourcePath = DEFAULT_HIBERNATE_RESOURCEPATH;

        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        props.load( is );
        //String sessionFactoryUrl = props.getProperty( SESSIONFACTORY_URL_PROPERTY );

        try {
            //Hibernate.configure();
            //_sessionFactory = (SessionFactory)_initialContext.lookup( sessionFactoryUrl );

            Datastore ds = Hibernate.createDatastore();
            ds.storeResource( hibernateXmlResourcePath, getClass().getClassLoader() );
            _sessionFactory = ds.buildSessionFactory();
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

    Session getSession() throws HibernateException, SQLException {
        if ( _sessionFactory == null ) throw new IllegalStateException("HibernatePersistenceManager must be initialized before calling getSession()!");
        Session session = _sessionFactory.openSession();
        //Connection conn = session.connection();
        //conn.setAutoCommit(false);
        return session;
    }

    private SessionFactory _sessionFactory;
    // private InitialContext _initialContext;

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
        } catch ( ObjectNotFoundException onfe ) {
            return EMPTYLIST;
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    private Type getHibernateType( Object param ) throws FindException {
        Type type;
        if ( param instanceof String )
            type = Hibernate.STRING;
        else if ( param instanceof Long )
            type = Hibernate.LONG;
        else
            throw new FindException( "I don't know how to find with parameters that aren't either String or Long!" );
        return type;

    }

    List doFind( PersistenceContext context, String query, Object param, Class paramClass) throws FindException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;
        try {
            return s.find( query, param, getHibernateType(param) );
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
            types[i] = getHibernateType(params[i]);

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
            return (Entity)o;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( HibernateException he ) {
            throw new FindException( he.toString(), he );
        }
    }

    void doUpdate( PersistenceContext context, Entity obj ) throws UpdateException {
        ContextHolder h = getContextHolder( context );
        Session s = h._session;

        try {
            s.saveOrUpdate( obj );
        } catch ( HibernateException he ) {
            throw new UpdateException( he.toString(), he );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
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
