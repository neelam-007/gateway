package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.xml.XpathEvaluator;
import com.l7tech.xml.xpath.XpathExpression;
import org.jaxen.JaxenException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
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
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, BadCredentialsException,
                   GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException,
                   PolicyRetryableException, ClientCertificateException, PolicyAssertionException, SAXException {
        final Ssg ssg = context.getSsg();
        final X509Certificate serverCert = ssg.getServerCertificateAlways();

        final XpathExpression xpathExpression = requestWssConfidentiality.getXpathExpression();
        try {
            final XpathEvaluator eval = XpathEvaluator.newEvaluator(context.getRequest().getXmlKnob().getDocumentReadOnly(),
                                                                    xpathExpression.getNamespaces());
            List<Element> elements = eval.selectElements(xpathExpression.getExpression());
            if (elements == null || elements.size() < 1) {
                log.info("ClientRequestWssConfidentiality: No elements matched xpath expression \"" +
                         xpathExpression.getExpression() + "\".  " +
                         "Assertion therefore fails.");
                return AssertionStatus.FALSIFIED;
            }
            DecorationRequirements wssReqs = context.getWssRequirements(requestWssConfidentiality);
            if (serverCert != null && requestWssConfidentiality.getRecipientContext().localRecipient())
                    wssReqs.setRecipientCertificate(serverCert);
            wssReqs.getElementsToEncrypt().addAll(elements);
            if (requestWssConfidentiality.getXEncAlgorithm() !=null) {
                wssReqs.setEncryptionAlgorithm(requestWssConfidentiality.getXEncAlgorithm());
                if (requestWssConfidentiality.getKeyEncryptionAlgorithm() != null) {
                    wssReqs.setKeyEncryptionAlgorithm(requestWssConfidentiality.getKeyEncryptionAlgorithm());
                }
            }
            return AssertionStatus.NONE;
        } catch (JaxenException e) {
            throw new PolicyAssertionException(requestWssConfidentiality, "ClientRequestWssConfidentiality: " +
                                                                          "Unable to execute xpath expression \"" +
                                                                          xpathExpression.getExpression() + "\"", e);
        }
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
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
