package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import com.l7tech.proxy.util.ClientLogger;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). May also enforce
 * xml encryption for the body element of the response.
 * <p/>
 * On the server side, this decorates a response with an xml d-sig and maybe xml-enc the response's body
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and decrypts
 * the response's body if necessary.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientResponseWssConfidentiality extends ClientAssertion {
    private static final ClientLogger log = ClientLogger.getInstance(ClientHttpClientCert.class);

    public ClientResponseWssConfidentiality(ResponseWssConfidentiality data) {
        responseWssConfidentiality = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * If this assertion includes xml-enc, the proxy will add a header to the request that tells the server
     * which xml-enc session to use.
     *
     * @param request might receive a header containing the xml-enc session
     * @return AssertionStatus.NONE if we are prepared to handle the eventual response
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException
     *                                    if an updated SSG cert is needed
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if the user cancels the logon dialog
     * @throws com.l7tech.proxy.datamodel.exceptions.BadCredentialsException    if the SSG rejects the SSG username/password when establishing the session
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws GeneralSecurityException,
            OperationCanceledException, BadCredentialsException,
            IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException
    {
        Ssg ssg = request.getSsg();

        // We'll need to know the server cert in order to check the signature
        if (SsgKeyStoreManager.getServerCert(ssg) == null)
            throw new ServerCertificateUntrustedException("Server cert is needed to check signatures, but has not yet been discovered");

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
      throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException {
        Document soapmsg = response.getResponseAsDocument();
        WssProcessor.ProcessorResult wssRes = response.getProcessorResult();
        // verify the appropriate elements were encrypted
        XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg, responseWssConfidentiality.getXpathExpression().getNamespaces());
        List selectedNodes = null;
        try {
            selectedNodes = evaluator.select(responseWssConfidentiality.getXpathExpression().getExpression());
        } catch (JaxenException e) {
            // this is thrown when there is an error in the expression
            // this is therefore a bad policy
            throw new ResponseValidationException(e);
        }

        // the element is not there so there is nothing to check
        if (selectedNodes.isEmpty()) {
            log.info("The element " + responseWssConfidentiality.getXpathExpression().getExpression() + " is not present in this response. " +
                        "the assertion therefore succeeds.");
            return AssertionStatus.NONE;
        }

        // to assert this, i must make sure that at least one of these nodes is part of the nodes
        // that were signed as per attesting the wss processor
        for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
            Object node = i.next();
            Element[] toto = wssRes.getElementsThatWereEncrypted();
            for (int j = 0; j < toto.length; j++) {
                if (toto[j] == node) {
                    // we got the bugger!
                    log.info("The element " + responseWssConfidentiality.getXpathExpression().getExpression() + " was found in this " +
                            "response. and is part of the elements that were encrypted as per the wss processor.");
                    return AssertionStatus.NONE;
                }
            }
        }
        log.info("The element was found in the response but does not appear to be encrypted. Returning FALSIFIED");
        return AssertionStatus.FALSIFIED;
    }

    public String getName() {
        return "XML Response Security - encrypt";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }


    private ResponseWssConfidentiality responseWssConfidentiality;
}
