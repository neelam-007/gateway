package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SamlpResponseBuilderAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SamlpResponseBuilderAssertion());
    }

    @Test
    public void testStatusChangeIsBackwardsCompatible_PreEscolarSp1() throws Exception {
        AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(SamlpResponseBuilderAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
        Assertion ass = WspReader.getDefault().parseStrictly(PRE_ESCOLAR_SP1_POLICY_XML, WspReader.INCLUDE_DISABLED);

        validateCannedAssertionXml(ass);
    }

    @Test
    public void testStatusChange_PostEscolarSp1Format() throws Exception {
        AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(SamlpResponseBuilderAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
        Assertion ass = WspReader.getDefault().parseStrictly(POST_ESCOLAR_SP1_POLICY_XML, WspReader.INCLUDE_DISABLED);

        validateCannedAssertionXml(ass);
    }

    private void validateCannedAssertionXml(Assertion ass) {
        AllAssertion allAss = (AllAssertion) ass;
        final List<Assertion> children = allAss.getChildren();
        final SamlpResponseBuilderAssertion respAss1 = (SamlpResponseBuilderAssertion) children.get(1);
        assertEquals(SamlStatus.SAML2_SUCCESS.getValue(), respAss1.getSamlStatusCode());

        final SamlpResponseBuilderAssertion respAss2 = (SamlpResponseBuilderAssertion) children.get(2);
        assertEquals(SamlStatus.SAML2_REQUESTER.getValue(), respAss2.getSamlStatusCode());

        final SamlpResponseBuilderAssertion respAss3 = (SamlpResponseBuilderAssertion) children.get(3);
        assertEquals(SamlStatus.SAML2_RESPONDER.getValue(), respAss3.getSamlStatusCode());
    }


    final String PRE_ESCOLAR_SP1_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"Backwards compatibility test\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:SamlpResponseBuilder>\n" +
            "            <L7p:StatusDetail stringValue=\"\"/>\n" +
            "            <L7p:StatusMessage stringValue=\"\"/>\n" +
            "            <L7p:ValidateWebSsoRules booleanValue=\"false\"/>\n" +
            "        </L7p:SamlpResponseBuilder>\n" +
            "        <L7p:SamlpResponseBuilder>\n" +
            "            <L7p:SamlStatus samlStatus=\"SAML2_REQUESTER\"/>\n" +
            "            <L7p:StatusDetail stringValue=\"\"/>\n" +
            "            <L7p:StatusMessage stringValue=\"\"/>\n" +
            "            <L7p:ValidateWebSsoRules booleanValue=\"false\"/>\n" +
            "        </L7p:SamlpResponseBuilder>\n" +
            "        <L7p:SamlpResponseBuilder>\n" +
            "            <L7p:SamlStatus samlStatus=\"SAML2_RESPONDER\"/>\n" +
            "            <L7p:StatusDetail stringValue=\"\"/>\n" +
            "            <L7p:StatusMessage stringValue=\"\"/>\n" +
            "            <L7p:ValidateWebSsoRules booleanValue=\"false\"/>\n" +
            "        </L7p:SamlpResponseBuilder>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private final static String POST_ESCOLAR_SP1_POLICY_XML =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n"+
            "    <wsp:All wsp:Usage=\"Required\">\n"+
            "        <L7p:CommentAssertion>\n"+
            "            <L7p:Comment stringValue=\"Backwards compatibility test\"/>\n"+
            "        </L7p:CommentAssertion>\n"+
            "        <L7p:SamlpResponseBuilder>\n"+
            "            <L7p:StatusDetail stringValue=\"\"/>\n"+
            "            <L7p:StatusMessage stringValue=\"\"/>\n"+
            "            <L7p:ValidateWebSsoRules booleanValue=\"false\"/>\n"+
            "        </L7p:SamlpResponseBuilder>\n"+
            "        <L7p:SamlpResponseBuilder>\n"+
            "            <L7p:SamlStatusCode stringValue=\"urn:oasis:names:tc:SAML:2.0:status:Requester\"/>\n"+
            "            <L7p:StatusDetail stringValue=\"\"/>\n"+
            "            <L7p:StatusMessage stringValue=\"\"/>\n"+
            "            <L7p:ValidateWebSsoRules booleanValue=\"false\"/>\n"+
            "        </L7p:SamlpResponseBuilder>\n"+
            "        <L7p:SamlpResponseBuilder>\n"+
            "            <L7p:SamlStatusCode stringValue=\"urn:oasis:names:tc:SAML:2.0:status:Responder\"/>\n"+
            "            <L7p:StatusDetail stringValue=\"\"/>\n"+
            "            <L7p:StatusMessage stringValue=\"\"/>\n"+
            "            <L7p:ValidateWebSsoRules booleanValue=\"false\"/>\n"+
            "        </L7p:SamlpResponseBuilder>\n"+
            "    </wsp:All>\n"+
            "</wsp:Policy>";

}
