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
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the SimpleRawTransportAssertion.
 */
public class ServerSimpleRawTransportAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerSimpleRawTransportAssertionTest.class.getName());

    @BeforeClass
    public static void setup() {
        AssertionRegistry.installEnhancedMetadataDefaults();
        System.setProperty("com.l7tech.logging.debug", "true");
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("assertion:SimpleRawTransport", new SimpleRawTransportAssertion().getFeatureSetName());
    }

    @Test
    public void testSimpleRouting() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
        ass.setMaxResponseBytes(0);

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
    public void testReponseSizeLimitSuccess() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
        ass.setMaxResponseBytes(20);

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
    public void testReponseSizeLimitFailure() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
        ass.setMaxResponseBytes(10);
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream());
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));
        AssertionStatus result = sass.checkRequest(context("<blah/>"));
        assertEquals(AssertionStatus.FAILED, result);
    }

    @Test
    public void testRoutingResponseIOException() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
        ass.setResponseContentType("text/xml");

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        
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
        ass.setTargetPort(2323);

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
        ass.setTargetPort(2323);
        ass.setResponseContentType("${customResponseContentType}");

        PolicyEnforcementContext context = context("<blah/>");
        context.setVariable("customContentType", "text/xml; charset=ISO-8859-1"); // intentionally use incorrect/mismatching variable name
        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        sass.socketFactory = new StubSocketFactory(new StubSocket(new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), new ByteArrayOutputStream())));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    @Test
    public void testVariableTargetHost() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("${varTargetHost}");
        ass.setTargetPort(2225);

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
        ass.setTargetPort(2225);

        final String responseStr = "<response/>";
        final String requestStr = "<blah/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("varTargetHots", "127.0.0.6"); // intentional typo
        ass.setResponseContentType(respContentType);

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        final StubSocketImpl sockimp1 = new StubSocketImpl(new ByteArrayInputStream(responseStr.getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp1));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    @Test
    public void testRoutingFromRequestMessageVariable() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
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
        ass.setTargetPort(2323);
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
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
        ass.setResponseTarget(new MessageTargetableSupport("responseVar"));

        final String responseStr = "<response/>";
        final String requestStr = "<defaultRequestContent/>";
        final String respContentType = "text/xml";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("responseVar", new Message());
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
    public void testRoutingToResponseMessageVariableThatAlreadyExistsAsNonMessage() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
        ass.setResponseTarget(new MessageTargetableSupport("responseVar"));

        final String requestStr = "<defaultRequestContent/>";
        PolicyEnforcementContext context = context(requestStr);
        context.setVariable("responseVar", "heyas");
        ass.setResponseContentType("text/xml");

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        final StubSocketImpl sockimp1 = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp1));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    @Test
    public void testRoutingFromRequestStringVariable() throws Exception {
        SimpleRawTransportAssertion ass = new SimpleRawTransportAssertion();
        ass.setTargetHost("127.0.0.3");
        ass.setTargetPort(2323);
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
        ass.setTargetPort(2323);
        ass.setRequestTarget(new MessageTargetableSupport("requestVar"));

        PolicyEnforcementContext context = context("<defaultRequest/>");
        context.setVariable("requetsVar", "<requestVarContent/>"); // intentional typo in var name
        ass.setResponseContentType("text/xml");

        final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();

        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        final StubSocketImpl sockimp1 = new StubSocketImpl(new ByteArrayInputStream("<response/>".getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp1));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
    }

    private static PolicyEnforcementContext context(String requestStr) {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(XmlUtil.stringAsDocument(requestStr)), new Message(), true);
    }

    private static StubSocketImpl simulateRequest(SimpleRawTransportAssertion ass, PolicyEnforcementContext context, ByteArrayOutputStream outputCapture, String responseStr) throws PolicyAssertionException, IOException {
        ServerSimpleRawTransportAssertion sass = new ServerSimpleRawTransportAssertion(ass, null, null);
        final StubSocketImpl sockimp = new StubSocketImpl(new ByteArrayInputStream(responseStr.getBytes()), outputCapture);
        sass.socketFactory = new StubSocketFactory(new StubSocket(sockimp));

        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        return sockimp;
    }
}
