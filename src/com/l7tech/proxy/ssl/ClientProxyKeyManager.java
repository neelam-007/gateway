/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgNotFoundException;
import org.apache.log4j.Category;

import javax.net.ssl.X509KeyManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
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
    private static final Category log = Category.getInstance(ClientProxyKeyManager.class);
    X509KeyManager defaultKeyManager = null;
    private SsgFinder ssgFinder = null;

    public ClientProxyKeyManager(SsgFinder ssgFinder) {
        this.ssgFinder = ssgFinder;
    }

    public PrivateKey getPrivateKey(String s) {
        try {
            log.info("ClientProxyKeyManager: getPrivateKey for " + s);
            Ssg ssg = ssgFinder.getSsgByHostname(s);
            PrivateKey pk = SsgKeyStoreManager.getPrivateKey(ssg);
            log.info("Returning PrivateKey: " + (pk == null ? "NULL" : "<it's a real key; numbers not shown>"));
            return pk;
        } catch (SsgNotFoundException e) {
            log.info(e);
            log.info("*** About to return NULL private key..");
            return null;
        } catch (GeneralSecurityException e) {
            log.error(e);
            throw new ClientProxySslException(e);
        } catch (IOException e) {
            log.error(e);
            throw new ClientProxySslException(e);
        }
    }

    public X509Certificate[] getCertificateChain(String s) {
        try {
            log.info("ClientProxyKeyManager: getCertificateChain for " + s);
            Ssg ssg = ssgFinder.getSsgByHostname(s);
            log.info("Found ssg: " + ssg);
            X509Certificate[] certs = SsgKeyStoreManager.getClientCertificateChain(ssg);
            log.info("Found " + certs.length + " client certificates with SSG " + ssg);
            if (certs.length < 1) {
                log.info("*** About to return NULL certificate array..");
                return null;
            }
            for (int i = 0; i < certs.length; i++) {
                X509Certificate cert = certs[i];
                log.info("Cert #" + i + " subject=" + cert.getSubjectDN());
            }
            return certs;
        } catch (SsgNotFoundException e) {
            log.info(e);
            log.info("*** About to return NULL certificate array..");
            return null;
        } catch (GeneralSecurityException e) {
            log.error(e);
            throw new ClientProxySslException(e);
        } catch (IOException e) {
            log.error(e);
            throw new ClientProxySslException(e);
        }
    }

    public String[] getClientAliases(String s, Principal[] principals) {
        log.info("ClientProxyKeyManager: getCurrentAliases for " + s);
        return null;
        /*
        List ssgs = ssgFinder.getSsgList();
        String[] aliases = new String[ssgs.size()];
        int idx = 0;
        for (Iterator i = ssgs.iterator(); i.hasNext(); ++idx) {
            Ssg ssg = (Ssg) i.next();
            if (SsgKeyStoreManager.isClientCertAvailabile(ssg))
                aliases[idx] = ssg.getSsgAddress();
        }
        return aliases;
        */
    }

    public String[] getServerAliases(String s, Principal[] principals) {
        throw new ClientProxySslException("SSL server sockets are not supported by the ClientProxyKeyManager");
    }

    public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
        throw new ClientProxySslException("SSL server sockets are not supported by the ClientProxyKeyManager");
    }

    public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
        log.info("ClientProxyKeyManager: chooseClientAlias for " + strings[0]);
        InetAddress ia = socket.getInetAddress();
        String hostname = ia.getHostName();
        try {
            Ssg ssg = ssgFinder.getSsgByHostname(hostname);
            if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                log.info("Will present client cert for hostname " + hostname);
                return hostname;
            }
        } catch (SsgNotFoundException e) {
        }
        log.info("No client cert found for this connection");
        return null;
    }
}
