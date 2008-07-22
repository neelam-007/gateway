package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.xml.XpathEvaluator;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Level;
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
public class ClientRequestWssIntegrity extends ClientAssertion {
    private static final Logger log = Logger.getLogger(ClientRequestWssIntegrity.class.getName());

    public ClientRequestWssIntegrity(RequestWssIntegrity data) {
        this.requestWssIntegrity = data;
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
        final XpathExpression xpathExpression = requestWssIntegrity.getXpathExpression();
        final XpathEvaluator eval = XpathEvaluator.newEvaluator(context.getRequest().getXmlKnob().getDocumentReadOnly(),
                                                                xpathExpression.getNamespaces());
        try {
            List elements = eval.selectElements(xpathExpression.getExpression());
            if (elements == null || elements.size() < 1) {
                log.info("ClientRequestWssIntegrity: No elements matched xpath expression \"" +
                         xpathExpression.getExpression() + "\".  " +
                         "Assertion therefore fails.");
                return AssertionStatus.NOT_APPLICABLE;
            }

            // get the client cert and private key
            // We must have credentials to get the private key
            DecorationRequirements wssReqs;
            if (requestWssIntegrity.getRecipientContext().localRecipient()) {
                wssReqs = context.getDefaultWssRequirements();
            } else {
                wssReqs = context.getAlternateWssRequirements(requestWssIntegrity.getRecipientContext());
            }
            wssReqs.getElementsToSign().addAll(elements);
            return AssertionStatus.NONE;
        } catch (JaxenException e) {
            throw new PolicyAssertionException(requestWssIntegrity, "ClientRequestWssIntegrity: " +
                                                                    "Unable to execute xpath expression \"" +
                                                                    xpathExpression.getExpression() + "\"", e);
        } catch (IOException e) {
            String msg = "Cannot initialize the recipient's  DecorationRequirements";
            log.log(Level.WARNING, msg, e);
            throw new PolicyAssertionException(requestWssIntegrity, msg, e);
        } catch (CertificateException e) {
            String msg = "Cannot initialize the recipient's  DecorationRequirements";
            log.log(Level.WARNING, msg, e);
            throw new PolicyAssertionException(requestWssIntegrity, msg, e);
        }
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        String str = "";
        if (requestWssIntegrity != null && requestWssIntegrity.getXpathExpression() != null)
            str = " matching XPath expression \"" + requestWssIntegrity.getXpathExpression().getExpression() + '"';
        return "Request WSS Integrity - sign elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    protected RequestWssIntegrity requestWssIntegrity;
}
