package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.message.Message;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Verifies that a specific element of the soap response was signed by the ssg.
 */
public class ClientResponseWssIntegrity extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientResponseWssIntegrity.class.getName());

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
                    context.prepareWsaMessageId(false, null, null);
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
     */
    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, InvalidDocumentFormatException, PolicyAssertionException
    {
        if (!data.getRecipientContext().localRecipient()) {
            log.fine("This is intended for another recipient, there is nothing to validate here.");
            return AssertionStatus.NONE;
        }
        
        final Message response = context.getResponse();
        if (!response.isSoap()) {
            log.info("Response is not SOAP; response integrity is therefore not applicable");
            return AssertionStatus.NOT_APPLICABLE;
        }
        final XmlKnob responseXml = response.getXmlKnob();
        final SecurityKnob responseSec = response.getSecurityKnob();
        Document soapmsg = responseXml.getDocumentReadOnly();
        ProcessorResult wssRes = responseSec.getProcessorResult();
        if (wssRes == null) {
            log.info("WSS processing was not done on this response.");
            return AssertionStatus.FAILED;
        }

        String sentMessageId = context.getMessageId();
        SignedElement[] wereSigned = wssRes.getElementsThatWereSigned();
        if (sentMessageId != null) {
            String receivedRelatesTo = context.isUseWsaMessageId() ?
                    SoapUtil.getWsaRelatesTo(soapmsg) :
                    SoapUtil.getL7aRelatesTo(soapmsg);

            final String wsaPrefix = context.isUseWsaMessageId() ? "wsa" : "L7a";
            log.log(Level.FINEST, MessageFormat.format("Response included {0}:RelatesTo of \"{1}\"", wsaPrefix, receivedRelatesTo));
            if (receivedRelatesTo != null) {
                if (!sentMessageId.equals(receivedRelatesTo.trim()))
                    throw new ResponseValidationException(MessageFormat.format("Response does not include {0}:RelatesTo matching {0}:MessageID from request", wsaPrefix));

                final Element relatesToEl = context.isUseWsaMessageId() ?
                        SoapUtil.getWsaRelatesToElement(soapmsg) :
                        SoapUtil.getL7aRelatesToElement(soapmsg);

                if (wereSigned != null && wereSigned.length > 0 && !wasElementSigned(wssRes, relatesToEl))
                    throw new ResponseValidationException(MessageFormat.format("Response included a matching {0}:RelatesTo, but it was not signed", wsaPrefix));
            }

            // Skip this check on subsequent ResponseWssIntegrity assertions.
            context.setMessageId(null);
        }

        ProcessorResultUtil.SearchResult result = null;
        try {
            result = ProcessorResultUtil.searchInResult(log,
                                                        soapmsg,
                                                        data.getXpathExpression().getExpression(),
                                                        data.getXpathExpression().getNamespaces(),
                                                        false,
                                                        wereSigned,
                                                        "signed");
        } catch (ProcessorException e) {
            throw new PolicyAssertionException(data, e);
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
