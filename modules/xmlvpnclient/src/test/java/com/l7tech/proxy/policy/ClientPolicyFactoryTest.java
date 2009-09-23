package com.l7tech.proxy.policy;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml2;
import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientUnknownAssertion;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

/**
 * @author alex
 */
public class ClientPolicyFactoryTest {

    @BeforeClass
    public static void setUp() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @Test
    public void testCompleteness() throws Exception {
        ClientPolicyFactory pfac = ClientPolicyFactory.getInstance();

        ClientAssertion foo;
        Assertion[] everything = BRIDGE_EVERYTHING;
        for ( Assertion anEverything : everything ) {
            foo = pfac.makeClientPolicy( anEverything );
            if ( !( anEverything instanceof UnknownAssertion ) ) {
                Assert.assertFalse( "Unknown assertion : " + foo.getName(), foo instanceof ClientUnknownAssertion );
            }
        }
    }

    @Test
    public void testComposite() throws Exception {
        AllAssertion all = new AllAssertion(Arrays.asList(BRIDGE_EVERYTHING));
        ClientPolicyFactory pfac = ClientPolicyFactory.getInstance();
        pfac.makeClientPolicy(all);
    }

    private static final List<FalseAssertion> LIST_OF_FALSE = Collections.singletonList(new FalseAssertion());
    public static Assertion[] BRIDGE_EVERYTHING = new Assertion[]{
        new HttpBasic(),
        new HttpDigest(),
        new WssBasic(),
        new EncryptedUsernameTokenAssertion(),
        new AllAssertion(LIST_OF_FALSE),
        new ExactlyOneAssertion(LIST_OF_FALSE),
        new OneOrMoreAssertion(LIST_OF_FALSE),
        new FalseAssertion(),
        new SslAssertion(),
        new TrueAssertion(),
        new RequireWssX509Cert(),
        new SecureConversation(),
        new RequireWssSignedElement(),
        new RequireWssEncryptedElement(),
        new WssSignElement(),
        new WssEncryptElement(),
        new WssReplayProtection(),
        new RequestWssKerberos(),
        new CookieCredentialSourceAssertion(),
        new RequireWssSaml(),
        new RequireWssSaml2(),
        new PreemptiveCompression(),
        new RemoteDomainIdentityInjection(),
        new RequireWssTimestamp(),
    };

}