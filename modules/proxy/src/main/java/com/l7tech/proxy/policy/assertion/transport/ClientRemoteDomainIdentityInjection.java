package com.l7tech.proxy.policy.assertion.transport;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;
import com.l7tech.util.InvalidDocumentFormatException;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.xml.sax.SAXException;

/**
 * Tells the XML VPN to try to inject the domain identity
 */
public class ClientRemoteDomainIdentityInjection extends ClientAssertionWithMetaSupport {

    @SuppressWarnings({"UnusedDeclaration"})
    public ClientRemoteDomainIdentityInjection(RemoteDomainIdentityInjection assertion) {
        super(assertion);
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException {
        return AssertionStatus.NONE;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext pac) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException {
        // flag the PAC for domain id injection on the way out
        pac.getDomainIdInjectionFlags().enable = true;
        return AssertionStatus.NONE;
    }
}
