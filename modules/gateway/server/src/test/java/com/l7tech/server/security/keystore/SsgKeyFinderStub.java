package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.common.io.CertGenParams;
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
    @Override
    public long getOid() {
        return 0;
    }

    @Override
    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.OTHER;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public boolean isKeyExportSupported() {
        return false;
    }

    @Override
    public SsgKeyStore getKeyStore() {
        return null;
    }

    @Override
    public List<String> getAliases() throws KeyStoreException {
        return Collections.emptyList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException, ObjectNotFoundException {
        throw new ObjectNotFoundException("alias not found");
    }

    @Override
    public CertificateRequest makeCertificateSigningRequest(String alias, CertGenParams certGenParams) throws InvalidKeyException, SignatureException, KeyStoreException {
        throw new InvalidKeyException("not found");
    }

    @Override
    public String getName() {
        return "Stub KeyFinder";
    }

    @Override
    public String getId() {
        return "stub";
    }
}
