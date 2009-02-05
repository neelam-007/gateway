package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;

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
    private static final Logger _logger = Logger.getLogger(PrivateKeysComboBox.class.getName());

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

    private static final PrivateKeyItem ITEM_DEFAULT_SSL = new PrivateKeyItem(-1, null, null) {
        public String toString() {
            return "<Default SSL Key>";
        }
    };

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
     * @param includeHardwareKeystore  if true, hardware keystores should be included in the list.
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
            final java.util.List<KeystoreFileEntityHeader> keystores = getTrustedCertAdmin().findAllKeystores(_includeHardwareKeystore);
            final List<PrivateKeyItem> items = new ArrayList<PrivateKeyItem>();
            items.add(ITEM_DEFAULT_SSL);
            for (KeystoreFileEntityHeader keystore : keystores) {
                for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(keystore.getOid())) {
                    items.add(new PrivateKeyItem(keystore.getOid(), keystore.getName(), entry.getAlias()));
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
        if (keystoreId == -1 || keyAlias == null) {
            setSelectedIndex(0);
            return 0;
        }

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
     * @return true if the current selection is set to the Default SSL Key.
     */
    public boolean isSelectedDefaultSsl() {
        final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
        return item != null && (item == ITEM_DEFAULT_SSL || item.keyAlias == null || item.keystoreId == -1);
    }

    public void selectDefaultSsl() {
        setSelectedIndex(0);
    }
}
