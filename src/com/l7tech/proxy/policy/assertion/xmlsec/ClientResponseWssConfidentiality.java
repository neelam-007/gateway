package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
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
        responseWssConfidentiality = data;
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
      throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException {
        Document soapmsg = response.getResponseAsDocument();
        WssProcessor.ProcessorResult wssRes = response.getProcessorResult();
        // verify the appropriate elements were encrypted
        XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg, responseWssConfidentiality.getXpathExpression().getNamespaces());
        List selectedNodes = null;
        final String xpath = responseWssConfidentiality.getXpathExpression().getExpression();
        try {
            selectedNodes = evaluator.select(xpath);
        } catch (JaxenException e) {
            // this is thrown when there is an error in the expression
            // this is therefore a bad policy
            throw new ResponseValidationException(e);
        }

        // the element is not there so there is nothing to check
        if (selectedNodes.isEmpty()) {
            log.info("The element " + xpath + " is not present in this response. " +
                        "the assertion therefore succeeds.");
            return AssertionStatus.NONE;
        }

        // to assert this, i must make sure that at least one of these nodes is part of the nodes
        // that were signed as per attesting the wss processor
        for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (!(obj instanceof Node)) {
                log.finest("Ignoring non-Node result of type " + obj.getClass().getName());
                continue;
            }

            Node node = (Node)obj;
            if (XmlUtil.elementIsEmpty(node)) {
                log.finest("The element " + xpath + " was found in this response, but was empty and so needn't be encrypted.");
                // TODO we currently short-circuit success as soon as ANY element is found.  We must check them all!
                return AssertionStatus.NONE;
            }

            Element[] toto = wssRes.getElementsThatWereEncrypted();
            for (int j = 0; j < toto.length; j++) {
                if (toto[j] == node) {
                    // we got the bugger!
                    log.fine("The element " + xpath + " was found in this " +
                            "response. and is part of the elements that were encrypted as per the wss processor.");
                    // TODO we currently short-circuit success as soon as ANY element is found.  We must check them all!
                    return AssertionStatus.NONE;
                }
            }
        }
        log.info("The element was found in the response but does not appear to be encrypted. Returning FALSIFIED");
        return AssertionStatus.FALSIFIED;
    }

    public String getName() {
        String str = "";
        XpathExpression xpe = responseWssConfidentiality.getXpathExpression();
        if (xpe != null)
            str = " matching XPath expression \"" + xpe.getExpression() + "\"";
        return "Response WSS Confidentiality: encrypt elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }


    private ResponseWssConfidentiality responseWssConfidentiality;
}
