package com.l7tech.proxy.policy.assertion.transport;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.util.InvalidDocumentFormatException;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

/**
 * This assertion is meant to instruct XML VPN Clients to compress payloads prior to
 * forwarding the request to the SSG
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 3, 2008<br/>
 */
public class ClientPreemptiveCompression extends ClientAssertion {
    private Logger logger = Logger.getLogger(ClientPreemptiveCompression.class.getName());
    public ClientPreemptiveCompression(PreemptiveCompression foo) {}
    
    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException {
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Compress Request";
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/folder.gif";
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext pac) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        // flag the PAC for compression
        pac.setSsgRequestedCompression(true);
        return AssertionStatus.NONE;
    }
}
