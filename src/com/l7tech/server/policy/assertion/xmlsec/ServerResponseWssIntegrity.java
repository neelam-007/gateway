package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this schedules decoration of a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerResponseWssIntegrity implements ServerAssertion {
    private SignerInfo signerInfo;

    public ServerResponseWssIntegrity(ResponseWssIntegrity data, ApplicationContext ctx) throws IOException {
        responseWssIntegrity = data;
        KeystoreUtils ku = (KeystoreUtils)ctx.getBean("keystore");
        signerInfo = ku.getSslSignerInfo();
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     * @param context
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        try {
            if (!context.getRequest().isSoap()) {
                logger.info("Request not SOAP; cannot verify WS-Security signature");
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        final XmlSecurityRecipientContext recipient = responseWssIntegrity.getRecipientContext();

        context.addDeferredAssertion(this, new ServerAssertion() {
            public AssertionStatus checkRequest(PolicyEnforcementContext context)
                    throws IOException, PolicyAssertionException
            {
                try {
                    if (!context.getResponse().isSoap()) {
                        logger.warning("Response not SOAP; cannot apply WS-Security signature");
                        return AssertionStatus.NOT_APPLICABLE;
                    }
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }


                // GET THE DOCUMENT
                Document soapmsg = null;
                try {
                    soapmsg = context.getResponse().getXmlKnob().getDocumentReadOnly();
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to sign";
                    logger.severe(msg);
                    return AssertionStatus.SERVER_ERROR;
                }

                XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg,
                                                                       responseWssIntegrity.getXpathExpression().getNamespaces());
                List selectedElements = null;
                try {
                    selectedElements = evaluator.selectElements(responseWssIntegrity.getXpathExpression().getExpression());
                } catch (JaxenException e) {
                    // this is thrown when there is an error in the expression
                    // this is therefore a bad policy
                    throw new PolicyAssertionException(e);
                }

                if (selectedElements == null || selectedElements.size() < 1) {
                    logger.fine("No matching elements to sign in response.  Returning success.");
                    return AssertionStatus.NONE;
                }

                DecorationRequirements wssReq = null;
                try {
                    if (!recipient.localRecipient()) {
                        wssReq = context.getResponse().getXmlKnob().getAlternateDecorationRequirements(recipient);
                    } else {
                        wssReq = context.getResponse().getXmlKnob().getOrMakeDecorationRequirements();
                    }
                } catch (SAXException e) {
                    throw new RuntimeException(e); // can't happen, we did this before successfully
                } catch (CertificateException e) {
                    String msg = "cannot set the recipient cert.";
                    logger.severe(msg);
                    return AssertionStatus.SERVER_ERROR;
                }
                wssReq.setSenderMessageSigningCertificate(signerInfo.getCertificateChain()[0]);
                wssReq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
                wssReq.getElementsToSign().addAll(selectedElements);
                wssReq.setSignTimestamp();
                logger.fine("Designated " + selectedElements.size() + " response elements for signing");

                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssIntegrity responseWssIntegrity;
}
