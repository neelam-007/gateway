package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ComboBox that can be used to choose an available SecurePassword.
 */
public class SecurePasswordComboBox extends JComboBox<SecurePassword> {
    private static Logger logger = Logger.getLogger(SecurePasswordComboBox.class.getName());
    private List<SecurePassword> securePasswords;

    public SecurePasswordComboBox() {
        reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
    }

    public SecurePasswordComboBox(SecurePassword.SecurePasswordType typeFilter) {
        reloadPasswordList(typeFilter);
    }

    public void reloadPasswordList() {
        reloadPasswordList(SecurePassword.SecurePasswordType.PASSWORD);
    }

    public void reloadPasswordList(SecurePassword.SecurePasswordType typeFilter) {
        try {
            SecurePassword selectedSecurePassword = this.getSelectedSecurePassword();
            securePasswords = new ArrayList<>(Registry.getDefault().getTrustedCertManager().findAllSecurePasswords());
            for (Iterator<SecurePassword> iterator = securePasswords.iterator(); iterator.hasNext(); ) {
                SecurePassword securePassword =  iterator.next();
                if (securePassword.getType() != typeFilter) {
                    iterator.remove();
                }
            }
            Collections.sort( securePasswords, new ResolvingComparator<>( new Resolver<SecurePassword,String>(){
                @Override
                public String resolve( final SecurePassword key ) {
                    return key.getName().toLowerCase();
                }
            }, false ) );
            setModel(new DefaultComboBoxModel<>(securePasswords.toArray(new SecurePassword[securePasswords.size()])));

            if (selectedSecurePassword != null && this.containsItem(selectedSecurePassword.getGoid())) {
                // Select currently selected item.
                this.setSelectedSecurePassword(selectedSecurePassword.getGoid());
            }
        } catch (FindException e) {
            final String msg = "Unable to list stored passwords: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this, "Unable to list stored passwords", msg, e);
        }
    }

    public SecurePassword getSelectedSecurePassword() {
        return (SecurePassword)getSelectedItem();
    }

    /**
     * @param goid the goid of the SecurePassword to select in the dropdown or #SecurePassword.DEFAULT_GOID if none should be selected.
     *            If the goid does not correspond to an available SecurePassword, the selected item will be shown as 'password details are unavailable'.
     */
    public void setSelectedSecurePassword(Goid goid) {
        Integer selectedIndex = null;
        for (int i = 0; i < securePasswords.size(); i++) {
            SecurePassword securePassword = securePasswords.get(i);
            if (Goid.equals(goid, securePassword.getGoid())) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex != null) {
            setSelectedIndex(selectedIndex);
        } else if (!Goid.isDefault(goid)) {
            // oid not found in available passwords - could be not readable by current user
            logger.log(Level.WARNING, "Password oid not available to current user");
            final SecurePassword unavailablePassword = new SecurePassword();
            unavailablePassword.setGoid(goid);
            unavailablePassword.setName("Current password (password details are unavailable)");
            securePasswords.add(0, unavailablePassword);
            setModel(new DefaultComboBoxModel<>(securePasswords.toArray(new SecurePassword[securePasswords.size()])));
            setSelectedIndex(0);
        } else {
            // password does not yet exist in the database
            setSelectedItem(null);
        }
    }

    public boolean containsItem(Goid goid) {
        for (SecurePassword securePassword : securePasswords) {
            if (Goid.equals(securePassword.getGoid(), goid)) {
                return true;
            }
        }

        return false;
    }
}
