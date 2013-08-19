package com.l7tech.server.security.keystore.luna;

import com.l7tech.objectmodel.Goid;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.security.keystore.*;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of SsgKeyStore that should work with a Luna device.
 */
public class LunaSsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    private static final Logger logger = Logger.getLogger(LunaSsgKeyStore.class.getName());

    private final Goid goid;
    private final SsgKeyStoreType type;
    private final String name;
    private final KeyStore keystore;
    private final AtomicBoolean checkedInit = new AtomicBoolean(false);

    public LunaSsgKeyStore(Goid goid, SsgKeyStoreType type, String name, KeyAccessFilter keyAccessFilter, SsgKeyMetadataManager metadataManager) throws KeyStoreException {
        super(keyAccessFilter, metadataManager);
        this.goid = goid;
        this.type = type;
        this.name = name;
        final String keystoreType = isUsingPkcs11() ? "PKCS11" : "Luna";
        this.keystore = JceProvider.getInstance().getKeyStore(keystoreType);
    }

    private static boolean isUsingPkcs11() {
        return JceProvider.PKCS11_ENGINE.equals(JceProvider.getEngineClass());
    }

    @Override
    protected KeyStore keyStore() throws KeyStoreException {
        maybeInit();
        return keystore;
    }

    private void maybeInit() throws KeyStoreException {
        if (checkedInit.getAndSet(true))
            return;

        try {
            keystore.load(null,null);
        } catch (IOException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(e);
        } catch (CertificateException e) {
            throw new KeyStoreException(e);
        }
    }

    @Override
    protected String getFormat() {
        return "luna";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public List<String> getAliases() throws KeyStoreException {
        // A Luna partition may contain various objects that don't follow the correct convention to be
        // usable as Java KeyStore key entries, so we need to filter them out.
        List<String> got = super.getAliases();
        List<String> ret = new ArrayList<String>();
        final KeyStore ks = keyStore();
        for (String alias : got) {
            try {
                if (null == ks.getKey(alias, getEntryPassword())) {
                    logger.log(Level.FINE, "Ignoring entry in Luna partition with no Key, alias = " + alias);
                    continue;
                }
            } catch (NoSuchAlgorithmException e) {
                // Ignore this key
                logger.log(Level.INFO, "Ignoring key entry in Luna partition with unsupported algorithm, alias = " + alias + "; error = " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            } catch (UnrecoverableKeyException e) {
                // Ignore this key
                logger.log(Level.INFO, "Ignoring unrecoverable key in Luna partition, alias = " + alias + "; error = " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }

            try {
                if (null == ks.getCertificateChain(alias)) {
                    logger.log(Level.FINE, "Ignoring entry in Luna partition with no certificate chain, alias = " + alias);
                    continue;
                }
            } catch (KeyStoreException e) {
                // Ignore this entry
                logger.log(Level.WARNING, "Ignoring key entry in Luna partition with bad certificate chain, alias = " + alias + "; error = " + ExceptionUtils.getMessage(e), e);
            }

            // All good
            ret.add(alias);

        }
        return ret;
    }

    @Override
    protected char[] getEntryPassword() {
        // Luna should not require a password -- should have logged into the partition early during startup.
        return new char[0];
    }

    @Override
    protected <OUT> Future<OUT> mutateKeystore(Runnable transactionCallback, Callable<OUT> mutator) throws KeyStoreException {
        // Luna doesn't use the DB, so just run in foreground and return result
        if (transactionCallback != null)
            transactionCallback.run();
        final FutureTask<OUT> task = new FutureTask<OUT>(mutator);
        task.run();
        return task;
    }

    @Override
    public Goid getGoid() {
        return goid;
    }

    @Override
    public SsgKeyStoreType getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isKeyExportSupported() {
        return false;
    }
}
