package com.l7tech.proxy.policy.assertion.transport;

import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.util.InvalidDocumentFormatException;

import java.security.GeneralSecurityException;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * This assertion is meant to instruct XML VPN Clients to compress payloads prior to
 * forwarding the request to the SSG
 * <p/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 */
public class ClientPreemptiveCompression extends ClientAssertionWithMetaSupport {
    public ClientPreemptiveCompression(PreemptiveCompression foo) {
        super(foo);
    }

    @Override
    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException {
        return AssertionStatus.NONE;
    }

    @Override
    public AssertionStatus decorateRequest(PolicyApplicationContext pac) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        // flag the PAC for compression
        pac.setSsgRequestedCompression(true);
        return AssertionStatus.NONE;
    }
}
