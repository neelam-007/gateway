/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import cirrus.hibernate.*;

import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
    public static String DEFAULT_PROPERTIES_RESOURCEPATH = "com/l7tech/objectmodel/hibernatepersistence.properties";
    public static String PROPERTIES_RESOURCEPATH_PROPERTY = "com.l7tech.objectmodel.hibernatepersistence.properties.resourcepath";

    public static void initialize() throws IOException, SQLException, NamingException {
        HibernatePersistenceManager me = new HibernatePersistenceManager();
        PersistenceManager.setInstance( me );
    }

    private HibernatePersistenceManager() throws IOException, SQLException, NamingException {
        Properties props = new Properties();

        String resourcePath = System.getProperty( PROPERTIES_RESOURCEPATH_PROPERTY );
        if ( resourcePath == null || resourcePath.length() == 0 )
            resourcePath = DEFAULT_PROPERTIES_RESOURCEPATH;

        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

        props.load( is );
        String dsUrl = props.getProperty(DATASOURCE_URL_PROPERTY);
        if ( dsUrl == null || dsUrl.length() == 0 ) throw new RuntimeException( "Couldn't find property " + DATASOURCE_URL_PROPERTY + "!" );

        _dataSource = (DataSource)new InitialContext().lookup(dsUrl);

        try {
            _dataStore = Hibernate.createDatastore().storeFile("hibernate.hbm.xml");
            _sessionFactory = _dataStore.buildSessionFactory(props);
        } catch ( HibernateException he ) {
            throw new SQLException( he.toString() );
        }
    }

    public Session getSession() throws SQLException {
        if ( _session == null ) {
            if ( _sessionFactory == null ) throw new IllegalStateException("HibernatePersistenceManager must be initialized before calling getSession()!");
        }

        _session = _sessionFactory.openSession( _dataSource.getConnection() );

        return _session;
    }

    private Session _session;
    private DataSource _dataSource;
    private Datastore _dataStore;
    private SessionFactory _sessionFactory;

    List doFind(String query, Object param, Class paramClass) {
        return null;
    }

    List doFind(String query, Object[] params, Class[] paramClasses) {
        return null;
    }

    Entity doLoad(Class clazz, long oid) {
        return null;
    }

    Entity doLoadForUpdate(Class clazz, long oid) {
        return null;
    }

    long doSave(Entity obj) {
        return 0;
    }

    void doDelete(Entity obj) {
    }


}
