package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

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
    private JLabel headerLabel;
    private JRadioButton copyAndPasteRadioButton;
    private JRadioButton fileRadioButton;
    private JRadioButton urlConnRadioButton;
    private JButton browseButton;
    private JTextField certFileName;
    private JTextArea copyAndPasteTextArea;
    private JTextField urlConnTextField;
    private X509Certificate cert = null;
    private boolean sslOptionAllowed;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertImportMethodsPanel.class.getName());

    /**
     * Constructor
     *
     * @param next  The next step panel
     */
    public CertImportMethodsPanel(WizardStepPanel next, boolean sslOptionAllowed) {
        super(next);
        this.sslOptionAllowed = sslOptionAllowed;
        initialize();
    }

    /**
     * Initialization of the winodw.
     */
    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        FontUtil.resizeFont(headerLabel, 1.2);

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
        if (sslOptionAllowed) {
            urlConnRadioButton.setSelected(true);
        } else {
            urlConnRadioButton.setEnabled(false);
            urlConnTextField.setEditable(false);
            fileRadioButton.setSelected(true);
        }

        updateEnableDisable();

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
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    public String getDescription() {
        return "Enter the HTTPS URL of the certificate, import the certificate file, or " +
                "cut and paste the certificate in PEM format into the window.";
    }

    /**
     * Perform the task upon the Next button being clicked.
     *
     * @return true if it is ok to proceed to the next panel. otherwise, false.
     */
    public boolean onNextButton() {

        InputStream is = null;
        CertificateFactory cf = CertUtils.getFactory();

        if (fileRadioButton.isSelected()) {
            try {
                is = new FileInputStream(new File(certFileName.getText().trim()));
                cert = (X509Certificate)CertUtils.getFactory().generateCertificate(is);

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
                return false;
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
                JOptionPane.showMessageDialog(this, resources.getString("view.error.url.io.error") + "\n" +
                                       urlConnTextField.getText().trim() + "\nPlease ensure the URL is correct.",
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
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
                        "<br>" + "Do you want to accept the certificate?" + "</br></html>",
                        "Hostname Mismatch",
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
                JOptionPane.showMessageDialog(this, resources.getString("view.error.pem.cert.begin.marker.missing") + PEM_CERT_BEGIN_MARKER + "\n",
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);
                return false;
            } else {
                // strip the begin marker
                certPEM = certPEM.substring(index + PEM_CERT_BEGIN_MARKER.length());
            }

            if((index = certPEM.indexOf(PEM_CERT_END_MARKER)) == -1) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.pem.cert.end.marker.missing") + PEM_CERT_END_MARKER + "\n",
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

            if (certDER == null || certDER.length < 1) {
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
                return false;
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
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        urlConnRadioButton = new JRadioButton();
        urlConnRadioButton.setSelected(false);
        urlConnRadioButton.setText("Retrieve via SSL Connection (HTTPS URL):");
        panel1.add(urlConnRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        fileRadioButton = new JRadioButton();
        fileRadioButton.setEnabled(true);
        fileRadioButton.setSelected(false);
        fileRadioButton.setText("Import from a File:");
        panel1.add(fileRadioButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        copyAndPasteRadioButton = new JRadioButton();
        copyAndPasteRadioButton.setText("Copy and Paste (Base64 PEM):");
        panel1.add(copyAndPasteRadioButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        urlConnTextField = new JTextField();
        panel1.add(urlConnTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        certFileName = new JTextField();
        panel1.add(certFileName, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        browseButton = new JButton();
        browseButton.setText("Browse");
        panel1.add(browseButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(2, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        copyAndPasteTextArea = new JTextArea();
        copyAndPasteTextArea.setRows(5);
        scrollPane1.setViewportView(copyAndPasteTextArea);
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 10, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        headerLabel = new JLabel();
        headerLabel.setText("Select one of the following methods for obtaining a certificate:");
        panel2.add(headerLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
