package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.common.security.xml.WssProcessor;

import java.io.IOException;
import java.util.logging.Logger;

import sun.security.x509.X500Name;

/**
 * This assertion verifies that the message contained an
 * xml digital signature. The cert used for the signature is
 * recorded in request.setPrincipalCredentials.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class ServerRequestWssX509Cert implements ServerAssertion {
    public ServerRequestWssX509Cert(RequestWssX509Cert subject) {
        this.subject = subject;
    }
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest)) {
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP type of messages");
        }
        SoapRequest soapreq = (SoapRequest)request;
        WssProcessor.ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null) {
            throw new PolicyAssertionException("This request was not processed for WSS level security.");
        }

        WssProcessor.SecurityToken[] tokens = wssResults.getSecurityTokens();
        if (tokens == null) {
            logger.info("No tokens were processed from this request. Returning AUTH_REQUIRED.");
            return AssertionStatus.AUTH_REQUIRED;
        }
        for (int i = 0; i < tokens.length; i++) {
            WssProcessor.SecurityToken tok = tokens[i];
            if (tok instanceof WssProcessor.X509SecurityToken) {
                WssProcessor.X509SecurityToken x509Tok = (WssProcessor.X509SecurityToken)tok;
                if (x509Tok.isPossessionProved()) {
                    X500Name x500name = new X500Name(x509Tok.asX509Certificate().getSubjectX500Principal().getName());
                    String certCN = x500name.getCommonName();
                    request.setPrincipalCredentials(new LoginCredentials(certCN,
                                                                         null,
                                                                         CredentialFormat.CLIENTCERT,
                                                                         null,
                                                                         x509Tok.asX509Certificate()));
                    logger.fine("Cert loaded as principal credential for CN:" + certCN);
                    // todo, it is possible that a request has more than one signature by more than one
                    // identity. we should refactor request.setPrincipalCredentials to be able to remember
                    // more than one proven identity.
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.info("This assertion did not find a proven x509 cert to use as credentials. Returning AUTH_REQUIRED.");
        return AssertionStatus.AUTH_REQUIRED;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private RequestWssX509Cert subject;
}
