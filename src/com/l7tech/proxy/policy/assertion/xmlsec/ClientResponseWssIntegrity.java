package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
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
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.List;
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
            throws ServerCertificateUntrustedException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, InvalidDocumentFormatException {
        Document soapmsg = response.getResponseAsDocument();

        WssProcessor.ProcessorResult wssResults = response.getProcessorResult();
        if (wssResults == null) {
            throw new IOException("This response was not processed for WSS level security.");
        }

        String sentMessageId = request.getL7aMessageId();
        if (sentMessageId == null)
            throw new IllegalStateException("Internal error: processing signed response, but we recorded no sending message id");
        String receivedRelatesTo = SoapUtil.getL7aRelatesTo(soapmsg);
        log.log(Level.FINEST, "Response included L7a:RelatesTo of \"" + receivedRelatesTo + "\"");
        if (receivedRelatesTo != null) {
            if (!sentMessageId.equals(receivedRelatesTo.trim()))
                throw new ResponseValidationException("Response does not include L7a:RelatesTo matching L7a:MessageID from request");
            if (!wasElementSigned(wssResults, SoapUtil.getL7aRelatesToElement(soapmsg)))
                throw new ResponseValidationException("Response included a matching L7a:RelatesTo, but it was not signed");
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
            Node node = (Node)i.next();
            if (wasElementSigned(wssResults, node)) {
                // we got the bugger!
                log.fine("The element " + responseWssIntegrity.getXpathExpression().getExpression() + " was found in this " +
                         "response. and is part of the elements that were signed as per the wss processor.");
                // TODO we currently short-circuit success as soon as ANY element is found.  We must check them all!
                return AssertionStatus.NONE;
            }
        }
        log.info("The element was found in the response but does not appear to be signed. Returning FALSIFIED");
        return AssertionStatus.FALSIFIED;
    }

    private boolean wasElementSigned(WssProcessor.ProcessorResult wssResults, Node node) {
        WssProcessor.SignedElement[] toto = wssResults.getElementsThatWereSigned();
        for (int j = 0; j < toto.length; j++) {
            if (toto[j].asElement() == node)
                return true;
        }
        return false;
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
