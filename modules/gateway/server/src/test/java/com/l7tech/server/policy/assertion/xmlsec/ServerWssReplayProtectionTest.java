package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BugNumber;
import com.l7tech.util.MockConfig;
import org.junit.Test;
import static org.junit.Assert.*;

import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.server.util.MessageId;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.util.TimeSource;
import com.l7tech.util.TestTimeSource;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

/**
 * 
 */
public class ServerWssReplayProtectionTest {

    private static final TimeSource testTimeSource = new TestTimeSource();

    /**
     * Basic success test
     */
    @Test
    public void testCustomProtection() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 15000, "var", null );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable( "var", "customid" );

        final AssertionStatus result = buildServerAssertion( wrp ).checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result );
    }

    /**
     * Basic success with scopes
     */
    @Test
    public void testCustomProtectionWithScope() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 15000, "var", "${myScope}" );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable( "var", "customid" );
        context.setVariable( "myScope", "scope" );

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result );

        context.setVariable( "var", "customid2" );
        final AssertionStatus result2 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result2 );

        context.setVariable( "var", "customid3" );
        final AssertionStatus result3 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result3 );
    }

    /**
     * Replay test
     */
    @Test
    public void testCustomProtectionReplay() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 15000, "var", null );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable( "var", "customid" );

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result1 );

        final AssertionStatus result2 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.BAD_REQUEST, result2 );
    }

    /**
     * Replay with scope variable
     */
    @Test
    public void testCustomProtectionReplayWithScope() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 15000, "var", "${scopeVar}" );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable( "var", "customid" );
        context.setVariable( "scopeVar", "customscope1" );

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result1 );

        context.setVariable( "scopeVar", "customscope2" );

        final AssertionStatus result2 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result2 );

        final AssertionStatus result3 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.BAD_REQUEST, result3 );
    }

    /**
     * Replay with static scope
     */
    @Test
    public void testCustomProtectionReplayWithStaticScope() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 900000, "var", "scope" );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable( "var", "customid" );

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NONE, result1 );

        final AssertionStatus result2 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.BAD_REQUEST, result2 );
    }

    /**
     * Should fail when MessageIdManager has not been initialized (e.g. when the Hazelcast instance cannot be created)
     */
    @Test
    public void testMessageIdManagerNotInitialized() throws Exception {
        final WssReplayProtection wrp = buildAssertion(15000, "var", null);
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("var", "customid");

        final ServerWssReplayProtection swrp = buildServerAssertion(wrp, false);

        final AssertionStatus result = swrp.checkRequest(context);
        assertEquals("Status", AssertionStatus.FAILED, result);
    }

    @Test
    public void testMissingIdVariable() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 900000, "var", null );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.FAILED, result1 );
    }

    @Test
    public void testEmptyIdVariable() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 900000, "var", null );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable( "var", "" );

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.FALSIFIED, result1 );
    }
    
    @Test
    public void testMissingScopeVariable() throws Exception {
        final WssReplayProtection wrp = buildAssertion( 900000, "var", "${scope}" );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.FAILED, result1 );
    }

    @Test
    @BugNumber(9990)
    public void testNoWSSOrWSA() throws Exception {
        final WssReplayProtection wrp = new WssReplayProtection();
        final Document request = XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body/></soapenv:Envelope>" );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message( request ), new Message());

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.NOT_APPLICABLE, result1 );
    }

    @Test
    @BugNumber(9990)
    public void testBadSecurity() throws Exception {
        final WssReplayProtection wrp = new WssReplayProtection();
        final Document request = XmlUtil.parse( "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\"  xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:BinarySecurityToken Type=\"UNKNOWN\"/></wsse:Security></soapenv:Header><soapenv:Body/></soapenv:Envelope>" );
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message( request ), new Message());

        final ServerWssReplayProtection swrp = buildServerAssertion( wrp );
        final AssertionStatus result1 = swrp.checkRequest(context);
        assertEquals( "Status", AssertionStatus.BAD_REQUEST, result1 );
    }

    private WssReplayProtection buildAssertion( final int expiry, final String idVar, final String scope ) {
        final WssReplayProtection wrp = new WssReplayProtection();
        wrp.setCustomProtection( true );
        wrp.setCustomExpiryTime( expiry );
        wrp.setCustomIdentifierVariable( idVar );
        wrp.setCustomScope( scope );
        return wrp;
    }

    private ServerWssReplayProtection buildServerAssertion(final WssReplayProtection wrp) {
        return buildServerAssertion(wrp, true);
    }

    private ServerWssReplayProtection buildServerAssertion(final WssReplayProtection wrp, final boolean initialized) {
        return new ServerWssReplayProtection(wrp, new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("serverConfig", new MockConfig(new Properties()));
            put("securityTokenResolver", new SimpleSecurityTokenResolver());
            put("messageIdManager", new MockMessageIdManager(wrp.getCustomExpiryTime(), initialized));
        }})){
            @Override
            protected TimeSource getTimeSource() {
                return testTimeSource;
            }
        };
    }

    private static final class MockMessageIdManager implements MessageIdManager {
        private final int expectedExpiryTime;
        private final Set<String> ids = new HashSet<>();
        private final boolean initialized;

        public MockMessageIdManager(int expectedExpiryTime, boolean initialized) {
            this.expectedExpiryTime = expectedExpiryTime;
            this.initialized = initialized;
        }

        @Override
        public void assertMessageIdIsUnique(final MessageId prospect) throws MessageIdCheckException {
            if (!initialized) {
                throw new MessageIdCheckException("MessageIdManager not initialized");
            }

            long expectTime = testTimeSource.currentTimeMillis()+expectedExpiryTime;
            if ( prospect.getNotValidOnOrAfterDate() != expectTime )
                throw new MessageIdCheckException("Expiry check failed, expected " + expectTime + " got " + prospect.getNotValidOnOrAfterDate());
            if ( ids.contains( prospect.getOpaqueIdentifier() ) ) throw new DuplicateMessageIdException();
            ids.add( prospect.getOpaqueIdentifier() );
        }
    }
}
