package com.l7tech.skunkworks.db;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.db.DbCompareTestUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
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
public class MysqlDatabaseUpgradeTest {
    private static final Logger logger = Logger.getLogger(MysqlDatabaseUpgradeTest.class.getName());

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private String HOST_NAME = DBCredentials.MYSQL_HOST;
    private int PORT = DBCredentials.MYSQL_PORT;
    private String DB_USER_NAME = "gateway";
    private String DB_USER_PASSWORD = "7layer";
    private String ADMIN_USER_NAME = DBCredentials.MYSQL_ROOT_USER;
    private String ADMIN_USER_PASSWORD = DBCredentials.MYSQL_ROOT_PASSWORD;

    private String NEW_DB_NAME = "mysql_database_upgrade_test__new_db_test";
    private String UPGRADE_DB_NAME = "mysql_database_upgrade_test__upgrade_db_test";
    private String OLD_DB_NAME = "mysql_database_upgrade_test__old_db_test";

    private Set<String> hosts = CollectionUtils.set("localhost");

    DBActions dbActions = new DBActions();

    DatabaseConfig newDBConfig;
    DatabaseConfig upgradeDBConfig;
    DatabaseConfig oldDBConfig;

    @Before
    public void before() throws IOException, SQLException {

        newDBConfig = new DatabaseConfig(HOST_NAME, PORT, NEW_DB_NAME, DB_USER_NAME, DB_USER_PASSWORD);
        newDBConfig.setDatabaseAdminUsername(ADMIN_USER_NAME);
        newDBConfig.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);
        upgradeDBConfig = new DatabaseConfig(newDBConfig);
        upgradeDBConfig.setName(UPGRADE_DB_NAME);

        oldDBConfig = new DatabaseConfig(newDBConfig);
        oldDBConfig.setName(OLD_DB_NAME);

        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(upgradeDBConfig, hosts, true, true, null);

        DBActions.DBActionsResult results = dbActions.createDb(newDBConfig, hosts, "etc/db/liquibase/ssg.xml", false);
        Assert.assertEquals("Could not create mysql new database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        results = dbActions.createDb(oldDBConfig, hosts, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_7.1.0.sql", false);
        Assert.assertEquals("Could not create mysql upgraded database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        results = dbActions.createDb(upgradeDBConfig, hosts, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_7.1.0.sql", false);
        Assert.assertEquals("Could not create mysql upgraded database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        results = dbActions.upgradeDbSchema(upgradeDBConfig,true, "etc/db/mysql", "etc/db/liquibase", null);
        Assert.assertEquals("Could not upgrade mysql database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

    }

    @After
    public void after(){
        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(upgradeDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(oldDBConfig, hosts, true, true, null);
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException {

        DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(upgradeDBConfig, true, false));
    }

    @Test
    public void compareNewLiquibase82ToSQL82() throws SQLException, IOException {
        DatabaseConfig ssg82sql = new DatabaseConfig(HOST_NAME, PORT, "ssg82sql", DB_USER_NAME, DB_USER_PASSWORD);
        ssg82sql.setDatabaseAdminUsername(ADMIN_USER_NAME);
        ssg82sql.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        DatabaseConfig ssg82liquibase = new DatabaseConfig(HOST_NAME, PORT, "ssg82liquibase", DB_USER_NAME, DB_USER_PASSWORD);
        ssg82liquibase.setDatabaseAdminUsername(ADMIN_USER_NAME);
        ssg82liquibase.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        try {
            DBActions.DBActionsResult result = dbActions.createDb(ssg82sql, hosts, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_8.2.00.sql", true);
            Assert.assertEquals("Could not create sql 82 database: " + result.getErrorMessage(), DBActions.StatusType.SUCCESS, result.getStatus());

            result = dbActions.upgradeDbSchema(ssg82sql, true, "etc/db/mysql", "etc/db/liquibase", null);
            Assert.assertEquals("Could not upgrade mysql database: " + result.getErrorMessage(), DBActions.StatusType.SUCCESS, result.getStatus());

            result = dbActions.createDb(ssg82liquibase, hosts, "etc/db/liquibase/ssg.xml", true);
            Assert.assertEquals("Could not create liquibase 82 database: " + result.getErrorMessage(), DBActions.StatusType.SUCCESS, result.getStatus());

            DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(ssg82sql, true, false), dbActions.getConnection(ssg82liquibase, true, false));
        } finally {
            dbActions.dropDatabase(ssg82sql, hosts, true, true, null);
            dbActions.dropDatabase(ssg82liquibase, hosts, true, true, null);
        }
    }

    @BugId("SSG-7870")
    @Test
    public void copyDBTest() throws SQLException {
        DatabaseConfig copyDBConfig = new DatabaseConfig(newDBConfig);
        try {
            copyDBConfig.setName(newDBConfig.getName() + "_copied");
            dbActions.copyDatabase(newDBConfig, copyDBConfig, true, null);
            DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(copyDBConfig, true, false), false);
        } finally {
            dbActions.dropDatabase(copyDBConfig, hosts, true, true, null);
        }
    }

    //This will test the full database upgrade process. This is what gets called when you upgrade the database using the ssg console.
    @Test
    public void upgradeDBTest() throws SQLException, IOException {
        boolean result = dbActions.upgradeDb(oldDBConfig, "etc/db/mysql", "etc/db/liquibase", dbActions.checkDbVersion(newDBConfig), null);
        Assert.assertTrue("Could not upgrade mysql database.", result);
        DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(oldDBConfig, true, false), false);
    }
}
