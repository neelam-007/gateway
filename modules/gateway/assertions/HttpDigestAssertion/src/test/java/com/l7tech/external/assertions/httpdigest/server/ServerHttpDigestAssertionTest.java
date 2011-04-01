package com.l7tech.external.assertions.httpdigest.server;

import com.l7tech.external.assertions.httpdigest.HttpDigestAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.PolicyProcessingTest;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.test.BugNumber;
import com.l7tech.util.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Test the HttpDigestAssertion.
 */
public class ServerHttpDigestAssertionTest {

    private static final Logger log = Logger.getLogger(ServerHttpDigestAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;
    private final long identityProvider = 111;

    /**
     * Moved from SamplePolicyTest as part of modularization.
     */
    @Test
    public void testDigestAuth() {
        String userAlice = "alice";
        // Require HTTP Digest auth.  Allow Alice in, but nobody else.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Identify:
            new HttpDigestAssertion(),

            // Authorize:
            new SpecificUser(identityProvider, userAlice, null, null),

            // Route:
            new HttpRoutingAssertion()
        }));
    }

    /**
     * Moved from SamplePolicyTest as part of modularization.
     */
    @Test
    public void testDigestGroup() {
        String groupStaff = "staff";
        // Require HTTP Digest auth with group.  All staff get to use this service.
        Assertion basicAuthPolicy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Identify:
            new HttpDigestAssertion(),

            // Authorize:
            new MemberOfGroup(identityProvider, groupStaff, "666"),

            // Route:
            new HttpRoutingAssertion()
        }));
    }

    /**
     * Test case for having two Http Digest assertions.
     * Also tests that the Gateway is backwards compatible with legacy policy XML, when the HTTP Digest modular
     * assertion is registered. 
     *
      * @throws Exception
     */
    @SuppressWarnings({"unchecked"})
    @Test
    @BugNumber(5813)
	public void testTwoHttpDigestAuth() throws Exception {
        //need to register the nonce so that we'll always be using the same nonce = 70ec76c747e23906120eec731341660f
        //create new instance of nonce info so that it can be registered into the digest session
        String nonce = "70ec76c747e23906120eec731341660f";
        Class classNonceInfo = Class.forName(DigestSessions.class.getName() + "$NonceInfo");
        Constructor constructor = classNonceInfo.getDeclaredConstructor(String.class, Long.TYPE, Integer.TYPE);
        constructor.setAccessible(true); //suppress Java language accesschecking

        DigestSessions digestSession = DigestSessions.getInstance();
        Field nonceInfoField = DigestSessions.class.getDeclaredField("_nonceInfos");
        nonceInfoField.setAccessible(true);
        Map<String,Object> nonceInfo = (Map<String,Object>) nonceInfoField.get(digestSession);    //grab the field from the digest session

        //register the nonce
        nonceInfo.put(nonce, constructor.newInstance(nonce, System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1), 3));

        //create request header for http digest
        String authHeader = "Digest username=\"testuser2\", realm=\"L7SSGDigestRealm\", nonce=\"70ec76c747e23906120eec731341660f\", " +
                "uri=\"/ssg/soap\", response=\"326f367c241545fd0628bc0becf5948e\", qop=auth, nc=00000001, " +
                "cnonce=\"c1f102ea2080f3694288f0841cbfc1b0\", opaque=\"2f9e9d78e4ec2de1258ee75634badb41\"";


        final PolicyProcessingTest processingTest = new PolicyProcessingTest();
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        final AssertionRegistry assertionRegistry = applicationContext.getBean("assertionRegistry", AssertionRegistry.class);
        try {
            //note if the assertion is not registered, it will fail below. This implicitly tests that HTTPDigest is backwards compatible.
            assertionRegistry.registerAssertion(HttpDigestAssertion.class);

            PublishedService ps = new PublishedService();
            String [] serviceInfo = new String[]{"/httpdigestauth", legacyPolicyXml};
            ps.setOid(2341234);
            ps.setName("/httpdigestauth".substring(1));
            ps.setRoutingUri("/httpdigestauth");
            ps.getPolicy().setXml(serviceInfo[1]);
            ps.getPolicy().setOid(ps.getOid());
            ps.setSoap(true);
            ps.setLaxResolution(true);

            processingTest.runTestForModularAssertion(ps, generalRequest, AssertionStatus.AUTH_REQUIRED, null);
            processingTest.runTestForModularAssertion(ps, generalRequest, AssertionStatus.NONE, authHeader);
        } finally {
            //unregister so other tests are not broken by this registration.
            final Method method = assertionRegistry.getClass().getDeclaredMethod("unregisterAssertion", Assertion.class);
            method.setAccessible(true);
            method.invoke(assertionRegistry, new HttpDigestAssertion());
        }
    }

    @Test
    public void testAssertionNotAvailableByDefault() throws Exception{
        final Assertion assertion = WspReader.getDefault().parsePermissively(legacyPolicyXml, WspReader.Visibility.includeDisabled);
        final boolean containsUnknown = containsNoUnknownAssertions((CompositeAssertion) assertion);

        Assert.assertTrue("HTTP Digest assertion should not be known", containsUnknown);
    }

    private boolean containsNoUnknownAssertions(CompositeAssertion parent){
        final List<Assertion> kids = parent.getChildren();
        if(kids.isEmpty()) return false;

        for (Assertion kid : kids) {
            if(kid instanceof CompositeAssertion){
                if(containsNoUnknownAssertions((CompositeAssertion) kid)){
                    return true;
                }
            }
            if(kid instanceof UnknownAssertion){
                return true;
            }
        }

        return false;
    }

    /**
     * This policy XML contains the same assertion xml for the HTTP Digest assertion that any existing policies will.
     * If the new modular assertion is installed, then it will just work, otherwise the HTTPDigest assertion will
     * behave like an unknown assertion.
     */
    private final static String legacyPolicyXml ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<exp:Export Version=\"3.0\"\n" +
            "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
            "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:HttpDigest/>\n" +
            "            <L7p:HttpDigest/>\n" +
            "            <L7p:HardcodedResponse>\n" +
            "                <L7p:Base64ResponseBody stringValue=\"PE1lc3NhZ2U+WW91IGhhdmUgcGFzc2VkITwvTWVzc2FnZT4=\"/>\n" +
            "            </L7p:HardcodedResponse>\n" +
            "        </wsp:All>\n" +
            "    </wsp:Policy>\n" +
            "</exp:Export>";
    
    private final static String generalRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope\n" +
            "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soapenv:Header/>\n" +
            "    <soapenv:Body>\n" +
            "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
            "            <tns:delay>0</tns:delay>\n" +
            "        </tns:listProducts>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";
}
