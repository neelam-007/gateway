package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ComboBox that can be used to choose an available SecurePassword.
 */
public class SecurePasswordComboBox extends JComboBox {
    private static Logger logger = Logger.getLogger(SecurePasswordComboBox.class.getName());
    private List<SecurePassword> securePasswords;

    public SecurePasswordComboBox() {
        reloadPasswordList();
    }

    public void reloadPasswordList() {
        try {
            securePasswords = Registry.getDefault().getTrustedCertManager().findAllSecurePasswords();
            setModel(new DefaultComboBoxModel(securePasswords.toArray()));
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

    public void setSelectedSecurePassword(long oid) {
        for (int i = 0; i < securePasswords.size(); i++) {
            SecurePassword securePassword = securePasswords.get(i);
            if (oid == securePassword.getOid()) {
                setSelectedIndex(i);
            }
        }
    }
}
