/*
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.CertEntryRow;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.X509Entity;
import com.l7tech.security.xml.SecurityTokenResolver;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Looks up any user certificate known to the SSG by a variety of search criteria.
 */
public class UserCertificateResolver extends SecurityTokenResolverSupport implements SecurityTokenResolver {

    //- PUBLIC

    /**
     * Construct the Gateway's security token resolver.
     *
     * @param clientCertManager      required
     */
    public UserCertificateResolver( final ClientCertManager clientCertManager )
    {
        this.clientCertManager = clientCertManager;
    }

    @Override
    public X509Certificate lookup( final String thumbprint ) {
        try {
            List<CertEntryRow> got = clientCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return got.get(0).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        }
    }

    @Override
    public X509Certificate lookupBySki( final String ski ) {
        try {
            List<? extends X509Entity> got = clientCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return got.get(0).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        }
    }

    @Override
    public X509Certificate lookupByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        try {
            List<? extends X509Entity> got = clientCertManager.findByIssuerAndSerial(issuer, serial);
            if (got != null && got.size() > 1) return got.get(0).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    //- PRIVATE

    private final ClientCertManager clientCertManager;
    
}