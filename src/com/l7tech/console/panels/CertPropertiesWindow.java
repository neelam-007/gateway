package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.CertUtils;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Calendar;
import java.util.logging.Logger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.rmi.RemoteException;

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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 1, null, null, null));
        final JButton _3;
        _3 = new JButton();
        saveButton = _3;
        _3.setText("Save");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _4;
        _4 = new JButton();
        cancelButton = _4;
        _4.setText("Cancel");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _5;
        _5 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final JTabbedPane _6;
        _6 = new JTabbedPane();
        _1.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 200), null));
        final JPanel _7;
        _7 = new JPanel();
        _7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 2, new Insets(20, 10, 10, 10), -1, -1));
        _6.addTab("General", _7);
        final JLabel _8;
        _8 = new JLabel();
        _8.setText("Certificate Name");
        _7.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _9;
        _9 = new JTextField();
        certNameTextField = _9;
        _7.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("Expired on");
        _7.add(_10, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _11;
        _11 = new JLabel();
        _11.setText("Issued by");
        _7.add(_11, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _12;
        _12 = new JLabel();
        _12.setText("Issued to");
        _7.add(_12, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _7.add(_13, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, 0, 2, 1, 6, null, null, null));
        final JTextField _14;
        _14 = new JTextField();
        certIssuedToTextField = _14;
        _14.setEditable(false);
        _7.add(_14, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _15;
        _15 = new JTextField();
        certIssuedByTextField = _15;
        _15.setEditable(false);
        _7.add(_15, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _16;
        _16 = new JTextField();
        certExpiredOnTextField = _16;
        _16.setEditable(false);
        _16.setText("");
        _7.add(_16, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JScrollPane _17;
        _17 = new JScrollPane();
        _6.addTab("Details", _17);
        final JPanel _18;
        _18 = new JPanel();
        certMainPanel = _18;
        _18.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), -1, -1));
        _17.setViewportView(_18);
        final JPanel _19;
        _19 = new JPanel();
        certPanel = _19;
        _19.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _18.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _20;
        _20 = new com.intellij.uiDesigner.core.Spacer();
        _18.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
        final JPanel _21;
        _21 = new JPanel();
        _21.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _6.addTab("Usage", _21);
        final JPanel _22;
        _22 = new JPanel();
        _22.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(10, 10, 10, 10), -1, -1));
        _21.add(_22, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JCheckBox _23;
        _23 = new JCheckBox();
        outboundSSLConnCheckBox = _23;
        _23.setText("Outbound SSL Connections ");
        _22.add(_23, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _24;
        _24 = new JCheckBox();
        signingClientCertCheckBox = _24;
        _24.setText("Signing Client Certificates");
        _22.add(_24, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _25;
        _25 = new JCheckBox();
        signingSAMLTokenCheckBox = _25;
        _25.setText("Signing SAML Tokens");
        _22.add(_25, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _26;
        _26 = new JCheckBox();
        signingServerCertCheckBox = _26;
        _26.setText("Signing Certificates for Outbound SSL Connections");
        _22.add(_26, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JPanel _27;
        _27 = new JPanel();
        _27.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 0, 10, 0), -1, -1));
        _22.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 0, null, null, null));
        final JLabel _28;
        _28 = new JLabel();
        _28.setText("The certificate is intented for:");
        _27.add(_28, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _29;
        _29 = new com.intellij.uiDesigner.core.Spacer();
        _21.add(_29, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    }


}
