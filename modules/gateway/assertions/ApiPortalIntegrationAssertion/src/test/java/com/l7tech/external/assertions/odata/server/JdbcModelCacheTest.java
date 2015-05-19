package com.l7tech.external.assertions.odata.server;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.odata.ODataProducerAssertion;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcModel;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.Config;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.apache.cxf.helpers.IOUtils;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

/**
 * Test the caching logic
 *
 * @author rraquepo, 1/15/14
 */
public class JdbcModelCacheTest {
  private PolicyEnforcementContext peCtx;

  private ODataProducerAssertion assertion;

  @Mock
  private JdbcConnectionManager jdbcConnectionManager;
  @Mock
  private JdbcConnectionPoolManager jdbcConnectionPoolManager;
  @Mock
  private JdbcConnection jdbcConnection;
  @Mock
  private Config config;

  private ServerPolicyFactory serverPolicyFactory;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  final String connectionName = "hsqldb";

  Message request;

  Message response;

  //for hsqldb use
  protected Connection connection;
  protected static JDBCDataSource dataSource;
  private static final String USERNAME = "sa";
  private static final String PASSWORD = "";
  private static final String DB = "jdbc:hsqldb:mem:testdb";
  private static final String TABLE_NAME = "Table_Name1";
  private static final String TABLE_NAME2 = "Table_Name2";

  static {            //this make sure there is one instance of datasource, as the cache key's of on it's toString()+hashcode() methods
    dataSource = new JDBCDataSource();
    dataSource.setDatabase(DB);
    dataSource.setUser(USERNAME);
    dataSource.setPassword(PASSWORD);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    //create assertion
    assertion = new ODataProducerAssertion();
    assertion.setConnectionName(connectionName);

    connection = DriverManager.getConnection(DB, USERNAME, PASSWORD);
    createDb(connection);

    when(jdbcConnectionManager.getJdbcConnection(Matchers.eq(connectionName))).thenReturn(jdbcConnection);
    when(jdbcConnectionPoolManager.getDataSource(Matchers.eq(connectionName))).thenReturn(dataSource);

    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    response = new Message(XmlUtil.stringAsDocument("<myresponse/>"));

    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    final AssertionRegistry assertionRegistry = new AssertionRegistry();
    assertionRegistry.afterPropertiesSet();
    serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
    GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
      put("assertionRegistry", assertionRegistry);
      put("policyFactory", serverPolicyFactory);
      put("jdbcConnectionManager", jdbcConnectionManager);
      put("jdbcConnectionPoolManager", jdbcConnectionPoolManager);
      put("serverConfig", config);
    }}));
    serverPolicyFactory.setApplicationContext(applicationContext);
    assertion.setConnectionName(connectionName);
  }

  @After
  public void cleanUp() throws Exception {
    update(connection, "DROP TABLE " + TABLE_NAME);
    try {
      update(connection, "DROP TABLE " + TABLE_NAME2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test01_Metadata_() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$metadata");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_METADATA_XML, payloadAsString);

    createTable2(connection);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_METADATA_XML, payloadAsString);//since the datasource is already cached, it should return the old metadata even though we created a new table

    //clear the cache, so it will re-read the schema again
    sass.getJdbcModelCache().getCache().clear();
    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_METADATA_XML2, payloadAsString);

    update(connection, "DROP TABLE " + TABLE_NAME2);

  }

  @Test
  public void test02_Metadata_WithMockCache() throws Exception {
    Map<String, JdbcModel> modelCache = mock(Map.class);
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$metadata");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    sass.getJdbcModelCache().setCache(modelCache);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_METADATA_XML, payloadAsString);

    createTable2(connection);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    sass.getJdbcModelCache().setCache(modelCache);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_METADATA_XML2, payloadAsString);

    update(connection, "DROP TABLE " + TABLE_NAME2);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    sass.getJdbcModelCache().setCache(modelCache);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_METADATA_XML, payloadAsString);

    verify(modelCache, times(3)).get(anyString());
    verify(modelCache, times(3)).put(anyString(), any(JdbcModel.class));

    verify(modelCache, never()).clear();

    when(config.getLongProperty(ODataProducerAssertion.PARAM_CACHE_WIPE_INTERVAL, ODataProducerAssertion.CACHE_WIPE_INTERVAL_DEFAULT)).thenReturn(1000L);
    final JdbcModelCache jdbcModelCache = JdbcModelCache.getInstance();
    jdbcModelCache.setConfig(config);
    jdbcModelCache.scheduleCleanup();
    Thread.sleep(1100);//introduce some delay to give the cleanup schedule some time to kick-in

    verify(modelCache, times(1)).clear();
  }

  private void createDb(Connection conn) throws SQLException {
    update(conn, "CREATE TABLE " + TABLE_NAME + " ( id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, str_col VARCHAR(256), num_col INTEGER)");
    update(conn, "INSERT INTO " + TABLE_NAME + " values(1,'value1','1')");
    update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value2','2')");
  }

  private void createTable2(Connection conn) throws SQLException {
    update(conn, "CREATE TABLE " + TABLE_NAME2 + " (id VARCHAR(255) PRIMARY KEY, str_col VARCHAR(256), num_col INTEGER)");

  }

  private void update(Connection conn, String expression) throws SQLException {
    Statement st = null;
    st = conn.createStatement();    // statements
    int i = st.executeUpdate(expression);    // run the query
    if (i == -1) {
      System.out.println("db error : " + expression);
    }
    st.close();

  }    // void upd

  private final static String TEXT_METADATA_XML = "<?xml version='1.0' encoding='utf-8'?><edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices m:DataServiceVersion=\"2.0\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema Namespace=\"JdbcModel\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityType Name=\"TableName1\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.Int32\" Nullable=\"false\" MaxLength=\"32\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType></Schema><Schema Namespace=\"JdbcEntities.Public\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityContainer Name=\"Public\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"TableName1s\" EntityType=\"JdbcModel.TableName1\"/></EntityContainer></Schema></edmx:DataServices></edmx:Edmx>";
  private final static String TEXT_METADATA_XML2 = "<?xml version='1.0' encoding='utf-8'?><edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices m:DataServiceVersion=\"2.0\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema Namespace=\"JdbcModel\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityType Name=\"TableName1\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.Int32\" Nullable=\"false\" MaxLength=\"32\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType><EntityType Name=\"TableName2\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"255\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType></Schema><Schema Namespace=\"JdbcEntities.Public\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityContainer Name=\"Public\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"TableName1s\" EntityType=\"JdbcModel.TableName1\"/><EntitySet Name=\"TableName2s\" EntityType=\"JdbcModel.TableName2\"/></EntityContainer></Schema></edmx:DataServices></edmx:Edmx>";
}
