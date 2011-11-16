package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.ElementEncryptionConfig;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.xml.xpath.XpathExpression;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
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
 */
public class ClientRequestWssConfidentiality extends ClientDomXpathBasedAssertion<RequireWssEncryptedElement> {
    private static final Logger log = Logger.getLogger(ClientRequestWssConfidentiality.class.getName());

    private static final boolean gcmSupported = isGcmSupported();

    public ClientRequestWssConfidentiality(RequireWssEncryptedElement data) {
        super(data);
    }

    /**
     * ClientProxy client-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    @Override
    public AssertionStatus decorateRequest(PolicyApplicationContext context)
            throws OperationCanceledException, BadCredentialsException,
                   GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException,
                   PolicyRetryableException, ClientCertificateException, PolicyAssertionException, SAXException {
        final Ssg ssg = context.getSsg();
        final X509Certificate serverCert = ssg.getServerCertificateAlways();

        final XpathExpression xpathExpression = data.getXpathExpression();
        try {
            final Document message = context.getRequest().getXmlKnob().getDocumentReadOnly();
            List<Element> elements = getCompiledXpath().rawSelectElements(message, null);
            if (elements == null || elements.size() < 1) {
                log.info("ClientRequestWssConfidentiality: No elements matched xpath expression \"" +
                         xpathExpression.getExpression() + "\".  " +
                         "Assertion therefore fails.");
                return AssertionStatus.FALSIFIED;
            }
            DecorationRequirements wssReqs = context.getWssRequirements(data);
            if (serverCert != null && data.getRecipientContext().localRecipient())
                    wssReqs.setRecipientCertificate(serverCert);
            for (Element element : elements) {
                wssReqs.addElementToEncrypt(element, new ElementEncryptionConfig(data.isEncryptContentsOnly()));
            }
            if (data.getXEncAlgorithmList() != null) {
                for (String uri : data.getXEncAlgorithmList()) {
                    if (isXencAlgorithmSupported(uri)) {
                        wssReqs.setEncryptionAlgorithm(uri);
                        break;
                    }
                }
            } else if (data.getXEncAlgorithm() !=null) {
                wssReqs.setEncryptionAlgorithm(data.getXEncAlgorithm());
            }
            if (data.getKeyEncryptionAlgorithm() != null) {
                wssReqs.setKeyEncryptionAlgorithm(data.getKeyEncryptionAlgorithm());
            }
            return AssertionStatus.NONE;
        } catch (JaxenException e) {
            throw new PolicyAssertionException(data, "ClientRequestWssConfidentiality: " +
                                                                          "Unable to execute xpath expression \"" +
                                                                          xpathExpression.getExpression() + "\"", e);
        }
    }

    @Override
    public AssertionStatus unDecorateReply(PolicyApplicationContext context) {
        // no action on response
        return AssertionStatus.NONE;
    }

    private boolean isXencAlgorithmSupported(String algUri) {
        try {
            if (XencUtil.doesEncryptionAlgorithmRequireGcm(algUri) && !gcmSupported)
                return false;
            XencUtil.getFlexKeyAlg(algUri);
            return true;
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private static boolean isGcmSupported() {
        try {
            JceProvider.getInstance().getAesGcmCipher();
            log.fine("AES GCM support is available");
            return true;
        } catch (Exception e) {
            log.info("AES GCM support is not available with the current security provider configuration");
            return false;
        }
    }
}
