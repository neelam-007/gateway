package com.l7tech.external.assertions.odata.server;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.odata.ODataProducerAssertion;
import com.l7tech.external.assertions.odata.server.producer.jdbc.JdbcCreateEntityCommand;
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
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import org.apache.commons.lang.StringUtils;
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
import org.odata4j.producer.resources.ODataEntitiesRequestResource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.xml.sax.SAXException;

/**
 * Really test with a valid database, in our case we will use an embedded db - hsqldb
 *
 * @author rraquepo, 9/24/13
 */
public class ServerODataAssertionHsqlDbTest {
  private PolicyEnforcementContext peCtx;

  private ODataProducerAssertion assertion;

  @Mock
  private JdbcConnectionManager jdbcConnectionManager;
  @Mock
  private JdbcConnectionPoolManager jdbcConnectionPoolManager;
  @Mock
  private JdbcConnection jdbcConnection;

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
  private static final String TABLE_NAME = "Table_Name1";
  private static final String TABLE_NAME2 = "Table_Name2";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    //create assertion
    assertion = new ODataProducerAssertion();
    assertion.setConnectionName(connectionName);

    connection = DriverManager.getConnection(DB, USERNAME, PASSWORD);
    createDb(connection);
    dataSource = new JDBCDataSource();
    dataSource.setDatabase(DB);
    dataSource.setUser(USERNAME);
    dataSource.setPassword(PASSWORD);

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
      put("serverConfig", ConfigFactory.getCachedConfig());
    }}));
    serverPolicyFactory.setApplicationContext(applicationContext);
    assertion.setConnectionName(connectionName);
  }

  @After
  public void cleanUp() throws Exception {
    update(connection, "DROP TABLE " + TABLE_NAME);
  }

  @Test
  public void testMetadata() throws Exception {
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
  }

  @Test
  public void testGetServiceDocument() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_SERVICE_DOCUMENT_XML, payloadAsString);
  }

  @Test
  public void testGetServiceDocumentJsonFormat() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/");
    hrequest.setMethod("GET");
    hrequest.setQueryString("$format=json");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_SERVICE_DOCUMENT_JSON, payloadAsString);
  }

  @Test
  public void testGetEntities() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_GET_ENTITIES_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetSingleEntity1() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(1)");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_GET_SINGLE_ENTITY_ID1_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetSingleEntity2() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(2)");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_GET_SINGLE_ENTITY_ID2_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetSingleEntity1WithSelect() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(1)");
    hrequest.setQueryString("$select=NumCol ");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_GET_SINGLE_ENTITY_ID1_SELECT_NUMCOL_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetSingleEntity1WithInvalidSelect() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(1)");
    hrequest.setQueryString("$select=InvalidColumn ");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_BAD_REQUEST_INVALID_SELECT, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetSingleEntityNotFound() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(100)");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_NOT_FOUND, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCount() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("2", removeUpdatedDateElement(payloadAsString));

    //let's add a new record, count should increase
    update(connection, "INSERT INTO " + TABLE_NAME + " values(3,'value3','3')");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("3", removeUpdatedDateElement(payloadAsString));

    //let's delete all record greater than id 1, count should decrease
    update(connection, "DELETE FROM " + TABLE_NAME + " WHERE id > 1");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("1", removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCountUsingInlineCount() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setQueryString("$inlinecount=allpages&$top=1");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("2", getCountElementValue(payloadAsString));
    assertEquals(TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_2_COUNT, removeUpdatedDateElement(payloadAsString));

    //let's add a new record, count should increase
    update(connection, "INSERT INTO " + TABLE_NAME + " values(3,'value3','3')");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("3", getCountElementValue(payloadAsString));
    assertEquals(TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_3_COUNT, removeUpdatedDateElement(payloadAsString));

    //let's delete all record greater than id 1, count should decrease
    update(connection, "DELETE FROM " + TABLE_NAME + " WHERE id > 1");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("1", getCountElementValue(payloadAsString));
    assertEquals(TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_1_COUNT, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCountWithFilter() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setQueryString("$filter=NumCol eq 1");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("1", removeUpdatedDateElement(payloadAsString));

    //let's add a new record, count should increase
    update(connection, "INSERT INTO " + TABLE_NAME + " values(3,'value3','3')");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("1", removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCountWithStartsLikeFilter() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setQueryString("$filter=startswith(StrCol,'val')");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("2", removeUpdatedDateElement(payloadAsString));

    //let's add a new record, count should increase
    update(connection, "INSERT INTO " + TABLE_NAME + " values(3,'value3','3')");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("3", removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setQueryString("$filter=startswith(StrCol,'value1')");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("1", removeUpdatedDateElement(payloadAsString));

    //test escaping of dangerous character in a LIKE statement namely % and _
    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setQueryString("$filter=startswith(StrCol,'%25val')");//%25 is the escape version of %, for a valid queryString
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("0", removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCountWithOrExpressionFilter() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setQueryString("$filter=StrCol eq 'value1' or StrCol eq 'value2'");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("2", removeUpdatedDateElement(payloadAsString));

    //let's add a new record, count should increase
    update(connection, "INSERT INTO " + TABLE_NAME + " values(3,'value3','3')");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("2", removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCountWithInvalidFilter() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s/$count");
    hrequest.setQueryString("$filter=InvalidColumn eq 1");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_BAD_REQUEST_INVALID_FILTER, payloadAsString);

  }

  @Test
  public void testGetEntitiesNotFound() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/NonExistingTable");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_NOT_FOUND, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntitiesCountNotFound() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/NonExistingTable/$count");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_NOT_FOUND, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testGetEntityNameNotFound() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/NonExistingTable(999)");
    hrequest.setMethod("GET");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_NOT_FOUND, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testDeleteEntity() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(1)");
    hrequest.setMethod("DELETE");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("", payloadAsString);//we don't expect any body

    assertEquals(1, recordCount(connection));

    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(2)");
    hrequest.setMethod("DELETE");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("", payloadAsString);//we don't expect any body

    assertEquals(0, recordCount(connection));
  }

  @Test
  public void testDeleteEntityNotFound() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(100)");
    hrequest.setMethod("DELETE");
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_BAD_REQUEST_NOT_DELETED, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testUpdateSingleEntity() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(1)");
    hrequest.setMethod("PUT");
    request = new Message(XmlUtil.stringAsDocument(TEXT_UPDATE_ENTITY_ID1_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("", payloadAsString);//UPDATE does not return any body

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(1)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_UPDATE_ENTITY_ID1_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testUpdateSingleEntityNotFound() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(444)");
    hrequest.setMethod("PUT");
    request = new Message(XmlUtil.stringAsDocument(TEXT_UPDATE_ENTITY_INVALID_ID_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("Record size should have not increased for UPDATE operation", beforeCount == afterCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_BAD_REQUEST_NOT_UPDATE, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testCreateSingleEntity() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_CREATE_ENTITY_ID999_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_ID999_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(999)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_ID999_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testCreateSingleEntityTestAutoIncrementKeys() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_CREATE_ENTITY_NOID_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_AUTOGEN_EXPECTED_RESULT_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(3)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_AUTOGEN_EXPECTED_RESULT_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testCreateSingleEntityTestAutoIncrementKeysPassingNegativeOneValue() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_CREATE_ENTITY_NEGATIVEID_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_AUTOGEN_EXPECTED_RESULT_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(3)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_AUTOGEN_EXPECTED_RESULT_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testCreateSingleEntityTestAutoGeneratedKey() throws Exception {
    createTable2(connection);

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
    assertEquals(TEXT_METADATA_XML2, payloadAsString);

    servletContext = new MockServletContext();
    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName2s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_CREATE_ENTITY_AUTOGENID_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount2(connection);
    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    int afterCount = recordCount2(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_EXPECT_WITHOUT_UUID_XML, removeGeneratedUuidElements(payloadAsString));

    String generatedUUID = getDPropIdElement(payloadAsString);
    assertNotNull(generatedUUID);
    assertTrue("GeneratedUUID should be 36 long string", generatedUUID.length() == 36);

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName2s('" + generatedUUID + "')");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertTrue(payloadAsString.contains(generatedUUID));
    assertEquals(TEXT_EXPECT_WITHOUT_UUID_XML, removeGeneratedUuidElements(payloadAsString));

    update(connection, "DROP TABLE " + TABLE_NAME2);
  }

  @Test
  public void tesCreateSingleEntityDuplicate() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_GET_SINGLE_ENTITY_ID1_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("Expecting no new record", beforeCount == afterCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_SERVICE_ERROR_EXCEPTION, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void tesCreateSingleEntityDuplicateWithInlineError() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_GET_SINGLE_ENTITY_ID1_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_SHOW_INLINE_ERROR), "true");

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("Expecting no new record", beforeCount == afterCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.FALSIFIED, status);
    assertNotNull(payloadAsString);
    assertTrue("integrity constraint violation should be in the response", payloadAsString.indexOf("integrity constraint violation") >= 0);
    assertEquals(TEXT_SERVICE_ERROR_EXCEPTION, removeInnerErrorElement(removeUpdatedDateElement(payloadAsString)));
  }

  @Test
  public void testCreateSingleEntityWithDoubleByteCharacter() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s");
    hrequest.setMethod("POST");
    request = new Message(XmlUtil.stringAsDocument(TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    int beforeCount = recordCount(connection);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(999)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(999)");
    hrequest.setMethod("PUT");
    request = new Message(XmlUtil.stringAsDocument(TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_EDIT_XML));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals("", payloadAsString);//UPDATE does not return any body

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(999)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_EDIT_XML, removeUpdatedDateElement(payloadAsString));
  }

  @Test
  public void testMultiPartBatch() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$batch");
    hrequest.setMethod("POST");
    request = makeMessage(MESS2_CREATE, MESS2_CONTENT_TYPE, FIRST_PART_MAX_BYTE);
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    int beforeCount = recordCount(connection);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);

    //now check individually
    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(999)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_ID999_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(555)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_ID555_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$batch");
    hrequest.setMethod("POST");
    request = makeMessage(MESS2_DELETE, MESS2_CONTENT_TYPE, FIRST_PART_MAX_BYTE);
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    afterCount = recordCount(connection);
    assertTrue("records count should be back after creating the records", afterCount == beforeCount);
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);

  }

  @Test
  public void testMultiPartBatch_WithFlags() throws Exception {
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$batch");
    hrequest.setMethod("POST");
    request = makeMessage(MESS2_CREATE_AND_UPDATE, MESS2_CONTENT_TYPE, FIRST_PART_MAX_BYTE);
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    int beforeCount = recordCount(connection);
    AssertionStatus status = sass.checkRequest(peCtx);
    int afterCount = recordCount(connection);
    assertTrue("records count should have increased after record creating", afterCount > beforeCount);
    String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertEquals(removeUpdatedDateElement(processBatchMarkers(payloadAsString, MESS_BATCH_SUCCESS_1)), removeUpdatedDateElement(payloadAsString));
    assertEquals("TableName1s", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_NAME));
    assertEquals("555", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_ID));
    assertEquals("200", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_STATUS));
    assertEquals(TEXT_CREATE_ENTITY_ID555_XML, removeUpdatedDateElement((String) peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_PAYLOAD)));
    assertEquals("200", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_STATUS));

    //now check individually
    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(999)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_ID999_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/TableName1s(555)");
    hrequest.setMethod("GET");
    request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    assertEquals(AssertionStatus.NONE, status);
    assertNotNull(payloadAsString);
    assertEquals(TEXT_CREATE_ENTITY_ID555_XML, removeUpdatedDateElement(payloadAsString));

    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$batch");
    hrequest.setMethod("POST");
    request = makeMessage(MESS2_DELETE_WITH_BAD, MESS2_CONTENT_TYPE, FIRST_PART_MAX_BYTE);
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

    //rollback will happen here due to a bad delete, so record should not increase from the last insert
    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    int newCount = recordCount(connection);
    assertTrue("records count should be back after creating the records", newCount == afterCount);
    assertEquals(AssertionStatus.NONE, status);
    assertEquals(processBatchMarkers(payloadAsString, MESS_BATCH_FAILED_1), payloadAsString);
    assertEquals("TableName1s", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_NAME));
    assertEquals("555", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_ID));
    assertEquals("412", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_STATUS));
    assertEquals(ODataEntitiesRequestResource.BATCH_FAILED_MESSAGE, removeUpdatedDateElement((String) peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_BODY)));

    //set transaction to false, to allow any existing delete to still take place even if one of them failed
    hrequest = new MockHttpServletRequest(servletContext);
    hrequest.setRequestURI("/OData.svc/$batch");
    hrequest.setMethod("POST");
    request = makeMessage(MESS2_DELETE_WITH_BAD, MESS2_CONTENT_TYPE, FIRST_PART_MAX_BYTE);
    request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
    peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_BATCH_TRANSACTION), "false");
    peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_BATCH_FAST_FAIL), "false");

    status = sass.checkRequest(peCtx);
    payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
    afterCount = recordCount(connection);
    assertTrue("records count should be back after creating the records " + afterCount + "==" + beforeCount, afterCount == beforeCount);
    assertEquals(AssertionStatus.NONE, status);
    assertEquals(processBatchMarkers(payloadAsString, MESS_BATCH_FAILED_2), payloadAsString);
    assertEquals("TableName1s", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_NAME));
    assertEquals("555", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_ENTITY_ID));
    assertEquals("200", peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_STATUS));
    assertEquals("", removeUpdatedDateElement((String) peCtx.getVariable(ODataProducerAssertion.ODATA_BATCH_LAST_BODY)));
  }

  /**
   * remove the date part of the response, as we can never match it at runtime
   */
  protected String removeUpdatedDateElement(String payloadAsString) {
    int start = payloadAsString.indexOf("<updated>");
    if (start < 0) {
      return payloadAsString;
    }
    int end = payloadAsString.indexOf("</updated>") + "</updated>".length();
    String processedString = payloadAsString.substring(0, start) + payloadAsString.substring(end);
    System.out.println(processedString);
    if (processedString.contains("<updated>")) {
      return removeUpdatedDateElement(processedString);
    }
    return processedString;
  }

  protected String removeInnerErrorElement(String payloadAsString) {
    int start = payloadAsString.indexOf("<innererror>");
    if (start < 0) {
      return payloadAsString;
    }
    int end = payloadAsString.indexOf("</innererror>") + "</innererror>".length();
    String processedString = payloadAsString.substring(0, start) + payloadAsString.substring(end);
    System.out.println(processedString);
    return processedString;
  }

  protected String removeGeneratedUuidElements(String payloadAsString) throws IOException, SAXException, TooManyChildElementsException, MissingRequiredElementException {
    final String payloadAsString3 = removeIdElement(payloadAsString);
    final String payloadAsString2 = removeDPropIdElement(payloadAsString3);
    final String payloadAsString1 = removeLinkElement(payloadAsString2);
    return removeUpdatedDateElement(payloadAsString1);
  }

  protected String getDPropIdElement(String payloadAsString) {
    int start = payloadAsString.indexOf("<d:Id>");
    if (start < 0) {
      return payloadAsString;
    }
    int end = payloadAsString.indexOf("</d:Id>");
    String processedString = payloadAsString.substring(start + "<d:Id>".length(), end);
    System.out.println(processedString);
    return processedString;
  }

  protected String removeIdElement(String payloadAsString) {
    int start = payloadAsString.indexOf("<id>");
    if (start < 0) {
      return payloadAsString;
    }
    int end = payloadAsString.indexOf("</id>") + "</id>".length();
    String processedString = payloadAsString.substring(0, start) + payloadAsString.substring(end);
    System.out.println(processedString);
    if (processedString.contains("<id>")) {
      return removeIdElement(processedString);
    }
    return processedString;
  }

  protected String removeDPropIdElement(String payloadAsString) {
    int start = payloadAsString.indexOf("<d:Id>");
    if (start < 0) {
      return payloadAsString;
    }
    int end = payloadAsString.indexOf("</d:Id>") + "</d:Id>".length();
    String processedString = payloadAsString.substring(0, start) + payloadAsString.substring(end);
    System.out.println(processedString);
    if (processedString.contains("<d:Id>")) {
      return removeDPropIdElement(processedString);
    }
    return processedString;
  }

  protected String removeLinkElement(String payloadAsString) {
    int start = payloadAsString.indexOf("<link");
    if (start < 0) {
      return payloadAsString;
    }
    final int end = StringUtils.indexOf(payloadAsString, "/>", start) + "/>".length();
    String processedString = payloadAsString.substring(0, start) + payloadAsString.substring(end);
    System.out.println(processedString);
    if (processedString.contains("<link")) {
      return removeLinkElement(processedString);
    }
    return processedString;
  }

  protected String processBatchMarkers(final String response, final String targetResponse) {
    int boundary_marker_start = response.indexOf(BATCH_BOUNDARY_MARKER);
    String boundary_marker = response.substring(boundary_marker_start, boundary_marker_start + BATCH_BOUNDARY_MARKER.length() + 36);
    int changeset_marker_start = response.indexOf(BATCH_CHANGESET_MARKER);
    String changeset_marker = response.substring(changeset_marker_start, changeset_marker_start + BATCH_CHANGESET_MARKER.length() + 36);
    String newResponse = targetResponse.replace("$$batchresponse_MARKER$$", boundary_marker);
    newResponse = newResponse.replace("$$changesetresponse_MARKER$$", changeset_marker);
    return newResponse;
  }

  private Message makeMessage(String message, String contentTypeValue, long maxSize) throws IOException, NoSuchPartException {
    InputStream mess = new ByteArrayInputStream(message.getBytes());
    ContentTypeHeader mr = ContentTypeHeader.parseValue(contentTypeValue);
    StashManager sm = new ByteArrayStashManager();
    //return new MimeBody(sm, mr, mess, maxSize);

    return new Message(sm, mr, mess);
  }

  /**
   * remove the date part of the response, as we can never match it at runtime
   */
  protected String getCountElementValue(String payloadAsString) {
    int start = payloadAsString.indexOf("<m:count>");
    if (start < 0) {
      return payloadAsString;
    }
    start = start + "<m:count>".length();
    int end = payloadAsString.indexOf("</m:count>");
    String processedString = payloadAsString.substring(start, end);
    System.out.println(processedString);
    if (processedString.contains("<m:count>")) {
      return getCountElementValue(processedString);
    }
    return processedString;
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

  private int recordCount(Connection conn) throws SQLException {
    Statement st = null;
    st = conn.createStatement();    // statements
    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLE_NAME);    // run the query
    while (rs.next()) {
      return rs.getInt(1);
    }
    st.close();
    return -1;
  }    // void upd

  private int recordCount2(Connection conn) throws SQLException {
    Statement st = null;
    st = conn.createStatement();    // statements
    ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TABLE_NAME2);    // run the query
    while (rs.next()) {
      return rs.getInt(1);
    }
    st.close();
    return -1;
  }    // void upd

  protected static String getAsVariableName(String varName) {
    if (varName.startsWith("${")) {
      return varName.substring(2, varName.length() - 1);
    }
    return varName;
  }

  private final static String TEXT_METADATA_XML = "<?xml version='1.0' encoding='utf-8'?><edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices m:DataServiceVersion=\"2.0\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema Namespace=\"JdbcModel\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityType Name=\"TableName1\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.Int32\" Nullable=\"false\" MaxLength=\"32\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType></Schema><Schema Namespace=\"JdbcEntities.Public\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityContainer Name=\"Public\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"TableName1s\" EntityType=\"JdbcModel.TableName1\"/></EntityContainer></Schema></edmx:DataServices></edmx:Edmx>";
  private final static String TEXT_SERVICE_DOCUMENT_XML = "<?xml version='1.0' encoding='utf-8'?><service xmlns=\"http://www.w3.org/2007/app\" xml:base=\"http://localhost:80/OData.svc/\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:app=\"http://www.w3.org/2007/app\"><workspace><atom:title>Default</atom:title><collection href=\"TableName1s\"><atom:title>TableName1s</atom:title></collection></workspace></service>";
  private final static String TEXT_SERVICE_DOCUMENT_JSON = "{\n" +
          "\"d\" : {\n" +
          "\"EntitySets\" : [\n" +
          "\"TableName1s\"\n" +
          "]\n" +
          "}\n" +
          "}";
  private final static String TEXT_GET_ENTITIES_XML = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableName1s</title><id>http://localhost:80/OData.svc/TableName1s</id><link rel=\"self\" title=\"TableName1s\" href=\"TableName1s\"/><entry><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol></m:properties></content></entry><entry><id>http://localhost:80/OData.svc/TableName1s(2)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(2)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">2</d:Id><d:StrCol>value2</d:StrCol><d:NumCol m:type=\"Edm.Int32\">2</d:NumCol></m:properties></content></entry></feed>";
  private final static String TEXT_GET_SINGLE_ENTITY_ID1_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_GET_SINGLE_ENTITY_ID1_SELECT_NUMCOL_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol><d:Id m:type=\"Edm.Int32\">1</d:Id></m:properties></content></entry>";
  private final static String TEXT_GET_SINGLE_ENTITY_ID2_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(2)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(2)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">2</d:Id><d:StrCol>value2</d:StrCol><d:NumCol m:type=\"Edm.Int32\">2</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_NOT_FOUND = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>NotFoundException</code><message lang=\"en-US\">Not Found</message></error>";
  private final static String TEXT_BAD_REQUEST_NOT_DELETED = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">Entity not deleted</message></error>";
  private final static String TEXT_UPDATE_ENTITY_ID1_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">9999</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_UPDATE_ENTITY_INVALID_ID_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(444)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(444)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">444</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">9999</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_CREATE_ENTITY_ID999_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(999)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(999)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">999</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_CREATE_ENTITY_ID555_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(555)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(555)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">555</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">8888</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_BAD_REQUEST_NOT_UPDATE = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">Entity not updated</message></error>";
  private final static String TEXT_SERVICE_ERROR_EXCEPTION = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>ServerErrorException</code><message lang=\"en-US\">Internal Server Error</message></error>";
  private final static String TEXT_CREATE_ENTITY_NOID_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\"/><author><name/></author><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_CREATE_ENTITY_AUTOGEN_EXPECTED_RESULT_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(3)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(3)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">3</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_CREATE_ENTITY_NEGATIVEID_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\"/><author><name/></author><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">-1</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_METADATA_XML2 = "<?xml version='1.0' encoding='utf-8'?><edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices m:DataServiceVersion=\"2.0\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema Namespace=\"JdbcModel\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityType Name=\"TableName1\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.Int32\" Nullable=\"false\" MaxLength=\"32\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType><EntityType Name=\"TableName2\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"255\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType></Schema><Schema Namespace=\"JdbcEntities.Public\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityContainer Name=\"Public\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"TableName1s\" EntityType=\"JdbcModel.TableName1\"/><EntitySet Name=\"TableName2s\" EntityType=\"JdbcModel.TableName2\"/></EntityContainer></Schema></edmx:DataServices></edmx:Edmx>";
  private final static String TEXT_CREATE_ENTITY_AUTOGENID_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\"/><author><name/></author><category term=\"JdbcModel.TableName2\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>" + JdbcCreateEntityCommand.MAGIC_STRING_UUID + "</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_EXPECT_WITHOUT_UUID_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\"/><author><name/></author><category term=\"JdbcModel.TableName2\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_1_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableName1s</title><id>http://localhost:80/OData.svc/TableName1s</id><link rel=\"self\" title=\"TableName1s\" href=\"TableName1s\"/><m:count>1</m:count><entry><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol></m:properties></content></entry></feed>";
  private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_2_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableName1s</title><id>http://localhost:80/OData.svc/TableName1s</id><link rel=\"self\" title=\"TableName1s\" href=\"TableName1s\"/><m:count>2</m:count><entry><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol></m:properties></content></entry></feed>";
  private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_3_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableName1s</title><id>http://localhost:80/OData.svc/TableName1s</id><link rel=\"self\" title=\"TableName1s\" href=\"TableName1s\"/><m:count>3</m:count><entry><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol></m:properties></content></entry></feed>";
  private final static String TEXT_BAD_REQUEST_INVALID_FILTER = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">There was an invalid field in the $filter parameter</message></error>";
  private final static String TEXT_BAD_REQUEST_INVALID_SELECT = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">There was an invalid field in the $select parameter</message></error>";

  private final static String TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(999)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(999)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">999</d:Id><d:StrCol>ö or á or бла бла хелло</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";
  private final static String TEXT_CREATE_ENTITY_DOUBLEBYTE_ID999_EDIT_XML = "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(999)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(999)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">999</d:Id><d:StrCol>ö or á or бла бла хелло</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>";

  private static long FIRST_PART_MAX_BYTE = 0L;
  public static final String MESS2_BOUNDARY = "----=Part_-763936460.00306951464153826";
  public static final String MESS2_CONTENT_TYPE = "multipart/mixed; type=\"text/plain\"; boundary=\"" +
          MESS2_BOUNDARY + "\"; start=\"1\"";
  public static final String MESS2_CREATE = "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "POST /OData.svc/TableName1s\r\n" +
          "Content-ID: 1\r\n" +
          "Content-Length: " + (TEXT_CREATE_ENTITY_ID999_XML.length()) + "\r\n" +
          "\r\n" +
          TEXT_CREATE_ENTITY_ID999_XML +
          "\r\n" +
          "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "POST /OData.svc/TableName1s\r\n" +
          "Content-ID: 2\r\n" +
          "Content-Length: " + (TEXT_CREATE_ENTITY_ID555_XML.length()) + "\r\n" +
          "\r\n" +
          TEXT_CREATE_ENTITY_ID555_XML +
          "\r\n" +
          "------=Part_-763936460.00306951464153826--\r\n";
  public static final String MESS2_DELETE = "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "DELETE /OData.svc/TableName1s('555')\r\n" +
          "Content-ID: 1\r\n" +
          "\r\n" +
          "\r\n" +
          "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "DELETE /OData.svc/TableName1s('999')\r\n" +
          "Content-ID: 2\r\n" +
          "\r\n" +
          "\r\n" +
          "------=Part_-763936460.00306951464153826--\r\n";
  public static final String MESS2_CREATE_AND_UPDATE = "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "POST /OData.svc/TableName1s\r\n" +
          "Content-ID: 1\r\n" +
          "Content-Length: " + (TEXT_CREATE_ENTITY_ID999_XML.length()) + "\r\n" +
          "\r\n" +
          TEXT_CREATE_ENTITY_ID999_XML +
          "\r\n" +
          "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "POST /OData.svc/TableName1s\r\n" +
          "Content-ID: 2\r\n" +
          "Content-Length: " + (TEXT_CREATE_ENTITY_ID555_XML.length()) + "\r\n" +
          "\r\n" +
          TEXT_CREATE_ENTITY_ID555_XML +
          "\r\n" +
          "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "PUT /OData.svc/TableName1s('555')\r\n" +
          "Content-ID: 3\r\n" +
          "Content-Length: " + (TEXT_CREATE_ENTITY_ID555_XML.length()) + "\r\n" +
          "\r\n" +
          TEXT_CREATE_ENTITY_ID555_XML +
          "\r\n" +
          "------=Part_-763936460.00306951464153826--\r\n";
  public static final String MESS2_DELETE_WITH_BAD = "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "DELETE /OData.svc/TableName1s('000')\r\n" +
          "Content-ID: 1\r\n" +
          "\r\n" +
          "\r\n" +
          "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "DELETE /OData.svc/TableName1s('999')\r\n" +
          "Content-ID: 2\r\n" +
          "\r\n" +
          "\r\n" +
          "------=Part_-763936460.00306951464153826\r\n" +
          "Content-Type: application/http\r\n" +
          "DELETE /OData.svc/TableName1s('555')\r\n" +
          "Content-ID: 3\r\n" +
          "\r\n" +
          "\r\n" +
          "------=Part_-763936460.00306951464153826--\r\n";
  public static final String BATCH_BOUNDARY_MARKER = "batchresponse_";
  public static final String BATCH_CHANGESET_MARKER = "changesetresponse_";
  public static final String MESS_BATCH_SUCCESS_1 = "\n" +
          "--$$batchresponse_MARKER$$\n" +
          "Content-Type: multipart/mixed; boundary=$$changesetresponse_MARKER$$\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 201 Created\n" +
          "Content-Type: application/atom+xml; charset=utf-8;\n" +
          "Location: http://localhost:80/OData.svc/TableName1s(999);\n" +
          "DataServiceVersion: 1.0;\n" +
          "\n" +
          "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(999)</id><title type=\"text\"/><updated>2014-11-25T21:35:34Z</updated><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(999)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">999</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">7777</d:NumCol></m:properties></content></entry>\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 201 Created\n" +
          "Content-Type: application/atom+xml; charset=utf-8;\n" +
          "Location: http://localhost:80/OData.svc/TableName1s(555);\n" +
          "DataServiceVersion: 1.0;\n" +
          "\n" +
          "<?xml version='1.0' encoding='utf-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><id>http://localhost:80/OData.svc/TableName1s(555)</id><title type=\"text\"/><updated>2014-11-25T21:35:34Z</updated><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(555)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">555</d:Id><d:StrCol>newly created record</d:StrCol><d:NumCol m:type=\"Edm.Int32\">8888</d:NumCol></m:properties></content></entry>\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 200 OK\n" +
          "DataServiceVersion: 1.0;\n" +
          "\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$--\n" +
          "--$$batchresponse_MARKER$$--\n";
  public static final String MESS_BATCH_FAILED_1 = "\n" +
          "--$$batchresponse_MARKER$$\n" +
          "Content-Type: multipart/mixed; boundary=$$changesetresponse_MARKER$$\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 400 Bad Request\n" +
          "\n" +
          "Entity not deleted\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 412 Precondition Failed\n" +
          "\n" +
          "Cannot process due to previous error(s). Transaction and/or Fast fail flag is probably to true\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 412 Precondition Failed\n" +
          "\n" +
          "Cannot process due to previous error(s). Transaction and/or Fast fail flag is probably to true\n" +
          "--$$changesetresponse_MARKER$$--\n" +
          "--$$batchresponse_MARKER$$--\n";
  public static final String MESS_BATCH_FAILED_2 = "\n" +
          "--$$batchresponse_MARKER$$\n" +
          "Content-Type: multipart/mixed; boundary=$$changesetresponse_MARKER$$\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 400 Bad Request\n" +
          "\n" +
          "Entity not deleted\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 200 OK\n" +
          "DataServiceVersion: 1.0;\n" +
          "\n" +
          "\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$\n" +
          "Content-Type: application/http\n" +
          "Content-Transfer-Encoding: binary\n" +
          "\n" +
          "HTTP/1.1 200 OK\n" +
          "DataServiceVersion: 1.0;\n" +
          "\n" +
          "\n" +
          "--$$changesetresponse_MARKER$$--\n" +
          "--$$batchresponse_MARKER$$--\n";
}
