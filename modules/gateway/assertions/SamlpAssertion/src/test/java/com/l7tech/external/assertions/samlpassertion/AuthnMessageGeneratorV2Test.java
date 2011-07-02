package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.external.assertions.samlpassertion.server.*;
import com.l7tech.external.assertions.samlpassertion.server.v2.AuthnRequestGenerator;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.security.saml.SamlConstants;
import saml.v2.protocol.AuthnRequestType;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * User: vchan
 */
public class AuthnMessageGeneratorV2Test extends SamlpMessageGeneratorTestCase<AuthnRequestType> {

    protected final String AUTHN_STATEMENT =
            "<L7p:AuthenticationStatement samlAuthenticationInfo=\"included\">\n" +
                    "    <L7p:AuthenticationMethods stringArrayValue=\"included\"/>\n" +
                    "</L7p:AuthenticationStatement>";

    @Test
    public void testCreateEmptyAuthzRequest() {

        try {
            SamlpRequestBuilderAssertion assertion = getAssertionFromXml(createAssertionXml(TEST_POLICY_TEMPLATE, AUTHN_STATEMENT));
            AbstractSamlp2MessageGenerator<AuthnRequestType> generator = createMessageGenerator(assertion);
            assertNotNull(generator);

            AuthnRequestType result = generator.create(assertion);
            assertNotNull(result);

            checkCommonRequestElements(result);

            assertNotNull(result.getSubject());

            JAXBElement<?> reqElement = generator.createJAXBElement(result);
            System.out.println( toXml(reqElement) );

        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }

    @Override
    protected int getSamlVersion() {
        return 2;
    }

    @Override
    protected AbstractSamlp2MessageGenerator<AuthnRequestType> createMessageGenerator(SamlpRequestBuilderAssertion assertion) {

        try {
            java.util.Map<String, Object> varMap = new HashMap<String, Object>();

            AuthnRequestGenerator gen = new AuthnRequestGenerator(varMap, new TestAudit());

            gen.setNameResolver( new NameIdentifierResolver(assertion) {
                @Override
                protected void parse() {
                    this.nameValue = "somebody@email-exchange.com";
                    this.nameFormat = SamlConstants.NAMEIDENTIFIER_EMAIL;
                }
            });
            gen.setIssuerNameResolver( new NameIdentifierResolver(assertion) {
                @Override
                protected void parse() {
                    this.nameValue = "Bob-the-issuer";
                }
            });
            gen.setAuthnMethodResolver( new MessageValueResolver<String>(assertion) {
                @Override
                protected void parse() {
                    this.value = SamlConstants.AUTHENTICATION_SAML2_TLS_CERT;
                }
            });
            gen.setAddressResolver( new InetAddressResolver(assertion) {
                @Override
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

    @Override
    protected void checkCommonRequestElements(AuthnRequestType request) {
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