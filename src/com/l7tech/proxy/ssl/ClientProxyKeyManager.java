/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.CurrentRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Key manager that knows how to find our per-SSG KeyStores.
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:50:49 PM
 */
public class ClientProxyKeyManager implements X509KeyManager {
    private static final Logger log = Logger.getLogger(ClientProxyKeyManager.class.getName());

    public PrivateKey getPrivateKey(String s) {
        try {
            log.info("ClientProxyKeyManager: getClientCertPrivateKey for " + s);
            // Find our current request
            Ssg ssg = CurrentRequest.getPeerSsg();
            if (ssg == null)
                throw new IllegalStateException("No peer Gateway is available in this thread");
            PrivateKey pk = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
            log.info("Returning PrivateKey: " + (pk == null ? "NULL" : "<it's a real key; numbers not shown>"));
            return pk;
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "Unable to obtain client certificate private key", e);
            throw new ClientProxySslException(e);
        } catch (BadCredentialsException e) {
            log.log(Level.SEVERE, "Unable to obtain client certificate private key", e);
            throw new ClientProxySslException(e);
        } catch (OperationCanceledException e) {
            log.log(Level.SEVERE, "Unable to obtain client certificate private key", e);
            throw new ClientProxySslException(e);
        } catch (KeyStoreCorruptException e) {
            log.log(Level.SEVERE, "Unable to obtain client certificate private key", e);
            throw new ClientProxySslException(e);
        }
    }

    public X509Certificate[] getCertificateChain(String s) {
        log.info("ClientProxyKeyManager: getCertificateChain for " + s);
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("No peer Gateway is available in this thread");
        X509Certificate[] certs = new X509Certificate[0];
        try {
            certs = SsgKeyStoreManager.getClientCertificateChain(ssg);
        } catch (KeyStoreCorruptException e) {
            log.log(Level.SEVERE, "Unable to get certificate chain for Gateway " + ssg, e);
            throw new ClientProxySslException(e);
        }
        log.info("Found " + certs.length + " client certificates with Gateway " + ssg);
        if (certs.length < 1) {
            log.info("*** About to return NULL certificate array..");
            return null;
        }
        for (int i = 0; i < certs.length; i++) {
            X509Certificate cert = certs[i];
            log.info("Cert #" + i + " subject=" + cert.getSubjectDN());
        }
        return certs;
    }

    public String[] getClientAliases(String s, Principal[] principals) {
        return null;
    }

    public String[] getServerAliases(String s, Principal[] principals) {
        throw new ClientProxySslException("SSL server sockets are not supported by the ClientProxyKeyManager");
    }

    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
        throw new ClientProxySslException("SSL server sockets are not supported by the ClientProxyKeyManager");
    }

    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
        log.info("ClientProxyKeyManager: Gateway is asking for our client certificate");
        Ssg ssg = CurrentRequest.getPeerSsg();
        if (ssg == null)
            throw new IllegalStateException("No peer Gateway is available in this thread");
        String hostname = ssg.getSsgAddress();
        try {
            if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                SsgKeyStoreManager.getClientCertPrivateKey(ssg); // make sure key is recoverable
                log.info("Will present client cert for hostname " + hostname);
                return hostname;
            }
        } catch (KeyStoreCorruptException e) {
            log.log(Level.SEVERE, "Unable to determine client certificate status", e);
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } catch (BadCredentialsException e) {
            log.info("Private key for client cert for Gateway " + ssg + " is currently unrecoverable; won't bother to present this cert");
            return null;
        } catch (OperationCanceledException e) {
            throw new RuntimeException(e);
        }
        log.info("No client cert found for this connection to hostname " + hostname);
        return null;
    }
}
