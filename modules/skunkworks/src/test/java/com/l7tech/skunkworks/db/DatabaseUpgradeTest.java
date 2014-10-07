package com.l7tech.skunkworks.db;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.db.DbCompareTestUtils;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This test will compare a freshly created database to one that has been created using the upgrade scripts from an old
 * version.
 *
 * @author Victor Kazakov
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
@RunWith(MockitoJUnitRunner.class)
public class DatabaseUpgradeTest {
    private static final Logger logger = Logger.getLogger(DatabaseUpgradeTest.class.getName());

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final String HOST_NAME = DBCredentials.MYSQL_HOST;
    private static final int PORT = DBCredentials.MYSQL_PORT;
    private static final String DB_USER_NAME = "gateway";
    private static final String DB_USER_PASSWORD = "7layer";
    private static final String ADMIN_USER_NAME = DBCredentials.MYSQL_ROOT_USER;
    private static final String ADMIN_USER_PASSWORD = DBCredentials.MYSQL_ROOT_PASSWORD;

    private static final String NEW_DB_NAME = "mysql_database_upgrade_test__new_db_test";
    private static final String OLD_DB_NAME = "mysql_database_upgrade_test__old_db_test";

    private static final Set<String> hosts = CollectionUtils.set("localhost");

    DBActions dbActions = new DBActions();

    DatabaseConfig newDBConfig;
    DatabaseConfig oldDBConfig;
    private String currentVersion;

    @Before
    public void before() throws IOException, SQLException, ParserConfigurationException, SAXException, XPathExpressionException, LiquibaseException {

        newDBConfig = new DatabaseConfig(HOST_NAME, PORT, NEW_DB_NAME, DB_USER_NAME, DB_USER_PASSWORD);
        newDBConfig.setDatabaseAdminUsername(ADMIN_USER_NAME);
        newDBConfig.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        oldDBConfig = new DatabaseConfig(newDBConfig);
        oldDBConfig.setName(OLD_DB_NAME);

        dbActions.dropDatabase(newDBConfig, hosts, true, true);
        dbActions.dropDatabase(oldDBConfig, hosts, true, true);

        DBActions.DBActionsResult results = dbActions.createDb(newDBConfig, hosts, "etc/db/liquibase", false);
        Assert.assertEquals("Could not create mysql new database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        currentVersion = dbActions.checkDbVersion(newDBConfig);

        dbActions.createDatabaseWithGrants(dbActions.getConnection(oldDBConfig, true), oldDBConfig, hosts);
        Liquibase liquibase = new Liquibase("ssg-8.2.00.xml", new FileSystemResourceAccessor("etc/db/liquibase"), new JdbcConnection(dbActions.getConnection(oldDBConfig, false, false)));
        liquibase.update("");
    }


    @After
    public void after(){
        dbActions.dropDatabase(newDBConfig, hosts, true, true);
        dbActions.dropDatabase(oldDBConfig, hosts, true, true);
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException, IOException {
        boolean result = dbActions.upgradeDb(oldDBConfig, "etc/db/mysql", "etc/db/liquibase", currentVersion, null);
        Assert.assertTrue("Could not upgrade mysql database", result);

        DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(oldDBConfig, true, false));
    }

    @Test
    public void testSkipVersionUpgrade() throws SQLException, LiquibaseException, IOException {
        PreparedStatement currentVersionStatement = dbActions.getConnection(newDBConfig, true, false).prepareStatement("select current_version from ssg_version");

        Liquibase liquibase = new Liquibase("modules/skunkworks/src/test/resources/com/l7tech/skunkworks/db/ssg-upgrade-to-A.2.xml", new FileSystemResourceAccessor(), new JdbcConnection(dbActions.getConnection(newDBConfig, true, false)));
        liquibase.update("");

        ResultSet resultSet = currentVersionStatement.executeQuery();
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals("The current version is incorrect", "A.2", resultSet.getString("current_version"));

        liquibase = new Liquibase("modules/skunkworks/src/test/resources/com/l7tech/skunkworks/db/ssg-upgrade-to-B.0.xml", new FileSystemResourceAccessor(), new JdbcConnection(dbActions.getConnection(newDBConfig, true, false)));
        liquibase.update("");

        resultSet = currentVersionStatement.executeQuery();
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals("The current version is incorrect", "B.0", resultSet.getString("current_version"));

        liquibase = new Liquibase("modules/skunkworks/src/test/resources/com/l7tech/skunkworks/db/ssg-upgrade-to-B.1.xml", new FileSystemResourceAccessor(), new JdbcConnection(dbActions.getConnection(newDBConfig, true, false)));
        liquibase.update("");

        resultSet = currentVersionStatement.executeQuery();
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals("The current version is incorrect", "B.1", resultSet.getString("current_version"));

    }
}
