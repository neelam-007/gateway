package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: rseminoff
 * Date: 21/01/13
 */
public class ResourceValidation {

    private static class PrivateKeyItem {
        public Goid keystoreId;
        public String keystoreName;
        public String keyAlias;
        public String keyAlgorithm;

        public PrivateKeyItem(final Goid keystoreId, final String keystoreName, final String keyAlias, final String keyAlgorithm) {
            this.keystoreId = keystoreId;
            this.keystoreName = keystoreName;
            this.keyAlias = keyAlias;
            this.keyAlgorithm = keyAlgorithm;
        }
        @Override
        public String toString() {
            return "'" + keyAlias + "'" + " in " + keystoreName;
        }
    }

    private static final PrivateKeyItem ITEM_DEFAULT_SSL = new PrivateKeyItem(PersistentEntity.DEFAULT_GOID, null, null, "NONE") {
        @Override
        public String toString() {
            return "<Default SSL Key>";
        }
    };

    private static final Logger logger = Logger.getLogger(ResourceValidation.class.getName());

    public static boolean validatePassword(Goid reference) {
        try {
            List<SecurePassword> securePasswords = new ArrayList<>(Registry.getDefault().getTrustedCertManager().findAllSecurePasswords());
            for (SecurePassword curPassword : securePasswords) {
                if (Goid.equals(curPassword.getGoid(), reference)) {
                    return true;    // Found the password in the database.
                }
            }
            return false;   // No password was found in the database
        } catch (Exception e) {
            final String msg = "Unable to list stored passwords: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(null, "Unable to list stored passwords", msg, e);
        }
        return false;
    }

    public static boolean validatePrivateKey(Goid referenceID, String referenceName) {
        try {
            final TrustedCertAdmin certAdmin = Registry.getDefault().getTrustedCertManager();
            final java.util.List<KeystoreFileEntityHeader> keystores = certAdmin.findAllKeystores(true);
            final List<PrivateKeyItem> items = new ArrayList<>();
            items.add(ITEM_DEFAULT_SSL);
            for (KeystoreFileEntityHeader keystore : keystores) {
                for (SsgKeyEntry entry : certAdmin.findAllKeys(keystore.getGoid(), true)) {
                    items.add(new PrivateKeyItem(keystore.getGoid(), keystore.getName(), entry.getAlias(), entry.getCertificate().getPublicKey().getAlgorithm()));
                }
            }

            // Check the list of keys for the one we're looking for
            for (PrivateKeyItem keyItem : items) {
                if ( (Goid.equals(keyItem.keystoreId, referenceID)) && (keyItem.keyAlias.equals(referenceName)) ) {
                    return true;    // Found the key.
                }
            }
        } catch (CertificateException | FindException | KeyStoreException e) {
            final String msg = "An error occurred fetching keys from Keystore: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(null, "Unable to list stored passwords", msg, e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "An error occurred fetching keys from the Keystore.", e);
        }

        return false;   // No Key found.
    }
}
