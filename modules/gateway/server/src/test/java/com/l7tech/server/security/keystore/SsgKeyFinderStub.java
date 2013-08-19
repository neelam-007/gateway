package com.l7tech.server.security.keystore;

import com.l7tech.common.io.CertGenParams;
import com.l7tech.common.io.KeyGenParams;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.util.NotFuture;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Future;

/**
 *
 */
public class SsgKeyFinderStub implements SsgKeyStore {
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
    public Goid getGoid() {
        return new Goid(0,0);
    }

    @Override
    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.OTHER;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean isKeyExportSupported() {
        return false;
    }

    @Override
    public SsgKeyStore getKeyStore() {
        return this;
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
    public void updateKeyMetadata(Goid keystoreId, String alias, SsgKeyMetadata metadata) throws UpdateException {
    }

    @Override
    public String getName() {
        return "Stub KeyFinder";
    }

    @Override
    public String getId() {
        return "stub";
    }

    @Override
    public Future<Boolean> deletePrivateKeyEntry( final Runnable transactionCallback, final String keyAlias ) throws KeyStoreException {
        return new NotFuture<Boolean>(false);
    }

    @Override
    public Future<X509Certificate> generateKeyPair( final Runnable transactionCallback, final String alias, final KeyGenParams keyGenParams, final CertGenParams certGenParams, final SsgKeyMetadata metadata ) throws GeneralSecurityException {
        throw new GeneralSecurityException("not implemented");
    }

    @Override
    public Future<Boolean> replaceCertificateChain( final Runnable transactionCallback, final String alias, final X509Certificate[] chain ) throws InvalidKeyException, KeyStoreException {
        SsgKeyEntry entry = entries.get(alias);
        if (entry == null)
            throw new InvalidKeyException("alias not found");
        entry.setCertificateChain( chain );
        return new NotFuture<Boolean>(true);
    }

    @Override
    public Future<Boolean> storePrivateKeyEntry( final Runnable transactionCallback, final SsgKeyEntry entry, final boolean overwriteExisting ) throws KeyStoreException {
        throw new KeyStoreException("not implemented");
    }
}
