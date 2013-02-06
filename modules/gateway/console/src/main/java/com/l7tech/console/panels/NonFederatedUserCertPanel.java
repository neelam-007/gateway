package com.l7tech.console.panels;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the Certificate Info dialog
 */
class NonFederatedUserCertPanel extends UserCertPanel {
    static Logger log = Logger.getLogger(NonFederatedUserCertPanel.class.getName());

    /**
     * Create a new NonFederatedUserCertPanel
     */
    public NonFederatedUserCertPanel(UserPanel userPanel, EntityListener l, boolean canUpdate) {
        super(userPanel, l, canUpdate);
    }

    /**
     * Creates if needed the Ok button.
     */
    protected JButton getRemoveCertButton() {
        if (removeCertButton == null) {
            removeCertButton = new JButton();
            removeCertButton.setText("Revoke");
            removeCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String msg;
                    if (userPanel.getProviderConfig().isWritable()) {
                        msg = "<html><center><b>Please confirm certificate revoke " +
                          "for user '" + user.getLogin() + "'<br>This will also" +
                          " revoke the user's password.</b></center></html>";
                    } else {
                        msg = "<html><center><b>Please confirm certificate revoke " +
                          "for user '" + user.getLogin() + "'<br>Contact the directory administrator to" +
                          " revoke the corresponding password.</b></center></html>";
                    }
                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                      msg, "Revoke User Certificate",
                      JOptionPane.YES_NO_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {

                        // revoke the user cert
                        try {
                            identityAdmin.revokeCert(user);
                            // reset values and redisplay
                            cert = null;
                            loadCertificateInfo();
                            // must tell parent to update user because version might have changed
                            EntityHeader eh = new EntityHeader();
                            eh.setStrId(user.getId());
                            eh.setName(user.getName());
                            eh.setType(EntityType.USER);
                            if (parentListener != null)
                                parentListener.entityUpdated(new EntityEvent(this, eh));
                        } catch (UpdateException e) {
                            log.log(Level.WARNING, "ERROR Revoking certificate", e);
                        } catch (ObjectNotFoundException e) {
                            log.log(Level.WARNING, "ERROR Revoking certificate", e);
                        } 
                    }
                }
            });
        }
        return removeCertButton;
    }

    protected boolean isCertOk(TrustedCert tc) throws IOException, CertificateException {
        // No need to check validity here
        return true;
    }
}

