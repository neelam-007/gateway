package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enforces that a specific element in a request is encrypted.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class ServerRequestWssConfidentiality implements ServerAssertion {
    public ServerRequestWssConfidentiality(RequestWssConfidentiality data) {
        this.data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest)) {
            logger.info("This type of assertion is only supported with SOAP type of messages");
            return AssertionStatus.BAD_REQUEST;
        }
        SoapRequest soapreq = (SoapRequest)request;
        WssProcessor.ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null) {
            throw new IOException("This request was not processed for WSS level security.");
        }

        // get the document
        Document soapmsg = null;
        try {
            soapmsg = soapreq.getDocument();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Cannot get payload document.", e);
            return AssertionStatus.BAD_REQUEST;
        }

        XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg, data.getXpathExpression().getNamespaces());
        List selectedNodes = null;
        final String xpath = data.getXpathExpression().getExpression();
        try {
            selectedNodes = evaluator.select(xpath);
        } catch (JaxenException e) {
            // this is thrown when there is an error in the expression
            // this is therefore a bad policy
            throw new PolicyAssertionException(e);
        }

        // the element is not there so there is nothing to check
        if (selectedNodes.isEmpty()) {
            logger.fine("The element " + xpath + " is not present in this request. " +
                        "the assertion therefore succeeds.");
            return AssertionStatus.NONE;
        }

        // to assert this, i must make sure that at least one of these nodes is part of the nodes
        // that were signed as per attesting the wss processor
        for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (!(obj instanceof Node)) {
                logger.warning("The xpath result included a non-Node object of type " + obj.getClass().getName());
                continue;
            }

            Node node = (Node)obj;
            if (XmlUtil.elementIsEmpty(node)) {
                logger.finer("The element " + xpath + " was found in this request but was empty and so needn't be encrypted.");
                // TODO we currently short-circuit success as soon as ANY element is found.  We must check them all!
                return AssertionStatus.NONE;
            }

            Element[] toto = wssResults.getElementsThatWereEncrypted();
            for (int j = 0; j < toto.length; j++) {
                if (toto[j] == node) {
                    // we got the bugger!
                    logger.fine("The element " + xpath + " was found in this " +
                            "request. and is part of the elements that were encrypted as per the wss processor.");
                    // TODO we currently short-circuit success as soon as ANY element is found.  We must check them all!
                    return AssertionStatus.NONE;
                }
            }
        }
        logger.fine("The element was found in the request but does not appear to be encrypted. Returning FALSIFIED");
        response.setPolicyViolated(true);
        return AssertionStatus.FALSIFIED;
    }

    protected RequestWssConfidentiality data;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
