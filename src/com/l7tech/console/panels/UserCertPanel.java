package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class UserCertPanel extends JPanel {
    static Logger log = Logger.getLogger(UserCertPanel.class.getName());

    private JButton removeCertButton;
    private JButton importCertButton;
    private User user;
    private X509Certificate cert;
    private FederatedUserPanel userPanel;
    private JLabel certStatusLabel;
    private JComponent certificateView = new JLabel();
    private X509Certificate ssgcert = null;
    IdentityAdmin identityAdmin = null;
    public static final GridBagConstraints CERTIFICATE_VIEW_CONSTRAINTS = new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.BOTH,
            new Insets(15, 15, 0, 15), 0, 0);
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());

    /**
     * Create a new CertificatePanel
     */
    public UserCertPanel(FederatedUserPanel userPanel) {
        this.userPanel = userPanel;
        this.addHierarchyListener(hierarchyListener);
        initComponents();
        identityAdmin = Registry.getDefault().getIdentityAdmin();
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        setLayout(new GridBagLayout());

        certStatusLabel = new JLabel();
        Font f = certStatusLabel.getFont();
        certStatusLabel.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
        add(certStatusLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.NONE,
                        new Insets(15, 15, 0, 15), 0, 0));

        add(certificateView, CERTIFICATE_VIEW_CONSTRAINTS);

        // Buttons
        add(getImportCertButton(),
                new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST,
                        GridBagConstraints.NONE,
                        new Insets(17, 0, 11, 10), 0, 0));
         add(getRemoveCertButton(),
                new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST,
                        GridBagConstraints.NONE,
                        new Insets(17, 0, 11, 5), 0, 0));
    }

    /**
     * Returns a properties instance filled out with info about the certificate.
     */
    private JComponent getCertView()
            throws CertificateEncodingException, NoSuchAlgorithmException {
        if (cert == null)
            return null;
        com.l7tech.common.gui.widgets.CertificatePanel i = new com.l7tech.common.gui.widgets.CertificatePanel(cert);
        i.setCertBorderEnabled(false);
        return i;
    }

    /**
     * Creates if needed the Ok button.
     */
    private JButton getRemoveCertButton() {
        if (removeCertButton == null) {
            removeCertButton = new JButton();
            removeCertButton.setText("Remove");
            removeCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String msg = "Are you sure you want to remove the user certificate?";

                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getMainWindow(),
                            msg, "Remove User Certificate",
                            JOptionPane.YES_NO_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {

                        // remove the user cert
                        try {
                            identityAdmin.revokeCert(user);  //todo: dialog on error?
                            // reset values and redisplay
                            cert = null;
                            loadCertificateInfo();
                            userPanel.getX509SubjectNameTextField().setEditable(true);

                        } catch (UpdateException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        } catch (ObjectNotFoundException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        } catch (RemoteException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        }
                    }
                }
            });
        }
        return removeCertButton;
    }

    /**
     * Creates if needed the Ok button.
     */
    private JButton getImportCertButton() {
        if (importCertButton == null) {
            importCertButton = new JButton();
            importCertButton.setText("Import");
            importCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {

                    CertImportMethodsPanel sp = new CertImportMethodsPanel(
                            new CertDetailsPanel(null) {
                                public boolean canFinish() {
                                    return true;
                                }
                            }, false);

                    JFrame f = TopComponents.getInstance().getMainWindow();
                    Wizard w = new AddCertificateWizard(f, sp);
                    w.addWizardListener(wizardListener);

                    // register itself to listen to the addEvent
                    //addEntityListener(listener);

                    w.pack();
                    w.setSize(780, 560);
                    Utilities.centerOnScreen(w);
                    w.setVisible(true);

                }
            });
        }
        return importCertButton;
    }

    /**
     * load certificate info and updates the data and status of the
     * form elements
     */
    private void loadCertificateInfo() {
        try {
            boolean enabled = cert != null;
            getRemoveCertButton().setEnabled(enabled);
            getImportCertButton().setEnabled(!enabled);
            if (enabled) {
                certStatusLabel.setText("Certificate Status: Imported");
            } else {
                certStatusLabel.setText("Certificate Status: Not Imported");
            }
            this.remove(certificateView);
            try {
                JComponent view = getCertView();
                if (view == null)
                    view = new JLabel();
                certificateView = new JScrollPane(view);
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
            add(certificateView, CERTIFICATE_VIEW_CONSTRAINTS);
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
        return (cert != null) ? true : false;
    }

    // hierarchy listener
    private final HierarchyListener hierarchyListener = new HierarchyListener() {
        /**
         * Called when the hierarchy has been changed.
         */
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
            String certstr = identityAdmin.getUserCert(user);
            if (certstr == null) {
                cert = null;
            }  else {
                byte[] certbytes = HexUtils.decodeBase64(certstr);
                cert =  CertUtils.decodeCert(certbytes);
            }
        } catch (FindException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        } catch (CertificateException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        } catch (IOException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        }
        return cert;
    }

    private void saveUserCert(TrustedCert tc) throws IOException, CertificateException, UpdateException {
        identityAdmin.recordNewUserCert(user, tc.getCertificate());
    }

    /**
     * The callback for saving the new cert to the database
     */
    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard) we.getSource();

            Object o = w.getWizardInput();

            if (o instanceof TrustedCert) {

                final TrustedCert tc = (TrustedCert) o;

                if (tc != null) {

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            boolean certImported = false;

                            try {

                                String subjectDNFromCert = tc.getCertificate().getSubjectDN().getName();

                                if (userPanel instanceof FederatedUserPanel) {
                                    if (checkCertRelatedToSSG(tc)) {


                                        JOptionPane.showMessageDialog(UserCertPanel.this,
                                                      "This cert cannot be associated to this user " +
                                                      "because it is related to the\n" +
                                                      "SecureSpan Gateway's root cert.",
                                                      "Cannot add this cert",
                                                      JOptionPane.ERROR_MESSAGE);
                                    } else {
                                        FederatedUserPanel fup = userPanel;
                                        if (userPanel.getUser().getSubjectDn().length() > 0) {

                                            if (subjectDNFromCert.compareToIgnoreCase(fup.getX509SubjectNameTextField().getText()) != 0) {
                                                //prompt you if he wants to replace the subject DN name
                                                Object[] options = {"Replace", "Cancel"};
                                                int result = JOptionPane.showOptionDialog(fup.mainWindow,
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
                                                    certImported = true;
                                                }
                                            } else {
                                                certImported = true;
                                            }
                                        } else {
                                            // simply copy the dn to the user panel
                                            fup.getX509SubjectNameTextField().setText(subjectDNFromCert);
                                            certImported = true;
                                        }
                                        if (certImported) {
                                            fup.getX509SubjectNameTextField().setEditable(false);
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                log.log(Level.WARNING, "There was an error saving the certificate", e);
                                JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.save.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            } catch (CertificateException e) {
                                log.log(Level.WARNING, "There was an error saving the certificate", e);
                                JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.save.error"),
                                        resources.getString("save.error.title"),
                                        JOptionPane.ERROR_MESSAGE);
                            }

                            if (certImported) {
                                try {
                                    saveUserCert(tc);

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
                                    log.log(Level.WARNING, "There was an error saving the certificate", e);
                                    JOptionPane.showMessageDialog(UserCertPanel.this, resources.getString("cert.save.error"),
                                            resources.getString("save.error.title"),
                                            JOptionPane.ERROR_MESSAGE);
                                }
                            }

                        }
                    });
                }
            }
        }

    };

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
        } catch (CertUtils.CertificateUntrustedException e) {
            // this is what we were hoping for
            log.finest("the cert is not related.");
            return false;
        }
        log.finest("The cert appears to be related!");
        return true;
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca = Registry.getDefault().getTrustedCertManager();
        return tca;
    }

}
