package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.SecurityContextToken;
import com.l7tech.common.security.xml.processor.SecurityToken;
import com.l7tech.common.security.xml.processor.X509SecurityToken;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML encryption on the soap response sent from the ssg server to the requestor (probably proxy).
 * <p/>
 * On the server side, this schedules decoration of a response with an xml encryption.
 * On the proxy side, this verifies that the Soap Response contains a valid xml encryption for the elements.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerResponseWssConfidentiality implements ServerAssertion {

    public ServerResponseWssConfidentiality(ResponseWssConfidentiality data) {
        responseWssConfidentiality = data;
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     */
    public AssertionStatus checkRequest(Request request, Response response)
            throws IOException, PolicyAssertionException
    {
        if (!(request instanceof SoapRequest))
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP requests");
        SoapRequest soapRequest = (SoapRequest)request;

        // Ecrypting the Response will require either the presence of a client cert (to encrypt the symmetric key)
        // or a SecureConversation in progress

        X509Certificate clientCert = null;
        SecurityContextToken secConvContext = null;
        ProcessorResult wssResult = soapRequest.getWssProcessorOutput();
        SecurityToken[] tokens = wssResult.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token instanceof X509SecurityToken) {
                X509SecurityToken x509token = (X509SecurityToken)token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        logger.log( Level.WARNING, "Request included more than one X509 security token whose key ownership was proven" );
                        return AssertionStatus.BAD_REQUEST; // todo verify that this return value is appropriate
                    }
                    clientCert = x509token.asX509Certificate();
                }
            } else if (token instanceof SecurityContextToken) {
                SecurityContextToken secConvTok = (SecurityContextToken)token;
                if (secConvTok.isPossessionProved()) {
                    secConvContext = secConvTok;
                }
            }
        }

        if (clientCert == null && secConvContext == null) {
            logger.log( Level.WARNING, "Unable to encrypt response. Request did not included x509 " +
                                       "token or secure conversation." );
            response.setAuthenticationMissing(true); // todo is it really, though?
            response.setPolicyViolated(true);
            return AssertionStatus.FAILED; // todo verify that this return value is appropriate
        }

        response.addDeferredAssertion(this, defferedDecoration(clientCert, secConvContext));
        return AssertionStatus.NONE;
    }

    private ServerAssertion defferedDecoration(final X509Certificate clientCert,
                                               final SecurityContextToken secConvTok) {
        return new ServerAssertion() {
            public AssertionStatus checkRequest(Request request, Response response)
                    throws IOException, PolicyAssertionException
            {
                if (!(response instanceof SoapResponse))
                    throw new PolicyAssertionException("This type of assertion is only supported with SOAP responses");
                SoapResponse soapResponse = (SoapResponse)response;

                // GET THE DOCUMENT
                Document soapmsg = null;
                try {
                    soapmsg = soapResponse.getDocument();
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to encrypt";
                    logger.severe(msg);
                    return AssertionStatus.SERVER_ERROR;
                }

                final XpathExpression xpath = responseWssConfidentiality.getXpathExpression();
                XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg,
                                                                       xpath.getNamespaces());
                List selectedElements = null;
                try {
                    selectedElements = evaluator.selectElements(xpath.getExpression());
                } catch (JaxenException e) {
                    // this is thrown when there is an error in the expression
                    // this is therefore a bad policy
                    throw new PolicyAssertionException(e);
                }

                if (selectedElements == null || selectedElements.size() < 1) {
                    logger.fine("No matching elements to encrypt in response.  Returning success.");
                    return AssertionStatus.NONE;
                }

                DecorationRequirements wssReq = soapResponse.getOrMakeDecorationRequirements();
                wssReq.getElementsToEncrypt().addAll(selectedElements);

                if (clientCert != null) {
                    SignerInfo si = KeystoreUtils.getInstance().getSignerInfo();
                    wssReq.setSenderCertificate(si.getCertificateChain()[0]);
                    wssReq.setSenderPrivateKey(si.getPrivate());
                    wssReq.setRecipientCertificate(clientCert);
                    wssReq.setSignTimestamp(true);
                }

                logger.finest("Designated " + selectedElements.size() + " response elements for encryption");

                return AssertionStatus.NONE;
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssConfidentiality responseWssConfidentiality;
}
