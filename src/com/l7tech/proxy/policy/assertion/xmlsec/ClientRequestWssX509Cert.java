package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.common.security.xml.SecurityProcessorException;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * This assertion means that the request must provide some xml signature.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class ClientRequestWssX509Cert extends ClientAssertion {
    public ClientRequestWssX509Cert(RequestWssX509Cert subject) {
    }
    
    public AssertionStatus decorateRequest(PendingRequest request) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, PolicyAssertionException {
        PendingRequest.WssDecoratorRequirements wssReqs = request.getWssRequirements();
        // get the client cert and private key
        // We must have credentials to get the private key
        Ssg ssg = request.getSsg();
        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        X509Certificate ssgCert = null;
        request.getCredentials();
        request.prepareClientCertificate();
        try {
            userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
            userCert = SsgKeyStoreManager.getClientCert(ssg);
            ssgCert = SsgKeyStoreManager.getServerCert(ssg);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        wssReqs.setRecipientCertificate(ssgCert);
        wssReqs.setSenderCertificate(userCert);
        wssReqs.setSenderPrivateKey(userPrivateKey);
        wssReqs.setSignTimestamp(true);
        return null;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, IOException, SAXException,
            ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "WSS Sign SOAP Request";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
