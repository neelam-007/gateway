package com.l7tech.server.security.keystore;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.security.prov.CertificateRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.util.*;

/**
 *
 */
public class SsgKeyFinderStub implements SsgKeyFinder {
    private final Map<String, SsgKeyEntry> entries;

    public SsgKeyFinderStub() {
        this(null);
    }

    public SsgKeyFinderStub(Collection<SsgKeyEntry> entries) {
        Map<String, SsgKeyEntry> map = new TreeMap<String, SsgKeyEntry>(String.CASE_INSENSITIVE_ORDER);
        if (entries != null) {
            for (SsgKeyEntry entry : entries) {
                map.put(entry.getAlias(), entry);
            }
        }
        this.entries = map;
    }

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
        return new ArrayList<String>(entries.keySet());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException, ObjectNotFoundException {
        SsgKeyEntry entry = entries.get(alias);
        if (entry == null)
            throw new ObjectNotFoundException("alias not found");
        return entry;
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
