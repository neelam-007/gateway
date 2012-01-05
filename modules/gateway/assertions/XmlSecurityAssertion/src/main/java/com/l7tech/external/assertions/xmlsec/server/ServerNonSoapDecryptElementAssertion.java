package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.xml.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidXpathException;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

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
        List<String> algorithms = new ArrayList<String>();
        List<Element> elements = new ArrayList<Element>();
        List<X509Certificate> recipientCerts = new ArrayList<X509Certificate>();

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
            Triple<String, NodeList, X509Certificate> result = XmlElementDecryptor.decryptAndReplaceElement(encryptedDataEl, securityTokenResolver, decryptionError, keyInfoErrorListener);
            String algorithm = result.left;
            X509Certificate recipientCert = result.right;
            int numNodes = result.middle.getLength();
            for (int i = 0; i < numNodes; i++) {
                Node got = result.middle.item(i);
                if (got instanceof Element) {
                    Element decryptedElement = (Element) got;
                    elements.add(decryptedElement);
                    algorithms.add(algorithm);
                    recipientCerts.add(recipientCert);
                }
            }
        }

        context.setVariable(assertion.prefix(VAR_ELEMENTS_DECRYPTED), elements.toArray(new Element[elements.size()]));
        context.setVariable(assertion.prefix(VAR_ENCRYPTION_METHOD_URIS), algorithms.toArray(new String[algorithms.size()]));
        context.setVariable(assertion.prefix(VAR_RECIPIENT_CERTIFICATES), recipientCerts.toArray(new X509Certificate[recipientCerts.size()]));

        return AssertionStatus.NONE;
    }
}
