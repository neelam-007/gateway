package com.l7tech.external.assertions.odata.server;

import com.l7tech.common.io.XmlUtil;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author rraquepo, 8/27/13
 */
public class ServerODataAssertionTest {
    private PolicyEnforcementContext peCtx;

    private ODataProducerAssertion assertion;

    @Mock
    private JdbcConnectionManager jdbcConnectionManager;
    @Mock
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;
    @Mock
    private JdbcConnection jdbcConnection;
    @Mock
    private Connection conn;
    @Mock
    private AbstractDataSource dataSource;
    @Mock
    private DatabaseMetaData databaseMetaData;
    @Mock
    private ResultSet schema;
    @Mock
    private ResultSet tables;
    @Mock
    private ResultSet columns;
    @Mock
    private ResultSet primaryKeys;

    private ServerPolicyFactory serverPolicyFactory;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    final String connectionName = "mockDb";

    Message request;

    Message response;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        //create assertion
        assertion = new ODataProducerAssertion();
        assertion.setConnectionName(connectionName);

        when(jdbcConnectionManager.getJdbcConnection(Matchers.eq(connectionName))).thenReturn(jdbcConnection);
        when(jdbcConnectionPoolManager.getDataSource(Matchers.eq(connectionName))).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getSchemas()).thenReturn(schema);
        when(schema.next()).thenReturn(false);
        when(tables.getString(1)).thenReturn("schema_Name1");
        when(tables.getString(3)).thenReturn("table_Name1");
        when(tables.getString(Matchers.eq("TABLE_SCHEM"))).thenReturn("schema_Name1");
        when(tables.getString(Matchers.eq("TABLE_NAME"))).thenReturn("table_Name1");
        when(tables.getString(Matchers.eq("TABLE_TYPE"))).thenReturn("TABLE");
        when(tables.next()).thenReturn(true).thenReturn(false);//just one table
        when(columns.next()).thenReturn(true).thenReturn(false);//just one column
        when(primaryKeys.next()).thenReturn(true).thenReturn(false);//just one key
        when(columns.getString(Matchers.eq("COLUMN_NAME"))).thenReturn("column_name_1");
        when(columns.getInt(Matchers.eq("DATA_TYPE"))).thenReturn(Types.VARCHAR);
        when(columns.getString(Matchers.eq("TYPE_NAME"))).thenReturn("VARCHAR");
        when(columns.getObject(Matchers.eq("TYPE_NAME"))).thenReturn(new Integer("80"));
        when(columns.getInt(Matchers.eq("ORDINAL_POSITION"))).thenReturn(1);
        when(columns.getInt(Matchers.eq("NULLABLE"))).thenReturn(DatabaseMetaData.columnNullable);
        when(primaryKeys.getString(Matchers.eq("COLUMN_NAME"))).thenReturn("column_name_1");
        when(primaryKeys.getInt(Matchers.eq("KEY_SEQ"))).thenReturn(1);
        when(primaryKeys.getString(Matchers.eq("PK_NAME"))).thenReturn("pk_column_name_1");
        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");//pretend we are using a MySQL database
        when(databaseMetaData.getTables(anyString(), anyString(), anyString(), any(String[].class))).thenReturn(tables);
        when(databaseMetaData.getColumns(anyString(), anyString(), anyString(), anyString())).thenReturn(columns);
        when(databaseMetaData.getPrimaryKeys(anyString(), anyString(), anyString())).thenReturn(primaryKeys);

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
        assertion.setConnectionName("mockDb");
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
    public void testMetadataWithContextVariables() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_RESOURCE_PATH), "$metadata");
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_HTTP_METHOD), "GET");

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_METADATA_XML, payloadAsString);
    }

    @Test
    public void testGeServiceDocument() throws Exception {
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
    public void testGeServiceDocumentWithContextVariables() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_RESOURCE_PATH), "/");
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_HTTP_METHOD), "GET");

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_SERVICE_DOCUMENT_XML, payloadAsString);
    }

    @Test
    public void testGeServiceDocumentJsonFormat() throws Exception {
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
    public void testGeServiceDocumentJsonFormatWithContextVariables() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_RESOURCE_PATH), "/");
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_HTTP_METHOD), "GET");
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_PARAM_FORMAT), "json");

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.NONE, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_SERVICE_DOCUMENT_JSON, payloadAsString);
    }

    @Test
    public void testBadRequestException() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/TableName1s/blahblah");
        hrequest.setMethod("GET");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_BAD_REQUEST_EXCEPTION, payloadAsString);
    }

//    @Test
//    public void testNotImplementedException() throws Exception {
//        MockServletContext servletContext = new MockServletContext();
//        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
//        hrequest.setRequestURI("/OData.svc/TableName1s/$batch");//we'll use $batch as we did not really implemented this
//        hrequest.setMethod("POST");
//        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
//        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
//
//        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
//        AssertionStatus status = sass.checkRequest(peCtx);
//        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
//        assertEquals(AssertionStatus.FALSIFIED, status);
//        assertNotNull(payloadAsString);
//        assertEquals(TEXT_NOT_IMPLEMENTED_EXCEPTION, payloadAsString);
//    }

    @Test
    public void testBadRequestException_batch() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/$batch");//we'll use $batch as we did not really implemented this
        hrequest.setMethod("POST");
        Message request2 = new Message();//create an empty one
        request2.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request2, response);

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_BAD_BATCH_REQUEST_EXCEPTION, payloadAsString);
    }

    @Test
    public void testBadRequestException_non_multipart_strict_batch() throws Exception {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/OData.svc/$batch");//we'll use $batch as we did not really implemented this
        hrequest.setMethod("POST");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        peCtx.setVariable(getAsVariableName(ODataProducerAssertion.ODATA_ALLOW_ANY_REQUEST_BODY_FOR_BATCH), "false");

        ServerODataProducerAssertion sass = (ServerODataProducerAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        String payloadAsString = IOUtils.toString(sass.getPayload(peCtx.getResponse()), "UTF-8");
        assertEquals(AssertionStatus.FALSIFIED, status);
        assertNotNull(payloadAsString);
        assertEquals(TEXT_BAD_BATCH_REQUEST_EXCEPTION, payloadAsString);
    }


    protected static String getAsVariableName(String varName) {
        if (varName.startsWith("${")) {
            return varName.substring(2, varName.length() - 1);
        }
        return varName;
    }

    private final static String TEXT_METADATA_XML = "<?xml version='1.0' encoding='utf-8'?><edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices m:DataServiceVersion=\"2.0\" xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema Namespace=\"JdbcModel\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityType Name=\"TableName1\"><Key><PropertyRef Name=\"ColumnName1\"/></Key><Property Name=\"ColumnName1\" Type=\"Edm.String\" Nullable=\"true\"/></EntityType></Schema><Schema Namespace=\"JdbcEntities.SchemaName1\" xmlns=\"http://schemas.microsoft.com/ado/2008/09/edm\"><EntityContainer Name=\"SchemaName1\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"TableName1s\" EntityType=\"JdbcModel.TableName1\"/></EntityContainer></Schema></edmx:DataServices></edmx:Edmx>";
    private final static String TEXT_SERVICE_DOCUMENT_XML = "<?xml version='1.0' encoding='utf-8'?><service xmlns=\"http://www.w3.org/2007/app\" xml:base=\"http://localhost:80/OData.svc/\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:app=\"http://www.w3.org/2007/app\"><workspace><atom:title>Default</atom:title><collection href=\"TableName1s\"><atom:title>TableName1s</atom:title></collection></workspace></service>";
    private final static String TEXT_SERVICE_DOCUMENT_JSON = "{\n" +
            "\"d\" : {\n" +
            "\"EntitySets\" : [\n" +
            "\"TableName1s\"\n" +
            "]\n" +
            "}\n" +
            "}";
    private final static String TEXT_BAD_REQUEST_EXCEPTION = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">Bad Request</message></error>";
    private final static String TEXT_BAD_BATCH_REQUEST_EXCEPTION = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>BadRequestException</code><message lang=\"en-US\">Expecting a valid multipart request and payload</message></error>";
    private final static String TEXT_NOT_IMPLEMENTED_EXCEPTION = "<?xml version='1.0' encoding='utf-8'?><error xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><code>NotImplementedException</code><message lang=\"en-US\">Not Implemented</message></error>";


}
