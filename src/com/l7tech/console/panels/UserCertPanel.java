package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.event.*;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.UserBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.HierarchyEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Locale;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class UserCertPanel extends JPanel {
    static Logger log = Logger.getLogger(UserCertPanel.class.getName());

    private JButton removeCertButton;
    private JButton importCertButton;
    private UserBean user;
    private X509Certificate cert;
    private UserPanel userPanel;
    private JLabel certStatusLabel;
    private JComponent certificateView = new JLabel();
    public static final GridBagConstraints CERTIFICATE_VIEW_CONSTRAINTS = new GridBagConstraints(0, 1, 2, 1, 1.0, 1.0,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.BOTH,
            new Insets(15, 15, 0, 15), 0, 0);
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());

    /**
     * Create a new CertificatePanel
     */
    public UserCertPanel(UserPanel userPanel) {
        this.userPanel = userPanel;
        this.addHierarchyListener(hierarchyListener);
        initComponents();
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
                            final ClientCertManager man = (ClientCertManager) Locator.getDefault().lookup(ClientCertManager.class);
                            man.revokeUserCert(user);
                        } catch (UpdateException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        } catch (ObjectNotFoundException e) {
                            log.log(Level.WARNING, "ERROR Removing certificate", e);
                        }
                        // reset values and redisplay
                        cert = null;
                        loadCertificateInfo();
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

    /**
     * obtain certificate from the application default
     * truststore.
     * This method is for testing and will be removed once the
     * cert support is implemented
     */
    private void getTestCertificate() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] trustStorPassword = Preferences.getPreferences().getTrustStorePassword().toCharArray();
            String trustStoreFile = Preferences.getPreferences().getTrustStoreFile();
            FileInputStream ksfis = new FileInputStream(trustStoreFile);
            try {
                ks.load(ksfis, trustStorPassword);
                for (Enumeration e = ks.aliases(); e.hasMoreElements();) {
                    String alias = (String) e.nextElement();
                    Certificate c = ks.getCertificate(alias);
                    if (c != null && c instanceof X509Certificate) {
                        cert = (X509Certificate) c;
                        break;
                    }
                }
            } catch (FileNotFoundException e) {
                log.log(Level.WARNING, "Could not find application trust store", e);
            } finally {
                if (ksfis != null) ksfis.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getUserCert() {
        ClientCertManager man = (ClientCertManager) Locator.getDefault().lookup(ClientCertManager.class);
        try {
            cert = (X509Certificate) man.getUserCert(user);
        } catch (FindException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        }
    }

    private void saveUserCert(TrustedCert tc) throws IOException, CertificateException, UpdateException {
        ClientCertManager man = (ClientCertManager) Locator.getDefault().lookup(ClientCertManager.class);

        man.recordNewUserCert(user, tc.getCertificate());
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

            Object o = w.getCollectedInformation();

            if (o instanceof TrustedCert) {

                final TrustedCert tc = (TrustedCert) o;

                if (tc != null) {

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {

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
                    });
                }
            }
        }

    };


}
