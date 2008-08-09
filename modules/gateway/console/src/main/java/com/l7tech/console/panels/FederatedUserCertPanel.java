package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.HexUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

public class FederatedUserCertPanel extends UserCertPanel {
    /**
     * Create a new NonFederatedUserCertPanel
     */
    public FederatedUserCertPanel(FederatedUserPanel userPanel, EntityListener entityListener, boolean canUpdate) {
        super(userPanel, entityListener, canUpdate);
    }

    protected JButton getRemoveCertButton() {
        if (removeCertButton == null) {
            removeCertButton = new JButton();
            removeCertButton.setText("Remove");
            removeCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String msg = "Are you sure you want to remove the user certificate?";

                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                            msg, "Remove User Certificate",
                            JOptionPane.YES_NO_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {

                        // remove the user cert
                        try {
                            identityAdmin.revokeCert(user);  //todo: dialog on error?
                            // reset values and redisplay
                            cert = null;
                            if (parentListener != null)
                                parentListener.entityUpdated(new EntityEvent(this, new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getName(), null)));

                            loadCertificateInfo();
                            if (userPanel instanceof FederatedUserPanel) {
                                FederatedUserPanel o = (FederatedUserPanel)userPanel;
                                o.getX509SubjectNameTextField().setEditable(true);
                            }

                        } catch (UpdateException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        } catch (ObjectNotFoundException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        }
                    }
                }
            });
        }
        return removeCertButton;
    }

    protected boolean isCertOk(TrustedCert tc) throws IOException, CertificateException {
        boolean ok = false;
        FederatedUserPanel fup = (FederatedUserPanel)userPanel;
        String subjectDNFromCert = tc.getSubjectDn();
        if (checkCertRelatedToSSG(tc)) {
            JOptionPane.showMessageDialog(this,
                          "This cert cannot be associated to this user " +
                          "because it is related to the\n" +
                          "SecureSpan Gateway's root cert.",
                          "Cannot add this cert",
                          JOptionPane.ERROR_MESSAGE);
        } else {
            if (userPanel.getUser().getSubjectDn().length() > 0) {

                if (subjectDNFromCert.compareToIgnoreCase(fup.getX509SubjectNameTextField().getText()) != 0) {
                    //prompt you if he wants to replace the subject DN name
                    Object[] options = {"Replace", "Cancel"};
                    int result = JOptionPane.showOptionDialog(fup.topParent,
                            "<html>The User's Subject DN is different from the one appearing in certificate being imported." +
                            "<br>The user's Subject DN: " +  fup.getX509SubjectNameTextField().getText() +
                            "<br>The Subject DN in the certiticate: " + subjectDNFromCert  +
                            "<br>The Subject DN will be replaced with the one from the certificate" +
                            "?<br>" +
                            "<center>The certificate will not be added if this operation is cancelled." +
                            "</center></html>",
                            "Replace the Subject DN?",
                            0, JOptionPane.WARNING_MESSAGE,
                            null, options, options[1]);
                    if (result == 0) {
                        fup.getX509SubjectNameTextField().setText(subjectDNFromCert);
                        ok = true;
                    }
                } else {
                    ok = true;
                }
            } else {
                // simply copy the dn to the user panel
                fup.getX509SubjectNameTextField().setText(subjectDNFromCert);
                return true;
            }
            if (ok) {
                fup.getX509SubjectNameTextField().setEditable(false);
            }
        }
        return ok;
    }

    /**
     * Check whether or not the passed cert is related somehow to the ssg's root cert.
     * @return true if it is
     */
    private boolean checkCertRelatedToSSG(TrustedCert trustedCert) throws IOException, CertificateException {
        if (ssgcert == null) {
            ssgcert = getTrustedCertAdmin().getSSGRootCert();
        }
        byte[] certbytes = HexUtils.decodeBase64(trustedCert.getCertBase64());
        X509Certificate[] chainToVerify = CertUtils.decodeCertChain(certbytes);
        try {
            CertUtils.verifyCertificateChain(chainToVerify, ssgcert, chainToVerify.length);
        } catch ( CertUtils.CertificateUntrustedException e) {
            // this is what we were hoping for
            log.finest("the cert is not related.");
            return false;
        }
        log.finest("The cert appears to be related!");
        return true;
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }
}
