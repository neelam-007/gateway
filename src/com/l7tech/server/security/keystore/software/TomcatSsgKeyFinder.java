package com.l7tech.server.security.keystore.software;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.security.keystore.SsgKeyEntry;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * A read-only KeyFinder that knows about only the SSL and CA keys in the old Tomcat keystore.
 */
public class TomcatSsgKeyFinder implements SsgKeyFinder {
    private final long id;
    private final String name;
    private final KeystoreUtils keystoreUtils;

    public TomcatSsgKeyFinder(long id, String name, KeystoreUtils keystoreUtils) {
        this.id = id;
        this.name = name;
        this.keystoreUtils = keystoreUtils;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.PKCS12_SOFTWARE;
    }

    public boolean isMutable() {
        return false;
    }

    public SsgKeyStore getKeyStore() {
        return null;
    }

    public List<String> getAliases() throws KeyStoreException {
        List<String> aliases = new ArrayList<String>();
        aliases.add("SSL");
        aliases.add("CA");
        return aliases;
    }

    public SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException {
        try {
            if ("SSL".equals(alias)) {
                return new SsgKeyEntry(getId(),
                                       alias,
                                       new X509Certificate[] { keystoreUtils.getSslCert() },
                                       (RSAPrivateKey)keystoreUtils.getSSLPrivateKey());
            } else if ("CA".equals(alias)) {
                return new SsgKeyEntry(getId(),
                                       alias,
                                       new X509Certificate[] { keystoreUtils.getRootCert() },
                                       null); // TODO should we include the private key as well, if this is a master node?
            }
            throw new KeyStoreException("No certificate chain available in static keystore with alias " + alias);
        } catch (IOException e) {
            throw new KeyStoreException("Unable to access certificate chain with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to access certificate chain with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
        }
    }
}
