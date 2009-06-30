package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.util.xml.PolicyEnforcementContextXpathVariableFinder;
import com.l7tech.util.CausedIOException;
import com.l7tech.xml.xpath.DeferredFailureDomCompiledXpathHolder;
import com.l7tech.message.Message;
import org.jaxen.JaxenException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
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
 */
public class ServerWssEncryptElement extends ServerAddWssEncryption<WssEncryptElement> {

    private static final Logger logger = Logger.getLogger(ServerWssEncryptElement.class.getName());
    private final Auditor auditor;
    private final DeferredFailureDomCompiledXpathHolder compiledXpath;

    public ServerWssEncryptElement( final WssEncryptElement data, final ApplicationContext ctx) throws IOException {
        super(data, data, data, data, logger);
        this.auditor = new Auditor(this, ctx, logger);
        this.compiledXpath = new DeferredFailureDomCompiledXpathHolder(assertion.getXpathExpression());
    }

    // despite the name of this method, i'm actually working on the response document here
    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        final EncryptionContext encryptionContext;
        try {
            encryptionContext = buildEncryptionContext( context );
        } catch ( MultipleTokensException mte ) {
            auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MORE_THAN_ONE_TOKEN);
            return AssertionStatus.BAD_REQUEST;
        }

        if ( !encryptionContext.hasEncryptionKey() ) {
            auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_NO_CERT_OR_SC_TOKEN);
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
                                              final EncryptionContext encryptionContext,
                                              final PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException
    {
        try {
            if (!message.isSoap()) {
                auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        // GET THE DOCUMENT
        final Document soapmsg;
        try {
            soapmsg = message.getXmlKnob().getDocumentReadOnly();

            final List selectedElements;
            try {
                selectedElements = compiledXpath.getCompiledXpath().rawSelectElements(soapmsg,
                        new PolicyEnforcementContextXpathVariableFinder(context));
            } catch (JaxenException e) {
                // this is thrown when there is an error in the expression
                // this is therefore a bad policy
                throw new PolicyAssertionException(assertion, e);
            }

            if (selectedElements == null || selectedElements.size() < 1) {
                auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_NOT_ENCRYPTED, messageDescription);
                return AssertionStatus.FALSIFIED;
            }
            DecorationRequirements wssReq = message.getSecurityKnob().getAlternateDecorationRequirements(encryptionContext.getRecipientContext());
            //noinspection unchecked
            wssReq.getElementsToEncrypt().addAll(selectedElements);
            wssReq.setEncryptionAlgorithm(assertion.getXEncAlgorithm());
            wssReq.setKeyEncryptionAlgorithm(assertion.getKeyEncryptionAlgorithm());
            applyDecorationRequirements( context, wssReq, encryptionContext );

            auditor.logAndAudit(AssertionMessages.WSS_ENCRYPT_MESSAGE_ENCRYPTED, messageDescription, String.valueOf(selectedElements.size()));
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to encrypt";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, e);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }
}
