package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import java.util.logging.Logger;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;

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
        responseWssIntegrity = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    public AssertionStatus decorateRequest(PendingRequest request)
            throws GeneralSecurityException,
            OperationCanceledException, BadCredentialsException,
            IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException
    {
        Ssg ssg = request.getSsg();

        // We'll need to know the server cert in order to check the signature
        if (SsgKeyStoreManager.getServerCert(ssg) == null)
            throw new ServerCertificateUntrustedException("Server cert is needed to check signatures, " +
                                                          "but has not yet been discovered");
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

        WssProcessor.ProcessorResult wssResults = response.getProcessorResult();
        if (wssResults == null) {
            throw new IOException("This response was not processed for WSS level security.");
        }

        XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg, responseWssIntegrity.getXpathExpression().getNamespaces());
        List selectedNodes = null;
        try {
            selectedNodes = evaluator.select(responseWssIntegrity.getXpathExpression().getExpression());
        } catch (JaxenException e) {
            // this is thrown when there is an error in the expression
            // this is therefore a bad policy
            throw new ResponseValidationException(e);
        }

        // the element is not there so there is nothing to check
        if (selectedNodes.isEmpty()) {
            log.info("The element " + responseWssIntegrity.getXpathExpression().getExpression() + " is not present in this response. " +
                     "the assertion therefore succeeds.");
            return AssertionStatus.NONE;
        }

        // to assert this, i must make sure that at least one of these nodes is part of the nodes
        // that were signed as per attesting the wss processor
        for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
            Object node = i.next();
            WssProcessor.SignedElement[] toto = wssResults.getElementsThatWereSigned();
            for (int j = 0; j < toto.length; j++) {
                if (toto[j].asElement() == node) {
                    // we got the bugger!
                    log.fine("The element " + responseWssIntegrity.getXpathExpression().getExpression() + " was found in this " +
                            "response. and is part of the elements that were signed as per the wss processor.");
                    return AssertionStatus.NONE;
                }
            }
        }
        log.info("The element was found in the response but does not appear to be signed. Returning FALSIFIED");
        return AssertionStatus.FALSIFIED;
    }

    public String getName() {
        String str = "";
        XpathExpression xpe = responseWssIntegrity.getXpathExpression();
        if (xpe != null)
            str = " matching XPath expression \"" + xpe.getExpression() + "\"";
        return "Response WSS Integrity: sign elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }


    private ResponseWssIntegrity responseWssIntegrity;
}
