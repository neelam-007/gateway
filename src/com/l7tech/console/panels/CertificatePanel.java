package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the Certificate Info dialog
 */
class CertificatePanel extends JPanel {
    static Logger log = Logger.getLogger(CertificatePanel.class.getName());
    private X509Certificate cert;
    private UserPanel userPanel;
    private JLabel certStatusLabel;
    private EntityListener parentListener;
    private JComponent certificateView = new JLabel();

    private final GridBagConstraints certificateViewConstraints = new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
      GridBagConstraints.NORTHWEST,
      GridBagConstraints.BOTH,
      new Insets(15, 15, 0, 15), 0, 0);

    /**
     * Create a new CertificatePanel
     */
    public CertificatePanel(UserPanel userPanel, EntityListener l) {
        this.userPanel = userPanel;
        this.addHierarchyListener(hierarchyListener);
        parentListener = l;
        initComponents();
        applyFormSecurity();
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        setLayout(new GridBagLayout());
        // setTitle("Certificate Information");

        //certificateTable = new JTable();
        //certificateTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        certStatusLabel = new JLabel();
        Font f = certStatusLabel.getFont();
        certStatusLabel.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
        add(certStatusLabel,
          new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(15, 15, 0, 15), 0, 0));

        add(certificateView, certificateViewConstraints);

        // Buttons
        add(getRevokeCertButton(),
          new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(17, 12, 11, 11), 0, 0));
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
    private JButton getRevokeCertButton() {
        if (revokeCertButton == null) {
            revokeCertButton = new JButton();
            revokeCertButton.setText("Revoke");
            revokeCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    String msg = null;
                    if (userPanel.getProviderConfig().isWritable()) {
                        msg = "<html><center><b>Please confirm certificate revoke " +
                          "for user '" + user.getLogin() + "'<br>This will also" +
                          " revoke the user's password.</b></center></html>";
                    } else {
                        msg = "<html><center><b>Please confirm certificate revoke " +
                          "for user '" + user.getLogin() + "'<br>Contact the directory administrator to" +
                          " revoke the corresponding password.</b></center></html>";
                    }
                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getMainWindow(),
                      msg, "Revoke User Certificate",
                      JOptionPane.YES_NO_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {

                        // revoke the user cert
                        try {
                            final ClientCertManager man = (ClientCertManager)SsmApplication.getApplication().getBean("clientCertManager");
                            man.revokeUserCert(user);
                            // must tell parent to update user because version might have changed
                            EntityHeader eh = new EntityHeader();
                            eh.setStrId(user.getUniqueIdentifier());
                            eh.setName(user.getName());
                            eh.setType(EntityType.USER);
                            parentListener.entityUpdated(new EntityEvent(this, eh));
                        } catch (UpdateException e) {
                            log.log(Level.WARNING, "ERROR Revoking certificate", e);
                        } catch (ObjectNotFoundException e) {
                            log.log(Level.WARNING, "ERROR Revoking certificate", e);
                        }
                        // reset values and redisplay
                        cert = null;
                        loadCertificateInfo();
                    }
                }
            });
        }
        return revokeCertButton;
    }

    private void applyFormSecurity() {
        // list components that are subject to security (they require the full admin role)
        userPanel.securityFormAuthorizationPreparer.prepare(new Component[]{
            getRevokeCertButton()
        });
    }


    /**
     * load certificate info and updates the data and status of the
     * form elements
     */
    private void loadCertificateInfo() {
        try {
            boolean enabled = cert != null;
            getRevokeCertButton().setEnabled(enabled);
            if (enabled) {
                certStatusLabel.setText("Certificate Status: Issued");
            } else {
                certStatusLabel.setText("Certificate Status: Not Issued");
            }
            this.remove(certificateView);
            try {
                JComponent view = getCertView();
                if (view == null)
                    view = new JLabel();
                certificateView = new JScrollPane(view);
            } catch (CertificateEncodingException e) {
                log.log(Level.SEVERE, "Unable to decode this user's certificate", e);
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.SEVERE, "Unable to process this user's certificate", e);
            }
            add(certificateView, certificateViewConstraints);
            getRootPane().getContentPane().invalidate();
        } catch (Exception e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
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
                if (CertificatePanel.this.isShowing()) {
                    user = userPanel.getUser();
                    //getTestCertificate();
                    getUserCert();
                    CertificatePanel.this.loadCertificateInfo();
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
                    String alias = (String)e.nextElement();
                    Certificate c = ks.getCertificate(alias);
                    if (c != null && c instanceof X509Certificate) {
                        cert = (X509Certificate)c;
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
        ClientCertManager man = (ClientCertManager)SsmApplication.getApplication().getBean("clientCertManager");
        try {
            cert = (X509Certificate)man.getUserCert(user);
        } catch (FindException e) {
            log.log(Level.WARNING, "There was an error loading the certificate", e);
        }
    }

    /**
     * UI Elements
     */
    private JButton revokeCertButton;

    /**
     * The Bridge whose certificate we are inspecting.
     */
    private UserBean user;

}

