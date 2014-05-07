package com.l7tech.external.assertions.siteminder;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test the SiteMinderAuthenticateAssertion.
 */
public class SiteMinderAuthenticateAssertionTest {

    private static final Logger log = Logger.getLogger(SiteMinderAuthenticateAssertionTest.class.getName());

    private ApplicationContext appCtx;
    private WspReader policyReader;

    @Before
    public void setUp() throws Exception {

        if ( appCtx == null ) {
            appCtx = ApplicationContexts.getTestApplicationContext();
            assertNotNull("Failed - cannot get Test Application Contesxt!");
        }

        if ( policyReader == null ) {
            final AssertionRegistry tmf = new AssertionRegistry();
            tmf.setApplicationContext(appCtx);
            tmf.registerAssertion(SiteMinderAuthenticateAssertion.class);
            WspConstants.setTypeMappingFinder(tmf);
            policyReader = new WspReader(tmf);

            assertNotNull("Failed - Unable to obtain WspReader");
        }
    }

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SiteMinderAuthenticateAssertion());
    }

    @Test
    public void testPolicyXmlBackwardsCompatibility() throws Exception {
        // Login element of the policy XML is no longer in use (that is embedded in the policyXml snippet below).
        // Test that it can be imported safely into the NamedUser attribute.
        final String policyXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SiteMinderAuthenticate>\n" +
                "            <L7p:LastCredential booleanValue=\"false\"/>\n" +
                "            <L7p:Login stringValue=\"sm_rainier\"/>\n" +
                "            <L7p:Prefix stringValue=\"siteminder\"/>\n" +
                "        </L7p:SiteMinderAuthenticate>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>\n";

        Assertion as = policyReader.parsePermissively(policyXml, WspReader.Visibility.omitDisabled);
        assertNotNull(as);
        assertTrue(as instanceof AllAssertion);
        AllAssertion allAs = (AllAssertion)as;

        Assertion childAs = allAs.getChildren().get(0);
        assertNotNull(childAs);
        assertTrue(childAs instanceof SiteMinderAuthenticateAssertion);
        SiteMinderAuthenticateAssertion smAs = (SiteMinderAuthenticateAssertion)childAs;

        assertEquals("Backwards compatibility failure - login attribute does not get mapped properly to NamedUser: ",smAs.getNamedUser(),"sm_rainier");
    }

}
