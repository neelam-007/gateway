package com.l7tech.server.config.db;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;
import java.util.Map;
import java.util.EnumSet;
import java.sql.Connection;
import java.sql.SQLException;

import com.l7tech.server.audit.AuditExporterImpl;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.PasswordPropertyCrypto;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.SsgConnector.Endpoint;
import org.hibernate.Session;

/**
 *
 */
public class SsgConnectorSqlTest extends TestCase {
    protected static final Logger logger = Logger.getLogger(SsgConnectorSqlTest.class.getName());

    private static final Logger log = Logger.getLogger(SsgConnectorSqlTest.class.getName());

    public SsgConnectorSqlTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SsgConnectorSqlTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSaveConnector() throws Exception {
        Connection db = connectToDefaultPartitionDatabase();

        SsgConnector c = new SsgConnector();
        c.setName("My funky connector");
        c.setPort(7433);
        c.setEnabled(true);
        c.setEndpoints(Endpoint.asCommaList(EnumSet.of(Endpoint.MESSAGE_INPUT, Endpoint.ADMIN_APPLET)));
        c.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);
        c.setKeyAlias("mykeyalias");
        c.setKeystoreOid(29382L);
        c.setScheme(SsgConnector.SCHEME_HTTP);
        c.setSecure(true);
        c.putProperty("My funky property 1", "my funky prop value 1");
        c.putProperty("My funky property 2", "my funky prop value 2");
        c.putProperty("My funky property 3", "my funky prop value 3");
        c.putProperty("My funky property 4", "my funky prop value 4");

        new SsgConnectorSql(c).save(db);
    }
    
    private Connection connectToDefaultPartitionDatabase() throws Exception {
        final OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions("default_");
        PasswordPropertyCrypto passwordCrypto = osFunctions.getPasswordPropertyCrypto();
        Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[]{
                SsgDatabaseConfigBean.PROP_DB_USERNAME,
                SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                SsgDatabaseConfigBean.PROP_DB_URL,
        });
        final String databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
        final String databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
        final String databasePasswdCrypt = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);
        final String databasePasswd = passwordCrypto.decryptIfEncrypted(databasePasswdCrypt);

        logger.info("using database url " + databaseURL);
        logger.info("using database user " + databaseUser);
        logger.info("using database passwd " + databasePasswd);

        final DBActions dbActions = new DBActions(osFunctions);
        return dbActions.getConnection(databaseURL, databaseUser, databasePasswd);
    }
}
