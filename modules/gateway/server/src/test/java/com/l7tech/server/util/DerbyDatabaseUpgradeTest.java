package com.l7tech.server.util;

import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.DbUpgradeUtil;
import com.l7tech.util.FileUtils;
import com.l7tech.util.db.DbCompareTestUtils;
import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

    private static final String DB_FOLDER = "_dbtest";
    private static final String NEW_DATABASE_NAME = DB_FOLDER + "/ssg_new_db_test";
    private static final String UPGRADED_DATABASE_NAME = DB_FOLDER + "/ssg_upgraded_db_test";
    //This will need to be upgraded for every
    private String softwareVersion = "";

    private EmbeddedDataSource40 newDBDataSource;
    private EmbeddedDataSource40 upgradeDBDataSource;

    @Before
    public void before() throws IOException, SQLException {
        File fileDbFolder = new File(DB_FOLDER);
        FileUtils.deleteDir(fileDbFolder);

        newDBDataSource = new EmbeddedDataSource40();
        newDBDataSource.setDatabaseName(NEW_DATABASE_NAME);
        newDBDataSource.setCreateDatabase("create");

        //The new freshly created database
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("com/l7tech/server/resources/ssg_embedded.sql");
        DerbyDbHelper.runScripts(newDBDataSource.getConnection(), resources, false);

        // Gets the version of the new database;
        Connection newDBConnection = newDBDataSource.getConnection();
        softwareVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(newDBConnection);
        newDBConnection.close();

        upgradeDBDataSource = new EmbeddedDataSource40();
        upgradeDBDataSource.setDatabaseName(UPGRADED_DATABASE_NAME);
        upgradeDBDataSource.setCreateDatabase("create");

        //create a database from the 7.1.0 sql script
        resources = new PathMatchingResourcePatternResolver().getResources("com/l7tech/server/resources/ssg_embedded_7.1.0.sql");
        DerbyDbHelper.runScripts(upgradeDBDataSource.getConnection(), resources, false);

        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(upgradeDBDataSource);
        EmbeddedDbSchemaUpdater dbUpdater = new EmbeddedDbSchemaUpdater(transactionManager, "com/l7tech/server/resources/derby") {
            @Override
            String getProductVersion() {
                return softwareVersion;
            }
        };
        dbUpdater.setDataSource(upgradeDBDataSource);

        //upgrade the 7.1.0 database to the latest version
        dbUpdater.ensureCurrentSchema();
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException {

        DbCompareTestUtils.compareNewToUpgradedDatabase(newDBDataSource.getConnection(), upgradeDBDataSource.getConnection());
    }
}
