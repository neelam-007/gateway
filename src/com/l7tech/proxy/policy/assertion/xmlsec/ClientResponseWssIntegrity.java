package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Verifies that a specific element of the soap response was signed by the ssg.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientResponseWssIntegrity extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientHttpClientCert.class.getName());

    public ClientResponseWssIntegrity(ResponseWssIntegrity data) {
        this.data = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws GeneralSecurityException,
            OperationCanceledException, BadCredentialsException,
            IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException
    {
        if (data.getRecipientContext().localRecipient()) {
            context.getPendingDecorations().put(this, new ClientDecorator() {
                public AssertionStatus decorateRequest(PolicyApplicationContext context) throws InvalidDocumentFormatException, IOException, SAXException {
                    log.log(Level.FINER, "Expecting a signed reply; will be sure to include L7a:MessageID");
                    context.prepareWsaMessageId();
                    return AssertionStatus.NONE;
                }
            });
        }
        return AssertionStatus.NONE;
    }

    /**
     * validate the signature of the response by the ssg server
     *
     * @param context
     * @return
     */
    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, InvalidDocumentFormatException, PolicyAssertionException
    {
        if (!data.getRecipientContext().localRecipient()) {
            log.fine("This is intended for another recipient, there is nothing to validate here.");
            return AssertionStatus.NONE;
        }
        
        final Message response = context.getResponse();
        final XmlKnob responseXml = response.getXmlKnob();
        Document soapmsg = responseXml.getDocumentReadOnly();
        ProcessorResult wssRes = responseXml.getProcessorResult();
        if (wssRes == null) {
            log.info("WSS processing was not done on this response.");
            return AssertionStatus.FAILED;
        }

        String sentMessageId = context.getL7aMessageId();
        if (sentMessageId != null) {
            String receivedRelatesTo = SoapUtil.getL7aRelatesTo(soapmsg);
            log.log(Level.FINEST, "Response included L7a:RelatesTo of \"" + receivedRelatesTo + "\"");
            if (receivedRelatesTo != null) {
                if (!sentMessageId.equals(receivedRelatesTo.trim()))
                    throw new ResponseValidationException("Response does not include L7a:RelatesTo matching L7a:MessageID from request");
                if (!wasElementSigned(wssRes, SoapUtil.getL7aRelatesToElement(soapmsg)))
                    throw new ResponseValidationException("Response included a matching L7a:RelatesTo, but it was not signed");

            }

            // Skip this check on subsequent ResponseWssIntegrity assertions.
            context.setL7aMessageId(null);
        }

        ProcessorResultUtil.SearchResult result = null;
        try {
            result = ProcessorResultUtil.searchInResult(log,
                                                        soapmsg,
                                                        data.getXpathExpression().getExpression(),
                                                        data.getXpathExpression().getNamespaces(),
                                                        false,
                                                        wssRes.getElementsThatWereSigned(),
                                                        "signed");
        } catch (ProcessorException e) {
            throw new PolicyAssertionException(e);
        }
        switch (result.getResultCode()) {
            case ProcessorResultUtil.NO_ERROR:
                return AssertionStatus.NONE;
            case ProcessorResultUtil.FALSIFIED:
                return AssertionStatus.FALSIFIED;
            default:
                return AssertionStatus.SERVER_ERROR;
        }
    }

    private boolean wasElementSigned(ProcessorResult wssResults, Node node) {
        SignedElement[] toto = wssResults.getElementsThatWereSigned();
        for (int j = 0; j < toto.length; j++) {
            if (toto[j].asElement() == node)
                return true;
        }
        return false;
    }

    public String getName() {
        String str = "";
        XpathExpression xpe = data.getXpathExpression();
        if (xpe != null)
            str = " matching XPath expression \"" + xpe.getExpression() + "\"";
        return "Response WSS Integrity: sign elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }


    private ResponseWssIntegrity data;
}
