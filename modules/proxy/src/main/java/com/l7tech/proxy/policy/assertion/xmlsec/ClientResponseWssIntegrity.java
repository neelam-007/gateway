package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.ProcessorResultUtil;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

/**
 * Verifies that a specific element of the soap response was signed by the ssg.
 */
public class ClientResponseWssIntegrity extends ClientDomXpathBasedAssertion<WssSignElement> {
    private static final Logger log = Logger.getLogger(ClientResponseWssIntegrity.class.getName());


    public ClientResponseWssIntegrity(WssSignElement data) throws InvalidXpathException {
        super(data);
    }

    @Override
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws GeneralSecurityException,
            OperationCanceledException, BadCredentialsException,
            IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException
    {
        if (data.getRecipientContext().localRecipient()) {
            context.getPendingDecorations().put(this, new ClientDecorator() {
                @Override
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
    @Override
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
        Document soapmsg = responseXml.getDocumentReadOnly();
        ProcessorResult wssRes = ClientResponseWssConfidentiality.getOrCreateWssResults(response);
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

        final Map<String, String> props = context.getSsg().getProperties();
        final boolean requireSingleSignature = !"false".equalsIgnoreCase(props.get("response.security.validateSingleSignature"));
        final boolean validateSigner = !"false".equalsIgnoreCase(props.get("response.security.validateSigningToken"));
        SigningSecurityToken signingToken = null;
        Element signatureElement = null;
        if ( wereSigned != null ) {
            for ( SignedElement signedElement : wereSigned ) {
                if ( signatureElement == null ) {
                    signingToken = signedElement.getSigningSecurityToken();
                    signatureElement = signedElement.getSignatureElement();
                } else if ( requireSingleSignature && signatureElement != signedElement.getSignatureElement() ){
                    throw new ResponseValidationException("Response included multiple signatures.");
                }
            }
        }

        ProcessorResultUtil.SearchResult result;
        try {
            result = ProcessorResultUtil.searchInResult(log,
                                                        soapmsg,
                                                        getCompiledXpath(),
                                                        null,
                                                        false,
                                                        wereSigned,
                                                        "signed");
        } catch (ProcessorException e) {
            throw new PolicyAssertionException(data, e);
        } catch (JaxenException e) {
            throw new PolicyAssertionException(data, e);
        }
        switch (result.getResultCode()) {
            case ProcessorResultUtil.NO_ERROR:
                return validateSigner( context, signingToken, validateSigner );
            case ProcessorResultUtil.FALSIFIED:
                return AssertionStatus.FALSIFIED;
            default:
                return AssertionStatus.SERVER_ERROR;
        }
    }

    private AssertionStatus validateSigner( final PolicyApplicationContext context,
                                            final SigningSecurityToken signingToken,
                                            final boolean validate ) {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        if ( !validate || context.isTrustedSigningToken( signingToken ) ) {
            status = AssertionStatus.NONE;    
        }

        return status;
    }

    private boolean wasElementSigned(ProcessorResult wssResults, Node node) {
        SignedElement[] toto = wssResults.getElementsThatWereSigned();
        for (SignedElement aToto : toto) {
            if (aToto.asElement() == node)
                return true;
        }
        return false;
    }

    @Override
    public String getName() {
        String str = "";
        XpathExpression xpe = data.getXpathExpression();
        if (xpe != null)
            str = " matching XPath expression \"" + xpe.getExpression() + "\"";
        return "Response WSS Integrity: sign elements" + str;
    }

    @Override
    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
