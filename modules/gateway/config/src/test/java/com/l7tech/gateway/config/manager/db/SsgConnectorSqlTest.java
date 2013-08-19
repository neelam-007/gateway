package com.l7tech.gateway.config.manager.db;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.SyspropUtil;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Logger;

/**
 *
 */
public class SsgConnectorSqlTest {
    private static final Logger logger = Logger.getLogger(SsgConnectorSqlTest.class.getName());

    private static final String url = "jdbc:mysql://localhost/ssg";
    private static final String username = "gateway";
    private static final String password = "7layer";

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "jdbc.drivers"
        );
    }

    @Test
    @Ignore("This is a developer test, requires local db.")
    public void testLoadConnectors() throws Exception {
        Connection db = getConnection();
        Collection<SsgConnector> got = SsgConnectorSql.loadAll(db);
        for (SsgConnector ssgConnector : got) {
            logger.info("Connector: " + ssgConnector);
        }
    }

    @Test
    @Ignore("This is a developer test, requires local db.")
    public void testSaveConnector() throws Exception {
        Connection db = getConnection();

        SsgConnector c = new SsgConnector();
        c.setName("My funky connector");
        c.setPort(7433);
        c.setEnabled(true);
        c.setEndpoints(Endpoint.asCommaList(EnumSet.of(Endpoint.MESSAGE_INPUT, Endpoint.ADMIN_APPLET)));
        c.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        c.setKeyAlias("mykeyalias");
        c.setKeystoreGoid(new Goid(0,29382L));
        c.setScheme(SsgConnector.SCHEME_HTTP);
        c.setSecure(true);
        c.putProperty("My funky property 1", "my funky prop value 1");
        c.putProperty("My funky property 2", "my funky prop value 2");
        c.putProperty("My funky property 3", "my funky prop value 3");
        c.putProperty("My funky property 4", "my funky prop value 4");

        new SsgConnectorSql(c).save(db);
    }

    private Connection getConnection() throws Exception {
        SyspropUtil.setProperty("jdbc.drivers", "com.mysql.jdbc.Driver");
        return DriverManager.getConnection(url, username, password);
    }
}
