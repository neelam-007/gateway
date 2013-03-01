package com.l7tech.skunkworks.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.jdbc.oracle.OracleDriver;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.jdbc.JdbcConnectionManagerImpl;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.jdbc.JdbcQueryingManagerImpl;
import com.l7tech.util.TimeSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collections;

/**
 * This class should be extended to allow subclassed to make jdbc calls to oracle through the gateway.
 *
 * @author Victor Kazakov
 */
@Ignore
public abstract class JdbcCallHelperIntegrationAbstractBaseTestClass {

    //The connection name
    protected static final String ConnectionName = "OracleConnection";
    private static JdbcQueryingManagerImpl jdbcQueryingManager;
    private static DataSource dataSource;

    /**
     * Sets up mocks and jdbc objects
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        JdbcConnectionManagerImpl jdbcConnectionManager = Mockito.spy(new JdbcConnectionManagerImpl());

        Mockito.doReturn(Arrays.asList(getJdbcConnection())).when(jdbcConnectionManager).findAll();

        JdbcConnectionPoolManager jdbcConnectionPoolManager = Mockito.spy(new JdbcConnectionPoolManager(jdbcConnectionManager));

        jdbcConnectionPoolManager.afterPropertiesSet();

        jdbcQueryingManager = new JdbcQueryingManagerImpl(jdbcConnectionPoolManager, jdbcConnectionManager, ServerConfig.getInstance(), new TimeSource());

        //This is needed to allow the drive to be found and loaded.
        DriverManager.registerDriver((OracleDriver) Class.forName("com.l7tech.jdbc.oracle.OracleDriver").newInstance());

        dataSource = jdbcConnectionPoolManager.getDataSource(ConnectionName);
    }

    @Before
    public void before() throws Exception {
    }

    /**
     * Performs the given create or drop query
     *
     * @param createQuery the create or drop query to perform
     */
    protected void createDropItem(String createQuery) {
        jdbcQueryingManager.performJdbcQuery(dataSource, createQuery, "qatest", 1, Collections.emptyList());
    }

    private static JdbcConnection getJdbcConnection() {
        final JdbcConnection jdbcConn = new JdbcConnection();
        String host = "qaoracledb.l7tech.com";
        String port = "1521";
        String sid = "XE";
        final String jdbcUrl = "jdbc:l7tech:oracle://" + host + ":" + port + ";ServiceName=" + sid;

        jdbcConn.setJdbcUrl(jdbcUrl);
        jdbcConn.setName(ConnectionName);
        jdbcConn.setUserName("qatest");
        jdbcConn.setPassword("7layer");
        final String driverClass = "com.l7tech.jdbc.oracle.OracleDriver";
        jdbcConn.setDriverClass(driverClass);
        jdbcConn.setMinPoolSize(1);
        jdbcConn.setMaxPoolSize(1);
        return jdbcConn;
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static JdbcQueryingManager getJdbcQueryingManager() {
        return jdbcQueryingManager;
    }
}
