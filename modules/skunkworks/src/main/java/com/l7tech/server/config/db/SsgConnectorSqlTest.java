package com.l7tech.server.config.db;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PasswordPropertyCrypto;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.partition.PartitionManager;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.sql.Connection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.logging.Logger;

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

    public void testLoadConnectors() throws Exception {
        Connection db = connectToDefaultPartitionDatabase();
        Collection<SsgConnector> got = SsgConnectorSql.loadAll(db);
        for (SsgConnector ssgConnector : got) {
            logger.info("Connector: " + ssgConnector);
        }
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

    public void testImportFtpProps() throws Exception {
        Connection db = connectToDefaultPartitionDatabase();

        List<SsgConnector> ftpConnectors = PartitionManager.getInstance().getPartition("default_").parseFtpEndpointsAsSsgConnectors();
        for (SsgConnector ftpConnector : ftpConnectors) {
            new SsgConnectorSql(ftpConnector).save(db);
        }
    }
    
    private Connection connectToDefaultPartitionDatabase() throws Exception {
        final OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions("default_");
        PasswordPropertyCrypto passwordCrypto = osFunctions.getPasswordPropertyCrypto();
        Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[]{
                DBInformation.PROP_DB_USERNAME,
                DBInformation.PROP_DB_PASSWORD,
                DBInformation.PROP_DB_URL,
        });
        final String databaseURL = dbProps.get(DBInformation.PROP_DB_URL);
        final String databaseUser = dbProps.get(DBInformation.PROP_DB_USERNAME);
        final String databasePasswdCrypt = dbProps.get(DBInformation.PROP_DB_PASSWORD);
        final String databasePasswd = passwordCrypto.decryptIfEncrypted(databasePasswdCrypt);

        logger.info("using database url " + databaseURL);
        logger.info("using database user " + databaseUser);
        logger.info("using database passwd " + databasePasswd);

        final DBActions dbActions = new DBActions(osFunctions);
        return dbActions.getConnection(databaseURL, databaseUser, databasePasswd);
    }
}
