package com.l7tech.skunkworks.db;

import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.db.DbCompareTestUtils;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

    private String HOST_NAME = DBCredentials.MYSQL_HOST;
    private int PORT = DBCredentials.MYSQL_PORT;
    private String DB_USER_NAME = "gateway";
    private String DB_USER_PASSWORD = "7layer";
    private String ADMIN_USER_NAME = DBCredentials.MYSQL_ROOT_USER;
    private String ADMIN_USER_PASSWORD = DBCredentials.MYSQL_ROOT_PASSWORD;

    private String NEW_DB_NAME = "mysql_database_upgrade_test__new_db_test";
    private String OLD_DB_NAME = "mysql_database_upgrade_test__old_db_test";

    private Set<String> hosts = CollectionUtils.set("localhost");

    DBActions dbActions = new DBActions();

    DatabaseConfig newDBConfig;
    DatabaseConfig oldDBConfig;

    @Before
    public void before() throws IOException, SQLException, ParserConfigurationException, SAXException, XPathExpressionException, LiquibaseException {

        newDBConfig = new DatabaseConfig(HOST_NAME, PORT, NEW_DB_NAME, DB_USER_NAME, DB_USER_PASSWORD);
        newDBConfig.setDatabaseAdminUsername(ADMIN_USER_NAME);
        newDBConfig.setDatabaseAdminPassword(ADMIN_USER_PASSWORD);

        oldDBConfig = new DatabaseConfig(newDBConfig);
        oldDBConfig.setName(OLD_DB_NAME);

        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(oldDBConfig, hosts, true, true, null);

        DBActions.DBActionsResult results = dbActions.createDb(newDBConfig, hosts, "etc/db/liquibase/ssg.xml", false);
        Assert.assertEquals("Could not create mysql new database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());

        results = dbActions.createDb(oldDBConfig, hosts, "etc/db/liquibase/ssg-8.2.00.xml", false);
        Assert.assertEquals("Could not create mysql upgraded database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());
    }


    @After
    public void after(){
        dbActions.dropDatabase(newDBConfig, hosts, true, true, null);
        dbActions.dropDatabase(oldDBConfig, hosts, true, true, null);
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException, IOException {
        DBActions.DBActionsResult result = dbActions.upgradeDbSchema(oldDBConfig, true, "etc/db/mysql", "etc/db/liquibase", null);
        Assert.assertEquals("Could not upgrade mysql database: " + result.getErrorMessage(), DBActions.StatusType.SUCCESS, result.getStatus());

        DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(newDBConfig, true, false), dbActions.getConnection(oldDBConfig, true, false));
    }

    @Test
    public void testRollback() throws SQLException, LiquibaseException, IOException, XPathExpressionException, ParserConfigurationException, SAXException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse("etc/db/liquibase/ssg-upgrade.xml");
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("/databaseChangeLog/include[@file]");
        NodeList filesNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<String> upgradeXMLs = new ArrayList<>();
        for (int i = 0; i < filesNodes.getLength(); i++) {
            upgradeXMLs.add(filesNodes.item(i).getAttributes().getNamedItem("file").getNodeValue());
        }

        DatabaseConfig rollbackDBConfig = new DatabaseConfig(oldDBConfig);
        rollbackDBConfig.setName("RolbackDBTest");
        dbActions.dropDatabase(rollbackDBConfig, hosts, true, true, null);
        DBActions.DBActionsResult results = dbActions.createDb(rollbackDBConfig, hosts, "etc/db/liquibase/ssg-8.2.00.xml", false);
        Assert.assertEquals("Could not create mysql upgraded database: " + results.getErrorMessage(), DBActions.StatusType.SUCCESS, results.getStatus());


        for (String upgradeXML : upgradeXMLs) {
            //upgrade and rollback
            Liquibase liquibase = new Liquibase("etc/db/liquibase/" + upgradeXML, new FileSystemResourceAccessor(), new JdbcConnection(dbActions.getConnection(rollbackDBConfig, true, false)));
            liquibase.tag(upgradeXML);
            liquibase.update("");
            liquibase.rollback(upgradeXML, "");

            DbCompareTestUtils.compareNewToUpgradedDatabase(dbActions.getConnection(oldDBConfig, true, false), dbActions.getConnection(rollbackDBConfig, true, false));

            liquibase.update("");

            liquibase = new Liquibase("etc/db/liquibase/" + upgradeXML, new FileSystemResourceAccessor(), new JdbcConnection(dbActions.getConnection(oldDBConfig, true, false)));
            liquibase.update("");
        }

        dbActions.dropDatabase(rollbackDBConfig, hosts, true, true, null);

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
