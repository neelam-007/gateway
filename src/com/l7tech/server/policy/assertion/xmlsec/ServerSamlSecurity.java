package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.saml.SamlAssertionFaultDetail;
import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Class <code>ServerSamlSecurity</code> represents the server side saml
 * security policy assertion element.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerSamlSecurity implements ServerAssertion {
    private SamlSecurity assertion;

    /**
     * Create the server side saml security policy element
     * 
     * @param sa the saml
     */
    public ServerSamlSecurity(SamlSecurity sa) {
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        assertion = sa;
    }

    /**
     * SSG Server-side processing of the given request.
     * 
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
                                    throws IOException, PolicyAssertionException {
        if (!assertion.getRecipientContext().localRecipient()) {
            logger.fine("This is intended for another recipient, there is nothing to validate here.");
            return AssertionStatus.NONE;
        }
        ProcessorResult wssResults;
        try {
            if (!context.getRequest().isSoap()) {
                logger.info("Request not SOAP; cannot check for SAML assertion");
                return AssertionStatus.NOT_APPLICABLE;
            }
            wssResults = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
        if (wssResults == null) {
            logger.info("This request did not contain any WSS level security.");
            context.setAuthenticationMissing();
            context.setRequestPolicyViolated();
            return AssertionStatus.FALSIFIED;
        }

        SecurityToken[] tokens = wssResults.getSecurityTokens();
        if (tokens == null) {
            logger.info("No tokens were processed from this request. Returning AUTH_REQUIRED.");
            context.setAuthenticationMissing();
            return AssertionStatus.AUTH_REQUIRED;
        }

        SamlAssertion samlAssertion = null;
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken tok = tokens[i];
            if (tok instanceof SamlSecurityToken) {
                SamlSecurityToken samlToken = (SamlSecurityToken)tok;
                if (samlToken.isPossessionProved()) {
                    SamlAssertion gotAss = samlToken.asSamlAssertion();
                    if (samlAssertion != null) {
                        logger.severe("We got a request that presented more than one valid signature from more " +
                                      "than one SAML assertion.  This is not currently supported.");
                        return AssertionStatus.BAD_REQUEST;
                    }
                    samlAssertion = gotAss;
                }
            }
        }

        if (samlAssertion != null) {
            // check that the right type of assertion is present
            if (assertion.getConfirmationMethodType() == SamlSecurity.CONFIRMATION_METHOD_HOLDER_OF_KEY) {
                if (!(samlAssertion.isHolderOfKey())) {
                    logger.warning("We got a request that presented a valid signature from a SAML assertion, but " +
                                   "it was not a Holder-of-Key assertion. " +
                                   "This policy assertion requires hok type of confirmation.");
                    context.setFaultDetail(new SamlAssertionFaultDetail(samlAssertion.getAssertionId()));
                    return AssertionStatus.BAD_REQUEST;
                }
            } else if (assertion.getConfirmationMethodType() == SamlSecurity.CONFIRMATION_METHOD_SENDER_VOUCHES) {
                if (samlAssertion.isHolderOfKey()) {
                    logger.warning("We got a request that presented a valid signature from a SAML assertion, but " +
                                   "it was not a Sender-vouches assertion. " +
                                   "This policy assertion requires sv type of confirmation.");
                    context.setFaultDetail(new SamlAssertionFaultDetail(samlAssertion.getAssertionId()));
                    return AssertionStatus.BAD_REQUEST;
                }
            }

            // Check expiry date
            if (samlAssertion.isExpiringSoon(0)) {
                final String msg = "SAML holder-of-key assertion in this request has expired.";
                logger.warning(msg);
                context.setFaultDetail(new SamlAssertionFaultDetail(samlAssertion.getAssertionId(),
                                                                    msg,
                                                                    SecureSpanConstants.FAULTCODE_INVALIDSECURITYTOKEN,
                                                                    SecureSpanConstants.FAULTDETAIL_SAML_REASON_CONDITION));
                return AssertionStatus.AUTH_FAILED;
            }
            // TODO check for not-yet-valid ticket

            // Save pincipal credential for later authentication
            X500Name x500name = new X500Name(samlAssertion.getSubjectCertificate().getSubjectX500Principal().getName());
            String subjectCN = x500name.getCommonName();
            context.setCredentials(new LoginCredentials(subjectCN,
                                                        null,
                                                        CredentialFormat.SAML,
                                                        assertion.getClass(),
                                                        null,
                                                        samlAssertion));
            logger.fine("SAML holder-of-key assertion loaded as principal credential for CN:" + subjectCN);
            return AssertionStatus.NONE;
        }

        logger.info("This assertion did not find a proven SAML assertion to use as credentials. Returning AUTH_REQUIRED.");
        context.setAuthenticationMissing();
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
