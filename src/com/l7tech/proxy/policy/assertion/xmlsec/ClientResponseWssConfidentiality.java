package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * Verifies that a specific element in the response was encrypted by the ssg.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientResponseWssConfidentiality extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientHttpClientCert.class.getName());

    public ClientResponseWssConfidentiality(ResponseWssConfidentiality data) {
        this.data = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    public AssertionStatus decorateRequest(PendingRequest request)
            throws GeneralSecurityException,
            OperationCanceledException, BadCredentialsException,
            IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException
    {
        // No action required
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
            throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException {
        Document soapmsg = response.getResponseAsDocument();
        ProcessorResult wssRes = response.getProcessorResult();

        ProcessorResultUtil.SearchResult result = null;
        try {
            result = ProcessorResultUtil.searchInResult(log,
                                                        soapmsg,
                                                        data.getXpathExpression().getExpression(),
                                                        data.getXpathExpression().getNamespaces(),
                                                        true,
                                                        wssRes.getElementsThatWereEncrypted(),
                                                        "encrypted");
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
