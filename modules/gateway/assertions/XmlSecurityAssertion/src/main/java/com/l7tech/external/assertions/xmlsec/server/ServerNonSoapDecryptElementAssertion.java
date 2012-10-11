package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.xml.KeyInfoElement;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.XmlElementDecryptor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Triple;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.*;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion.*;

/**
 * Server side implementation of the NonSoapDecryptElementAssertion.
 *
 * @see com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion
 */
public class ServerNonSoapDecryptElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapDecryptElementAssertion> {
    private final SecurityTokenResolver securityTokenResolver;

    public ServerNonSoapDecryptElementAssertion(NonSoapDecryptElementAssertion assertion, BeanFactory beanFactory)
            throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException
    {
        super(assertion);
        this.securityTokenResolver = beanFactory.getBean("securityTokenResolver", SecurityTokenResolver.class);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> elementsToDecrypt) throws Exception {
        final boolean reportContentsOnly = assertion.isReportContentsOnly();

        List<String> algorithms = new ArrayList<String>();
        List<Element> elements = new ArrayList<Element>();
        List<X509Certificate> recipientCerts = new ArrayList<X509Certificate>();
        List<Boolean> contentsOnly = reportContentsOnly ? new ArrayList<Boolean>() : null;

        for (Element encryptedDataEl : elementsToDecrypt) {
            final Functions.UnaryVoid<Throwable> decryptionError = new Functions.UnaryVoid<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    logAndAudit(MessageProcessingMessages.ERROR_XML_DECRYPTION);
                }
            };
            final XmlElementDecryptor.KeyInfoErrorListener keyInfoErrorListener = new XmlElementDecryptor.KeyInfoErrorListener() {
                @Override
                public void onUnsupportedKeyInfoFormat(KeyInfoElement.UnsupportedKeyInfoFormatException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unrecognized KeyInfo format: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                }

                @Override
                public void onInvalidDocumentFormat(InvalidDocumentFormatException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unable to parse KeyInfo: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                }
            };
            final Node encryptedDataParentNode = encryptedDataEl.getParentNode();
            final Element encryptedDataParentElement = encryptedDataParentNode instanceof Element ? (Element)encryptedDataParentNode : null;

            Triple<String, NodeList, X509Certificate> result = XmlElementDecryptor.unwrapDecryptAndReplaceElement(encryptedDataEl, securityTokenResolver, decryptionError, keyInfoErrorListener);
            String algorithm = result.left;
            X509Certificate recipientCert = result.right;
            int numNodes = result.middle.getLength();

            Set<Element> reportedElements = reportContentsOnly ? new HashSet<Element>() : null;
            Set<Node> decryptedNodes = reportContentsOnly ? new HashSet<Node>() : null;

            for (int i = 0; i < numNodes; i++) {
                Node got = result.middle.item(i);
                if (reportContentsOnly)
                    decryptedNodes.add(got);
                if (got instanceof Element) {
                    Element decryptedElement = (Element) got;
                    elements.add(decryptedElement);
                    algorithms.add(algorithm);
                    recipientCerts.add(recipientCert);
                    if (contentsOnly != null)
                        contentsOnly.add(Boolean.FALSE);
                }
            }

            // Check for elements not yet reported whose contents was fully encrypted
            if (reportContentsOnly) {
                for (Node decryptedNode : decryptedNodes) {
                    Node parentNode = decryptedNode.getParentNode();
                    if (!(parentNode instanceof Element))
                        continue; // parent not an Element; we will not report its contents as encrypted
                    Element parent = (Element) parentNode;
                    if (reportedElements.contains(parent))
                        continue; // Already reported this parent element as encrypted (either fully or contents-only)

                    // Ensure that all of parent element's non-Attr children were encrypted
                    boolean atLeastOneKidEncrypted = false;
                    boolean anyKidNotEncrypted = false;
                    NodeList kids = parent.getChildNodes();
                    final int numKids = kids.getLength();
                    for (int i = 0; i < numKids; ++i) {
                        Node kid = kids.item(i);
                        if (kid instanceof Attr) {
                            // Needn't (can't) appear
                            continue;
                        }
                        if (decryptedNodes.contains(kid)) {
                            atLeastOneKidEncrypted = true;
                        } else {
                            anyKidNotEncrypted = true;
                        }
                    }

                    if (atLeastOneKidEncrypted && !anyKidNotEncrypted) {
                        // Report this node's parent element as content-encrypted since all its child nodes were encrypted
                        reportedElements.add(parent);
                        elements.add(parent);
                        algorithms.add(algorithm);
                        recipientCerts.add(recipientCert);
                        contentsOnly.add(Boolean.TRUE);
                    }
                }

                // Final check -- handle the empty element contents case, where eg <password foo="bar"><EncryptedData...></password>
                // gets decrypted to just <password foo="bar"/>.  If an EncryptedData was removed without producing any replacement nodes,
                // we will report its former parent node as content-encrypted if and only if it is now completely empty (aside from attributes).
                if (numNodes == 0 && encryptedDataParentElement != null) {
                    // If encryptedDataParentElement is now completely empty, report it as having been contents-only encrypted.
                    boolean atLeastOneNonAttrKid = false;
                    NodeList kids = encryptedDataParentElement.getChildNodes();
                    int numKids = kids.getLength();
                    for (int i = 0; i < numKids; ++i) {
                        Node kid = kids.item(i);
                        if (!(kid instanceof Attr)) {
                            atLeastOneNonAttrKid = true;
                            break;
                        }
                    }

                    if (!atLeastOneNonAttrKid) {
                        // Report the original parent element as content-encrypted since it contained nothing but the EncryptedData, which when decrypted contained nothing at all.
                        reportedElements.add(encryptedDataParentElement);
                        elements.add(encryptedDataParentElement);
                        algorithms.add(algorithm);
                        recipientCerts.add(recipientCert);
                        contentsOnly.add(Boolean.TRUE);
                    }
                }
            }
        }

        context.setVariable(assertion.prefix(VAR_ELEMENTS_DECRYPTED), elements.toArray(new Element[elements.size()]));
        context.setVariable(assertion.prefix(VAR_ENCRYPTION_METHOD_URIS), algorithms.toArray(new String[algorithms.size()]));
        context.setVariable(assertion.prefix(VAR_RECIPIENT_CERTIFICATES), recipientCerts.toArray(new X509Certificate[recipientCerts.size()]));
        if (reportContentsOnly)
            context.setVariable(assertion.prefix(VAR_CONTENT_ONLY), contentsOnly.toArray(new Boolean[contentsOnly.size()]));

        return AssertionStatus.NONE;
    }
}
