package com.l7tech.server.security.keystore.software;

import com.l7tech.gateway.common.security.BouncyCastleCertUtils;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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

    public String getId() {
        return String.valueOf(getOid());
    }

    public long getOid() {
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
        return aliases;
    }

    public SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException {
        try {
            if ("SSL".equals(alias)) {
                return new SsgKeyEntry(getOid(),
                                       alias,
                                       new X509Certificate[] { keystoreUtils.getSslCert() },
                                       keystoreUtils.getSSLPrivateKey());
            } else if ("CA".equals(alias)) {
                return new SsgKeyEntry(getOid(),
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

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public CertificateRequest makeCertificateSigningRequest(String alias, String dn) throws InvalidKeyException, SignatureException, KeyStoreException {
        try {
            X500Principal dnObj = new X500Principal(dn);
            final PublicKey rsaPublic;
            final PrivateKey rsaPrivate;
            if ("SSL".equals(alias)) {
                rsaPublic = keystoreUtils.getSslCert().getPublicKey();
                rsaPrivate = keystoreUtils.getSSLPrivateKey();
            } else if ("CA".equals(alias)) {
                // TODO should we move the logic for handling ca cert removal/insertion into keystoreUtils?
                throw new InvalidKeyException("Unable to generate CSR using the CA root cert");
            } else
                throw new KeyStoreException("No certificate chain available in static keystore with alias " + alias);
            KeyPair keyPair = new KeyPair(rsaPublic, rsaPrivate);
            return BouncyCastleCertUtils.makeCertificateRequest(dnObj, keyPair);
        } catch (NoSuchProviderException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        }
    }
}
