package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.util.ResourceUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class responsible for managing the creation/destruction of the database connecitons.
 */
public enum JdbcConnectionManager {
    INSTANCE;

    /**
     * key = connectionUrl + driver + user + pass
     * value = connection to the db
     */
    private static Map<String, Connection> connections = new HashMap<String, Connection>();
    private static final Logger logger = Logger.getLogger(JdbcConnectionManager.class.getName());

    public synchronized Connection getConnection(String connectionUrl, String driver, String user, String pass) throws IOException {
        if (connectionUrl == null || driver == null || user == null || pass == null) throw new NullPointerException();
        String key = connectionUrl + driver + user + pass;

        Connection conn = connections.get(key);

        //if the connection already exists, return it
        if (conn != null){
            logger.log(Level.WARNING, "Using existing database connection.");
            return conn;
        }

        //otherwise, create it
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(connectionUrl, user, pass);
            logger.log(Level.WARNING, "Creating new database connection.");
        } catch (ClassNotFoundException cnfe) {
            throw new IOException(cnfe);
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }

        //add it to the map of connections
        connections.put(key, conn);

        return conn;
    }

    public static synchronized void clearConnections() {
        //close and remove all connections
        Set<Map.Entry<String, Connection>> entries = connections.entrySet();
        for (Map.Entry<String, Connection> entry : entries) {
            ResourceUtils.closeQuietly(entry.getValue());
            connections.remove(entry.getKey());
        }
    }
}
