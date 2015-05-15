package com.l7tech.external.assertions.analytics.server;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.analytics.AnalyticsAssertion;
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
import com.l7tech.util.SyspropUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
 * Really test the Capture Analytics Assertion
 *
 * @author rraquepo, 8/6/14
 */
public class ServerAnalyticsAssertionTest {
  private PolicyEnforcementContext peCtx;

  private AnalyticsAssertion assertion;

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
  protected JDBCDataSource dataSource;
  private static final String USERNAME = "sa";
  private static final String PASSWORD = "";
  private static final String DB = "jdbc:hsqldb:mem:testdb";
  private static final String TABLE_NAME = "API_METRICS";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    //create assertion
    assertion = new AnalyticsAssertion();
    assertion.setConnectionName(connectionName);

    connection = DriverManager.getConnection(DB, USERNAME, PASSWORD);
    createDb(connection);
    dataSource = new JDBCDataSource();
    dataSource.setDatabase(DB);
    dataSource.setUser(USERNAME);
    dataSource.setPassword(PASSWORD);

    when(jdbcConnectionManager.getJdbcConnection(Matchers.eq(connectionName))).thenReturn(jdbcConnection);
    when(jdbcConnectionPoolManager.getDataSource(Matchers.eq(connectionName))).thenReturn(dataSource);
    when(config.getIntProperty(AnalyticsAssertion.PARAM_ANALYTICS_CAPTURE_BATCH_SIZE, AnalyticsAssertion.ANALYTICS_CAPTURE_BATCH_SIZE_DEFAULT)).thenReturn(10);

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
  }

  //this test is time/thread dependent
  @Test
  public void testCaptureMetrics() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/Sample-EndPoint/1?apiKey=123");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerAnalyticsAssertion sass = (ServerAnalyticsAssertion) serverPolicyFactory.compilePolicy(assertion, false);

    AssertionStatus status = sass.checkRequest(peCtx);
    assertEquals(AssertionStatus.NONE, status);

    assertFalse(MetricsQueueProcessor.getInstance().getQueue().isEmpty());
    assertEquals(MetricsQueueProcessor.getInstance().getQueue().size(), 1);

    Thread.sleep((3 * 1000) + 100);//introduce some delay to give the schedule some time to kick-in

    assertTrue(MetricsQueueProcessor.getInstance().getQueue().isEmpty());
    assertEquals(MetricsQueueProcessor.getInstance().getQueue().size(), 0);

    assertEquals(recordCount(connection), 1);

  }

  //this test is time/thread dependent
  @Test
  public void testCaptureMetrics_20_hits() throws Exception {
    MockServletContext servletContext = new MockServletContext();

    ServerAnalyticsAssertion sass = (ServerAnalyticsAssertion) serverPolicyFactory.compilePolicy(assertion, false);

    int REC_COUNT = 20;
    for (int i = 0; i < REC_COUNT; i++) {
      MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
      hrequest.setRequestURI("/Sample-EndPoint/1?count=" + i);
      hrequest.setMethod("GET");
      request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
      request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
      peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

      AssertionStatus status = sass.checkRequest(peCtx);
      assertEquals(AssertionStatus.NONE, status);
    }

    assertFalse(MetricsQueueProcessor.getInstance().getQueue().isEmpty());
    assertEquals(MetricsQueueProcessor.getInstance().getQueue().size(), REC_COUNT);

    Thread.sleep((3 * 1000) + 100);//introduce some delay to give the schedule some time to kick-in

    assertEquals(MetricsQueueProcessor.getInstance().getQueue().size(), 10);

    Thread.sleep((3 * 1000) + 100);//introduce some delay to give the schedule some time to kick-in

    assertTrue(MetricsQueueProcessor.getInstance().getQueue().isEmpty());
    assertEquals(MetricsQueueProcessor.getInstance().getQueue().size(), 0);


    assertEquals(recordCount(connection), REC_COUNT);

  }

  @Test
  public void testMessageQueueProcessor() throws Exception {
    ConcurrentLinkedQueue<HitsMetric> queue = mock(ConcurrentLinkedQueue.class);
    HitsMetric hit1 = new HitsMetric();
    hit1.setConnectionName(connectionName);
    when(queue.isEmpty()).thenReturn(false).thenReturn(true);
    when(queue.poll()).thenReturn(hit1).thenReturn(null);
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/Sample-EndPoint/1?apiKey=123");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerAnalyticsAssertion sass = (ServerAnalyticsAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    MetricsQueueProcessor.getInstance().setQueue(queue);

    AssertionStatus status = sass.checkRequest(peCtx);
    assertEquals(AssertionStatus.NONE, status);

    SyspropUtil.setProperty("com.l7tech.analytics.capture.writeInterval", "1000");//1000ms=1sec
    MetricsQueueProcessor.getInstance().scheduleTask();
    Thread.sleep(1100);//introduce some delay to give the schedule some time to kick-in

    verify(queue, times(1)).isEmpty();
  }

  private void createDb(Connection conn) throws SQLException {
    update(conn, CREATE_STATEMENT);
  }

  private void update(Connection conn, String expression) throws SQLException {
    Statement st = null;
    st = conn.createStatement();    // statements
    int i = st.executeUpdate(expression);    // run the query
    if (i == -1) {
      System.out.println("db error : " + expression);
    }
    st.close();

  }    //

  private int recordCount(Connection conn) throws SQLException {
    Statement st = null;
    st = conn.createStatement();    // statements
    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLE_NAME);    // run the query
    while (rs.next()) {
      return rs.getInt(1);
    }
    st.close();
    return -1;
  }    // voi

  /*
     INSERT INTO API_METRICS (1=UUID, 2=SSG_NODE_ID, 3=SSG_REQUEST_ID, 4=RESOLUTION, " +
     "5=RESOLUTION_TIME_INTERVAL, 6=ROLLUP_START_TIME, 7=SSG_REQUEST_START_TIME, 8=SSG_REQUEST_END_TIME, " +
     "9=SSG_SERVICE_ID, 10=SSG_PORTAL_API_ID, " +
     "11=HTTP_METHOD, 12=HTTP_PUT_COUNT, 13=HTTP_POST_COUNT, 14=HTTP_DELETE_COUNT, 15=HTTP_GET_COUNT, 16=HTTP_OTHER_COUNT, " +
     "17=SERVICE_URI, 18=AUTH_TYPE, 19=HTTP_RESPONSE_STATUS, " +
     "20=SUCCESS_COUNT, 21=ERROR_COUNT, 22=PROXY_LATENCY, " +
     "23=BACKEND_LATENCY, 24=TOTAL_LATENCY, " +
     "25=APPLICATION_UUID, 26=ORGANIZATION_UUID, 27=ACCOUNT_PLAN_UUID, 28=API_PLAN_UUID, " +
     "29=CUSTOM_TAG1, 30=CUSTOM_TAG2, 31=CUSTOM_TAG3, 32=CUSTOM_TAG4, 33=CUSTOM_TAG5) VALUES" +
      */
    private final static String CREATE_STATEMENT = "CREATE TABLE " + TABLE_NAME
            + " ( UUID VARCHAR(255) PRIMARY KEY, SSG_NODE_ID VARCHAR(256), SSG_REQUEST_ID VARCHAR(256), RESOLUTION INTEGER,"
            + " RESOLUTION_TIME_INTERVAL INTEGER, ROLLUP_START_TIME INTEGER, SSG_REQUEST_START_TIME BIGINT , SSG_REQUEST_END_TIME BIGINT ,"
            + " SSG_SERVICE_ID VARCHAR(255), SSG_PORTAL_API_ID VARCHAR(255), REQUEST_IP VARCHAR(255), HTTP_METHOD VARCHAR(255),"
            + " HTTP_PUT_COUNT INTEGER, HTTP_POST_COUNT INTEGER, HTTP_DELETE_COUNT INTEGER,HTTP_GET_COUNT INTEGER,HTTP_OTHER_COUNT INTEGER,"
            + " SERVICE_URI VARCHAR(255), AUTH_TYPE VARCHAR(255), HTTP_RESPONSE_STATUS INTEGER,"
            + " SUCCESS_COUNT INTEGER, ERROR_COUNT INTEGER, PROXY_LATENCY INTEGER,"
            + " BACKEND_LATENCY INTEGER, TOTAL_LATENCY INTEGER,"
            + " APPLICATION_UUID VARCHAR(255), ORGANIZATION_UUID VARCHAR(255), ACCOUNT_PLAN_UUID VARCHAR(255),"
            + " API_PLAN_UUID VARCHAR(255), CUSTOM_TAG1 VARCHAR(255), CUSTOM_TAG2 VARCHAR(255),"
            + " CUSTOM_TAG3 VARCHAR(255), CUSTOM_TAG4 VARCHAR(255), CUSTOM_TAG5 VARCHAR(255)"
            + " ) ";
}
