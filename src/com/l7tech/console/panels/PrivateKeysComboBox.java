package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
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
    private static final Logger _logger = Logger.getLogger(AssertionKeyAliasEditor.class.getName());

    /** An item in the combo box. */
    private static class PrivateKeyItem {
        public long keystoreId;
        public String keystoreName;
        public String keyAlias;
        public PrivateKeyItem(final long keystoreId, final String keystoreName, final String keyAlias) {
            this.keystoreId = keystoreId;
            this.keystoreName = keystoreName;
            this.keyAlias = keyAlias;
        }
        public String toString() {
            return "'" + keyAlias + "'" + " in " + keystoreName;
        }
    }

    private boolean _includeHardwareKeystore;

    /**
     * Creates a combo box prepopulated with a list of private keys from all SSG
     * keystores; and with none selected initially.
     */
    public PrivateKeysComboBox() {
        this(true);
    }

    /**
     * Creates a combo box prepopulated with a list of private keys from specified SSG
     * keystores; and with none selected initially.
     */
    public PrivateKeysComboBox(final boolean includeHardwareKeystore) {
        _includeHardwareKeystore = includeHardwareKeystore;
        populate();
        setSelectedIndex(-1);
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    private void populate() {
        try {
            final java.util.List<TrustedCertAdmin.KeystoreInfo> keystores = getTrustedCertAdmin().findAllKeystores(_includeHardwareKeystore);
            final List<PrivateKeyItem> items = new ArrayList<PrivateKeyItem>();
            for (TrustedCertAdmin.KeystoreInfo keystore : keystores) {
                for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(keystore.id)) {
                    items.add(new PrivateKeyItem(keystore.id, keystore.name, entry.getAlias()));
                }
            }
            setModel(new DefaultComboBoxModel(items.toArray()));
        } catch (Exception e) {
            _logger.log(Level.WARNING, "Problem when listing keys in keystores.", e);
        }
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
        for (int i = 0; i < getItemCount(); ++ i) {
            final PrivateKeyItem item = (PrivateKeyItem)getItemAt(i);
            if (item.keystoreId == keystoreId && item.keyAlias.equals(keyAlias)) {
                setSelectedIndex(i);
                return i;
            }
        }
        setSelectedIndex(-1);
        return -1;
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

    /**
     * Check if the given alias corresponds to the default SSL key.
     *
     * @param alias the alias to check.  required
     * @return true if the specified alias appears to be the default SSL private key.
     */
    public boolean isDefaultSslKey(String alias) {
        // TODO find a way to make this not hardcoded;
        //      or better yet, remove this method completely once there is no longer any meaningful difference
        //      between the "default SSL key" and any other private key in the system
        return "SSL".equals(alias);
    }


}
