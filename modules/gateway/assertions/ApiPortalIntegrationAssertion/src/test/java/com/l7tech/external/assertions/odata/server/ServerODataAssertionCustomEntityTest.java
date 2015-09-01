package com.l7tech.external.assertions.odata.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.odata.ODataProducerAssertion;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author rraquepo, 7/30/14
 */
public class ServerODataAssertionCustomEntityTest {
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
        createTable2(connection);
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
        update(connection, "DROP TABLE " + TABLE_NAME2);
    }

    @Test
    public void testMetadata() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/$metadata");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

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
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

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
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_SERVICE_DOCUMENT_JSON, payloadAsString);
    }

    @Test
    public void testGetEntitiesJson() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests");
        hrequest.setMethod("GET");
        hrequest.setQueryString("$format=json&apply=groupby(Id,RefName)");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_GET_ENTITIES_JSON, removeUpdatedDateElement(payloadAsString));
    }

    @Test
    public void testGetEntitiesXml() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests");
        hrequest.setMethod("GET");
        hrequest.setQueryString("apply=groupby(Id,RefName)");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_GET_ENTITIES_XML, removeUpdatedDateElement(payloadAsString));
    }

    @Test
    public void testGetEntitiesCount() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("apply=groupby(Id,RefName)");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        System.out.print(payloadAsString);
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
    }

    @Test
    public void testGetEntitiesCountUsingInlineCount() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests");
        hrequest.setQueryString("$inlinecount=allpages&$top=1&apply=groupby(Id,RefName)");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("3", getCountElementValue(payloadAsString));
        assertEquals(TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_3_COUNT, removeUpdatedDateElement(payloadAsString));

        //let's add a new record, count should increase
        update(connection, "INSERT INTO " + TABLE_NAME2 + " values(4,'ref name4','3')");

        status = sass.checkRequest(peCtx);
        payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("4", getCountElementValue(payloadAsString));
        assertEquals(TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_4_COUNT, removeUpdatedDateElement(payloadAsString));

        //let's delete all record greater than id 1, count should decrease
        update(connection, "DELETE FROM " + TABLE_NAME2 + " WHERE id > 1");

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
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("$filter=RefName eq 'ref name1'&apply=groupby(Id,RefName)");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("1", removeUpdatedDateElement(payloadAsString));

        //let's add a new record, count should increase
        update(connection, "INSERT INTO " + TABLE_NAME2 + " values(4,'ref name4','4')");

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
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("$filter=startswith(RefName,'ref name')&apply=groupby(Id,RefName)");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("3", removeUpdatedDateElement(payloadAsString));

        //let's add a new record, count should increase
        update(connection, "INSERT INTO " + TABLE_NAME2 + " values(4,'ref name4','4')");

        status = sass.checkRequest(peCtx);
        payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("4", removeUpdatedDateElement(payloadAsString));

        hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("$filter=startswith(RefName,'ref name1')&apply=groupby(Id,RefName)");
        hrequest.setMethod("GET");
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        status = sass.checkRequest(peCtx);
        payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("1", removeUpdatedDateElement(payloadAsString));

        //test escaping of dangerous character in a LIKE statement namely % and _
        hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("$filter=startswith(RefName,'%25ref name1')&apply=groupby(Id,RefName)");//%25 is the escape version of %, for a valid queryString
        hrequest.setMethod("GET");
        request = new Message(XmlUtil.stringAsDocument("<myrequest/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

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
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("$filter=RefName eq 'ref name1' or RefName eq 'ref name2' or RefName eq 'ref name4'&apply=groupby(Id,RefName)");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("2", removeUpdatedDateElement(payloadAsString));

        //let's add a new record, count should increase
        update(connection, "INSERT INTO " + TABLE_NAME2 + " values(4,'ref name4','4')");

        status = sass.checkRequest(peCtx);
        payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals("3", removeUpdatedDateElement(payloadAsString));
    }

    @Test
    public void testGetEntitiesCountWithInvalidFilter() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableGroupingTests/$count");
        hrequest.setQueryString("$filter=InvalidColumn eq 1");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_CUSTOM_ENTITIES), CUSTOM_ENTITIES);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_BAD_REQUEST_INVALID_FILTER, payloadAsString);

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

    protected String removeGeneratedUuidElements(String payloadAsString) {
        return removeUpdatedDateElement(removeLinkElement(removeDPropIdElement(removeIdElement(payloadAsString))));
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
        int end = payloadAsString.indexOf("</link>") + "</link>".length();
        String processedString = payloadAsString.substring(0, start) + payloadAsString.substring(end);
        System.out.println(processedString);
        if (processedString.contains("<link")) {
            return removeLinkElement(processedString);
        }
        return processedString;
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
        update(conn, "CREATE TABLE " + TABLE_NAME + " ( id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, str_col VARCHAR(256), num_col INTEGER, ref_id INTEGER)");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(1,'value1','1','1')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value2','2','2')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value3','3','1')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value4','4','2')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value5','5','1')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value6','6','2')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value7','7','1')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value8','8','2')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value9','9','1')");
        update(conn, "INSERT INTO " + TABLE_NAME + " values(NULL,'value10','9','2')");
    }

    private void createTable2(Connection conn) throws SQLException {
        update(conn, "CREATE TABLE " + TABLE_NAME2 + " (id VARCHAR(255) PRIMARY KEY, ref_name VARCHAR(256), num_col INTEGER)");
        update(conn, "INSERT INTO " + TABLE_NAME2 + " values(1,'ref name1','1')");
        update(conn, "INSERT INTO " + TABLE_NAME2 + " values(2,'ref name2','2')");
        update(conn, "INSERT INTO " + TABLE_NAME2 + " values(3,'ref name3','3')");
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

    private final static String TEXT_METADATA_XML = "<?xml version='1.0' encoding='utf-8'?><edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices m:DataServiceVersion=\"2.0\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema Namespace=\"JdbcModel\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityType Name=\"TableName1\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.Int32\" Nullable=\"false\" MaxLength=\"32\"/><Property Name=\"StrCol\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/><Property Name=\"RefId\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType><EntityType Name=\"TableName2\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"255\"/><Property Name=\"RefName\" Type=\"Edm.String\" Nullable=\"true\" MaxLength=\"256\"/><Property Name=\"NumCol\" Type=\"Edm.Int32\" Nullable=\"true\" MaxLength=\"32\"/></EntityType><EntityType Name=\"TableGroupingTest\"><Key><PropertyRef Name=\"Id\"/></Key><Property Name=\"Id\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"36\"/><Property Name=\"Ctr\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"10\"/><Property Name=\"RefName\" Type=\"Edm.String\" Nullable=\"false\" MaxLength=\"36\"/></EntityType></Schema><Schema Namespace=\"JdbcEntities.Public\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityContainer Name=\"Public\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"TableName1s\" EntityType=\"JdbcModel.TableName1\"/><EntitySet Name=\"TableName2s\" EntityType=\"JdbcModel.TableName2\"/><EntitySet Name=\"TableGroupingTests\" EntityType=\"JdbcModel.TableGroupingTest\"/></EntityContainer></Schema></edmx:DataServices></edmx:Edmx>";
    private final static String TEXT_SERVICE_DOCUMENT_XML = "<?xml version='1.0' encoding='utf-8'?><service xmlns=\"http://www.w3.org/2007/app\" xml:base=\"http://localhost:80/OData.svc/\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:app=\"http://www.w3.org/2007/app\"><workspace><atom:title>Default</atom:title><collection href=\"TableName1s\"><atom:title>TableName1s</atom:title></collection><collection href=\"TableName2s\"><atom:title>TableName2s</atom:title></collection><collection href=\"TableGroupingTests\"><atom:title>TableGroupingTests</atom:title></collection></workspace></service>";
    private final static String TEXT_SERVICE_DOCUMENT_JSON = "{\n" +
            "\"d\" : {\n" +
            "\"EntitySets\" : [\n" +
            "\"TableName1s\", \"TableName2s\", \"TableGroupingTests\"\n" +
            "]\n" +
            "}\n" +
            "}";
    private final static String TEXT_GET_ENTITIES_XML = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableGroupingTests</title><id>http://localhost:80/OData.svc/TableGroupingTests</id><link rel=\"self\" title=\"TableGroupingTests\" href=\"TableGroupingTests\"/><entry><id>http://localhost:80/OData.svc/TableGroupingTests('1')</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableGroupingTest\" href=\"TableGroupingTests('1')\"/><category term=\"JdbcModel.TableGroupingTest\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>1</d:Id><d:Ctr m:type=\"Edm.Int32\">5</d:Ctr><d:RefName>ref name1</d:RefName></m:properties></content></entry><entry><id>http://localhost:80/OData.svc/TableGroupingTests('2')</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableGroupingTest\" href=\"TableGroupingTests('2')\"/><category term=\"JdbcModel.TableGroupingTest\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>2</d:Id><d:Ctr m:type=\"Edm.Int32\">5</d:Ctr><d:RefName>ref name2</d:RefName></m:properties></content></entry><entry><id>http://localhost:80/OData.svc/TableGroupingTests('3')</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableGroupingTest\" href=\"TableGroupingTests('3')\"/><category term=\"JdbcModel.TableGroupingTest\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>3</d:Id><d:Ctr m:type=\"Edm.Int32\">0</d:Ctr><d:RefName>ref name3</d:RefName></m:properties></content></entry></feed>";
    private final static String TEXT_GET_ENTITIES_JSON = "{\n" +
            "\"d\" : {\n" +
            "\"results\" : [\n" +
            "{\n" +
            "\"__metadata\" : {\n" +
            "\"uri\" : \"http://localhost:80/OData.svc/TableGroupingTests('1')\", \"type\" : \"JdbcModel.TableGroupingTest\"\n" +
            "}, \"Id\" : \"1\", \"Ctr\" : 5, \"RefName\" : \"ref name1\"\n" +
            "}, {\n" +
            "\"__metadata\" : {\n" +
            "\"uri\" : \"http://localhost:80/OData.svc/TableGroupingTests('2')\", \"type\" : \"JdbcModel.TableGroupingTest\"\n" +
            "}, \"Id\" : \"2\", \"Ctr\" : 5, \"RefName\" : \"ref name2\"\n" +
            "}, {\n" +
            "\"__metadata\" : {\n" +
            "\"uri\" : \"http://localhost:80/OData.svc/TableGroupingTests('3')\", \"type\" : \"JdbcModel.TableGroupingTest\"\n" +
            "}, \"Id\" : \"3\", \"Ctr\" : 0, \"RefName\" : \"ref name3\"\n" +
            "}\n" +
            "]\n" +
            "}\n" +
            "}";
    private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_1_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableGroupingTests</title><id>http://localhost:80/OData.svc/TableGroupingTests</id><link rel=\"self\" title=\"TableGroupingTests\" href=\"TableGroupingTests\"/><m:count>1</m:count><entry><id>http://localhost:80/OData.svc/TableGroupingTests('1')</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableGroupingTest\" href=\"TableGroupingTests('1')\"/><category term=\"JdbcModel.TableGroupingTest\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>1</d:Id><d:Ctr m:type=\"Edm.Int32\">5</d:Ctr><d:RefName>ref name1</d:RefName></m:properties></content></entry></feed>";
    //private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_2_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableName1s</title><id>http://localhost:80/OData.svc/TableName1s</id><link rel=\"self\" title=\"TableName1s\" href=\"TableName1s\"/><m:count>2</m:count><entry><id>http://localhost:80/OData.svc/TableName1s(1)</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableName1\" href=\"TableName1s(1)\"/><category term=\"JdbcModel.TableName1\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id m:type=\"Edm.Int32\">1</d:Id><d:StrCol>value1</d:StrCol><d:NumCol m:type=\"Edm.Int32\">1</d:NumCol></m:properties></content></entry></feed>";
    private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_3_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableGroupingTests</title><id>http://localhost:80/OData.svc/TableGroupingTests</id><link rel=\"self\" title=\"TableGroupingTests\" href=\"TableGroupingTests\"/><m:count>3</m:count><entry><id>http://localhost:80/OData.svc/TableGroupingTests('1')</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableGroupingTest\" href=\"TableGroupingTests('1')\"/><category term=\"JdbcModel.TableGroupingTest\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>1</d:Id><d:Ctr m:type=\"Edm.Int32\">5</d:Ctr><d:RefName>ref name1</d:RefName></m:properties></content></entry></feed>";
    private final static String TEXT_GET_ENTITIES_WITH_TOP_AND_INLINECOUNT_4_COUNT = "<?xml version='1.0' encoding='utf-8'?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" xml:base=\"http://localhost:80/OData.svc/\"><title type=\"text\">TableGroupingTests</title><id>http://localhost:80/OData.svc/TableGroupingTests</id><link rel=\"self\" title=\"TableGroupingTests\" href=\"TableGroupingTests\"/><m:count>4</m:count><entry><id>http://localhost:80/OData.svc/TableGroupingTests('1')</id><title type=\"text\"/><author><name/></author><link rel=\"edit\" title=\"TableGroupingTest\" href=\"TableGroupingTests('1')\"/><category term=\"JdbcModel.TableGroupingTest\" scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\"/><content type=\"application/xml\"><m:properties><d:Id>1</d:Id><d:Ctr m:type=\"Edm.Int32\">5</d:Ctr><d:RefName>ref name1</d:RefName></m:properties></content></entry></feed>";
    private final static String TEXT_BAD_REQUEST_INVALID_FILTER = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">There was an invalid field in the $filter parameter</message></error>";
    private final static String TEXT_BAD_REQUEST_INVALID_SELECT = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">There was an invalid field in the $select parameter</message></error>";

    private final static String CUSTOM_ENTITIES = "<l7:CustomEntities xmlns:l7=\"http://ns.l7tech.com/2012/04/api-management\">\t\n" +
            "<l7:Entity>\n" +
            "\t<l7:tableName>Table_Grouping_Test</l7:tableName>\n" +
            "\t<l7:customQuery>SELECT t1.id, t1.ref_name, COUNT(t2.id) AS ctr FROM " + TABLE_NAME2 + " t1 LEFT JOIN " + TABLE_NAME + " t2 ON t1.id=t2.ref_id </l7:customQuery>\n" +
            "\t<l7:columns>\n" +
            "\t\t<l7:Column>\n" +
            "\t\t\t<l7:columnName>id</l7:columnName>\n" +
            "\t\t\t<l7:columnType>1</l7:columnType>\n" +
            "\t\t\t<l7:columnTypeName>INT</l7:columnTypeName>\n" +
            "\t\t\t<l7:columnSize>36</l7:columnSize>\n" +
            "\t\t\t<l7:isNullable>false</l7:isNullable>\n" +
            "\t\t\t<l7:ordinalPosition>1</l7:ordinalPosition>\n" +
            "\t\t</l7:Column>\n" +
            "\t\t<l7:Column>\n" +
            "\t\t\t<l7:columnName>ctr</l7:columnName>\n" +
            "\t\t\t<l7:columnType>12</l7:columnType>\n" +
            "\t\t\t<l7:columnTypeName>INT</l7:columnTypeName>\n" +
            "\t\t\t<l7:columnSize>10</l7:columnSize>\n" +
            "\t\t\t<l7:isNullable>false</l7:isNullable>\n" +
            "\t\t\t<l7:ordinalPosition>2</l7:ordinalPosition>\n" +
            "\t\t</l7:Column>\n" +
            "\t\t<l7:Column>\n" +
            "\t\t\t<l7:columnName>ref_name</l7:columnName>\n" +
            "\t\t\t<l7:columnType>12</l7:columnType>\n" +
            "\t\t\t<l7:columnTypeName>VARCHAR</l7:columnTypeName>\n" +
            "\t\t\t<l7:columnSize>36</l7:columnSize>\n" +
            "\t\t\t<l7:isNullable>false</l7:isNullable>\n" +
            "\t\t\t<l7:ordinalPosition>2</l7:ordinalPosition>\n" +
            "\t\t</l7:Column>\t\t\n" +
            "\t</l7:columns>\n" +
            "\t<l7:primaryKeys>\n" +
            "\t\t<l7:PrimaryKey>\n" +
            "\t\t\t<l7:columnName>id</l7:columnName>\n" +
            "\t\t\t<l7:sequenceNumber>1</l7:sequenceNumber>\n" +
            "\t\t\t<l7:primaryKeyName>PRIMARY</l7:primaryKeyName>\n" +
            "\t\t</l7:PrimaryKey>\n" +
            "\t</l7:primaryKeys>\n" +
            "</l7:Entity>\n" +
            "</l7:CustomEntities>";


}