package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.xml.xpath.XpathExpression;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
public class ClientRequestWssIntegrity extends ClientDomXpathBasedAssertion<RequestWssIntegrity> {
    private static final Logger log = Logger.getLogger(ClientRequestWssIntegrity.class.getName());

    public ClientRequestWssIntegrity(RequestWssIntegrity data) {
        super(data);
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
        final XpathExpression xpathExpression = data.getXpathExpression();
        try {
            final Document message = context.getRequest().getXmlKnob().getDocumentReadOnly();
            List<Element> elements = getCompiledXpath().rawSelectElements(message, null);
            if (elements == null || elements.size() < 1) {
                log.info("ClientRequestWssIntegrity: No elements matched xpath expression \"" +
                         xpathExpression.getExpression() + "\".  " +
                         "Assertion therefore fails.");
                return AssertionStatus.NOT_APPLICABLE;
            }

            // get the client cert and private key
            // We must have credentials to get the private key
            DecorationRequirements wssReqs = context.getWssRequirements(data);
            wssReqs.getElementsToSign().addAll(elements);
            return AssertionStatus.NONE;
        } catch (JaxenException e) {
            throw new PolicyAssertionException(data, "ClientRequestWssIntegrity: " +
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
        if (data != null && data.getXpathExpression() != null)
            str = " matching XPath expression \"" + data.getXpathExpression().getExpression() + '"';
        return "Request WSS Integrity - sign elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
