package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.security.xml.ElementEncryptionConfig;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.xpath.DeferredFailureDomCompiledXpathHolder;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

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
 */
public class ServerWssEncryptElement extends ServerAddWssEncryption<WssEncryptElement> {
    private final DeferredFailureDomCompiledXpathHolder compiledXpath;

    public ServerWssEncryptElement( final WssEncryptElement data ) throws IOException {
        super(data);
        this.compiledXpath = new DeferredFailureDomCompiledXpathHolder(assertion.getXpathExpression());
    }

    // despite the name of this method, i'm actually working on the response document here
    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        final AddWssEncryptionContext encryptionContext;
        try {
            encryptionContext = buildEncryptionContext( context );
        } catch ( AddWssEncryptionSupport.MultipleTokensException mte ) {
            logAndAudit(AssertionMessages.WSS_ENCRYPT_MORE_THAN_ONE_TOKEN);
            return AssertionStatus.BAD_REQUEST;
        }

        if ( !encryptionContext.hasEncryptionKey() ) {
            logAndAudit(AssertionMessages.WSS_ENCRYPT_NO_CERT_OR_SC_TOKEN);
        }

        return addDecorationRequirements(
                                 message,
                                 messageDescription,
                                 encryptionContext,
                                 context);
    }

    /**
     * Immediately configure response decoration.
     *
     * @param message The message to be encrypted
     * @param messageDescription Description of the message to be encrypted
     * @param encryptionContext The encryption settings
     * @param context  the PolicyEnforcementContext.  Required.
     * @return the AssertionStatus
     * @throws com.l7tech.policy.assertion.PolicyAssertionException  if the XPath expression is invalid
     * @throws java.io.IOException if there is a problem gathering info from the request
     */
    private AssertionStatus addDecorationRequirements(
                                              final Message message,
                                              final String messageDescription,
                                              final AddWssEncryptionContext encryptionContext,
                                              final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        try {
            if (!message.isSoap()) {
                logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        // GET THE DOCUMENT
        final Document soapmsg;
        try {
            soapmsg = message.getXmlKnob().getDocumentReadOnly();

            final List<Element> selectedElements;
            try {
                selectedElements = compiledXpath.getCompiledXpath().rawSelectElements(soapmsg,
                        new PolicyEnforcementContextXpathVariableFinder(context));
            } catch (JaxenException e) {
                logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID_MORE_INFO, new String[] { "XPath evaluation error: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e) );
                return AssertionStatus.SERVER_ERROR;
            }

            if (selectedElements == null || selectedElements.size() < 1) {
                logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_NOT_ENCRYPTED, messageDescription);
                return AssertionStatus.FALSIFIED;
            }
            DecorationRequirements wssReq = message.getSecurityKnob().getAlternateDecorationRequirements(encryptionContext.getRecipientContext());
            for (Element element : selectedElements) {
                wssReq.addElementToEncrypt(element, new ElementEncryptionConfig(assertion.isEncryptContentsOnly()));
            }
            wssReq.setEncryptionAlgorithm(assertion.getXEncAlgorithm());
            wssReq.setKeyEncryptionAlgorithm(assertion.getKeyEncryptionAlgorithm());
            String keyReference = assertion.getKeyReference();
            if ( keyReference == null || KeyReference.SKI.getName().equals(keyReference) ) {
                wssReq.setEncryptionKeyInfoInclusionType(KeyInfoInclusionType.STR_SKI);
            } else if (KeyReference.ISSUER_SERIAL.getName().equals(keyReference)) {
                wssReq.setEncryptionKeyInfoInclusionType(KeyInfoInclusionType.ISSUER_SERIAL);
            } else if (KeyReference.BST.getName().equals(keyReference)) {
                wssReq.setEncryptionKeyInfoInclusionType(KeyInfoInclusionType.CERT);
            } else if (KeyReference.KEY_NAME.getName().equals(keyReference)) {
                wssReq.setEncryptionKeyInfoInclusionType(KeyInfoInclusionType.KEY_NAME);
            }
            applyDecorationRequirements( context, wssReq, encryptionContext );

            logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_ENCRYPTED, messageDescription, String.valueOf(selectedElements.size()));
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to encrypt";
            logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
            return AssertionStatus.SERVER_ERROR;
        }
    }
}
