package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap request sent from the proxy to the ssg server. Also does XML
 * Encryption of the request's body if the assertion's property requires it.
 * <p/>
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope and
 * maybe decyphers the body.
 * <p/>
 * On the proxy side, this must decorate a request with an xml d-sig and maybe encrypt the body.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientRequestWssConfidentiality extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientRequestWssConfidentiality.class.getName());

    public ClientRequestWssConfidentiality(RequestWssConfidentiality data) {
        this.requestWssConfidentiality = data;
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * ClientProxy client-side processing of the given request.
     *
     * @param request The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws OperationCanceledException, BadCredentialsException,
                   GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException,
                   PolicyRetryableException, ClientCertificateException
    {

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
                final XpathExpression xpathExpression = requestWssConfidentiality.getXpathExpression();
                final XpathEvaluator eval = XpathEvaluator.newEvaluator(request.getDecoratedSoapEnvelope(),
                                                                        xpathExpression.getNamespaces());
                try {
                    List elements = eval.selectElements(xpathExpression.getExpression());
                    if (elements == null || elements.size() < 1) {
                        log.info("ClientRequestWssConfidentiality: No elements matched xpath expression \"" +
                                 xpathExpression.getExpression() + "\".  " +
                                 "Will not sign any additional elements.");
                        return AssertionStatus.NONE;
                    }

                    // get the client cert and private key
                    // We must have credentials to get the private key
                    WssDecorator.DecorationRequirements wssReqs = request.getWssRequirements();
                    wssReqs.getElementsToEncrypt().addAll(elements);
                    return AssertionStatus.NONE;
                } catch (JaxenException e) {
                    throw new PolicyAssertionException("ClientRequestWssConfidentiality: " +
                                                       "Unable to execute xpath expression \"" +
                                                       xpathExpression.getExpression() + "\"", e);
                }
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        String str = "";
        if (requestWssConfidentiality != null && requestWssConfidentiality.getXpathExpression() != null)
            str = " matching XPath expression \"" + requestWssConfidentiality.getXpathExpression().getExpression() + '"';
        return "Request WSS Confidentiality - encrypt elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    protected RequestWssConfidentiality requestWssConfidentiality;
}
