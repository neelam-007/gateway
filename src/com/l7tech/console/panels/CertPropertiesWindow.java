package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.VersionException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * This class provides a dialog for viewing a trusted certificate and its usage.
 * Users can modify the cert name and ussage via the dialog.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CertPropertiesWindow extends JDialog {

    private JPanel mainPanel;
    private JPanel certMainPanel;
    private JPanel certPanel;
    private JTextField certExpiredOnTextField;
    private JTextField certIssuedToTextField;
    private JTextField certIssuedByTextField;
    private JTextField certNameTextField;
    private JCheckBox signingServerCertCheckBox;
    private JCheckBox signingSAMLTokenCheckBox;
    private JCheckBox signingClientCertCheckBox;
    private JCheckBox outboundSSLConnCheckBox;

    private JButton saveButton;
    private JButton cancelButton;
    private TrustedCert trustedCert = null;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertPropertiesWindow.class.getName());


    /**
     * Constructor
     *
     * @param owner The parent component.
     * @param tc   The trusted certificate.
     * @param editable  TRUE if the properties are editable
     */
    public CertPropertiesWindow(Dialog owner, TrustedCert tc, boolean editable) {
        super(owner, resources.getString("cert.properties.dialog.title"), true);
        trustedCert = tc;
        initialize(editable);
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Initialization of the window
     * @param editable  TRUE if the properties are editable
     */
    private void initialize(boolean editable) {

        JRootPane rp = this.getRootPane();
        rp.setPreferredSize(new Dimension(550, 350));

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        certMainPanel.setBackground(Color.white);
        certPanel.setLayout(new FlowLayout());
        certPanel.setBackground(Color.white);

        // disable the fields if the properties should not be modified
        if(!editable) {
            disableAll();
        }

        populateData();

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                TrustedCert tc = null;

                // create a new trusted cert
                try {
                    tc = (TrustedCert) trustedCert.clone();
                } catch (CloneNotSupportedException e) {
                    String errmsg = "Internal error! Unable to clone the trusted certificate.";
                    logger.severe(errmsg);
                    JOptionPane.showMessageDialog(mainPanel, errmsg + " \n" + "The certificate has not been updated",
                            resources.getString("save.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    // udpate the trusted cert with the new values
                    updateTrustedCert(tc);

                    // save the cert
                    getTrustedCertAdmin().saveCert(tc);

                } catch (SaveException e) {
                    logger.warning("Unable to save the trusted certificate in server");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.save.error"),
                            resources.getString("save.error.title"),
                            JOptionPane.ERROR_MESSAGE);

                } catch (RemoteException e) {
                    logger.severe("Unable to execute remote call due to remote exception");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.remote.exception"),
                            resources.getString("save.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } catch (VersionException e) {
                    logger.warning("Unable to save the trusted certificate: " + trustedCert.getName() + "; version exception.");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.version.error"),
                            resources.getString("save.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } catch (UpdateException e) {
                    logger.warning("Unable to update the trusted certificate in server");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.update.error"),
                            resources.getString("save.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }

                // suceeded, update the original trusted cert
                updateTrustedCert(trustedCert);

                // close the dialog
                dispose();

            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                //hide();
                dispose();
            }
        });

        signingClientCertCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setClientDesc(); }
        });
        signingClientCertCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setClientDesc(); }
        });

        signingServerCertCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setServerDesc(); }
        });
        signingServerCertCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setServerDesc(); }
        });

        signingSAMLTokenCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setSamlDesc(); }
        });
        signingSAMLTokenCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setSamlDesc(); }
        });

        outboundSSLConnCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setSslDesc(); }
        });
        outboundSSLConnCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setSslDesc(); }
        });

    }

    private void setSslDesc() {
        descriptionText.setText(resources.getString("usage.desc.ssl"));
    }

    private void setSamlDesc() {
        descriptionText.setText(resources.getString("usage.desc.saml"));
    }

    private void setServerDesc() {
        descriptionText.setText(resources.getString("usage.desc.server"));
    }

    private void setClientDesc() {
        descriptionText.setText(resources.getString("usage.desc.client"));
    }

    /**
     * Disable all fields and buttons
     */
    private void disableAll() {

        // all text fields
        certNameTextField.setEnabled(false);

        // all check boxes
        signingServerCertCheckBox.setEnabled(false);
        signingSAMLTokenCheckBox.setEnabled(false);
        signingClientCertCheckBox.setEnabled(false);
        outboundSSLConnCheckBox.setEnabled(false);

        // all buttons except the Cancel button
        saveButton.setEnabled(false);
    }

    /**
     * Populate the data to the views
     */
    private void populateData() {
        if (trustedCert == null) {
            return;
        }

        X509Certificate cert = null;
        try {
            cert = trustedCert.getCertificate();
        } catch (CertificateException e) {
             logger.warning(resources.getString("cert.decode.error"));
            JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.decode.error"),
                                           resources.getString("save.error.title"),
                                           JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            logger.warning(resources.getString("cert.decode.error"));
            JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.decode.error"),
                                           resources.getString("save.error.title"),
                                           JOptionPane.ERROR_MESSAGE);
        }

        // populate the general data
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(cert.getNotAfter());
        certExpiredOnTextField.setText(sdf.format(cal.getTime()));

        certIssuedToTextField.setText(CertUtils.extractUsernameFromClientCertificate(cert));
        certIssuedByTextField.setText(CertUtils.extractIssuerNameFromClientCertificate(cert));
        certNameTextField.setText(trustedCert.getName());

        // populate the details
        JComponent certView = getCertView(cert);

        if (certView == null) {
            certView = new JLabel();
        }

        certPanel.add(certView);

        // populate the usage
        if (trustedCert.isTrustedForSigningClientCerts()) {
            signingClientCertCheckBox.setSelected(true);
        }

        if (trustedCert.isTrustedForSigningSamlTokens()) {
            signingSAMLTokenCheckBox.setSelected(true);
        }

        if (trustedCert.isTrustedForSigningServerCerts()) {
            signingServerCertCheckBox.setSelected(true);
        }

        if (trustedCert.isTrustedForSsl()) {
            outboundSSLConnCheckBox.setSelected(true);
        }
    }

    /**
     * Returns a properties instance filled out with info about the certificate.
     */
    private JComponent getCertView(X509Certificate cert) {

        com.l7tech.common.gui.widgets.CertificatePanel certPanel = null;
        try {
            certPanel = new com.l7tech.common.gui.widgets.CertificatePanel(cert);
            certPanel.setCertBorderEnabled(false);
        } catch (CertificateEncodingException ee) {
            logger.warning("Unable to decode the certificate: " + trustedCert.getName());
        } catch (NoSuchAlgorithmException ae) {
            logger.warning("Unable to decode the certificate: " + trustedCert.getName() + ", Algorithm is not supported:" + cert.getSigAlgName());
        }

        return certPanel;
    }

    /**
     * Update the trusted cert object with the current settings (from the form).
     * @param tc  The trusted cert to be updated.
     */
    private void updateTrustedCert(TrustedCert tc) {
        if(tc != null) {
            tc.setName(certNameTextField.getText().trim());
            tc.setTrustedForSigningClientCerts(signingClientCertCheckBox.isSelected());
            tc.setTrustedForSigningSamlTokens(signingSAMLTokenCheckBox.isSelected());
            tc.setTrustedForSigningServerCerts(signingServerCertCheckBox.isSelected());
            tc.setTrustedForSsl(outboundSSLConnCheckBox.isSelected());
        }
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  The object reference.
     * @throws RuntimeException  if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca =
                (TrustedCertAdmin) Locator.
                getDefault().lookup(TrustedCertAdmin.class);
        if (tca == null) {
            throw new RuntimeException("Could not find registered " + TrustedCertAdmin.class);
        }

        return tca;
    }


    private JTextPane descriptionText;
    private JScrollPane descriptionPane;
}
