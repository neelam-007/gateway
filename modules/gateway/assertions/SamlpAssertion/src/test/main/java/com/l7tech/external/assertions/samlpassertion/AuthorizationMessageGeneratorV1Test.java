package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp1MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.InetAddressResolver;
import com.l7tech.external.assertions.samlpassertion.server.MessageValueResolver;
import com.l7tech.external.assertions.samlpassertion.server.NameIdentifierResolver;
import com.l7tech.external.assertions.samlpassertion.server.v1.AuthorizationDecisionQueryGenerator;
import com.l7tech.server.audit.Auditor;
import saml.v1.protocol.AuthorizationDecisionQueryType;
import saml.v1.protocol.RequestType;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * User: vchan
 */
public class AuthorizationMessageGeneratorV1Test extends SamlpMessageGeneratorTest<RequestType> {


    public void testCreateEmptyAuthzRequest() {

        try {
            SamlpRequestBuilderAssertion assertion = getAssertionFromXml(TEST_POLICY_SIMPLE);
            AbstractSamlp1MessageGenerator<AuthorizationDecisionQueryType> generator = createMessageGenerator(assertion);
            assertNotNull(generator);

            RequestType result = generator.create(assertion);
            assertNotNull(result);

            checkCommonRequestElements(result);

            assertNotNull(result.getAuthorizationDecisionQuery());
            assertNotNull(result.getAuthorizationDecisionQuery().getSubject());

            JAXBElement<?> reqElement = generator.createJAXBElement(result);
            System.out.println( toXml(reqElement) );

        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }

    protected int getSamlVersion() {
        return 1;
    }

    protected AbstractSamlp1MessageGenerator<AuthorizationDecisionQueryType> createMessageGenerator(SamlpRequestBuilderAssertion assertion) {

        try {
            Auditor auditor = new Auditor(this, appCtx, null);
            java.util.Map<String, Object> varMap = new HashMap<String, Object>();

            AuthorizationDecisionQueryGenerator gen = new AuthorizationDecisionQueryGenerator(varMap, auditor);

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
//            gen.setClientCertResolver( new MessageValueResolver<X509Certificate>(assertion) {
//
//                protected void parse() {
//
//                }
//            });
            gen.setAuthnMethodResolver( new MessageValueResolver<String>(assertion) {
                protected void parse() {
                    this.value = SamlConstants.AUTHENTICATION_SAML2_TLS_CERT;
                }
            });
            gen.setAddressResolver( new InetAddressResolver(assertion) {
                protected void parse() {
                    try {
                        this.address = InetAddress.getByName("vinsanity.l7tech.com");
                    } catch (UnknownHostException uhex) {
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

    protected void checkCommonRequestElements(RequestType request) {
        // more to come
        // ID
        assertNotNull(request.getRequestID());
        assertTrue(request.getRequestID().startsWith("samlp-"));
        // Version
        assertEquals(BigInteger.valueOf(1), request.getMajorVersion());
        assertEquals(BigInteger.valueOf(1), request.getMinorVersion());
        // Issue Instant
        assertNotNull(request.getIssueInstant());
        // Signature
        assertNull(request.getSignature());
    }
}