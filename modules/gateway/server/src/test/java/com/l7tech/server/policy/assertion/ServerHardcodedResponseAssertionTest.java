package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeBodyTest;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.*;

public class ServerHardcodedResponseAssertionTest {
    private static final int STATUS = 400;
    private HardcodedResponseAssertion assertion;
    private PolicyEnforcementContext policyContext;
    private TestAudit testAudit;

    @Before
    public void setup() throws Exception {
        assertion = new HardcodedResponseAssertion();
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        testAudit = new TestAudit();
    }

    @Test
    public void testStaticResponse() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.responseBodyString( "Test static response" );
        responseAssertion.setResponseContentType( "text/plain" );

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);
        assertEquals( AssertionStatus.NONE, assertionStatus );

        byte[] responseBytes = IOUtils.slurpStream( policyContext.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
        assertEquals( "Test static response", new String( responseBytes ) );

        assertFalse( testAudit.isAnyAuditPresent() );
    }

    @Test
    public void testVariablesResponse() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.responseBodyString( "Hi ${joe}!  Hope ${sue} is well." );
        responseAssertion.setResponseContentType( "text/plain" );

        policyContext.setVariable( "joe", "Joe Schmoe" );
        policyContext.setVariable( "sue", "Susan Schmoe" );

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);
        assertEquals( AssertionStatus.NONE, assertionStatus );

        byte[] responseBytes = IOUtils.slurpStream( policyContext.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
        assertEquals( "Hi Joe Schmoe!  Hope Susan Schmoe is well.", new String( responseBytes ) );

        assertFalse( testAudit.isAnyAuditPresent() );
    }

    @Test
    public void checkRequestDefaultStatus() throws Exception {
        final AssertionStatus assertionStatus = getServerAssertion(assertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(Integer.valueOf(HardcodedResponseAssertion.DEFAULT_STATUS).intValue(), policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestStringStatus() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus(String.valueOf(STATUS));
        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestIntStatus() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus(STATUS);

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestContextVariableStatus() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", STATUS);

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(STATUS, policyContext.getResponse().getHttpResponseKnob().getStatus());
    }

    @Test
    public void checkRequestContextVariableStatusNotAnInteger() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "notaninteger");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: notaninteger"));
    }

    @Test
    public void checkRequestContextVariableStatusTooLarge() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "2147483648");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: 2147483648"));
    }

    @Test
    public void checkRequestContextVariableStatusZero() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "0");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: 0"));
    }

    @Test
    public void checkRequestContextVariableStatusNegative() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        policyContext.setVariable("status", "-1");

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: -1"));
    }

    @Test
    public void checkRequestContextVariableStatusDoesNotExist() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseStatus("${status}");
        // do not set ${status} on policy context

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresentContaining("Invalid response status: "));
    }

    @Test
    @BugId( "SSG-9719" )
    public void checkRequestCopyBinaryMessage() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.responseBodyString( "${mess}" );
        responseAssertion.setResponseContentType( "application/octet-stream" );

        byte[] bodyBytes = new byte[ 2048 ];
        new Random( 2837L ).nextBytes( bodyBytes );
        Message message = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( bodyBytes ) );
        policyContext.setVariable( "mess", message );

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);
        assertEquals( AssertionStatus.NONE, assertionStatus );

        byte[] responseBytes = IOUtils.slurpStream( policyContext.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
        assertArrayEquals( bodyBytes, responseBytes );

        assertFalse( testAudit.isAnyAuditPresent() );
    }

    @Test
    @BugId( "SSG-9719" )
    public void checkRequestCopyMessagePart_firstPart() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.responseBodyString( "${mess.parts[0]}" );
        responseAssertion.setResponseContentType( "application/octet-stream" );

        byte[] sourceBytes = MimeBodyTest.MESS.getBytes( Charsets.UTF8 );
        ContentTypeHeader contentType = ContentTypeHeader.parseValue( MimeBodyTest.MESS_CONTENT_TYPE );
        Message message = new Message( new ByteArrayStashManager(), contentType, new ByteArrayInputStream( sourceBytes ) );
        policyContext.setVariable( "mess", message );

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);
        assertEquals( AssertionStatus.NONE, assertionStatus );

        byte[] wantBytes = IOUtils.slurpStream( message.getMimeKnob().getPart( 0 ).getInputStream( false ) );
        byte[] responseBytes = IOUtils.slurpStream( policyContext.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
        assertArrayEquals( wantBytes, responseBytes );

        assertFalse( testAudit.isAnyAuditPresent() );
    }

    @Test
    @BugId( "SSG-9719" )
    public void checkRequestCopyMessagePart_secondPart() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.responseBodyString( "${mess.parts[1]}" );
        responseAssertion.setResponseContentType( "application/octet-stream" );

        byte[] sourceBytes = MimeBodyTest.MESS.getBytes( Charsets.UTF8 );
        ContentTypeHeader contentType = ContentTypeHeader.parseValue( MimeBodyTest.MESS_CONTENT_TYPE );
        Message message = new Message( new ByteArrayStashManager(), contentType, new ByteArrayInputStream( sourceBytes ) );
        policyContext.setVariable( "mess", message );

        final AssertionStatus assertionStatus = getServerAssertion(responseAssertion).checkRequest(policyContext);
        assertEquals( AssertionStatus.NONE, assertionStatus );

        byte[] wantBytes = IOUtils.slurpStream( message.getMimeKnob().getPart( 1 ).getInputStream( false ) );
        byte[] responseBytes = IOUtils.slurpStream( policyContext.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
        assertArrayEquals( wantBytes, responseBytes );

        assertFalse( testAudit.isAnyAuditPresent() );
    }

    @Test
    @BugId( "SSG-9719" )
    public void checkRequestCopyMessagePart_noSuchPart() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.responseBodyString( "${mess.parts[1]}" );
        responseAssertion.setResponseContentType( "application/octet-stream" );

        byte[] sourceBytes = MimeBodyTest.MESS.getBytes( Charsets.UTF8 );
        ContentTypeHeader contentType = ContentTypeHeader.parseValue( MimeBodyTest.MESS_CONTENT_TYPE );
        Message message = new Message( new ByteArrayStashManager(), contentType, new ByteArrayInputStream( sourceBytes ) );
        IOUtils.copyStream( message.getMimeKnob().getPart( 1 ).getInputStream( true ), new NullOutputStream() );
        policyContext.setVariable( "mess", message );

        AssertionStatus assertionStatus;
        try {
            assertionStatus = getServerAssertion( responseAssertion ).checkRequest( policyContext );
        } catch ( AssertionStatusException e ) {
            assertionStatus = e.getAssertionStatus();
        }
        assertEquals( AssertionStatus.SERVER_ERROR, assertionStatus );

        byte[] responseBytes = IOUtils.slurpStream( policyContext.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream() );
        assertEquals( 0, responseBytes.length );

        assertTrue( testAudit.isAuditPresentContaining( "message has no part" ) );
    }

    private ServerHardcodedResponseAssertion getServerAssertion(final HardcodedResponseAssertion assertion) throws PolicyAssertionException {
        final ServerHardcodedResponseAssertion serverAssertion = new ServerHardcodedResponseAssertion(assertion, ApplicationContexts.getTestApplicationContext());
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        return serverAssertion;
    }
}
