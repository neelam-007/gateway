package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientDecorator;
import com.l7tech.common.security.xml.WssDecorator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

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
    private RequestWssX509Cert subject;

    public ClientRequestWssX509Cert(RequestWssX509Cert subject) {
        this.subject = subject;
    }
    
    public AssertionStatus decorateRequest(final PendingRequest request) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, ClientCertificateException,
            IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException,
            PolicyRetryableException, PolicyAssertionException
    {
        request.getCredentials();
        request.prepareClientCertificate();

        final Ssg ssg = request.getSsg();
        final PrivateKey userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
        final X509Certificate userCert = SsgKeyStoreManager.getClientCert(ssg);
        final X509Certificate ssgCert = SsgKeyStoreManager.getServerCert(ssg);

        // add a pending decoration that will be applied only if the rest of this policy branch succeeds
        request.getPendingDecorations().put(this, new ClientDecorator() {
            public AssertionStatus decorateRequest(PendingRequest request)
            {
                // get the client cert and private key
                // We must have credentials to get the private key
                WssDecorator.DecorationRequirements wssReqs = request.getWssRequirements();
                wssReqs.setRecipientCertificate(ssgCert);
                wssReqs.setSenderCertificate(userCert);
                wssReqs.setSenderPrivateKey(userPrivateKey);
                wssReqs.setSignTimestamp(true);
                return AssertionStatus.NONE;
            }
        });

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, IOException, SAXException,
            ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException
    {
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
