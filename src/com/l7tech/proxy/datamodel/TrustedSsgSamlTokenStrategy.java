/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.wstrust.TokenServiceClient;
import com.l7tech.common.xml.WsTrustRequestType;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the default SAML Authentication Token strategy, which knows how to get a SAML token from
 * the specified trusted Ssg.
 */
public class TrustedSsgSamlTokenStrategy extends AbstractSamlTokenStrategy {
    private static final Logger log = Logger.getLogger(TrustedSsgSamlTokenStrategy.class.getName());
    private final Ssg tokenServerSsg;

    /**
     * @param tokenServerSsg what SSG is going to give me a SAML token
     */
    public TrustedSsgSamlTokenStrategy(Ssg tokenServerSsg)
    {
        super(SecurityTokenType.SAML_ASSERTION, tokenServerSsg);
        this.tokenServerSsg = tokenServerSsg;
    }

    protected SamlAssertion acquireSamlAssertion()
            throws OperationCanceledException, GeneralSecurityException,
            KeyStoreCorruptException, BadCredentialsException, IOException
    {
        log.log(Level.INFO, "Applying for SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
        SamlAssertion s;
        // TODO extract the strategies for getting tokenServer client cert, private key, and server cert
        URL url = new URL("http",
                          tokenServerSsg.getSsgAddress(),
                          tokenServerSsg.getSsgPort(),
                          SecureSpanConstants.TOKEN_SERVICE_FILE);

        Date timestampCreatedDate = tokenServerSsg.getRuntime().getDateTranslatorToSsg().translate(new Date());

        s = TokenServiceClient.obtainSamlAssertion(tokenServerSsg.getRuntime().getHttpClient(), null, url,
                                                   tokenServerSsg.getServerCertificateAlways(),
                                                   timestampCreatedDate,
                                                   tokenServerSsg.getClientCertificate(),
                                                   tokenServerSsg.getClientCertificatePrivateKey(),
                                                   WsTrustRequestType.ISSUE,
                                                   SecurityTokenType.SAML_ASSERTION,
                                                   null, // no base
                                                   null, // no appliesTo
                                                   null, // no wstIssuer
                                                   true);
        log.log(Level.INFO, "Obtained SAML holder-of-key assertion from Gateway " + tokenServerSsg.toString());
        return s;
    }
}
