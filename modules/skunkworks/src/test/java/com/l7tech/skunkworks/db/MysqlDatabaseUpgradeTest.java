package com.l7tech.skunkworks.db;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.db.DbCompareTestUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

/**
 * This test will compare a freshly created database to one that has been created using the upgrade scripts from an old
 * version.
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class MysqlDatabaseUpgradeTest {
    private String HOST_NAME = DBCredentials.MYSQL_HOST;
    private int PORT = DBCredentials.MYSQL_PORT;
    private String DB_USER_NAME = "gateway";
    private String DB_USER_PASSWORD = "7layer";
    private String ADMIN_USER_NAME = DBCredentials.MYSQL_ROOT_USER;
    private String ADMIN_USER_PASSWORD = DBCredentials.MYSQL_ROOT_PASSWORD;

    private String NEW_DB_NAME = "mysql_database_upgrade_test__new_db_test";
    private String UPGRADE_DB_NAME = "mysql_database_upgrade_test__upgrade_db_test";

    private Set<String> hosts = CollectionUtils.set("localhost");

    DBActions dbActions = new DBActions();

    DatabaseConfig newDBConfig;
    DatabaseConfig upgradeDBConfig;

    @Before
    public void before() throws IOException, SQLException {

        newDBConfig = new DatabaseConfig(HOST_NAME, PORT, NEW_DB_NAME, DB_USER_NAME, DB_USER_PASSWORD);
        newDBConfig.setDatabaseAdminUsername(ADMIN_USER_NAME);
        newDBConfig.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);
        upgradeDBConfig = new DatabaseConfig(newDBConfig);
        upgradeDBConfig.setName(UPGRADE_DB_NAME);

        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(upgradeDBConfig, hosts, true, true, null);

        DBActions.DBActionsResult results = dbActions.createDb(newDBConfig, hosts, "etc/db/mysql/ssg.sql", false);
        Assert.assertEquals("Could not create mysql new database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        results = dbActions.createDb(upgradeDBConfig, hosts, "modules/gateway/config/src/test/resources/com/l7tech/server/resources/ssg_7.1.0.sql", false);
        Assert.assertEquals("Could not create mysql upgraded database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        results = dbActions.upgradeDbSchema(upgradeDBConfig,true, "7.1.0", dbActions.checkDbVersion(newDBConfig),"etc/db/mysql/ssg.sql", null);
        Assert.assertEquals("Could not upgrade mysql database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

    }

    @After
    public void after(){
        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(upgradeDBConfig, hosts, true, true, null);
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException {

        DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(upgradeDBConfig, true, false));
    }
}
