package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssDecorator;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import java.util.logging.Logger;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

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
    private static final Logger log = Logger.getLogger(ClientHttpClientCert.class.getName());

    public ClientRequestWssIntegrity(RequestWssIntegrity data) {
        this.requestWssIntegrity = data;
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
        request.getCredentials();
        request.prepareClientCertificate();

        // get the client cert and private key
        // We must have credentials to get the private key
        final Ssg ssg = request.getSsg();
        final X509Certificate userCert = SsgKeyStoreManager.getClientCert(ssg);
        final PrivateKey userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
        final X509Certificate ssgCert = SsgKeyStoreManager.getServerCert(ssg);

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
                final XpathExpression xpathExpression = requestWssIntegrity.getXpathExpression();
                final XpathEvaluator eval = XpathEvaluator.newEvaluator(request.getDecoratedSoapEnvelope(),
                                                                        xpathExpression.getNamespaces());
                try {
                    List elements = eval.selectElements(xpathExpression.getExpression());
                    if (elements == null || elements.size() < 1) {
                        log.info("ClientRequestWssIntegrity: No elements matched xpath expression \"" +
                                 xpathExpression.getExpression() + "\".  " +
                                 "Will not sign any additional elements.");
                        return AssertionStatus.NONE;
                    }

                    // get the client cert and private key
                    // We must have credentials to get the private key
                    WssDecorator.DecorationRequirements wssReqs = request.getWssRequirements();
                    wssReqs.setRecipientCertificate(ssgCert);
                    wssReqs.setSenderCertificate(userCert);
                    wssReqs.setSenderPrivateKey(userPrivateKey);
                    wssReqs.getElementsToSign().addAll(elements);
                    return AssertionStatus.NONE;
                } catch (JaxenException e) {
                    throw new PolicyAssertionException("ClientRequestWssIntegrity: " +
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
        if (requestWssIntegrity != null && requestWssIntegrity.getXpathExpression() != null)
            str = " matching XPath expression \"" + requestWssIntegrity.getXpathExpression().getExpression() + '"';
        return "Request WSS Integrity - sign elements" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    protected RequestWssIntegrity requestWssIntegrity;
}
