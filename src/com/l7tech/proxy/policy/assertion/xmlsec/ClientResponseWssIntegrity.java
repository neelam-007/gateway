package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.ProcessorResultUtil;
import com.l7tech.common.security.xml.processor.SignedElement;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
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

    public AssertionStatus decorateRequest(PendingRequest request)
            throws GeneralSecurityException,
            OperationCanceledException, BadCredentialsException,
            IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException
    {
        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request) throws InvalidDocumentFormatException {
                log.log(Level.FINER, "Expecting a signed reply; will be sure to include L7a:MessageID");
                request.prepareWsaMessageId();
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    /**
     * validate the signature of the response by the ssg server
     *
     * @param request
     * @param response
     * @return
     */
    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)
            throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, InvalidDocumentFormatException, PolicyAssertionException {
        Document soapmsg = response.getResponseAsDocument();
        ProcessorResult wssRes = response.getProcessorResult();

        String sentMessageId = request.getL7aMessageId();
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
            request.setL7aMessageId(null);
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
