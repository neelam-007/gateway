package com.l7tech.ssg.objectmodel.hibernate;

import cirrus.hibernate.*;

import javax.sql.DataSource;
import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author alex
 */
public class HibernatePersistence implements Persistence {
    public void init() throws SQLException {
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

    public Connection getConnection() throws SQLException {
        Connection connection = _dataSource.getConnection();
        _session = _sessions.openSession( connection );
        return connection;
    }

    private Session _session;
    private DataSource _dataSource;
    private Datastore _dataStore;
    private SessionFactory _sessions;
}
