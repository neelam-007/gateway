/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.dbcp.*;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class VersionCheckBenchmarkTest extends TestCase {
    static final String JDBC_URL = "jdbc:mysql://localhost/test?user=webuser&password=resubew";
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static DataSource dataSource;

    int runnableInvocationsCounter = 0;

    public VersionCheckBenchmarkTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(VersionCheckBenchmarkTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            // this setup is run before all tests
            protected void setUp() throws Exception {
                Class.forName(JDBC_DRIVER);
                dataSource = VersionCheckBenchmarkTest.setupDataSource(JDBC_URL);
                // dataSource = VersionCheckBenchmarkTest.setupMySqlDataSource(JDBC_URL, 3306, null, null);
            }

            protected void tearDown() throws Exception {
            }
        };
        return wrapper;

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testPooling() throws Exception {
        Connection conn = dataSource.getConnection();
        Connection underconn = null;
        if (conn instanceof DelegatingConnection) {
            underconn = ((DelegatingConnection)conn).getInnermostDelegate();
        } else {
            return; // skip this test
        }
        assertTrue(underconn != null);
        Connection conn2 = dataSource.getConnection();
        Connection underconn2 = null;
        if (conn2 instanceof DelegatingConnection) {
            underconn2 = ((DelegatingConnection)conn2).getInnermostDelegate();
        } else {
            return; // skip this test
        }
        assertTrue(underconn2 != null);
        assertTrue(underconn != underconn2);
        conn2.close();
        conn.close();
        Connection conn3 = dataSource.getConnection();
        Connection underconn3 = null;
        if (conn3 instanceof DelegatingConnection) {
            underconn3 = ((DelegatingConnection)conn3).getInnermostDelegate();
        } else {
            return; // skip this test
        }
        assertTrue(underconn3 == underconn || underconn3 == underconn2);
        conn3.close();
    }

    public void testVersionCheckBenchmark() throws Exception {
        Connection[] connections = new Connection[100];
        for (int i = 0; i < connections.length; i++) {
            connections[i] = dataSource.getConnection();
        }

        for (int i = 0; i < connections.length; i++) {
            connections[i].close();
        }

        int count = 100;
        runnableInvocationsCounter = 0;

        Runnable testRunnable = new Runnable() {
            public void run() {
                Connection conn = null;
                try {
                    conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement("select version from test_version_check where id = ?");
                    ps.setInt(1, 1);
                    ResultSet rs = ps.executeQuery();
                    rs.getInt(1);
                } catch (SQLException e) {

                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                        }
                    }
                }
                runnableInvocationsCounter++;
            }
        };
        BenchmarkRunner rr = new BenchmarkRunner(testRunnable, count);
        rr.run();
        assertTrue("Expected " + count + " invocations, received  " +
          runnableInvocationsCounter, count == runnableInvocationsCounter);
    }

    private static DataSource setupDataSource(String connectURI) throws Exception {
        //
        // First, we'll need a ObjectPool that serves as the
        // actual pool of connections.
        //
        // We'll use a GenericObjectPool instance, although
        // any ObjectPool implementation will suffice.
        //
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        //connectionPool.setMaxActive(100);
        connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);

        //
        // Next, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        // We'll use the DriverManagerConnectionFactory,
        // using the connect string passed in the command line
        // arguments.
        //
        ConnectionFactory connectionFactory =
          new DriverManagerConnectionFactory(connectURI, null);

        //
        // Now we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        //
        PoolableConnectionFactory poolableConnectionFactory =
          new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);

        //
        // Finally, we create the PoolingDriver itself,
        // passing in the object pool we created.
        //
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);

        return dataSource;
    }
}
