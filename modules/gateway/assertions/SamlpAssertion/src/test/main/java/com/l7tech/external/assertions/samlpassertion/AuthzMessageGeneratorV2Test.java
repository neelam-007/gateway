package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.external.assertions.samlpassertion.server.*;
import com.l7tech.external.assertions.samlpassertion.server.v2.AuthzDecisionQueryGenerator;
import com.l7tech.server.audit.Auditor;
import saml.v2.protocol.AuthzDecisionQueryType;

import javax.xml.bind.JAXBElement;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * User: vchan
 */
public class AuthzMessageGeneratorV2Test extends SamlpMessageGeneratorTest<AuthzDecisionQueryType> {


    public void testCreateEmptyAuthzRequest() {

        try {
            SamlpRequestBuilderAssertion assertion = getAssertionFromXml(TEST_POLICY_SIMPLE);
            AbstractSamlp2MessageGenerator<AuthzDecisionQueryType> generator = createMessageGenerator(assertion);
            assertNotNull(generator);

            AuthzDecisionQueryType result = generator.create(assertion);
            assertNotNull(result);

            checkCommonRequestElements(result);

            assertNotNull(result.getSubject());

            JAXBElement<?> reqElement = generator.createJAXBElement(result);
            System.out.println( toXml(reqElement) );

        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }

    protected int getSamlVersion() {
        return 2;
    }

    protected AbstractSamlp2MessageGenerator<AuthzDecisionQueryType> createMessageGenerator(SamlpRequestBuilderAssertion assertion) {

        try {
            Auditor auditor = new Auditor(this, appCtx, null);
            java.util.Map<String, Object> varMap = new HashMap<String, Object>();

            AuthzDecisionQueryGenerator gen = new AuthzDecisionQueryGenerator(varMap, auditor);

            gen.setNameResolver( new NameIdentifierResolver(assertion) {
                protected void parse() {
                    this.nameValue = "somebody@email-exchange.com";
                    this.nameFormat = SamlConstants.NAMEIDENTIFIER_EMAIL;
                }
            });
            gen.setIssuerNameResolver( new NameIdentifierResolver(assertion) {
                protected void parse() {
                    this.nameValue = "Bob-the-issuer";
                }
            });
            gen.setAuthnMethodResolver( new MessageValueResolver<String>(assertion) {
                protected void parse() {
                    this.value = SamlConstants.AUTHENTICATION_SAML2_TLS_CERT;
                }
            });
            gen.setAddressResolver( new InetAddressResolver(assertion) {
                protected void parse() {
                    try{
                        this.address = InetAddress.getByName("192.168.1.144");
                    } catch (UnknownHostException badHost) {
                        fail("can't create InetAddress for AddressResolver");
                    }
                }
            });

            return gen;

        } catch (Exception ex) {
            failUnexpected("createMessageGenerator() failed to create Generator: ", ex);
        }
        return null;
    }

    protected void checkCommonRequestElements(AuthzDecisionQueryType request) {
        // ID
        assertNotNull(request.getID());
        assertTrue(request.getID().startsWith("samlp2-"));
        // Version
        assertEquals(SamlpRequestConstants.SAML_VERSION_2_0, request.getVersion());
        // Issue Instant
        assertNotNull(request.getIssueInstant());
        // Optional attributes
        assertEquals("http://consent", request.getConsent());
        assertEquals("http://dest", request.getDestination());
        // Issuer
        assertNotNull(request.getIssuer());
        // Extension
        assertNull(request.getExtensions());
        // Signature
        assertNull(request.getSignature());
    }
}