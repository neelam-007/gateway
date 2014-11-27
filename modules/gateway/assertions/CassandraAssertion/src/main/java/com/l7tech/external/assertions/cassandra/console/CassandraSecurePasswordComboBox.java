package com.l7tech.external.assertions.cassandra.console;

import com.l7tech.console.panels.SecurePasswordComboBox;
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
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 22/10/13
 * Time: 2:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class CassandraSecurePasswordComboBox extends SecurePasswordComboBox {

    private static Logger logger = Logger.getLogger(CassandraSecurePasswordComboBox.class.getName());
    private List<SecurePassword> securePasswords;

    @Override
    public void reloadPasswordList(SecurePassword.SecurePasswordType typeFilter) {
        try {

            //create a default password option
            SecurePassword defaultPassword = new SecurePassword();
            defaultPassword.setName("");
            defaultPassword.setType(typeFilter);

            securePasswords = new ArrayList<SecurePassword>();
            securePasswords.add(defaultPassword); //add default secure password
            securePasswords.addAll( Registry.getDefault().getTrustedCertManager().findAllSecurePasswords() );

            for (Iterator<SecurePassword> iterator = securePasswords.iterator(); iterator.hasNext(); ) {
                SecurePassword securePassword =  iterator.next();
                if (securePassword.getType() != typeFilter) {
                    iterator.remove();
                }
            }
            Collections.sort(securePasswords, new ResolvingComparator<SecurePassword, String>(new Resolver<SecurePassword, String>() {
                @Override
                public String resolve(final SecurePassword key) {
                    return key.getName().toLowerCase();
                }
            }, false));
            setModel(new DefaultComboBoxModel(securePasswords.toArray()));
        } catch (FindException e) {
            final String msg = "Unable to list stored passwords: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this, "Unable to list stored passwords", msg, e);
        }
    }

    @Override
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
            logger.log(Level.WARNING, "Password goid not available to current user");
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

    @Override
    public boolean containsItem(Goid goid) {
        for (SecurePassword securePassword : securePasswords) {
            if (Goid.equals(securePassword.getGoid(), goid)) {
                return true;
            }
        }

        return false;
    }
}
