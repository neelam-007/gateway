package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

import java.security.GeneralSecurityException;
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
        //todo
        return null;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws BadCredentialsException,
            OperationCanceledException, GeneralSecurityException, IOException, SAXException,
            ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException {
        //todo
        return null;
    }

    public String getName() {
        //todo
        return null;
    }

    public String iconResource(boolean open) {
        //todo
        return null;
    }
}
