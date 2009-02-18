package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.saml.SamlConstants;
import static org.junit.Assert.*;
import org.junit.*;

/**
 *
 */
public class SamlIssuerAssertionTest {

    @BeforeClass
    public static void init() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @Test
    public void testAuthStatement() throws Exception {
        SamlIssuerAssertion sia = new SamlIssuerAssertion();
        final SamlAuthenticationStatement authst = new SamlAuthenticationStatement();
        authst.setAuthenticationMethods(new String[] { SamlConstants.X509_PKI_AUTHENTICATION });
        sia.setAuthenticationStatement(authst);

        assertTrue(WspWriter.getPolicyXml(sia).contains(SamlConstants.X509_PKI_AUTHENTICATION));
    }
}
