package com.l7tech.server.security;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.ext.security.Signer;
import com.l7tech.policy.assertion.ext.security.SignerException;
import com.l7tech.policy.assertion.ext.security.SignerServices;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.security.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignerServicesImpl implements SignerServices {
    private static final Logger logger = Logger.getLogger(SignerServicesImpl.class.getName());

    private final SsgKeyStoreManager ssgKeyStoreManager;
    private final DefaultKey defaultKey;

    /**
     *
     * @param ssgKeyStoreManager the SSG keystore manager
     * @param defaultKey the default key
     */
    public SignerServicesImpl(SsgKeyStoreManager ssgKeyStoreManager, DefaultKey defaultKey) {
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        this.defaultKey = defaultKey;
    }

    @Override
    public Signer createSigner(String keyId) throws SignerException {
        SsgKeyEntry ssgKeyEntry = this.getSsgKeyEntry(keyId);
        if (ssgKeyEntry != null) {
            try {
                return new SignerImpl(ssgKeyEntry.getPrivateKey());
            } catch (UnrecoverableKeyException e) {
                throw new SignerException("Unable to create signer: " + ExceptionUtils.getMessage(e), e);
            }
        } else {
            return null;
        }
    }

    private SsgKeyEntry getSsgKeyEntry(String keyId) throws SignerException {
        if (keyId == null || keyId.length() == 0) {
            throw new SignerException("The key ID cannot be empty.");
        }

        SsgKeyEntry ssgKeyEntry = null;

        if (keyId.equals(SignerServices.KEY_ID_SSL)) {
            try {
                ssgKeyEntry = defaultKey.getSslInfo();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to find Gateway's default SSL key: " + ExceptionUtils.getMessage(e), e);
                ssgKeyEntry = null;
            }
        } else if (keyId.equals(SignerServices.KEY_ID_CA)) {
            ssgKeyEntry = defaultKey.getCaInfo();
        } else {
            String[] keyIdSplit = keyId.split(":");
            if (keyIdSplit.length != 2) {
                throw new SignerException("Invalid key ID format.");
            }

            String keystoreId = keyIdSplit[0];
            String keyAlias = keyIdSplit[1];
            if (keystoreId.equals("-1") || Goid.isDefault(Goid.parseGoid(keystoreId))) {
                // Search all keystores.
                //
                try {
                    ssgKeyEntry = ssgKeyStoreManager.lookupKeyByKeyAlias(keyAlias, Goid.DEFAULT_GOID);
                } catch (FindException | KeyStoreException e) {
                    logger.log(Level.WARNING, "Unable to find Gateway private key: " + ExceptionUtils.getMessage(e), e);
                    ssgKeyEntry = null;
                }
            } else {
                Goid keystoreGoid = Goid.parseGoid(keystoreId);
                // Search in specified keystore.
                //
                try {
                    for (SsgKeyFinder ssgKeyFinder : ssgKeyStoreManager.findAll()) {
                        if (keystoreGoid.equals(ssgKeyFinder.getKeyStore().getGoid())) {
                            ssgKeyEntry = ssgKeyFinder.getCertificateChain(keyAlias);
                            break;
                        }
                    }
                } catch (FindException | KeyStoreException e) {
                    logger.log(Level.WARNING, "Unable to find Gateway private key: " + ExceptionUtils.getMessage(e), e);
                    ssgKeyEntry = null;
                }
            }
        }

        return ssgKeyEntry;
    }
}