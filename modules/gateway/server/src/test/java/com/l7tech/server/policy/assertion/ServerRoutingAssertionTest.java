package com.l7tech.server.policy.assertion;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.RandomInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.message.Message;
import com.l7tech.message.TcpKnob;
import com.l7tech.message.TcpKnobAdapter;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.KerberosAuthenticationSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.token.http.HttpNegotiateToken;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.KerberosSigningSecurityTokenImpl;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.security.auth.kerberos.KerberosTicket;
import java.io.IOException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static com.l7tech.common.TestDocuments.*;
import static com.l7tech.policy.assertion.RoutingAssertion.*;
import static com.l7tech.server.policy.assertion.MutateOption.LEAVE_AS_SOAP;
import static com.l7tech.server.policy.assertion.MutateOption.MUTATE_INTO_NON_SOAP;
import static com.l7tech.server.policy.assertion.ThrowOption.SHOULD_NOT_THROW;
import static com.l7tech.server.policy.assertion.ThrowOption.SHOULD_THROW;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerRoutingAssertionTest {
    private static final String VAL_NOACTOR = null;
    private static final String VAL_SECURESPAN = SoapConstants.L7_SOAP_ACTOR;
    private static boolean soap11 = true;

    @Test
    public void testIgnoreSecHeader() throws Exception {
        assertOneSec(doHandle(SHOULD_NOT_THROW, IGNORE_SECURITY_HEADER, null), VAL_SECURESPAN, true);
    }

    @Test
    public void testHandleNonXml() throws Exception {
        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.parseValue("application/binary"), new RandomInputStream(7, 2048));
        message.getSecurityKnob();
        assertNotXml(doHandle(SHOULD_NOT_THROW, message, REMOVE_CURRENT_SECURITY_HEADER, null));
    }

    @Test
    public void testHandleNoSecurityKnob() throws Exception {
        Message message = makeMessage();
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
            dreq.addElementToEncrypt(SoapUtil.getBodyElement(message.getXmlKnob().getDocumentWritable()));
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
            dreq.addElementToEncrypt(SoapUtil.getBodyElement(message.getXmlKnob().getDocumentWritable()));
            decorator.decorateMessage(message, dreq);
        }

        // Add a third Security header with actor="james"
        {
            WssDecorator decorator = new WssDecoratorImpl();
            DecorationRequirements dreq = new DecorationRequirements();
            dreq.setSecurityHeaderActor("james");
            dreq.setSenderMessageSigningCertificate(TestDocuments.getFimSigningCertificate());
            dreq.setSenderMessageSigningPrivateKey(TestDocuments.getFimSigningPrivateKey());
            dreq.setSignTimestamp(true);
            decorator.decorateMessage(message, dreq);
        }

        doHandle(SHOULD_NOT_THROW, message, PROMOTE_OTHER_SECURITY_HEADER, "alfred");
        assertSecCount(message, 2);
        assertSec(message, VAL_SECURESPAN, true);
        assertSec(message, "james", false);
    }

    @Test
    public void testHandleSanitize() throws Exception {
        assertOneSec(doHandle(SHOULD_NOT_THROW, CLEANUP_CURRENT_SECURITY_HEADER, null), null, false);
        doSoap12( new Functions.NullaryVoidThrows<Exception>(){ @Override public void call() throws Exception {
            assertOneSec(doHandle(SHOULD_NOT_THROW, CLEANUP_CURRENT_SECURITY_HEADER, null), null, false);
        } } );
    }

    @Test
    public void testHandleSanitizeNoActor() throws Exception {
        DecorationRequirements dreq = new DecorationRequirements();
        dreq.setSecurityHeaderActor(VAL_NOACTOR);
        Message message = makeProcessedMessage(dreq);

        doHandle(SHOULD_NOT_THROW, message, CLEANUP_CURRENT_SECURITY_HEADER, null);
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

        doHandle(SHOULD_THROW, message, CLEANUP_CURRENT_SECURITY_HEADER, null);

        // Have to remove mutant second header to continue examining message
        clonedHeader.getParentNode().removeChild(clonedHeader);

        assertOneSec(message, VAL_SECURESPAN, true);

    }

    @Test
    public void testHandleRemove() throws Exception {
        assertNoSec(doHandle(SHOULD_NOT_THROW, REMOVE_CURRENT_SECURITY_HEADER, null));
        doSoap12( new Functions.NullaryVoidThrows<Exception>(){ @Override public void call() throws Exception {
            assertNoSec(doHandle(SHOULD_NOT_THROW, REMOVE_CURRENT_SECURITY_HEADER, null));
        } } );
    }

    @Test
    @BugNumber(6625)
    public void testHandleNonSoapSanitizeCurrent() throws Exception {
        assertNotSoap(doHandle(MUTATE_INTO_NON_SOAP, SHOULD_NOT_THROW, CLEANUP_CURRENT_SECURITY_HEADER, null));
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

    private void doSoap12( Functions.NullaryVoidThrows<Exception> callme ) throws Exception {
        try {
            soap11 = false;
            callme.call();
        } finally {
            soap11 = true;
        }
    }

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
        else
            assertNull("expected null actor", actval);

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
        Message message = makeMessage();

        // Decorate message
        WssDecorator decorator = new WssDecoratorImpl();
        dreq.setSignTimestamp(true);
        dreq.setSenderMessageSigningCertificate(TestDocuments.getDotNetServerCertificate());
        dreq.setSenderMessageSigningPrivateKey(TestDocuments.getDotNetServerPrivateKey());
        decorator.decorateMessage(message, dreq);

        // Now process message and store processor results
        WssProcessorImpl processor = new WssProcessorImpl(message);
        ProcessorResult pr = processor.processMessage();
        message.getSecurityKnob().setProcessorResult(pr);
        return message;
    }

    private static Message makeMessage() throws IOException, SAXException {
        if ( soap11 )
            return new Message(getTestDocument(PLACEORDER_CLEARTEXT));
        else
            return new Message(getTestDocument(PLACEORDER_CLEARTEXT_S12));
    }

    private TestServerRoutingAssertion makeTestSra() {
        return new TestServerRoutingAssertion();
    }

    private static class TestServerRoutingAssertion extends ServerRoutingAssertion<HttpRoutingAssertion> {
        public TestServerRoutingAssertion() {
            super(new HttpRoutingAssertion(), null);
        }

        @Override
        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            return AssertionStatus.NOT_APPLICABLE;
        }

        @Override
        public void doAttachSamlSenderVouches( RoutingAssertionWithSamlSV assertion, Message message, LoginCredentials svInputCredentials, SignerInfo signerInfo) throws SAXException, IOException, SignatureException, CertificateException, UnrecoverableKeyException {
            super.doAttachSamlSenderVouches( assertion, message, svInputCredentials, signerInfo);
        }
    }

    private String toString(Message mess) throws IOException, SAXException {
        return XmlUtil.nodeToString(mess.getXmlKnob().getDocumentReadOnly());
    }

    private static LoginCredentials makeLoginCredentials() {
        return LoginCredentials.makeLoginCredentials(new HttpBasicToken("joe", "password".toCharArray()), HttpBasic.class);
    }

    private static SignerInfo makeSignerInfo() throws Exception {
        return new SignerInfo(TestDocuments.getDotNetServerPrivateKey(), new X509Certificate[] { TestDocuments.getDotNetServerCertificate() });
    }

    @Test
    public void testAttachSamlNoCreds() throws Exception {
        final Message mess = makeMessage();
        String messXml = toString(mess);
        makeTestSra().doAttachSamlSenderVouches( new HttpRoutingAssertion(), mess, null, null);
        String afterXml = toString(mess);
        assertEquals(messXml, afterXml);
    }

    @Test
    public void testAttachSaml() throws Exception {
        final Message mess = makeMessage();
        mess.attachKnob(TcpKnob.class, new TcpKnobAdapter() {
            @Override
            public String getRemoteAddress() {
                return "127.0.0.1";
            }

            @Override
            public String getRemoteHost() {
                return "127.0.0.1";
            }

            @Override
            public int getLocalPort() {
                return 8080;
            }
        });
        String messXml = toString(mess);
        makeTestSra().doAttachSamlSenderVouches(new HttpRoutingAssertion(), mess, makeLoginCredentials(), makeSignerInfo());
        String afterXml = toString(mess);
        assertFalse(messXml.equals(afterXml));
    }

    @Test
    @BugNumber(12785)
    public void testGetDelegatedKerberosTicket_constrainedDelegation() throws Exception {
        PolicyEnforcementContext mockContext = mock(PolicyEnforcementContext.class);

        Message request = makeMessage();
        when(mockContext.getRequest()).thenReturn(request);
        AuthenticationContext mockAuthenticationContext = mock(AuthenticationContext.class);
        when(mockContext.getAuthenticationContext(request)).thenReturn(mockAuthenticationContext);
        List<LoginCredentials> credentials = new ArrayList<LoginCredentials>();
        byte[] key = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7};
        byte[] bytes = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};
        KerberosServiceTicket kst = new KerberosServiceTicket("client", "service", key, 0, new KerberosGSSAPReqTicket(bytes));
        credentials.add(LoginCredentials.makeLoginCredentials(new KerberosAuthenticationSecurityToken(kst), Assertion.class));
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentials);
;
        TestServerRoutingAssertion partiallyMockedFixture = spy(makeTestSra());

        KerberosServiceTicket ticket = partiallyMockedFixture.getDelegatedKerberosTicket(mockContext, "testhost");

        verify(partiallyMockedFixture, never()).getLastKerberosSigningSecurityToken(any(ProcessorResult.class));
        verify(partiallyMockedFixture, never()).wrapKerberosServiceTicketForDelegation(any(KerberosServiceTicket.class), anyString());
        assertEquals(kst, ticket);

    }

    @Test
    @BugNumber(12785)
    public void testGetDelegatedKerberosTicket_unconstrainedDelegation() throws Exception {
        PolicyEnforcementContext mockContext = mock(PolicyEnforcementContext.class);

        Message request = makeMessage();
        when(mockContext.getRequest()).thenReturn(request);
        AuthenticationContext mockAuthenticationContext = mock(AuthenticationContext.class);
        when(mockContext.getAuthenticationContext(request)).thenReturn(mockAuthenticationContext);
        List<LoginCredentials> credentials = new ArrayList<LoginCredentials>();
        byte[] key = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7};
        byte[] bytes = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};
        KerberosServiceTicket kst = new KerberosServiceTicket("client", "service", key, 0, new KerberosGSSAPReqTicket(bytes));
        KerberosTicket mockKerberosTicket = mock(KerberosTicket.class);

        KerberosServiceTicket delegatedTicket = new KerberosServiceTicket("client", "service", key, 0, new KerberosGSSAPReqTicket(bytes),mockKerberosTicket);
        credentials.add(LoginCredentials.makeLoginCredentials(new HttpNegotiateToken(kst), Assertion.class));
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentials);

        TestServerRoutingAssertion partiallyMockedFixture = spy(makeTestSra());
        doReturn(delegatedTicket).when(partiallyMockedFixture).wrapKerberosServiceTicketForDelegation(kst, "testhost");

        KerberosServiceTicket ticket = partiallyMockedFixture.getDelegatedKerberosTicket(mockContext, "testhost");

        verify(partiallyMockedFixture, times(1)).wrapKerberosServiceTicketForDelegation(kst, "testhost");
        assertEquals(delegatedTicket, ticket);

    }

    @Test
    @BugNumber(12785)
    public void testGetDelegatedKerberosTicket_ticketSupliedViaWSSTokenProfile() throws Exception {
        PolicyEnforcementContext mockContext = mock(PolicyEnforcementContext.class);

        Message request = makeMessage();
        ProcessorResult mockProcessorResult = mock(ProcessorResult.class);
        request.getSecurityKnob().setProcessorResult(mockProcessorResult);
        when(mockContext.getRequest()).thenReturn(request);
        AuthenticationContext mockAuthenticationContext = mock(AuthenticationContext.class);
        when(mockContext.getAuthenticationContext(request)).thenReturn(mockAuthenticationContext);
        List<LoginCredentials> credentials = new ArrayList<LoginCredentials>();
        byte[] key = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7};
        byte[] bytes = {0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};
        byte[] bytes2 = {0x10, 0x10, 0x12, 0x13, 0x14, 0x15, 0x6, 0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};
        KerberosServiceTicket kst = new KerberosServiceTicket("client", "service", key, 0, new KerberosGSSAPReqTicket(bytes));

        final KerberosSigningSecurityTokenImpl something = new KerberosSigningSecurityTokenImpl(kst, "something");
        XmlSecurityToken[] xmlSecurityTokens = { something };
        when(mockProcessorResult.getXmlSecurityTokens()).thenReturn(xmlSecurityTokens);

        KerberosTicket mockKerberosTicket = mock(KerberosTicket.class);
        KerberosServiceTicket delegatedTicket = new KerberosServiceTicket("client", "service", key, 0, new KerberosGSSAPReqTicket(bytes),mockKerberosTicket);
        KerberosServiceTicket kst2 = new KerberosServiceTicket("client2", "service2", key, 1, new KerberosGSSAPReqTicket(bytes2));
        credentials.add(LoginCredentials.makeLoginCredentials(new HttpNegotiateToken(kst2), Assertion.class));
        when(mockAuthenticationContext.getCredentials()).thenReturn(credentials);

        TestServerRoutingAssertion partiallyMockedFixture = spy(makeTestSra());
        doReturn(delegatedTicket).when(partiallyMockedFixture).wrapKerberosServiceTicketForDelegation(kst, "testhost");

        KerberosServiceTicket ticket = partiallyMockedFixture.getDelegatedKerberosTicket(mockContext, "testhost");
        verify(partiallyMockedFixture, times(1)).getLastKerberosSigningSecurityToken(mockProcessorResult);
        verify(partiallyMockedFixture, times(1)).wrapKerberosServiceTicketForDelegation(kst, "testhost");
        assertEquals(delegatedTicket, ticket);

    }
}
