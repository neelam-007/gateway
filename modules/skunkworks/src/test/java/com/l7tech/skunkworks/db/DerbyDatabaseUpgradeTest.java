package com.l7tech.skunkworks.db;

import com.l7tech.server.util.DerbyDbHelper;
import com.l7tech.server.util.EmbeddedDbSchemaUpdater;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.DbUpgradeUtil;
import com.l7tech.util.FileUtils;
import com.l7tech.util.db.DbCompareTestUtils;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * This test will compare a freshly created database to one that has been created using the upgrade scripts from an old
 * version.
 *
 * @author Victor Kazakov
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
@RunWith(MockitoJUnitRunner.class)
public class DerbyDatabaseUpgradeTest {

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final String DB_FOLDER = "build/test/_dbtest";
    private static final String NEW_DATABASE_NAME = DB_FOLDER + "/ssg_new_db_test";
    private static final String UPGRADED_DATABASE_NAME = DB_FOLDER + "/ssg_upgraded_db_test";
    //This will need to be upgraded for every
    private static String softwareVersion = "";

    private static EmbeddedDataSource40 newDBDataSource;
    private static EmbeddedDataSource40 upgradeDBDataSource;

    @BeforeClass
    public static void beforeClass() throws IOException, SQLException, LiquibaseException {
        File fileDbFolder = new File(DB_FOLDER);
        FileUtils.deleteDir(fileDbFolder);

        newDBDataSource = new EmbeddedDataSource40();
        newDBDataSource.setDatabaseName(NEW_DATABASE_NAME);
        newDBDataSource.setCreateDatabase("create");

        //The new freshly created database
        Liquibase liquibase = new Liquibase("etc/db/liquibase/ssg.xml", new FileSystemResourceAccessor(), new JdbcConnection(newDBDataSource.getConnection()));
        liquibase.update("");

        // Gets the version of the new database;
        Connection newDBConnection = newDBDataSource.getConnection();
        softwareVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(newDBConnection);
        newDBConnection.close();

        upgradeDBDataSource = new EmbeddedDataSource40();
        upgradeDBDataSource.setDatabaseName(UPGRADED_DATABASE_NAME);
        upgradeDBDataSource.setCreateDatabase("create");

        //create a database from the 7.1.0 sql script
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("com/l7tech/server/resources/ssg_embedded_7.1.0.sql");
        DerbyDbHelper.runScripts(upgradeDBDataSource.getConnection(), resources, false);

        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(upgradeDBDataSource);
        EmbeddedDbSchemaUpdater dbUpdater = new EmbeddedDbSchemaUpdater(transactionManager, "com/l7tech/server/resources/derby", "etc/db/liquibase/") {
            @Override
            protected String getProductVersion() {
                return softwareVersion;
            }
        };
        dbUpdater.setDataSource(upgradeDBDataSource);

        //upgrade the 7.1.0 database to the latest version
        dbUpdater.ensureCurrentSchema();
    }

    @AfterClass
    public static void afterClass() {
        File fileDbFolder = new File(DB_FOLDER);
        FileUtils.deleteDir(fileDbFolder);
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException {

        DbCompareTestUtils.compareNewToUpgradedDatabase(newDBDataSource.getConnection(), upgradeDBDataSource.getConnection());
    }

    @Test
    public void compareNewLiquibase82ToSQL82() throws SQLException, IOException, LiquibaseException {
        EmbeddedDataSource40 derbyssg82sql = new EmbeddedDataSource40();
        derbyssg82sql.setDatabaseName(DB_FOLDER + "/derbyssg82sql");
        derbyssg82sql.setCreateDatabase("create");

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("com/l7tech/server/resources/ssg_embedded_8.2.00.sql");
        DerbyDbHelper.runScripts(derbyssg82sql.getConnection(), resources, false);

        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(derbyssg82sql);
        EmbeddedDbSchemaUpdater dbUpdater = new EmbeddedDbSchemaUpdater(transactionManager, "com/l7tech/server/resources/derby", "etc/db/liquibase/") {
            @Override
            protected String getProductVersion() {
                return "8.3.pre";
            }
        };
        dbUpdater.setDataSource(derbyssg82sql);
        dbUpdater.ensureCurrentSchema();

        EmbeddedDataSource40 derbyssg82liquibase = new EmbeddedDataSource40();
        derbyssg82liquibase.setDatabaseName(DB_FOLDER + "/derbyssg82liquibase");
        derbyssg82liquibase.setCreateDatabase("create");

        Liquibase liquibase = new Liquibase("etc/db/liquibase/ssg-8.2.00.xml", new FileSystemResourceAccessor(), new JdbcConnection(derbyssg82liquibase.getConnection()));
        liquibase.update("");

        DbCompareTestUtils.compareNewToUpgradedDatabase(derbyssg82sql.getConnection(), derbyssg82liquibase.getConnection());
    }
}
