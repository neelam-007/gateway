package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.common.xml.saml.SamlHolderOfKeyAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Class <code>ServerSamlSecurity</code> represents the server side saml
 * security policy assertion element.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerSamlSecurity implements ServerAssertion {
    private SamlSecurity assertion;
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

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
     * @param request  (In/Out) The request to check.  May be modified by processing.
     * @param response (Out) The response to send back.  May be replaced during processing.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(Request request, Response response)
            throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest)) {
            logger.info("Request is not a SoapRequest; assertion therefore fails");
            return AssertionStatus.NOT_APPLICABLE;
        }
        SoapRequest soapreq = (SoapRequest)request;
        WssProcessor.ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null)
            throw new IOException("This request was not processed for WSS level security.");

        WssProcessor.SecurityToken[] tokens = wssResults.getSecurityTokens();
        if (tokens == null) {
            logger.info("No tokens were processed from this request. Returning AUTH_REQUIRED.");
            response.setAuthenticationMissing(true);
            return AssertionStatus.AUTH_REQUIRED;
        }

        SamlAssertion samlAssertion = null;
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken tok = tokens[i];
            if (tok instanceof WssProcessor.SamlSecurityToken) {
                WssProcessor.SamlSecurityToken samlToken = (WssProcessor.SamlSecurityToken)tok;
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
            if (!(samlAssertion instanceof SamlHolderOfKeyAssertion)) {
                logger.warning("We got a request that presented a valid signature from a SAML assertion, but it was not a Holder-of-Key assertion.");
                return AssertionStatus.BAD_REQUEST;
            }
            SamlHolderOfKeyAssertion hok = (SamlHolderOfKeyAssertion)samlAssertion;

            // Check expiry date
            Calendar expires = hok.getExpires();
            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            if (!expires.after(now)) {
                logger.warning("SAML holder-of-key assertion in this request has expired; assertion therefore fails.");
                return AssertionStatus.AUTH_FAILED;
            }

            // Save pincipal credential for later authentication
            X500Name x500name = new X500Name(hok.getSubjectCertificate().getSubjectX500Principal().getName());
            String subjectCN = x500name.getCommonName();
            request.setPrincipalCredentials(new LoginCredentials(subjectCN,
                                                                 null,
                                                                 CredentialFormat.SAML,
                                                                 assertion.getClass(),
                                                                 null,
                                                                 samlAssertion));
            logger.fine("SAML holder-of-key assertion loaded as principal credential for CN:" + subjectCN);
            return AssertionStatus.NONE;
        }

        logger.info("This assertion did not find a proven SAML assertion to use as credentials. Returning AUTH_REQUIRED.");
        response.setAuthenticationMissing(true);
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
