/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
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
import java.util.Iterator;
import java.util.List;

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
            return SsgKeyStoreManager.getPrivateKey(ssg);
        } catch (SsgNotFoundException e) {
            log.error(e);
            throw new ClientProxySslException(e);
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
            return SsgKeyStoreManager.getClientCertificateChain(ssg);
        } catch (SsgNotFoundException e) {
            log.error(e);
            throw new ClientProxySslException(e);
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
        List ssgs = ssgFinder.getSsgList();
        String[] aliases = new String[ssgs.size()];
        int idx = 0;
        for (Iterator i = ssgs.iterator(); i.hasNext(); ++idx) {
            Ssg ssg = (Ssg) i.next();
            aliases[idx] = ssg.getSsgAddress();
        }
        return aliases;
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
        return hostname;
    }
}
