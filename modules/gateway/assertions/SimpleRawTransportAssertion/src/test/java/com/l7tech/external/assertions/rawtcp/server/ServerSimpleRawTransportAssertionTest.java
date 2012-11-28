package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.common.io.IOExceptionThrowingInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * Test the SimpleRawTransportAssertion.
 */
public class ServerSimpleRawTransportAssertionTest {

    @BeforeClass
    public static void setup() {
        AssertionRegistry.installEnhancedMetadataDefaults();
        SyspropUtil.setProperty( "com.l7tech.logging.debug", "true" );
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.logging.debug"
        );
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals( "assertion:SimpleRawTransport", new SimpleRawTransportAssertion().getFeatureSetName() );
    }

    @Test
    public void testSimpleRouting() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText("0");

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    @BugNumber(9106)
    public void testResponseSizeLimitSuccess() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText("20");

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    @BugNumber(9106)
    public void testResponseSizeLimitFailure() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText("11");
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        AssertionStatus result = sass.checkRequest(context("<blah/>"));
        assertEquals( AssertionStatus.FAILED, result );
    }

    @Test
    @BugNumber(6407)
    public void testResponseSizeLimitSuccessFromVariable() throws Exception {
        testWithResponseSizeLimitVariable( 11, AssertionStatus.FAILED );
        testWithResponseSizeLimitVariable( 12, AssertionStatus.NONE );
    }

    private void testWithResponseSizeLimitVariable( final int sizeLimit,
                                                    final AssertionStatus expectedStatus ) throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText("${limit}");
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        final PolicyEnforcementContext policyEnforcementContext = context( "<blah/>" );
        policyEnforcementContext.setVariable( "limit", sizeLimit );
        AssertionStatus result = sass.checkRequest( policyEnforcementContext );
        assertEquals(expectedStatus, result);

    }

    @Test
    public void testRoutingResponseIOException() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        
        @SuppressWarnings({"ThrowableInstanceNeverThrown"})
        final StubSocketImpl sockimp = new StubSocketImpl(new IOExceptionThrowingInputStream(new IOException("OUCH")), new NullOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));

        AssertionStatus result = sass.checkRequest(context("<blah/>"));
        assertEquals(AssertionStatus.FAILED, result);
    }

    @Test
    public void testVariableContentType() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml; charset=iso-8859-1";
        PolicyEnforcementContext context = context(requestStr);

        context.setVariable("customResponseContentType", respContentType);
        ass.setResponseContentType("${customResponseContentType}");

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    public void testVariableContentTypeWithVarNotFound() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setResponseContentType("${customResponseContentType}");

        PolicyEnforcementContext context = context("<blah/>");
        context.setVariable("customContentType", "text/xml; charset=ISO-8859-1"); // intentionally use incorrect/mismatching variable name
        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        sass.socketFactory = new StubSocketFactory(new StubSocket(new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream())));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    @Test
    public void testVariableTargetHost() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("${varTargetHost}");
        ass.setTargetPort("2225");

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("varTargetHost", "127.0.0.6");
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.6:2225", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    public void testVariableTargetHostWithVarNotFound() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("${varTargetHost}");
        ass.setTargetPort("2225");

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("varTargetHots", "127.0.0.6"); // intentional typo
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp1 = new StubSocketImpl(new ByteArrayInputStream(responseStr.getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp1));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    @Test
    public void testRoutingFromRequestMessageVariable() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setRequestTarget(new MessageTargetableSupport("requestVar"));

        final String responseStr = "<response/>";
        final String requestStr = "<requestVarContent/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context("<defaultRequestContent/>");
        context.setVariable("requestVar", new Message(XmlUtil.stringAsDocument(requestStr)));
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    @BugNumber(9105)
    public void testRoutingToResponseMessageVariable() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setResponseTarget(new MessageTargetableSupport("responseVar"));

        final String responseStr = "<response/>";
        final String requestStr = "<defaultRequestContent/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        final Message responseMess = (Message) context.getVariable("responseVar");
        assertNotNull(responseMess);
        assertEquals(respContentType, responseMess.getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());

        assertEquals(responseStr, new String(IOUtils.slurpStream(responseMess.getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    public void testRoutingToResponseMessageVariableThatAlreadyExists() throws Exception {
        doTestRoutingToResponseMessageVariableThatAlreadyExists( new Message() );
    }

    @Test
    public void testRoutingToResponseMessageVariableThatAlreadyExistsAsNonMessage() throws Exception {
        doTestRoutingToResponseMessageVariableThatAlreadyExists( "" );
    }

    public void doTestRoutingToResponseMessageVariableThatAlreadyExists( final Object value ) throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setResponseTarget(new MessageTargetableSupport("responseVar"));

        final String responseStr = "<response/>";
        final String requestStr = "<defaultRequestContent/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("responseVar", value);
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        final Message responseMess = (Message) context.getVariable("responseVar");
        assertNotNull(responseMess);
        assertEquals(respContentType, responseMess.getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(responseMess.getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }


    @Test
    public void testRoutingFromRequestStringVariable() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setRequestTarget(new MessageTargetableSupport("requestVar"));

        final String responseStr = "<response/>";
        final String requestStr = "<requestVarContent/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context("<defaultRequestContent/>");
        context.setVariable("requestVar", requestStr);
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.3:2323", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    public void testRoutingFromRequestNoSuchVariable() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setRequestTarget(new MessageTargetableSupport("requestVar"));

        PolicyEnforcementContext context = context("<defaultRequest/>");
        context.setVariable("requetsVar", "<requestVarContent/>"); // intentional typo in var name
        ass.setResponseContentType("text/xml");

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp1 = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp1));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    private static PolicyEnforcementContext context(String requestStr) {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(XmlUtil.stringAsDocument(requestStr)), new Message(), true);
    }

    private static StubSocketImpl simulateRequest(SimpleRawTransportAssertion ass, PolicyEnforcementContext context, ByteArrayOutputStream outputCapture, String responseStr) throws PolicyAssertionException, IOException {
        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream(responseStr.getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        return sockimp;
    }

    @Test
    @BugNumber(9621)
    public void testUnknownHostReasonCode() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText( "11" );
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new IOExceptionThrowingInputStream(new UnknownHostException("OUCH")), new NullOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals( AssertionStatus.FAILED, result );
        assertEquals(ServerSimpleRawTransportAssertion.HOST_NOT_FOUND, context.getVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE));
    }

    @Test
    @BugNumber(9621)
    public void testSocketTimeoutReasonCode() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText( "11" );
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new IOExceptionThrowingInputStream(new SocketTimeoutException("OUCH")), new NullOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals( AssertionStatus.FAILED, result );
        assertEquals(ServerSimpleRawTransportAssertion.SOCKET_TIMEOUT, context.getVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE));
    }

    @Test
    @BugNumber(9621)
    public void testConnectionRefuseReasonCode() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText( "11" );
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new IOExceptionThrowingInputStream(new ConnectException("OUCH")), new NullOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals( AssertionStatus.FAILED, result );
        assertEquals(ServerSimpleRawTransportAssertion.CONNECTION_REFUSE, context.getVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE));
    }

    @Test
    @BugNumber(9621)
    public void testUndefinedReasonCode() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText( "11" );
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new IOExceptionThrowingInputStream(new IOException("OUCH")), new NullOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals( AssertionStatus.FAILED, result );
        assertEquals(ServerSimpleRawTransportAssertion.UNDEFINED, context.getVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE));
    }

    @Test
    @BugNumber(9621)
    public void testResponseSizeLimitFailureReasonCode() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("2323");
        ass.setMaxResponseBytesText("11");
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, result);
        assertEquals(ServerSimpleRawTransportAssertion.DATA_SIZE_LIMIT_EXCEED, context.getVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE));
    }

    @Test
    @BugNumber(9621)
    public void testVariableTargetPort() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("${varTargetHost}");
        ass.setTargetPort("${varTargetPort}");

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("varTargetHost", "127.0.0.6");
        context.setVariable("varTargetPort", 2225);
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        final StubSocketImpl sockimp = simulateRequest(ass, context, outputCapture, responseStr);

        assertEquals(respContentType, context.getResponse().getMimeKnob().getOuterContentType().getFullValue());
        assertEquals(requestStr, outputCapture.toString());
        assertEquals(responseStr, new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()), Charsets.UTF8));
        assertEquals("/127.0.0.6:2225", sockimp.sawConnectHost);
        assertTrue(sockimp.sawShutOut);

        context.close();
        assertTrue(sockimp.closed);
    }

    @Test
    @BugNumber(9621)
    public void testVariableTargetPortFailure() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("${abcd}");
        ass.setMaxResponseBytesText( "11" );
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals( AssertionStatus.SERVER_ERROR, result );
    }

    @Test
    @BugNumber(9621)
    public void testVariableTargetPortSuccess() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort("${abcd}");
        ass.setMaxResponseBytesText( "20" );
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        PolicyEnforcementContext context = context("<blah/>");
        context.setVariable("abcd", "2323");
        AssertionStatus result = sass.checkRequest(context);
        assertEquals( AssertionStatus.NONE, result );
        assertEquals(ServerSimpleRawTransportAssertion.SUCCESS, context.getVariable(SimpleRawTransportAssertion.VAR_RAW_TCP_REASON_CODE));
    }
}
