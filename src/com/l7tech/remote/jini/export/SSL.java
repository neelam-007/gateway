/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.remote.jini.export;

import com.l7tech.common.util.KeystoreUtils;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.*;
import java.util.LinkedList;

/**
 * Utility class that creates and holds the Subject based on the SSL private key and cert.
 * The subject is used on export the admin services over SSL.
 * @author emil
 * @version Nov 1, 2004
 */
public class SSL {

    private static Subject subject = null;
    public synchronized static Subject getSslSubject() throws IOException, CertificateException, KeyStoreException {
        if (subject !=null) {
            return subject;
        }
        X500PrivateCredential privateCredential = getSslPrivateCredential();
        CertPath sslCertificateChain = getSslCertificateChain();
        X500Principal sslPrincipal = getSslPrincipal();
        Subject s = new Subject();
        s.getPrivateCredentials().add(privateCredential);
        s.getPublicCredentials().add(sslCertificateChain);
        s.getPrincipals().add(sslPrincipal);
        s.setReadOnly();
        subject = s;
        return subject;
    }

    public static X500Principal getSslPrincipal() throws IOException, CertificateException {
        return KeystoreUtils.getInstance().getSslCert().getSubjectX500Principal();
    }

    public static CertPath getSslCertificateChain() throws KeyStoreException, CertificateException {
        KeyStore keyStore = KeystoreUtils.getInstance().getSSLKeyStore();
        Certificate[] fromKeyStore = keyStore.getCertificateChain(KeystoreUtils.TOMCATALIAS);
        if (fromKeyStore == null
          || fromKeyStore.length == 0
          || !(fromKeyStore[0] instanceof X509Certificate)) {
            throw new CertificateException("Unable to find X.509 certificate chain in keystore");
        } else {
            LinkedList certList = new LinkedList();
            for (int i = 0; i < fromKeyStore.length; i++) {
                certList.add(fromKeyStore[i]);
            }
            CertificateFactory certF = CertificateFactory.getInstance("X.509");
            CertPath cpath = certF.generateCertPath(certList);

            return cpath;
        }
    }

    private static X500PrivateCredential getSslPrivateCredential()
      throws IOException, CertificateException, KeyStoreException {
        final KeystoreUtils ki = KeystoreUtils.getInstance();
        return new X500PrivateCredential(ki.getSslCert(), ki.getSSLPrivateKey(), KeystoreUtils.TOMCATALIAS);
    }

    
}