package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.CertUtils;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.Locale;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;

/**
 * This class provides users with a form for specifying the source or contents of the trusted 
 * certificate to be added to the trusted certificate store.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertImportMethodsPanel extends WizardStepPanel {

    private static String PEM_CERT_BEGIN_MARKER = "-----BEGIN CERTIFICATE-----";
    private static String PEM_CERT_END_MARKER = "-----END CERTIFICATE-----";
    private JPanel mainPanel;
    private JRadioButton copyAndPasteRadioButton;
    private JRadioButton fileRadioButton;
    private JRadioButton urlConnRadioButton;
    private JButton browseButton;
    private JTextField certFileName;
    private JTextArea copyAndPasteTextArea;
    private JTextField urlConnTextField;
    private X509Certificate cert = null;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertImportMethodsPanel.class.getName());

    /**
     * Constructor
     *
     * @param next  The next step panel
     */
    public CertImportMethodsPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    /**
     * Initialization of the winodw.
     */
    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                //Create a file chooser
                final JFileChooser fc = new JFileChooser();

                int returnVal = fc.showOpenDialog(CertImportMethodsPanel.this);

                File file = null;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    file = fc.getSelectedFile();

                    certFileName.setText(file.getAbsolutePath());
                } else {
                    // cancelled by user
                }

            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(copyAndPasteRadioButton);
        bg.add(fileRadioButton);
        bg.add(urlConnRadioButton);

        // urlConnection as the default
        urlConnRadioButton.setSelected(true);
        browseButton.setEnabled(false);

        copyAndPasteRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });

        fileRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });

        urlConnRadioButton.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        });
    }

    /**
     * Enable/disable the fields according the current selection.
     */
    private void updateEnableDisable() {
        if (copyAndPasteRadioButton.isSelected()) {
            browseButton.setEnabled(false);
            copyAndPasteTextArea.setEnabled(true);
            urlConnTextField.setEnabled(false);
            certFileName.setEnabled(false);

        } else if (fileRadioButton.isSelected()) {
            browseButton.setEnabled(true);
            copyAndPasteTextArea.setEnabled(false);
            urlConnTextField.setEnabled(false);
            certFileName.setEnabled(true);
        }

        if (urlConnRadioButton.isSelected()) {
            browseButton.setEnabled(false);
            copyAndPasteTextArea.setEnabled(false);
            urlConnTextField.setEnabled(true);
            certFileName.setEnabled(false);
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Enter Certificate Info";
    }

    /**
     * Perform the task upon the Next button being clicked.
     *
     * @return true if it is ok to proceed to the next panel. otherwise, false.
     */
    public boolean onNextButton() {

        InputStream is = null;
        CertificateFactory cf = null;

        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            logger.severe(resources.getString("view.error.get.cert.factory"));
            JOptionPane.showMessageDialog(this, resources.getString("view.error.get.cert.factory"),
                                    resources.getString("view.error.title"),
                                    JOptionPane.ERROR_MESSAGE);
            return false;

        }

        if (fileRadioButton.isSelected()) {
            try {
                is = new FileInputStream(new File(certFileName.getText().trim()));
                cert = (X509Certificate) cf.generateCertificate(is);

            } catch (FileNotFoundException fne) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.filenotfound"),
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                return false;

            } catch (CertificateException ce) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.cert.generate"),
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (urlConnRadioButton.isSelected()) {

            String hostnameURL = "";
            try {
                URL url = new URL(urlConnTextField.getText().trim());
                hostnameURL = url.getHost();
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.urlMalformed"),
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
            }

            try {

                X509Certificate[] certs = getTrustedCertAdmin().retrieveCertFromUrl(urlConnTextField.getText().trim(), true);
                cert = certs[0];

            } catch (TrustedCertAdmin.HostnameMismatchException e) {
                // This should never happen since we ask retrieveCertFromUrl() to ignore the mismatch
                // It is a coding error if HostnameMismatchException is caught
                logger.severe("Server coding error! Unexpected HostnameMismatchException caught");

                return false;

            } catch (IllegalArgumentException iae) {
                 JOptionPane.showMessageDialog(this, iae.getMessage(),
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
                return false;

            } catch (IOException ioe) {
                logger.warning("Unable to retrieve certificate via SSL connection: " + urlConnTextField.getText().trim());
                return false;
            }


            // retrieve the value of cn
            String subjectName = cert.getSubjectDN().getName();
            String cn = CertUtils.extractUsernameFromClientCertificate(cert);

            // use the subjectDN name if the CN attribute not found
            if (cn.length() == 0) {
                cn = subjectName;
            }

            if (!hostnameURL.equals(cn)) {
                Object[] options = {"Accept", "Cancel"};
                int result = JOptionPane.showOptionDialog(null,
                        "<html>The hostname in URL does not match with the certificate's subject name. " +
                        "<br>" + "Hostname in URL: " + hostnameURL + "</br>" +
                        "<br>" + "Subject DN in Certificate: " + cn + "</br>" +
                        "<br>" + "Do you still want to view the certificate?" + "</br></html>",
                        "Hostname mismatch",
                        0, JOptionPane.WARNING_MESSAGE,
                        null, options, options[1]);

                // abort if the user does not accept the hostname mismatch
                if (result == 1) {
                    return false;
                }
            }

        } else if (copyAndPasteRadioButton.isSelected()) {

            String certPEM = copyAndPasteTextArea.getText();
            int index = -1;

            if((index = certPEM.indexOf(PEM_CERT_BEGIN_MARKER)) == -1) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.pem.cert.begin.marker.missing") + PEM_CERT_BEGIN_MARKER,
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                return false;
            } else {
                // strip the begin marker
                certPEM = certPEM.substring(index + PEM_CERT_BEGIN_MARKER.length());
            }

            if((index = certPEM.indexOf(PEM_CERT_END_MARKER)) == -1) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.pem.cert.end.marker.missing") + PEM_CERT_END_MARKER,
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);

                return false;
            } else {
                // strip the end marker
                certPEM = certPEM.substring(0, index);
            }

            byte[] certDER = null;
            try {
                certDER = HexUtils.decodeBase64(certPEM, true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.pem.cert.decode"),
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
                return false;
            }

            is = new ByteArrayInputStream(certDER);

            try {
                cert = (X509Certificate) cf.generateCertificate(is);
            } catch (CertificateException e) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.cert.generate"),
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
            }

        }

        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.close.InputStream"),
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                // continue regardless of this error.
            }
        }

        return true;
    }


    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {

        if (settings != null) {

            if (settings instanceof TrustedCert) {

                TrustedCert tc = (TrustedCert) settings;

                try {
                    tc.setCertificate(cert);
                } catch (CertificateEncodingException e) {
                    JOptionPane.showMessageDialog(this, resources.getString("cert.encode.error"),
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
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
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JRadioButton _3;
        _3 = new JRadioButton();
        urlConnRadioButton = _3;
        _3.setText("Retrieve via SSL Connection");
        _3.setSelected(false);
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _4;
        _4 = new JRadioButton();
        fileRadioButton = _4;
        _4.setText("Import from a File");
        _4.setEnabled(true);
        _4.setSelected(false);
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _5;
        _5 = new JRadioButton();
        copyAndPasteRadioButton = _5;
        _5.setText("Copy and Paste (Base64 PEM)");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JTextField _6;
        _6 = new JTextField();
        urlConnTextField = _6;
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _7;
        _7 = new JTextField();
        certFileName = _7;
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JButton _8;
        _8 = new JButton();
        browseButton = _8;
        _8.setText("Browse");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JScrollPane _9;
        _9 = new JScrollPane();
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 2, 1, 0, 3, 7, 7, null, null, null));
        final JTextArea _10;
        _10 = new JTextArea();
        copyAndPasteTextArea = _10;
        _10.setRows(5);
        _9.setViewportView(_10);
        final com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _12;
        _12 = new JPanel();
        _12.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 0, 10, 0), -1, -1));
        _1.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 0, null, null, null));
        final JLabel _13;
        _13 = new JLabel();
        _13.setText("Select one of the following method for obtaining a certificate:");
        _12.add(_13, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }

}
