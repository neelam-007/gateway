package com.l7tech.external.assertions.mtom.server;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import org.junit.Test;
import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;
import java.io.ByteArrayInputStream;

import com.l7tech.external.assertions.mtom.MtomValidateAssertion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.util.ResourceUtils;
import com.l7tech.policy.assertion.AssertionStatus;

/**
 *
 */
public class ServerMtomValidateAssertionTest {

    private static final Logger logger = Logger.getLogger(ServerMtomEncodeAssertionTest.class.getName());

    @Test
    public void testValidate() throws Exception {
        final MtomValidateAssertion.ValidationRule rule = new MtomValidateAssertion.ValidationRule();
        rule.setSize( 1024 );
        rule.setXpathExpression( new XpathExpression( "/s:Envelope/s:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s","http://schemas.xmlsoap.org/soap/envelope/");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} ) );

        final MtomValidateAssertion mva = new MtomValidateAssertion();
        mva.setRequireEncoded( true );
        mva.setValidationRules( new MtomValidateAssertion.ValidationRule[]{ rule } );

        final ServerMtomValidateAssertion smva = new ServerMtomValidateAssertion(
                mva,
                new LogOnlyAuditor( logger )
        );

        final String message =
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\r\n" +
                "Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" ?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body><ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\"><arg0><data><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com\"></xop:Include></data><name>payload.txt</name></arg0></ns2:echoFile></S:Body></S:Envelope>\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com>\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a--";
        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( message.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smva.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testDuplicateContentIdentifiers() throws Exception {
       final MtomValidateAssertion.ValidationRule rule = new MtomValidateAssertion.ValidationRule();
        rule.setSize( 1024 );
        rule.setXpathExpression( new XpathExpression( "/s:Envelope/s:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s","http://schemas.xmlsoap.org/soap/envelope/");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} ) );

        final MtomValidateAssertion mva = new MtomValidateAssertion();
        mva.setRequireEncoded( true );
        mva.setValidationRules( new MtomValidateAssertion.ValidationRule[]{ rule } );

        final List<String> auditedMessages = new ArrayList<String>();
        final ServerMtomValidateAssertion smva = new ServerMtomValidateAssertion(
                mva,
                new LogOnlyAuditor( logger ){
                    @Override
                    public void logAndAudit( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
                        auditedMessages.add( MessageFormat.format(msg.getMessage(), params ) );
                        super.logAndAudit( msg, params, e );
                    }
                }
        );

        final String message =
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\r\n" +
                "Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" ?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body><ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\"><arg0><data><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com\"></xop:Include></data><name>payload.txt</name></arg0></ns2:echoFile></S:Body></S:Envelope>\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com>\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com>\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a--";
        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( message.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smva.checkRequest( context );
            assertEquals( "status falsified", AssertionStatus.FALSIFIED, status );
            boolean sawAudit = false;
            for ( String auditMessage : auditedMessages ) {
                if ( auditMessage.contains("MIME part Content-IDs are not unique" )) {
                    sawAudit = true;
                }
            }
            assertTrue("failed due to content-id duplication", sawAudit);
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

   @Test
    public void testSelectIncludeElement() throws Exception {
       final MtomValidateAssertion.ValidationRule rule = new MtomValidateAssertion.ValidationRule();
        rule.setSize( 1024 );
        rule.setXpathExpression( new XpathExpression( "/s:Envelope/s:Body/tns:echoFile/arg0/data/*", new HashMap<String,String>(){{
                    put("s","http://schemas.xmlsoap.org/soap/envelope/");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} ) );

        final MtomValidateAssertion mva = new MtomValidateAssertion();
        mva.setRequireEncoded( true );
        mva.setValidationRules( new MtomValidateAssertion.ValidationRule[]{ rule } );

        final List<String> auditedMessages = new ArrayList<String>();
        final ServerMtomValidateAssertion smva = new ServerMtomValidateAssertion(
                mva,
                new LogOnlyAuditor( logger ){
                    @Override
                    public void logAndAudit( final AuditDetailMessage msg, final String[] params, final Throwable e ) {
                        auditedMessages.add( MessageFormat.format(msg.getMessage(), params ) );
                        super.logAndAudit( msg, params, e );
                    }
                }
        );

        final String message =
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\r\n" +
                "Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" ?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body><ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\"><arg0><data><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com\"></xop:Include></data><name>payload.txt</name></arg0></ns2:echoFile></S:Body></S:Envelope>\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com>\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\n" +
                "\r\n" +
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a--";
        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( message.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smva.checkRequest( context );
            assertEquals( "status falsified", AssertionStatus.FALSIFIED, status );
            boolean sawAudit = false;
            for ( String auditMessage : auditedMessages ) {
                if ( auditMessage.contains("Element is an XOP Include (the parent element is required)" )) {
                    sawAudit = true;
                }
            }
            assertTrue("failed due to invalid XPath", sawAudit);
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }
}
