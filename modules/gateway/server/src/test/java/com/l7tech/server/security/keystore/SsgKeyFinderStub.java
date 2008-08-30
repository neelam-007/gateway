package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.security.prov.CertificateRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SsgKeyFinderStub implements SsgKeyFinder {
    public long getOid() {
        return 0;
    }

    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.OTHER;
    }

    public boolean isMutable() {
        return false;
    }

    public SsgKeyStore getKeyStore() {
        return null;
    }

    public List<String> getAliases() throws KeyStoreException {
        return Collections.emptyList();
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException {
        throw new KeyStoreException("alias not found");
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public CertificateRequest makeCertificateSigningRequest(String alias, String dn) throws InvalidKeyException, SignatureException, KeyStoreException {
        throw new InvalidKeyException("not found");
    }

    public String getName() {
        return "Stub KeyFinder";
    }

    public String getId() {
        return "stub";
    }
}
