package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.List;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this schedules decoration of a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerResponseWssIntegrity implements ServerAssertion {

    public ServerResponseWssIntegrity(ResponseWssIntegrity data) {
        responseWssIntegrity = data;
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     */
    public AssertionStatus checkRequest(Request request, Response response)
            throws IOException, PolicyAssertionException
    {
        if (!(request instanceof SoapRequest))
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP requests");

        response.addDeferredAssertion(this, new ServerAssertion() {
            public AssertionStatus checkRequest(Request request, Response response)
                    throws IOException, PolicyAssertionException
            {
                if (!(response instanceof SoapResponse))
                    throw new PolicyAssertionException("This type of assertion is only supported with SOAP responses");

                SoapResponse soapResponse = (SoapResponse)response;

                // GET THE DOCUMENT
                Document soapmsg = null;
                try {
                    soapmsg = soapResponse.getDocument();
                } catch (SAXException e) {
                    String msg = "cannot get an xml document from the response to sign";
                    logger.severe(msg);
                    return AssertionStatus.SERVER_ERROR;
                }

                XpathEvaluator evaluator = XpathEvaluator.newEvaluator(soapmsg,
                                                                       responseWssIntegrity.getXpathExpression().getNamespaces());
                List selectedElements = null;
                try {
                    selectedElements = evaluator.selectElements(responseWssIntegrity.getXpathExpression().getExpression());
                } catch (JaxenException e) {
                    // this is thrown when there is an error in the expression
                    // this is therefore a bad policy
                    throw new PolicyAssertionException(e);
                }

                if (selectedElements == null || selectedElements.size() < 1) {
                    logger.fine("No matching elements to sign in response.  Returning success.");
                    return AssertionStatus.NONE;
                }

                SignerInfo si = KeystoreUtils.getInstance().getSignerInfo();
                WssDecorator.DecorationRequirements wssReq = soapResponse.getOrMakeDecorationRequirements();
                wssReq.setSenderCertificate(si.getCertificateChain()[0]);
                wssReq.setSenderPrivateKey(si.getPrivate());
                wssReq.getElementsToSign().addAll(selectedElements);
                wssReq.setSignTimestamp(true);
                logger.fine("Designated " + selectedElements.size() + " response elements for signing");

                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssIntegrity responseWssIntegrity;
}
