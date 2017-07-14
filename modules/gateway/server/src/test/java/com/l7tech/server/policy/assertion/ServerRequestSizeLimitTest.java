package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author jbufu
 */
public class ServerRequestSizeLimitTest {

    private static final String POLICY_XML_LARGE_LIMI = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:RequestSizeLimit>\n" +
            "                <L7p:Limit longValue=\"4896768\"/>\n" +
            "            </L7p:RequestSizeLimit>\n" +
            "    </wsp:Policy>\n";

    private static final String POLICY_XML_TARGET_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:RequestSizeLimit>\n" +
            "                <L7p:Target target=\"RESPONSE\"/>\n" +
            "            </L7p:RequestSizeLimit>\n" +
            "    </wsp:Policy>\n";

    private static final String VARIABLE_NAME_VAR = "var";
    private static final String FILE_NAME_LARGE_MESSAGE = "largeMessage.xml";
    private static final String XML_STRING_FOO = "<foo/>";
    private static final String SIZE_LIMIT_SMALL = "1";
    private static final String SIZE_LIMIT_LARGE = "4782";
    private static final String FAILURE_REQUEST_SIZE_LIMIT = "Expected request size limit " + SIZE_LIMIT_LARGE
            + " kbytes, got '";
    private static final String FAILURE_REQUEST_MESSAGE_TARGET = "Expected response message target, got '";
    public static final String EXCEPTION_MESSAGE_NON_SIZE_RELATED = "Non-size related exception";

    @Test
    public void testCompatibilityBug5044Format() throws Exception {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);

        final WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(RequestSizeLimit.class);

        final RequestSizeLimit assertion = (RequestSizeLimit) wspReader.parseStrictly(
                POLICY_XML_LARGE_LIMI,
                WspReader.INCLUDE_DISABLED);
        assertTrue(FAILURE_REQUEST_SIZE_LIMIT + assertion.getLimit(),
                assertion.getLimit().equals(SIZE_LIMIT_LARGE));
    }


    @Test
    public void testIsMessageTargetable() throws Exception {
        final AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);

        final WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(RequestSizeLimit.class);

        final RequestSizeLimit assertion = (RequestSizeLimit) wspReader.parseStrictly(
                POLICY_XML_TARGET_RESPONSE,
                WspReader.INCLUDE_DISABLED);
        assertTrue(FAILURE_REQUEST_MESSAGE_TARGET + assertion.getTarget(),
                assertion.getTarget().equals(TargetMessageType.RESPONSE));
    }

    @Test
    public void testRequestSizeLimit() throws Exception {
        final RequestSizeLimit ass = new RequestSizeLimit();
        ass.setLimit(SIZE_LIMIT_SMALL);
        ass.setTarget(TargetMessageType.REQUEST);
        
        final ServerRequestSizeLimit serverAss = new ServerRequestSizeLimit(ass);
        AssertionStatus result;

        // small message
        final Message smallRequest = new Message(XmlUtil.stringAsDocument(XML_STRING_FOO));
        result = serverAss.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(smallRequest,null));
        assertEquals(AssertionStatus.NONE, result);

        //large message > 1KB
        final Message bigRequest = new Message(XmlUtil.parse(getClass().getResourceAsStream(FILE_NAME_LARGE_MESSAGE)));
        result = serverAss.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(bigRequest,null));
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    @Test
    public void testVariableSizeLimit() throws Exception {
        final RequestSizeLimit ass = new RequestSizeLimit();
        ass.setLimit(SIZE_LIMIT_SMALL);
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable(VARIABLE_NAME_VAR);

        final ServerRequestSizeLimit serverAss = new ServerRequestSizeLimit(ass);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(null,null);

        AssertionStatus result;

        // small message
        final Message smallRequest = new Message(XmlUtil.stringAsDocument(XML_STRING_FOO));
        context.setVariable(VARIABLE_NAME_VAR,smallRequest);
        result = serverAss.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        // large message > 1KB
        final Message bigRequest = new Message(XmlUtil.parse(getClass().getResourceAsStream(FILE_NAME_LARGE_MESSAGE)));
        context.setVariable(VARIABLE_NAME_VAR, bigRequest);
        result = serverAss.checkRequest(context);
        assertEquals(AssertionStatus.FALSIFIED, result);
    }

    @Test
    public void testNonSizeRelatedIOException() throws IOException, PolicyAssertionException, SAXException {
        final RequestSizeLimit clientAssertion = new RequestSizeLimit();
        clientAssertion.setLimit(SIZE_LIMIT_SMALL);
        clientAssertion.setTarget(TargetMessageType.REQUEST);

        final MimeKnob mimeKnob = mock(MimeKnob.class);
        final IOException ioException = new IOException(EXCEPTION_MESSAGE_NON_SIZE_RELATED);
        doThrow(ioException).when(mimeKnob).setContentLengthLimit(anyLong());
        final Message request = new Message();
        Whitebox.setInternalState(request, "mimeKnob", mimeKnob);

        final PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                null);

        final ServerRequestSizeLimit serverAssertion = new ServerRequestSizeLimit(clientAssertion);

        @SuppressWarnings("unchecked")
        final AtomicReference<Audit> auditReference = (AtomicReference<Audit>) Whitebox.getInternalState(
                serverAssertion, "auditReference");
        final Audit audit = mock(Audit.class);
        auditReference.set(audit);

        final AssertionStatus result = serverAssertion.checkRequest(pec);

        assertEquals(result, AssertionStatus.SERVER_ERROR);
        verify(audit, times(1)).logAndAudit(
                AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO,
                new String[] {EXCEPTION_MESSAGE_NON_SIZE_RELATED},
                ioException);
    }

}

