package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.console.event.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.AccessControlException;
import java.security.cert.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public abstract class UserCertPanel extends JPanel {
    static Logger log = Logger.getLogger(UserCertPanel.class.getName());

    protected JButton removeCertButton;
    protected JButton importCertButton;
    protected JButton exportCertButton;
    protected User user;
    protected X509Certificate cert;
    protected UserPanel userPanel;
    private JLabel certStatusLabel;
    private JPanel certificateViewPanel;
    protected X509Certificate ssgcert = null;
    IdentityAdmin identityAdmin = null;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    protected final EntityListener parentListener;
    private boolean canUpdate;

    /**
     * Create a new NonFederatedUserCertPanel
     */
    public UserCertPanel(UserPanel userPanel, EntityListener entityListener, boolean canUpdate) {
        this.userPanel = userPanel;
        this.addHierarchyListener(hierarchyListener);
        this.parentListener = entityListener;
        this.canUpdate = canUpdate;
        initComponents();
        identityAdmin = Registry.getDefault().getIdentityAdmin();
        applyFormSecurity();
    }

    private void applyFormSecurity() {
        getImportCertButton().setEnabled(canUpdate);
        getRemoveCertButton().setEnabled(canUpdate);
    }


    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        certStatusLabel = new JLabel();
        Font f = certStatusLabel.getFont();
        certStatusLabel.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        labelPanel.setBorder(new EmptyBorder(8,0,8,0));
        labelPanel.add(certStatusLabel);
        mainPanel.add(labelPanel);

        certificateViewPanel = new JPanel();
        certificateViewPanel.setLayout(new BorderLayout());
        mainPanel.add(certificateViewPanel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBorder(new EmptyBorder(8,0,0,0));
        buttonPanel.add(Box.createGlue());
        buttonPanel.add(getImportCertButton());
        buttonPanel.add(getExportCertButton());
        buttonPanel.add(getRemoveCertButton());

        mainPanel.add(buttonPanel);

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    /**
     * Returns a properties instance filled out with info about the certificate.
     */
    private JComponent getCertView()
            throws CertificateEncodingException, NoSuchAlgorithmException {
        if (cert == null)
            return null;
        com.l7tech.gui.widgets.CertificatePanel i = new com.l7tech.gui.widgets.CertificatePanel(cert);
        i.setCertBorderEnabled(false);
        return i;
    }

    /**
     * Creates if needed the Remove/Revoke button.
     */
    protected abstract JButton getRemoveCertButton();

    protected JButton getImportCertButton() {
        if (importCertButton == null) {
            importCertButton = new JButton();
            importCertButton.setText("Import");
            importCertButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {

                    CertImportMethodsPanel sp = new CertImportMethodsPanel(
                            new CertDetailsPanel(null) {
                                @Override
                                public boolean canFinish() {
                                    return true;
                                }
                            }, false);

                    Frame f = TopComponents.getInstance().getTopParent();
                    Wizard w = new AddCertificateWizard(f, sp);
                    w.addWizardListener(wizardListener);

                    // register itself to listen to the addEvent
                    //addEntityListener(listener);

                    w.pack();
                    Utilities.centerOnScreen(w);
                    DialogDisplayer.display(w);

                }
            });
        }
        return importCertButton;
    }

   protected JButton getExportCertButton() {
        if (exportCertButton == null) {
            exportCertButton = new JButton();
            exportCertButton.setText("Export");
            exportCertButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (cert != null) {
                        AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                try {
                                    GuiCertUtil.exportCertificate(SwingUtilities.getWindowAncestor(UserCertPanel.this), cert);
                                } catch (AccessControlException ace) {
                                    TopComponents.getInstance().showNoPrivilegesErrorMessage();
                                }
                                return null;
                            }
                        });
                    }
                    else {
                        // something is wrong, the button should not have been enabled
                        exportCertButton.setEnabled(false);
                    }
                }
            });
        }
        return exportCertButton;
    }

    /**
     * load certificate info and updates the data and status of the
     * form elements
     */
    protected void loadCertificateInfo() {
        try {
            boolean enabled = cert != null;
            getRemoveCertButton().setEnabled(canUpdate && enabled);
            getExportCertButton().setEnabled(enabled);
            getImportCertButton().setEnabled(canUpdate && !enabled);
            if (enabled) {
                certStatusLabel.setText("Certificate Status: Imported");
            } else {
                certStatusLabel.setText("Certificate Status: Not Imported");
            }
            try {
                JComponent view = getCertView();
                if (view == null)
                    view = new JLabel();
                certificateViewPanel.removeAll();
                certificateViewPanel.add(new JScrollPane(view));
            } catch (CertificateEncodingException e) {
                log.log(Level.WARNING, "Unable to decode this user's certificate", e);
                JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.decode.error"),
                        resources.getString("load.error.title"),
                        JOptionPane.ERROR_MESSAGE);
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.WARNING, "Unable to process this user's certificate", e);
                JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.decode.error"),
                        resources.getString("load.error.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            getRootPane().getContentPane().invalidate();
        } catch (Exception e) {
            log.log(Level.SEVERE, "There was an error loading the certificate", e);
            JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.save.error"),
                    resources.getString("load.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean certExist() {
        user = userPanel.getUser();
        getUserCert();
        return (cert != null);
    }

    // hierarchy listener
    private final HierarchyListener hierarchyListener = new HierarchyListener() {
        /**
         * Called when the hierarchy has been changed.
         */
        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            long flags = e.getChangeFlags();
            if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                if (UserCertPanel.this.isShowing()) {
                    user = userPanel.getUser();
                    //getTestCertificate();
                    getUserCert();
                    UserCertPanel.this.loadCertificateInfo();
                }
            }
        }
    };

    X509Certificate getUserCert() {
        try {
            if (user == null) {
                user = userPanel.getUser();
            }
            if (user instanceof LdapUser) {
                LdapUser luser = (LdapUser)user;
                try {
                    cert = getCertForLdapUser(luser);
                    canUpdate = false;
                } catch (LdapCertsNotEnabledException e) {
                    cert = getCertFromInternalTrustStore(user);
                    canUpdate = true;
                }
            } else {
                cert = getCertFromInternalTrustStore(user);
                canUpdate = true;
            }
        } catch (FindException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        } catch (CertificateException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        }
        applyFormSecurity();
        return cert;
    }

    private X509Certificate getCertFromInternalTrustStore(User user) throws FindException, CertificateException, IOException {
        String certstr = identityAdmin.getUserCert(user);
        X509Certificate theCert;
        if (certstr == null) {
            theCert = null;
        } else {
            byte[] certbytes = HexUtils.decodeBase64(certstr);
            theCert = CertUtils.decodeCert(certbytes);
        }
        return theCert;
    }

    private X509Certificate getCertForLdapUser(LdapUser luser) throws FindException, CertificateException, IOException, LdapCertsNotEnabledException {
        final IdentityProviderConfig whichLdap = identityAdmin.findIdentityProviderConfigByID(luser.getProviderId());
        if (whichLdap == null) return null;
        if (whichLdap.canIssueCertificates()) throw new LdapCertsNotEnabledException();

        return luser.getCertificate();
    }

    private void saveUserCert(TrustedCert tc) throws IOException, CertificateException, UpdateException {
        identityAdmin.recordNewUserCert(user, tc.getCertificate());
    }

    /**
     * The callback for saving the new cert to the database
     */
    protected WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        @Override
        public void wizardFinished(WizardEvent we) {
            // update the provider
            Wizard w = (Wizard)we.getSource();

            Object o = w.getWizardInput();

            if (o == null) return;
            if (!(o instanceof TrustedCert)) throw new IllegalStateException("Wizard returned a " + o.getClass().getName() + ", was expecting a " + TrustedCert.class.getName());

            final TrustedCert tc = (TrustedCert)o;

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!isCertOk(tc)) return;

                        saveUserCert(tc);

                        if (parentListener != null)
                            parentListener.entityUpdated(new EntityEvent(this, new EntityHeader(user.getId(), EntityType.USER, user.getName(), null)));

                        // reset values and redisplay
                        cert = null;

                        getUserCert();

                        loadCertificateInfo();
                    } catch (UpdateException e) {
                        log.log(Level.WARNING, "There was an error saving the certificate", e);
                        JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.save.error"),
                                resources.getString("save.error.title"),
                                JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e) {
                        log.log(Level.WARNING, "There was an error saving the certificate", e);
                        JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.save.error"),
                                resources.getString("save.error.title"),
                                JOptionPane.ERROR_MESSAGE);
                    } catch (CertificateException e) {
                        String msg;
                        if (e instanceof CertificateNotYetValidException)
                            msg = resources.getString("cert.notyetvalid.error");
                        else if (e instanceof CertificateExpiredException)
                            msg = resources.getString("cert.expired.error");
                        else
                            msg = resources.getString("cert.save.error");
                        log.log(Level.WARNING, "There was an error saving the certificate", e);
                        JOptionPane.showMessageDialog(UserCertPanel.this, msg,
                                resources.getString("save.error.title"),
                                JOptionPane.ERROR_MESSAGE);
                    }

                }
            });
        }
    };

    protected abstract boolean isCertOk(TrustedCert tc) throws IOException, CertificateException;

    private class LdapCertsNotEnabledException extends Throwable {
    }
}
