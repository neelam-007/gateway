package com.l7tech.external.assertions.mtom.server;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.w3c.dom.Document;
import com.l7tech.external.assertions.mtom.MtomEncodeAssertion;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.util.ResourceUtils;
import com.l7tech.policy.assertion.AssertionStatus;

import java.util.logging.Logger;
import java.util.HashMap;

/**
 *
 */
public class ServerMtomEncodeAssertionTest {

    private static final Logger logger = Logger.getLogger(ServerMtomEncodeAssertionTest.class.getName());

    private static final String message = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
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
    public void testEncode() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status ok", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertFalse( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testFailIfNotFound() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 0 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/daeta", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status fail", AssertionStatus.FALSIFIED, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            System.out.println(output);
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    @Test
    public void testOptimizationThreshold() throws Exception {
        final MtomEncodeAssertion mea = new MtomEncodeAssertion();
        mea.setAlwaysEncode( true );
        mea.setFailIfNotFound( true );
        mea.setOptimizationThreshold( 2048 );
        mea.setXpathExpressions( new XpathExpression[]{
                new XpathExpression( "/s12:Envelope/s12:Body/tns:echoFile/arg0/data", new HashMap<String,String>(){{
                    put("s12","http://www.w3.org/2003/05/soap-envelope");
                    put("tns","http://www.layer7tech.com/services/jaxws/echoservice");
                }} )
        } );

        final ServerMtomEncodeAssertion smea = buildServerAssertion( mea );
        final Document requestDoc = XmlUtil.parse( message );
        PolicyEnforcementContext context = null;
        try {
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(requestDoc), null );
            AssertionStatus status = smea.checkRequest( context );
            assertEquals( "status fail", AssertionStatus.NONE, status );

            final String output = XmlUtil.nodeToString( context.getRequest().getXmlKnob().getDocumentReadOnly() );
            System.out.println(output);
            assertTrue( "Message raw", message.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
            assertTrue( "Message encoded", output.contains("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZ"));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private ServerMtomEncodeAssertion buildServerAssertion( final MtomEncodeAssertion mea ) {
        return new ServerMtomEncodeAssertion(
                mea,
                new LogOnlyAuditor( logger ),
                new StashManagerFactory(){
                    @Override
                    public StashManager createStashManager() {
                        return new ByteArrayStashManager();
                    }
                }

        );
    }
}
