package com.l7tech.skunkworks.jdbc;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.jdbc.oracle.OracleDriver;
import com.l7tech.server.jdbc.JdbcConnectionManagerImpl;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.jdbc.JdbcQueryingManagerImpl;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class should be extended to allow subclassed to make jdbc calls to oracle through the gateway.
 *
 * @author Victor Kazakov
 */
@Ignore
public abstract class JdbcCallHelperIntegrationAbstractBaseTestClass {

    //The connection name
    protected static final String ConnectionName = "MyConnection";
    private static JdbcQueryingManagerImpl jdbcQueryingManager;
    private static DataSource dataSource;
    private static Map<String, String> configProperties = new HashMap<>();
    private static MockConfig mockConfig = new MockConfig(getConfigProperties());

    /**
     * Sets up mocks and jdbc objects
     *
     * @throws Exception
     */
    public static void beforeClass(@Nullable JdbcConnection jdbcConnection) throws Exception {
        JdbcConnectionManagerImpl jdbcConnectionManager = Mockito.spy(new JdbcConnectionManagerImpl());

        Mockito.doReturn(Arrays.asList(jdbcConnection == null ? getJdbcConnection() : jdbcConnection)).when(jdbcConnectionManager).findAll();

        JdbcConnectionPoolManager jdbcConnectionPoolManager = Mockito.spy(new JdbcConnectionPoolManager(jdbcConnectionManager));

        jdbcConnectionPoolManager.afterPropertiesSet();

        jdbcQueryingManager = new JdbcQueryingManagerImpl(jdbcConnectionPoolManager, jdbcConnectionManager, getMockConfig(), new TimeSource());

        //This is needed to allow the drive to be found and loaded.
        DriverManager.registerDriver((OracleDriver) Class.forName("com.l7tech.jdbc.oracle.OracleDriver").newInstance());

        dataSource = jdbcConnectionPoolManager.getDataSource(ConnectionName);
    }

    public static Map<String, String> getConfigProperties() {
        return configProperties;
    }

    public static MockConfig getMockConfig() {
        return mockConfig;
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

    protected static JdbcConnection getJdbcConnection() {
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
        jdbcConn.setAdditionalProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("c3p0.preferredTestQuery", "select 1 from dual")
                .put("c3p0.idleConnectionTestPeriod", "60") // This must be less then the connection drop time (databases, firewalls, routers, etc may drop inactive connection)
                .put("c3p0.maxIdleTime", "0") // We do not need to discard connections that are idle unless there are more connections then the minPoolSize
                .put("c3p0.maxIdleTimeExcessConnections", "30") // For efficiency this should be less then idleConnectionTestPeriod
                .put("EnableCancelTimeout", "true")
//                .put("SpyAttributes", "(log=(file)D:\\\\datadirect.log;logTName=yes;)")
                .map());
        return jdbcConn;
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static JdbcQueryingManager getJdbcQueryingManager() {
        return jdbcQueryingManager;
    }
}
