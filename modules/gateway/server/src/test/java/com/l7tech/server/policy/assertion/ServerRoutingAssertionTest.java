package com.l7tech.server.policy.assertion;

import com.l7tech.common.TestDocuments;
import static com.l7tech.common.TestDocuments.PLACEORDER_CLEARTEXT;
import static com.l7tech.common.TestDocuments.getTestDocument;
import com.l7tech.common.io.RandomInputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import static com.l7tech.policy.assertion.RoutingAssertion.*;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import static com.l7tech.server.policy.assertion.MutateOption.LEAVE_AS_SOAP;
import static com.l7tech.server.policy.assertion.MutateOption.MUTATE_INTO_NON_SOAP;
import static com.l7tech.server.policy.assertion.ThrowOption.SHOULD_NOT_THROW;
import static com.l7tech.server.policy.assertion.ThrowOption.SHOULD_THROW;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class ServerRoutingAssertionTest {
    private static final String VAL_NOACTOR = SecurityActor.NOACTOR.getValue();
    private static final String VAL_SECURESPAN = SecurityActor.L7ACTOR.getValue();

    @Test
    public void testHandleNonXml() throws Exception {
        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue("application/binary"), new RandomInputStream(7, 2048));
        message.getSecurityKnob();
        assertNotXml(doHandle(SHOULD_NOT_THROW, message, REMOVE_CURRENT_SECURITY_HEADER, null));
    }

    @Test
    public void testHandleNoSecurityKnob() throws Exception {
        Message message = new Message(getTestDocument(PLACEORDER_CLEARTEXT));
        assertNoSec(doHandle(SHOULD_NOT_THROW, message, REMOVE_CURRENT_SECURITY_HEADER, null));
    }

    @Test
    public void testHandleUnknownOption() throws Exception {
        assertOneSec(doHandle(SHOULD_NOT_THROW, 983837, null), VAL_SECURESPAN, true);
    }

    @Test
    public void testHandlePromoteOtherNullOther() throws Exception {
        assertNoSec(doHandle(SHOULD_NOT_THROW, PROMOTE_OTHER_SECURITY_HEADER, null));
    }

    @Test
    public void testHandlePromoteOtherTwoSoapHeaders() throws Exception {
        Message message = makeProcessedMessage();
        Document doc = doc(message);
        Element header = SoapUtil.getHeaderElement(doc);
        Element body = SoapUtil.getBodyElement(doc);
        Node clonedHeader = header.cloneNode(true);
        header.getParentNode().insertBefore(clonedHeader, body);
        doHandle(SHOULD_THROW, message, PROMOTE_OTHER_SECURITY_HEADER, "alfred");

        // Have to remove mutant second header to continue examining message
        clonedHeader.getParentNode().removeChild(clonedHeader);

        assertOneSec(message, VAL_SECURESPAN, true);
    }

    @Test
    public void testHandlePromoteOtherNonexistentOther() throws Exception {
        Message message = doHandle(SHOULD_NOT_THROW, PROMOTE_OTHER_SECURITY_HEADER, "alfred");
        assertNoSec(message);
    }

    @Test
    public void testHandlePromoteOther() throws Exception {
        Message message = makeProcessedMessage();

        // Add a second Security header with actor="alfred"
        {
            WssDecorator decorator = new WssDecoratorImpl();
            DecorationRequirements dreq = new DecorationRequirements();
            dreq.setSecurityHeaderActor("alfred");
            dreq.setRecipientCertificate(TestDocuments.getEttkServerCertificate());
            dreq.getElementsToEncrypt().add(SoapUtil.getBodyElement(message.getXmlKnob().getDocumentWritable()));
            decorator.decorateMessage(message, dreq);
        }

        assertOneSec(doHandle(SHOULD_NOT_THROW, message, PROMOTE_OTHER_SECURITY_HEADER, "alfred"), null, true); // Promoting a header shouldn't change its mustUnderstand
    }

    public void testHandlePromoteOtherLeavingTwo() throws Exception {
        Message message = makeProcessedMessage();

        // Add a second Security header with actor="alfred"
        {
            WssDecorator decorator = new WssDecoratorImpl();
            DecorationRequirements dreq = new DecorationRequirements();
            dreq.setSecurityHeaderActor("alfred");
            dreq.setRecipientCertificate(TestDocuments.getEttkServerCertificate());
            dreq.getElementsToEncrypt().add(SoapUtil.getBodyElement(message.getXmlKnob().getDocumentWritable()));
            decorator.decorateMessage(message, dreq);
        }

        // Add a third Security header with actor="james"
        {
            WssDecorator decorator = new WssDecoratorImpl();
            DecorationRequirements dreq = new DecorationRequirements();
            dreq.setSecurityHeaderActor("james");
            dreq.setSenderMessageSigningCertificate(TestDocuments.getFimSigningCertificate());
            dreq.setSenderMessageSigningPrivateKey(TestDocuments.getFimSigningPrivateKey());
            dreq.setSignTimestamp();
            decorator.decorateMessage(message, dreq);
        }

        doHandle(SHOULD_NOT_THROW, message, PROMOTE_OTHER_SECURITY_HEADER, "alfred");
        assertSecCount(message, 2);
        assertSec(message, VAL_SECURESPAN, true);
        assertSec(message, "james", false);
    }

    @Test
    public void testHandleSanitize() throws Exception {
        assertOneSec(doHandle(SHOULD_NOT_THROW, LEAVE_CURRENT_SECURITY_HEADER_AS_IS, null), null, false);
    }

    @Test
    public void testHandleSanitizeNoActor() throws Exception {
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderActor(VAL_NOACTOR);
        Message message = makeProcessedMessage(dreq);

        doHandle(SHOULD_NOT_THROW, message, LEAVE_CURRENT_SECURITY_HEADER_AS_IS, null);
        assertOneSec(message, VAL_NOACTOR, false);
    }

    @Test
    public void testHandleSanitizeTwoSoapHeaders() throws Exception {
        Message message = makeProcessedMessage();
        Document doc = doc(message);
        Element header = SoapUtil.getHeaderElement(doc);
        Element body = SoapUtil.getBodyElement(doc);
        Node clonedHeader = header.cloneNode(true);
        header.getParentNode().insertBefore(clonedHeader, body);

        doHandle(SHOULD_THROW, message, LEAVE_CURRENT_SECURITY_HEADER_AS_IS, null);

        // Have to remove mutant second header to continue examining message
        clonedHeader.getParentNode().removeChild(clonedHeader);

        assertOneSec(message, VAL_SECURESPAN, true);

    }

    @Test
    public void testHandleRemove() throws Exception {
        assertNoSec(doHandle(SHOULD_NOT_THROW, REMOVE_CURRENT_SECURITY_HEADER, null));
    }

    @Test
    @BugNumber(6625)
    public void testHandleNonSoapSanitizeCurrent() throws Exception {
        assertNotSoap(doHandle(MUTATE_INTO_NON_SOAP, SHOULD_NOT_THROW, LEAVE_CURRENT_SECURITY_HEADER_AS_IS, null));
    }

    @Test
    @BugNumber(6625)
    public void testHandleNonSoapRemoveCurrent() throws Exception {
        assertNotSoap(doHandle(MUTATE_INTO_NON_SOAP, SHOULD_NOT_THROW, REMOVE_CURRENT_SECURITY_HEADER, null));
    }

    @Test
    @BugNumber(6625)
    public void testHandleNonSoapPromoteOther() throws Exception {
        assertNotSoap(doHandle(MUTATE_INTO_NON_SOAP, SHOULD_THROW, PROMOTE_OTHER_SECURITY_HEADER, "alfred"));
    }

    //
    // Non-test support methods below
    //

    private void assertNotXml(Message message) throws Exception {
        assertFalse("Must not be XML", message.isXml());
    }

    private void assertNotSoap(Message message) throws Exception {
        assertTrue("Must be XML", message.isXml());
        assertFalse("Must not be SOAP", SoapUtil.isSoapMessage(message.getXmlKnob().getDocumentReadOnly()));
    }
    
    private void assertNoSec(Message message) throws Exception {
        Document doc = doc(message);
        List<Element> secs = SoapUtil.getSecurityElements(doc);
        assertEquals("Must be zero Security headers", 0, secs.size());
    }

    private void assertOneSec(Message message, String expectedActor, boolean mustUnderstand) throws Exception {
        List<Element> secs = secs(message);
        assertEquals("Must be exactly one Security header", 1, secs.size());
        assertSec(secs.iterator().next(), expectedActor, mustUnderstand);
    }

    private void assertSec(Message message, String actor, boolean mustUnderstand) throws Exception {
        Element sec = SoapUtil.getSecurityElement(message.getXmlKnob().getDocumentReadOnly(), actor);
        assertNotNull("No Security header found for actor=" + actor, sec);
        assertSec(sec, actor, mustUnderstand);
    }

    private void assertSec(Element sec, String expectedActor, boolean mustUnderstand) throws InvalidDocumentFormatException {
        final String actval = SoapUtil.getActorValue(sec);
        if (expectedActor != null)
            assertEquals("actor value must match expected", expectedActor, actval);

        final String mustval = SoapUtil.getMustUnderstandAttributeValue(sec);
        if (mustUnderstand) {
            assertNotNull("mustUnderstand must be present", mustval);
            assertTrue("mustUnderstand should be asserted", isMustUnderstand(mustval));
        } else {
            assertFalse("mustUnderstand shoud not be asserted", isMustUnderstand(mustval));
        }
    }

    private void assertSecCount(Message message, int expectedNumberOfSecurityHeaders) throws Exception {
        assertEquals("Wrong number of Security headers", expectedNumberOfSecurityHeaders, secs(message).size());
    }

    private List<Element> secs(Message message) throws InvalidDocumentFormatException, SAXException, IOException {
        return SoapUtil.getSecurityElements(doc(message));
    }

    private boolean isMustUnderstand(String mustval) {
        return "1".equals(mustval) || "true".equals(mustval);
    }

    private Document doc(Message message) throws SAXException, IOException {
        return message.getXmlKnob().getDocumentReadOnly();
    }

    private Message doHandle(ThrowOption throwOption, int secHeaderHandlingOption, String otherToPromote) throws Exception {
        return doHandle(LEAVE_AS_SOAP, throwOption, secHeaderHandlingOption, otherToPromote);
    }

    private Message doHandle(MutateOption mutateOption, ThrowOption throwOption, int secHeaderHandlingOption, String otherToPromote) throws Exception {
        Message message = makeProcessedMessage();
        if (MUTATE_INTO_NON_SOAP.equals(mutateOption))
            message.getMimeKnob().getFirstPart().setBodyBytes("<foo/>".getBytes());
        doHandle(throwOption, message, secHeaderHandlingOption, otherToPromote);
        return message;
    }

    private Message doHandle(ThrowOption throwOption, Message message, int secHeaderHandlingOption, String otherToPromote) throws SAXException, IOException {
        TestServerRoutingAssertion sra = makeTestSra();
        try {
            sra.handleProcessedSecurityHeader(message, secHeaderHandlingOption, otherToPromote);
            if (SHOULD_THROW.equals(throwOption))
                fail("Did not catch expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            if (SHOULD_NOT_THROW.equals(throwOption))
                fail("Caught unexpected AssertionStatusException: status=" + e.getAssertionStatus() + ": " + ExceptionUtils.getMessage(e));
        }
        return message;
    }

    // Return a message with a security header that has been processed and saved in the security knob
    private Message makeProcessedMessage() throws Exception {
        return makeProcessedMessage(new DecorationRequirements());
    }

    private Message makeProcessedMessage(DecorationRequirements dreq) throws Exception {
        Message message = new Message(getTestDocument(PLACEORDER_CLEARTEXT));

        // Decorate message
        WssDecorator decorator = new WssDecoratorImpl();
        dreq.setSignTimestamp();
        dreq.setSenderMessageSigningCertificate(TestDocuments.getDotNetServerCertificate());
        dreq.setSenderMessageSigningPrivateKey(TestDocuments.getDotNetServerPrivateKey());
        decorator.decorateMessage(message, dreq);

        // Now process message and store processor results
        WssProcessorImpl processor = new WssProcessorImpl(message);
        ProcessorResult pr = processor.processMessage();
        message.getSecurityKnob().setProcessorResult(pr);
        return message;
    }

    private TestServerRoutingAssertion makeTestSra() {
        return new TestServerRoutingAssertion();
    }

    private static class TestServerRoutingAssertion extends ServerRoutingAssertion<HttpRoutingAssertion> {
        public TestServerRoutingAssertion() {
            super(new HttpRoutingAssertion(), null, null);
        }

        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            return AssertionStatus.NOT_APPLICABLE;
        }
    }
}
