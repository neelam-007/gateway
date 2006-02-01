/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.security.saml.SamlAssertionGenerator;
import com.l7tech.common.security.saml.SubjectStatement;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.xml.CertificateResolver;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.SimpleCertificateResolver;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a token strategy that knows how to generate (or reuse) an up-to-date sender-vouches assertion
 * vouching for the specified username.  Users will need to maintain one strategy instance for each
 * vouched-for identity.  Keeping them in an LRU cache of some kind would be a good idea.
 */
public class SenderVouchesSamlTokenStrategy extends AbstractSamlTokenStrategy {
    private static final Logger log = Logger.getLogger(SenderVouchesSamlTokenStrategy.class.getName());
    private final String subjectUsername;

    /**
     * Create a token strategy that will generated sender-vouches assertions vouching for the specified username.
     * Created assertions will be cached within the strategy instance and reused as long as they haven't expired.
     *
     * @param subjectUsername  the username to include as the Subject in the generated SAML assertions.  Must not be null or empty.
     */
    public SenderVouchesSamlTokenStrategy(String subjectUsername) {
        super(SecurityTokenType.SAML_ASSERTION, null); // Use the strategy itself for locking
        if (subjectUsername == null || subjectUsername.length() < 1) throw new IllegalArgumentException("A non-empty subjectUsername must be provided.");
        this.subjectUsername = subjectUsername;
    }

    protected SamlAssertion acquireSamlAssertion(Ssg ssg)
            throws OperationCanceledException, GeneralSecurityException, KeyStoreCorruptException,
                   BadCredentialsException, IOException
    {
        log.log(Level.INFO, "Creating new SAML sender-vouches assertion vouching for username " + subjectUsername);

        PrivateKey privateKey = ssg.getClientCertificatePrivateKey();
        if (privateKey == null) throw new IllegalStateException("Unable to sign sender-vouches assertion: client certificate private key is not available");
        X509Certificate clientCertificate = ssg.getClientCertificate();
        if (clientCertificate == null) throw new IllegalStateException("Unable to sign sender-vouches assertion: client certificate is not available");


        SignerInfo si = new SignerInfo(privateKey, new X509Certificate[] { clientCertificate });

        SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
        opts.setClientAddress(InetAddressUtil.getLocalHost());       // TODO allow override from API caller (i.e. portal)
        opts.setExpiryMinutes(5);                // TODO configurable?
        opts.setId(SamlAssertionGenerator.generateAssertionId("SSB-SamlAssertion"));
        opts.setSignAssertion(true);             // TODO configurable?
        opts.setUseThumbprintForSignature(true); // TODO configurable?

        LoginCredentials credentials = new LoginCredentials(subjectUsername, null, HttpBasic.class);
        SubjectStatement authenticationStatement = SubjectStatement.createAuthenticationStatement(credentials,
                                                                                                  SubjectStatement.SENDER_VOUCHES,
                                                                                                  true);
        SamlAssertionGenerator sag = new SamlAssertionGenerator(si);
        CertificateResolver thumbResolver = new SimpleCertificateResolver(new X509Certificate[] { clientCertificate, ssg.getServerCertificate() });

        try {
            return new SamlAssertion(sag.createAssertion(authenticationStatement, opts).getDocumentElement(), thumbResolver);
        } catch (SAXException e) {
            throw new RuntimeException("Unable to generated SAML sender-vouches assertion", e); // can't happen
        }
    }
}
