/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import com.l7tech.common.util.HexUtils;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author alex
 * @version $Revision$
 */
public class TrustedCert extends NamedEntityImp implements Serializable {
    public String getUsageDescription() {
        StringBuffer buf = new StringBuffer();
        if (trustedForSsl) add(buf, "SSL");
        if (trustedForSigningServerCerts) add(buf,"Sign Server");
        if (trustedForSigningClientCerts) add(buf,"Sign Client");
        if (trustedForSigningSamlTokens) add(buf,"Sign SAML");
        return buf.toString();
    }

    private void add(StringBuffer buf, String s) {
        if ( buf.length() == 0 )
            buf.append(s);
        else {
            buf.append( ", " );
            buf.append(s);
        }
    }

    public synchronized X509Certificate getCertificate() throws CertificateException, IOException {
        if ( cachedCert == null ) {
            if (certFactory == null ) certFactory = CertificateFactory.getInstance(CERT_FACTORY_TYPE);
            ByteArrayInputStream bais = new ByteArrayInputStream(HexUtils.decodeBase64(certBase64));
            cachedCert = (X509Certificate)certFactory.generateCertificate(bais);
        }
        return cachedCert;
    }

    public synchronized void setCertificate(X509Certificate cert) throws CertificateEncodingException {
        this.cachedCert = cert;
        this.certBase64 = HexUtils.encodeBase64( cert.getEncoded() );
    }

    public synchronized String getCertBase64() {
        return certBase64;
    }

    public synchronized void setCertBase64( String certBase64 ) {
        this.certBase64 = certBase64;
    }

    public boolean isTrustedForSsl() {
        return trustedForSsl;
    }

    public void setTrustedForSsl( boolean trustedForSsl ) {
        this.trustedForSsl = trustedForSsl;
    }

    public boolean isTrustedForSigningClientCerts() {
        return trustedForSigningClientCerts;
    }

    public void setTrustedForSigningClientCerts( boolean trustedForSigningClientCerts ) {
        this.trustedForSigningClientCerts = trustedForSigningClientCerts;
    }

    public boolean isTrustedForSigningServerCerts() {
        return trustedForSigningServerCerts;
    }

    public void setTrustedForSigningServerCerts( boolean trustedForSigningServerCerts ) {
        this.trustedForSigningServerCerts = trustedForSigningServerCerts;
    }

    public boolean isTrustedForSigningSamlTokens() {
        return trustedForSigningSamlTokens;
    }

    public void setTrustedForSigningSamlTokens( boolean trustedForSigningSamlTokens ) {
        this.trustedForSigningSamlTokens = trustedForSigningSamlTokens;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn( String subjectDn ) {
        this.subjectDn = subjectDn;
    }

    private transient X509Certificate cachedCert;
    private transient CertificateFactory certFactory;

    private String certBase64;
    private String subjectDn;
    private boolean trustedForSsl;
    private boolean trustedForSigningClientCerts;
    private boolean trustedForSigningServerCerts;
    private boolean trustedForSigningSamlTokens;
    private static final String CERT_FACTORY_TYPE = "SunX509";
}
