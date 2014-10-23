package com.l7tech.skunkworks.db;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.db.DbCompareTestUtils;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.DbUpgradeUtil;
import com.l7tech.util.ResourceUtils;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

    private static String HOST_NAME = DBCredentials.MYSQL_HOST;
    private static int PORT = DBCredentials.MYSQL_PORT;
    private static String DB_USER_NAME = "gateway";
    private static String DB_USER_PASSWORD = "7layer";
    private static String ADMIN_USER_NAME = DBCredentials.MYSQL_ROOT_USER;
    private static String ADMIN_USER_PASSWORD = DBCredentials.MYSQL_ROOT_PASSWORD;

    private static String NEW_DB_NAME = "mysql_database_upgrade_test__new_db_test";
    private static String UPGRADE_DB_NAME = "mysql_database_upgrade_test__upgrade_db_test";
    private static String OLD_DB_NAME = "mysql_database_upgrade_test__old_db_test";

    private static Set<String> hosts = CollectionUtils.set("localhost");

    static DBActions dbActions = new DBActions();

    static DatabaseConfig newDBConfig;
    static DatabaseConfig upgradeDBConfig;
    static DatabaseConfig oldDBConfig;

    @BeforeClass
    public static void beforeClass() throws IOException, SQLException {

        newDBConfig = new DatabaseConfig(HOST_NAME, PORT, NEW_DB_NAME, DB_USER_NAME, DB_USER_PASSWORD);
        newDBConfig.setDatabaseAdminUsername(ADMIN_USER_NAME);
        newDBConfig.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);
        upgradeDBConfig = new DatabaseConfig(newDBConfig);
        upgradeDBConfig.setName(UPGRADE_DB_NAME);

        oldDBConfig = new DatabaseConfig(newDBConfig);
        oldDBConfig.setName(OLD_DB_NAME);

        dbActions.dropDatabase(newDBConfig, hosts, true, true);
        dbActions.dropDatabase(upgradeDBConfig, hosts, true, true);

        DBActions.DBActionsResult results = dbActions.createDb(newDBConfig, hosts, "etc/db/liquibase", false);
        Assert.assertEquals("Could not create mysql new database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        dbActions.createDatabaseWithGrants(dbActions.getConnection(oldDBConfig, true), oldDBConfig, hosts);
        createTables(oldDBConfig, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_7.1.0.sql");

        dbActions.createDatabaseWithGrants(dbActions.getConnection(upgradeDBConfig, true), upgradeDBConfig, hosts);
        createTables(upgradeDBConfig, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_7.1.0.sql");

        boolean upgradeSuccess = dbActions.upgradeDb(upgradeDBConfig, "etc/db/mysql", "etc/db/liquibase", dbActions.checkDbVersion(newDBConfig), null);
        Assert.assertTrue("Could not upgrade mysql database", upgradeSuccess);

    }

    @AfterClass
    public static void afterClass(){
        dbActions.dropDatabase(newDBConfig, hosts, true, true);
        dbActions.dropDatabase(upgradeDBConfig, hosts, true, true);
        dbActions.dropDatabase(oldDBConfig, hosts, true, true);
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException {

        DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(upgradeDBConfig, true, false));
    }

    @Test
    public void compareNewLiquibase82ToSQL82() throws SQLException, IOException, LiquibaseException {
        DatabaseConfig ssg82sql = new DatabaseConfig(HOST_NAME, PORT, "ssg82sql", DB_USER_NAME, DB_USER_PASSWORD);
        ssg82sql.setDatabaseAdminUsername(ADMIN_USER_NAME);
        ssg82sql.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        DatabaseConfig ssg82liquibase = new DatabaseConfig(HOST_NAME, PORT, "ssg82liquibase", DB_USER_NAME, DB_USER_PASSWORD);
        ssg82liquibase.setDatabaseAdminUsername(ADMIN_USER_NAME);
        ssg82liquibase.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        try {
            dbActions.createDatabaseWithGrants(dbActions.getConnection(ssg82sql, true), ssg82sql, hosts);
            createTables(ssg82sql, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_8.2.00.sql");
            dbActions.legacyUpgrade(ssg82sql, dbActions.getConnection(ssg82sql, true, false), new File("etc/db/mysql"), null);

            dbActions.createDatabaseWithGrants(dbActions.getConnection(ssg82liquibase, true), ssg82liquibase, hosts);
            Liquibase liquibase = new Liquibase("etc/db/liquibase/ssg-8.2.00.xml", new FileSystemResourceAccessor(), new JdbcConnection(dbActions.getConnection(ssg82liquibase, true, false)));
            liquibase.update("");

            DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(ssg82sql, true, false), dbActions.getConnection(ssg82liquibase, true, false));
        } finally {
            dbActions.dropDatabase(ssg82sql, hosts, true, true);
            dbActions.dropDatabase(ssg82liquibase, hosts, true, true);
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
            dbActions.dropDatabase(copyDBConfig, hosts, true, true);
        }
    }

    public static void createTables( DatabaseConfig databaseConfig, String dbCreateScript ) throws SQLException, IOException {
        if ( dbCreateScript != null ) {
            String[] sql = DbUpgradeUtil.getStatementsFromFile(dbCreateScript);
            if ( sql != null ) {
                logger.info( "Creating schema for " + databaseConfig.getName() + " database" );

                Connection connection = null;
                try {
                    connection = dbActions.getConnection(databaseConfig, false);
                    executeUpdates(connection, sql);
                } finally {
                    ResourceUtils.closeQuietly(connection);
                }
            }
        } else {
            logger.info("Skipping creation of tables and rows");
        }
    }

    private static void executeUpdates(@NotNull final Connection connection, @NotNull final String[] sqlStatements) throws SQLException {
        Statement statement = null;

        if (sqlStatements.length > 0) {
            int i = 0;
            try {
                statement = connection.createStatement();

                for (i = 0; i < sqlStatements.length; i++) {
                    statement.executeUpdate(sqlStatements[i]);
                }
            }
            catch (SQLException sqle) {
                System.out.println(sqlStatements[i]);
                throw sqle;
            }
            finally {
                ResourceUtils.closeQuietly(statement);
            }
        }
    }
}
