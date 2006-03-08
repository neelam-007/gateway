package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.security.token.EncryptedElement;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Verifies that a specific element in the response was encrypted by the ssg.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientResponseWssConfidentiality extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientResponseWssConfidentiality.class.getName());

    public ClientResponseWssConfidentiality(ResponseWssConfidentiality data) {
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
        // No action required
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException {
        if (!data.getRecipientContext().localRecipient()) {
            log.fine("This is intended for another recipient, there is nothing to validate here.");
            return AssertionStatus.NONE;
        }
        Document soapmsg = context.getResponse().getXmlKnob().getDocumentReadOnly();
        ProcessorResult wssRes = context.getResponse().getSecurityKnob().getProcessorResult();
        if (wssRes == null) {
            log.info("WSS processing was not done on this response.");
            return AssertionStatus.FAILED;
        }

        ProcessorResultUtil.SearchResult result;
        try {
            EncryptedElement[] elementsThatWereEncrypted = wssRes.getElementsThatWereEncrypted();
            String xEncAlgorithm = data.getXEncAlgorithm();

            if (xEncAlgorithm !=null) {
                Collection filteredElements = new ArrayList();
                for (int i = elementsThatWereEncrypted.length - 1; i >= 0; i--) {
                    EncryptedElement encryptedElement = elementsThatWereEncrypted[i];
                    if (xEncAlgorithm.equals(encryptedElement.getAlgorithm())) {
                        filteredElements.add(encryptedElement);
                    }
                }
                elementsThatWereEncrypted = (EncryptedElement[])filteredElements.toArray(new EncryptedElement[] {});
            }
            result = ProcessorResultUtil.searchInResult(log,
                                                        soapmsg,
                                                        data.getXpathExpression().getExpression(),
                                                        data.getXpathExpression().getNamespaces(),
                                                        true,
                                                        elementsThatWereEncrypted,
                                                        "encrypted");
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

    public String getName() {
        String str = "";
        XpathExpression xpe = data.getXpathExpression();
        if (xpe != null)
            str = " matching XPath expression \"" + xpe.getExpression() + "\"";
        return "Response WSS Confidentiality: encrypt elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    private ResponseWssConfidentiality data;
}
