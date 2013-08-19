package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;

import javax.swing.*;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private static final PrivateKeyItem ITEM_DEFAULT_SSL = new PrivateKeyItem(GoidEntity.DEFAULT_GOID, null, null, "NONE") {
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

    private void populate() {
        try {
            if (!Registry.getDefault().isAdminContextPresent()) {
                _logger.log(Level.WARNING, "Unable to populate PrivateKeysComboBox: Not connected to Gateway");
                setModel(new DefaultComboBoxModel(new PrivateKeyItem[0]));
                return;
            }
            final TrustedCertAdmin certAdmin = Registry.getDefault().getTrustedCertManager();
            final java.util.List<KeystoreFileEntityHeader> keystores = certAdmin.findAllKeystores(_includeHardwareKeystore);
            final List<PrivateKeyItem> items = new ArrayList<PrivateKeyItem>();
            if (_includeDefaultSslKey)
                items.add(ITEM_DEFAULT_SSL);
            for (KeystoreFileEntityHeader keystore : keystores) {
                for (SsgKeyEntry entry : certAdmin.findAllKeys(keystore.getGoid(), _includeRestrictedAccessKeys)) {
                    items.add(new PrivateKeyItem(keystore.getGoid(), keystore.getName(), entry.getAlias(), entry.getCertificate().getPublicKey().getAlgorithm()));
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
        final Goid keystoreId = getSelectedKeystoreId();
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
    public int select(final Goid keystoreId, final String keyAlias) {
        if (keyAlias == null) {
            setSelectedIndex(-1);
            return 0;
        }

        // Try exact match first; check for legacy keystore match only if that fails
        int idx = findIndex(keystoreId, keyAlias, false);
        if (idx < 0) idx = findIndex(keystoreId, keyAlias, true);
        setSelectedIndex(idx);
        return idx;
    }

    private int findIndex(Goid keystoreId, String keyAlias, boolean matchLegacy) {
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

    private static boolean keystoreIdMatches(Goid wantId, Goid id, boolean matchLegacy) {
        // Checks for wildcard keystore ID (-1), exact match with keystore ID, or (if matchLegacy) if the key used to be in the Software Static keystore (removed for 5.0)
        return Goid.equals(wantId, id) || Goid.isDefault(wantId) || (matchLegacy && Goid.equals(new Goid(0,0), wantId));
    }

    /**
     * @return keystore ID of current selection; -1 if none selected
     */
    public Goid getSelectedKeystoreId() {
        final PrivateKeyItem item = (PrivateKeyItem)getSelectedItem();
        if (item == null) return GoidEntity.DEFAULT_GOID;
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
        return item != null && (item == ITEM_DEFAULT_SSL || item.keyAlias == null || Goid.isDefault(item.keystoreId));
    }

    public void selectDefaultSsl() {
        setSelectedIndex(getModel().getSize() > 0 ? 0 : -1);
    }

    public static void main(String[] args) {
        new PrivateKeysComboBox();
    }

    public boolean isIncludeRestrictedAccessKeys() {
        return _includeRestrictedAccessKeys;
    }

    /**
     * Change inclusion of restricted-access private keys (eg, audit viewer key).
     * <p/>Remember to call {@link #repopulate()} after changing this setting.
     *
     * @param includeRestrictedAccessKeys true to include restricted access keys in the list, false to leave them out.
     */
    public void setIncludeRestrictedAccessKeys(boolean includeRestrictedAccessKeys) {
        this._includeRestrictedAccessKeys = includeRestrictedAccessKeys;
    }

    public boolean isIncludeDefaultSslKey() {
        return _includeDefaultSslKey;
    }

    /**
     * Change inclusion of default SSL key setting.
     * <p/>Remember to call {@link #repopulate()} after changing this setting.
     *
     * @param incDef true to include a special entry just for the default SSL key, that binds to it with a null alias and keystore ID of -1.
     */
    public void setIncludeDefaultSslKey(boolean incDef) {
        this._includeDefaultSslKey = incDef;
    }

    public boolean isIncludeHardwareKeystore() {
        return _includeHardwareKeystore;
    }

    /**
     * Change hardware keystore setting.
     * <p/>Remember to call {@link #repopulate()} after changing this setting.
     *
     * @param incHardware true to include hardware keystore keys in the list; false to omit them.
     */
    public void setIncludeHardwareKeystore(boolean incHardware) {
        this._includeHardwareKeystore = incHardware;
    }
}
