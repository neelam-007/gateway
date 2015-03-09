package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageBufferingAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;

public class ServerMessageBufferingAssertionJavaTest {

    private TestAudit audit;
    private MessageBufferingAssertion assertion;
    private ServerMessageBufferingAssertion serverAssertion;
    private MimeKnob requestMimeKnob;
    private PolicyEnforcementContext pec;

    @Before
    public void setUp() throws Exception {
        audit = new TestAudit();

        assertion = new MessageBufferingAssertion();
        serverAssertion = new ServerMessageBufferingAssertion(assertion, audit.factory());

        Message request = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT,
                new ByteArrayInputStream("blah".getBytes(Charsets.UTF8)));

        requestMimeKnob = request.getMimeKnob();

        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
    }

    /**
     * fail if message is not initialized
     */
    @Test
    public void testDoCheckRequest_ResponseMessageNotInitialized_MessageNotInitializedAuditedAndAssertionError() throws Exception {
        assertion.setTarget(TargetMessageType.RESPONSE);

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(pec));

        assertTrue(audit.isAuditPresent(AssertionMessages.MESSAGE_NOT_INITIALIZED));
    }

    /**
     * buffer target message immediately when so configured
     */
    @Test
    public void testDoCheckRequest_AlwaysBufferSpecified_MessageBuffered() throws Exception {
        setBuffer(true);

        assertFalse(requestMimeKnob.getFirstPart().isBodyStashed());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertFalse(requestMimeKnob.isBufferingDisallowed());
        assertTrue(requestMimeKnob.getFirstPart().isBodyStashed());
        assertTrue(requestMimeKnob.getFirstPart().isBodyRead());
        assertTrue(requestMimeKnob.getFirstPart().isBodyAvailable());
    }

    /**
     * fail to buffer target message if it is already gone
     */
    @Test
    public void testDoCheckRequest_AlwaysBufferSpecifiedButMessageDestroyed_NoSuchPartAuditedAndAssertionError() throws Exception {
        setBuffer(true);

        IOUtils.copyStream(requestMimeKnob.getEntireMessageBodyAsInputStream(true), new NullOutputStream());

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(pec));

        assertFalse(requestMimeKnob.isBufferingDisallowed());
        assertTrue(requestMimeKnob.getFirstPart().isBodyRead());
        assertFalse(requestMimeKnob.getFirstPart().isBodyStashed());
        assertFalse(requestMimeKnob.getFirstPart().isBodyAvailable());
        assertTrue(audit.isAuditPresent(AssertionMessages.NO_SUCH_PART));
    }

    /**
     * disallow buffering when so configured
     */
    @Test
    public void testDoCheckRequest_NeverBufferSpecified_MessageNotBuffered() throws Exception {
        setBuffer(false);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertTrue(requestMimeKnob.isBufferingDisallowed());
        assertFalse(requestMimeKnob.getFirstPart().isBodyRead());
        assertFalse(requestMimeKnob.getFirstPart().isBodyStashed());
        assertTrue(requestMimeKnob.getFirstPart().isBodyAvailable());
    }

    /**
     * fail to disallow buffering if message already buffered
     */
    @Test
    public void testDoCheckRequest_NeverBufferSpecifiedButMessageAlreadyBuffered_MessageAlreadyBufferedAuditedAndAssertionFails() throws Exception {
        setBuffer(false);

        requestMimeKnob.getContentLength();

        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(pec));

        assertTrue(requestMimeKnob.isBufferingDisallowed());
        assertTrue(requestMimeKnob.getFirstPart().isBodyRead());
        assertTrue(requestMimeKnob.getFirstPart().isBodyStashed());
        assertTrue(requestMimeKnob.getFirstPart().isBodyAvailable());
        assertTrue(audit.isAuditPresent(AssertionMessages.MESSAGE_ALREADY_BUFFERED));
    }

    private void setBuffer(boolean buffer) {
        assertion.setAlwaysBuffer(buffer);
        assertion.setNeverBuffer(!buffer);
    }
}