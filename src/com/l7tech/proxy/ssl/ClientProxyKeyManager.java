/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Key manager that knows how to find our per-SslPeer Key information.
 */
public class ClientProxyKeyManager implements X509KeyManager {
    private static final Logger log = Logger.getLogger(ClientProxyKeyManager.class.getName());

    public PrivateKey getPrivateKey(String s) {
        try {
            log.log(Level.FINER, "ClientProxyKeyManager: getClientCertPrivateKey for " + s);
            // Find our current request
            SslPeer peer = CurrentSslPeer.get();
            if (peer == null)
                throw new IllegalStateException("No SSL peer is available in this thread");
            PrivateKey pk = peer.getClientCertificatePrivateKey();
            log.log(Level.FINE, "Returning PrivateKey: " + (pk == null ? "NULL" : "<it's a real key; numbers not shown>"));
            return pk;
        } catch (BadCredentialsException e) {
            log.log(Level.SEVERE, "Unable to obtain client certificate private key", e);
            throw new ClientProxySslException(e);
        } catch (OperationCanceledException e) {
            log.log(Level.SEVERE, "Unable to obtain client certificate private key", e);
            throw new ClientProxySslException(e);
        }
    }

    public X509Certificate[] getCertificateChain(String s) {
        log.log(Level.FINER, "ClientProxyKeyManager: getCertificateChain for " + s);
        SslPeer peer = CurrentSslPeer.get();
        if (peer == null)
            throw new IllegalStateException("No SSL peer is available in this thread");
        X509Certificate[] certs = new X509Certificate[] { peer.getClientCertificate() };
        log.info("Found " + certs.length + " client certificates with Gateway " + peer);
        if (certs.length < 1) {
            log.log(Level.FINE, "*** About to return NULL certificate array");
            return null;
        }
        for (int i = 0; i < certs.length; i++) {
            X509Certificate cert = certs[i];
            log.log(Level.FINER, "Cert #" + i + " subject=" + cert.getSubjectDN());
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
        log.log(Level.FINER, "ClientProxyKeyManager: Gateway is asking for our client certificate");
        SslPeer peer = CurrentSslPeer.get();
        if (peer == null)
            throw new IllegalStateException("No peer Gateway is available in this thread");
        String hostname = peer.getHostname();
        try {
            if (peer.getClientCertificate() != null) {
                peer.getClientCertificatePrivateKey(); // make sure key is recoverable
                log.log(Level.FINE, "Will present client cert for hostname " + hostname);
                return hostname;
            }
        } catch (BadCredentialsException e) {
            log.log(Level.FINE, "Private key for client cert for Gateway " + peer + " is currently unrecoverable; won't bother to present this cert");
            return null;
        } catch (OperationCanceledException e) {
            throw new RuntimeException(e);
        }
        log.log(Level.FINE, "No client cert found for this connection to hostname " + hostname);
        return null;
    }
}
