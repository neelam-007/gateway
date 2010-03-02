package com.l7tech.external.assertions.mtom.server;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.logging.Logger;
import java.util.Properties;
import java.io.ByteArrayInputStream;

import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.MockConfig;
import com.l7tech.external.assertions.mtom.MtomDecodeAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;

/**
 * 
 */
public class ServerMtomDecodeAssertionTest {

    private static final Logger logger = Logger.getLogger(ServerMtomEncodeAssertionTest.class.getName());
    private static final String message =
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
    private static final String securedMessage =
                "--uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\r\n" +
                "Content-Id: <rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\r\n" +
                "Content-Type: application/xop+xml;charset=utf-8;type=\"text/xml\"\r\n" +
                "Content-Transfer-Encoding: binary\r\n" +
                "\r\n" +
                "<?xml version=\"1.0\" ?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" S:actor=\"secure_span\" S:mustUnderstand=\"1\"/></S:Header><S:Body><ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\"><arg0><data><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:5b217328-8151-452a-8aa7-03dace49949d@example.jaxws.sun.com\"></xop:Include></data><name>payload.txt</name></arg0></ns2:echoFile></S:Body></S:Envelope>\r\n" +
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
    private static final String xmlMessage = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
                "    <S:Body>\n" +
                "        <ns2:echoFile xmlns:ns2=\"http://www.layer7tech.com/services/jaxws/echoservice\">\n" +
                "            <arg0>\n" +
                "                <data>QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkKQUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODkK</data>\n" +
                "                <name>payload.txt</name>\n" +
                "            </arg0>\n" +
                "        </ns2:echoFile>\n" +
                "    </S:Body>\n" +
                "</S:Envelope>";


    @Test
    public void testDecode() throws Exception {
        final MtomDecodeAssertion mda = new MtomDecodeAssertion();
        mda.setProcessSecuredOnly( false );
        mda.setRemovePackaging( true );
        mda.setRequireEncoded( true );

        final ServerMtomDecodeAssertion smda = buildServerAssertion( mda );

        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( message.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smda.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            assertFalse( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message decoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "SOAP type", mess.getMimeKnob().getOuterContentType().getType().equalsIgnoreCase("text"));
            assertTrue( "SOAP subtype", mess.getMimeKnob().getOuterContentType().getSubtype().equalsIgnoreCase("xml"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testDecodeIfSecured() throws Exception {
        final MtomDecodeAssertion mda = new MtomDecodeAssertion();
        mda.setProcessSecuredOnly( true );
        mda.setRemovePackaging( true );
        mda.setRequireEncoded( true );

        final ServerMtomDecodeAssertion smda = buildServerAssertion( mda );

        {
            PolicyEnforcementContext context = null;
            try {
                Message mess = new Message(
                        new ByteArrayStashManager(),
                        ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                        new ByteArrayInputStream( message.getBytes() ));
                context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
                AssertionStatus status = smda.checkRequest( context );
                assertEquals( "status ok", AssertionStatus.NONE, status );

                final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
                assertFalse( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
                assertFalse( "Message decoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
                assertFalse( "SOAP type", mess.getMimeKnob().getOuterContentType().getType().equalsIgnoreCase("text"));
                assertFalse( "SOAP subtype", mess.getMimeKnob().getOuterContentType().getSubtype().equalsIgnoreCase("xml"));
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }

        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( securedMessage.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smda.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            assertFalse( "Message raw", securedMessage.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message decoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "SOAP type", mess.getMimeKnob().getOuterContentType().getType().equalsIgnoreCase("text"));
            assertTrue( "SOAP subtype", mess.getMimeKnob().getOuterContentType().getSubtype().equalsIgnoreCase("xml"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testDecodeToCustomTarget() throws Exception {
        final MtomDecodeAssertion mda = new MtomDecodeAssertion();
        mda.setProcessSecuredOnly( false );
        mda.setRemovePackaging( true );
        mda.setRequireEncoded( true );
        mda.setOutputTarget( new MessageTargetableSupport("output") );

        final ServerMtomDecodeAssertion smda = buildServerAssertion( mda );

        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( message.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smda.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final Message outputMessage = context.getTargetMessage(new MessageTargetableSupport("output"));
            final String output = XmlUtil.nodeToString( outputMessage.getXmlKnob().getDocumentReadOnly() );
            assertFalse( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message decoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "SOAP type", outputMessage.getMimeKnob().getOuterContentType().getType().equalsIgnoreCase("text"));
            assertTrue( "SOAP subtype", outputMessage.getMimeKnob().getOuterContentType().getSubtype().equalsIgnoreCase("xml"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testRequireEncoded() throws Exception {
        final MtomDecodeAssertion mda = new MtomDecodeAssertion();
        mda.setProcessSecuredOnly( false );
        mda.setRemovePackaging( true );
        mda.setRequireEncoded( true );

        {
            final ServerMtomDecodeAssertion smda = buildServerAssertion( mda );
            PolicyEnforcementContext context = null;
            try {
                Message mess = new Message(XmlUtil.parse( xmlMessage ));
                context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
                AssertionStatus status = smda.checkRequest( context );
                assertEquals( "status fail", AssertionStatus.BAD_REQUEST, status );
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }
        {
            mda.setRequireEncoded( false );
            final ServerMtomDecodeAssertion smda = buildServerAssertion( mda );
            PolicyEnforcementContext context = null;
            try {
                Message mess = new Message(XmlUtil.parse( xmlMessage ));
                context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
                AssertionStatus status = smda.checkRequest( context );
                assertEquals( "status ok", AssertionStatus.NONE, status );
            } finally {
                ResourceUtils.closeQuietly( context );
            }
        }
    }

    @Test
    public void testRemovePackaging() throws Exception {
        final MtomDecodeAssertion mda = new MtomDecodeAssertion();
        mda.setProcessSecuredOnly( false );
        mda.setRemovePackaging( false );
        mda.setRequireEncoded( true );

        final ServerMtomDecodeAssertion smda = buildServerAssertion( mda );

        PolicyEnforcementContext context = null;
        try {
            Message mess = new Message(
                    new ByteArrayStashManager(),
                    ContentTypeHeader.parseValue("multipart/related;start=\"<rootpart*45ac4aae-b978-40c3-b093-18e82e03ce3a@example.jaxws.sun.com>\";type=\"application/xop+xml\";boundary=\"uuid:45ac4aae-b978-40c3-b093-18e82e03ce3a\";start-info=\"text/xml\""),
                    new ByteArrayInputStream( message.getBytes() ));
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( mess, null );
            AssertionStatus status = smda.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            assertFalse( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message decoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Multipart type", mess.getMimeKnob().getOuterContentType().getType().equalsIgnoreCase("multipart"));
            assertTrue( "Multipart subtype", mess.getMimeKnob().getOuterContentType().getSubtype().equalsIgnoreCase("related"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private ServerMtomDecodeAssertion buildServerAssertion( final MtomDecodeAssertion mda ) {
        return new ServerMtomDecodeAssertion(
                mda,
                new LogOnlyAuditor( logger ),
                new StashManagerFactory(){
                    @Override
                    public StashManager createStashManager() {
                        return new ByteArrayStashManager();
                    }
                },
                new MockConfig( new Properties(){{ setProperty( ClusterProperty.asServerConfigPropertyName(MtomDecodeAssertion.PROP_DECODE_SECURED), "true"); }} )
        );
    }

}
