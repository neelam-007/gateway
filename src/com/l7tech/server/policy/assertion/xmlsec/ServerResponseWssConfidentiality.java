package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.*;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.server.message.PolicyEnforcementContext;
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
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        ProcessorResult wssResult;
        try {
            if (!context.getRequest().isSoap()) {
                logger.info("Request not SOAP; unable to check for WS-Security encrypted elements");
                return AssertionStatus.BAD_REQUEST;
            }
            wssResult = context.getRequest().getXmlKnob().getProcessorResult();
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        if (wssResult == null) {
            logger.info("This request did not contain any WSS level security.");
            context.setPolicyViolated(true);
            return AssertionStatus.FAILED;
        }

        // Ecrypting the Response will require either the presence of a client cert (to encrypt the symmetric key)
        // or a SecureConversation in progress

        X509Certificate clientCert = null;
        SecurityContextToken secConvContext = null;
        SecurityToken[] tokens = wssResult.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token instanceof X509SecurityToken) {
                X509SecurityToken x509token = (X509SecurityToken)token;
                if (x509token.isPossessionProved()) {
                    if (clientCert != null) {
                        logger.log( Level.WARNING, "Request included more than one X509 security token whose key ownership was proven" );
                        return AssertionStatus.BAD_REQUEST; // todo make multiple security tokens work
                    }
                    clientCert = x509token.asX509Certificate();
                }
            } else if (token instanceof SamlSecurityToken) {
                SamlSecurityToken samlToken = (SamlSecurityToken)token;
                if (samlToken.isPossessionProved()) {
                    if (clientCert != null) {
                        logger.log( Level.WARNING, "Request included more than one X509 security token whose key ownership was proven" );
                        return AssertionStatus.BAD_REQUEST; // todo make multiple security tokens work
                    }
                    clientCert = samlToken.getSubjectCertificate();
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
            context.setAuthenticationMissing(true); // todo is it really, though?
            context.setPolicyViolated(true);
            return AssertionStatus.FAILED; // todo verify that this return value is appropriate
        }

        context.addDeferredAssertion(this, deferredDecoration(clientCert, secConvContext));
        return AssertionStatus.NONE;
    }

    private ServerAssertion deferredDecoration(final X509Certificate clientCert,
                                               final SecurityContextToken secConvTok) { // todo what is secConvTok for?
        return new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                try {
                    if (!context.getResponse().isSoap()) {
                        logger.warning("Response not SOAP; unable to encrypt response elements");
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }

                // GET THE DOCUMENT
                Document soapmsg = null;
                try {
                    soapmsg = context.getResponse().getXmlKnob().getDocument(false);

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

                    DecorationRequirements wssReq = context.getResponse().getXmlKnob().getOrMakeDecorationRequirements();
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
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to encrypt";
                    logger.severe(msg);
                    return AssertionStatus.SERVER_ERROR;
                }
            }
        };
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssConfidentiality responseWssConfidentiality;
}
