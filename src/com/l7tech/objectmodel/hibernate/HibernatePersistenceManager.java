package com.l7tech.objectmodel.hibernate;

import cirrus.hibernate.*;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.List;
import java.sql.Connection;
import java.sql.SQLException;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.PersistenceManager;

/**
 * @author alex
 */
public class HibernatePersistenceManager extends PersistenceManager {
    public static void initialize() throws SQLException {
        HibernatePersistenceManager me = new HibernatePersistenceManager();
        PersistenceManager.setInstance( me );
    }

    private HibernatePersistenceManager() throws SQLException {
        Properties props = new Properties();
        // TODO: Load properties from file

        _dataSource = null;
        try {
            _dataStore = Hibernate.createDatastore().storeFile("hibernate.hbm.xml");
            _sessions = _dataStore.buildSessionFactory(props);
        } catch ( HibernateException he ) {
            throw new SQLException( he.toString() );
        }
    }

    private Session _session;
    private DataSource _dataSource;
    private Datastore _dataStore;
    private SessionFactory _sessions;

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
