package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A combo box prepopulated with a list of private keys in the SSG keystore.
 *
 * @since SecureSpan 4.0
 * @see AssertionKeyAliasEditor
 * @author rmak
 */
public class PrivateKeysComboBox extends JComboBox {
    private static final Logger _logger = Logger.getLogger(PrivateKeysComboBox.class.getName());

    /** An item in the combo box. */
    private static class PrivateKeyItem {
        public long keystoreId;
        public String keystoreName;
        public String keyAlias;
        public String keyAlgorithm;

        public PrivateKeyItem(final long keystoreId, final String keystoreName, final String keyAlias, final String keyAlgorithm) {
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

    private static final PrivateKeyItem ITEM_DEFAULT_SSL = new PrivateKeyItem(-1, null, null, "NONE") {
        @Override
        public String toString() {
            return "<Default SSL Key>";
        }
    };

    private boolean _includeHardwareKeystore;
    private boolean _includeDefaultSslKey;
    private boolean _includeRestrictedAccessKeys;

    /**
     * Sorts the private key by alias while maintaining that the default private key label is the first one
     * on the list.
     */
    public static class PrivateKeyItemComparator implements Comparator<PrivateKeyItem> {
        @Override
        public int compare(PrivateKeyItem pk1, PrivateKeyItem pk2) {
            if (pk1.keyAlias == null) {
                return -1;
            } else if (pk2.keyAlias == null) {
                return 1;
            } else {
                return pk1.keyAlias.compareToIgnoreCase(pk2.keyAlias);
            }
        }
    }

    /**
     * Creates a combo box prepopulated with a list of private keys from all SSG
     * keystores; and with none selected initially.
     */
    public PrivateKeysComboBox() {
        this(true, true, true);
    }

    /**
     * Creates a combo box prepopulated with a list of private keys from specified SSG keystores.
     * @param includeHardwareKeystore  if true, hardware keystores should be included in the list.
     * @param includeDefaultSslKey   if true, includes the < Default SSK Key > in the list.  Otherwise, only explicitly
     *                               configured key aliases are permitted.
     * @param includeRestrictedAccessKeys if true, includes keys marked as restricted access (eg, the audit viewer private key) in the list.
     */
    public PrivateKeysComboBox(final boolean includeHardwareKeystore, final boolean includeDefaultSslKey, final boolean includeRestrictedAccessKeys) {
        _includeHardwareKeystore = includeHardwareKeystore;
        _includeDefaultSslKey = includeDefaultSslKey;
        _includeRestrictedAccessKeys = includeRestrictedAccessKeys;
        populate();
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    private void populate() {
        try {
            final TrustedCertAdmin certAdmin = getTrustedCertAdmin();
            if (certAdmin == null) {
                _logger.log(Level.WARNING, "Unable to populate PrivateKeysComboBox: No TrustedCertAdmin available (not connected to Gateway?)");
                setModel(new DefaultComboBoxModel(new PrivateKeyItem[0]));
                return;
            }
            final java.util.List<KeystoreFileEntityHeader> keystores = certAdmin.findAllKeystores(_includeHardwareKeystore);
            final List<PrivateKeyItem> items = new ArrayList<PrivateKeyItem>();
            if (_includeDefaultSslKey)
                items.add(ITEM_DEFAULT_SSL);
            for (KeystoreFileEntityHeader keystore : keystores) {
                for (SsgKeyEntry entry : certAdmin.findAllKeys(keystore.getOid(), _includeRestrictedAccessKeys)) {
                    items.add(new PrivateKeyItem(keystore.getOid(), keystore.getName(), entry.getAlias(), entry.getCertificate().getPublicKey().getAlgorithm()));
                }
            }
            Collections.sort(items, new PrivateKeyItemComparator());
            setModel(new DefaultComboBoxModel(items.toArray()));
        } catch (FindException fe) {
            _logger.log(Level.WARNING, "Problem when listing keys in keystores.", fe);
        } catch (KeyStoreException kse) {
            _logger.log(Level.WARNING, "Problem when listing keys in keystores.", kse);
        } catch (CertificateException ce) {
            _logger.log(Level.WARNING, "Problem when listing keys in keystores.", ce);
        } catch (IOException ioe) {
            _logger.log(Level.WARNING, "Problem when listing keys in keystores.", ioe);
        }
        // bug #6596 - catch individual checked exception rather than Exception base class
    }

    /**
     * Refreshes the list of items in the combo box with the latest list of
     * private keys in the SSG keystore. And tries to keep the currently selected
     * key selected if it is still available in the refreshed list.
     *
     * @return index of selected item; or -1 if none selected
     */
    public int repopulate() {
        // Saves the current selection.
        final long keystoreId = getSelectedKeystoreId();
        final String keyAlias = getSelectedKeyAlias();

        this.removeAllItems();
        populate();

        // Reselects the previous selection; if still available.
        return select(keystoreId, keyAlias);
    }

    /**
     * Select the key matching the given keystore ID and private key alias.
     * If there is no match, the previous selection is unselected.
     *
     * @param keystoreId    the keystore ID
     * @param keyAlias      the private key alias
     * @return index of selected key; or -1 if no match
     */
    public int select(final long keystoreId, final String keyAlias) {
        if (keyAlias == null) {
            setSelectedIndex(0);
            return 0;
        }

        // Try exact match first; check for legacy keystore match only if that fails
        int idx = findIndex(keystoreId, keyAlias, false);
        if (idx < 0) idx = findIndex(keystoreId, keyAlias, true);
        setSelectedIndex(idx);
        return idx;
    }

    private int findIndex(long keystoreId, String keyAlias, boolean matchLegacy) {
        if (keyAlias == null)
            return getItemCount() > 0 && getItemAt(0) == ITEM_DEFAULT_SSL ? 0 : -1;
        for (int i = 0; i < getItemCount(); ++ i) {
            final PrivateKeyItem item = (PrivateKeyItem)getItemAt(i);
            if (keystoreIdMatches(keystoreId, item.keystoreId, matchLegacy) && keyAlias.equalsIgnoreCase(item.keyAlias)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean keystoreIdMatches(long wantId, long id, boolean matchLegacy) {
        // Checks for wildcard keystore ID (-1), exact match with keystore ID, or (if matchLegacy) if the key used to be in the Software Static keystore (removed for 5.0)
        return wantId == id || wantId == -1 || (matchLegacy && wantId == 0);
    }

    /**
     * @return keystore ID of current selection; -1 if none selected
     */
    public long getSelectedKeystoreId() {
        final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
        if (item == null) return -1;
        return item.keystoreId;
    }

    /**
     * @return key alias of current selection; null if none selected
     */
    public String getSelectedKeyAlias() {
        final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
        if (item == null) return null;
        return item.keyAlias;
    }

    /** @return key algorithm of current select ("RSA", "EC", "ECDSA") or null if none selected. */
    public String getSelectedKeyAlgorithm() {
        final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
        if (item == null) return null;
        return item.keyAlgorithm;
    }

    /**
     * @return true if the current selection is set to the Default SSL Key.
     */
    public boolean isSelectedDefaultSsl() {
        final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
        return item != null && (item == ITEM_DEFAULT_SSL || item.keyAlias == null || item.keystoreId == -1);
    }

    public void selectDefaultSsl() {
        setSelectedIndex(0);
    }

    public static void main(String[] args) {
        new PrivateKeysComboBox();
    }

    public boolean isIncludeRestrictedAccessKeys() {
        return _includeRestrictedAccessKeys;
    }

    public void setIncludeRestrictedAccessKeys(boolean includeRestrictedAccessKeys) {
        if (this._includeRestrictedAccessKeys != includeRestrictedAccessKeys) {
            this._includeRestrictedAccessKeys = includeRestrictedAccessKeys;

            // Repopulate list and restore previous selection
            final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
            populate();
            if (item != null)
                select(item.keystoreId, item.keyAlias);
        }
    }
}
