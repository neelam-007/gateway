package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.Locale;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertImportMethodsPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JPanel certImportMethodsPane;
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

    public CertImportMethodsPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

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

    public boolean onNextButton() {
        boolean rc = false;
        InputStream is = null;
        CertificateFactory cf = null;

        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            logger.severe("Unable to get certificate factory object");
        }

        if (fileRadioButton.isSelected()) {
            try {
                is = new FileInputStream(new File(certFileName.getText().trim()));
                cert = (X509Certificate) cf.generateCertificate(is);
                is.close();
                rc = true;
            } catch (FileNotFoundException fne) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.filenotfound"),
                        resources.getString("view.error.title"),
                        JOptionPane.ERROR_MESSAGE);

            } catch (IOException ioe) {
                logger.warning("Unable to read the file: " + certFileName.getText().trim());
            } catch (CertificateException ce) {
                logger.warning("Unable to generate certificate. Certificate Exception caught.");
            }
        } else if (urlConnRadioButton.isSelected()) {
            try {

                // test if the URL is well formed first by creating a URL object
                URL url = new URL(urlConnTextField.getText().trim());

                X509Certificate[] certs = getTrustedCertAdmin().retrieveCertFromUrl(urlConnTextField.getText().trim(), true);
                cert = certs[0];
                rc = true;

            } catch (TrustedCertAdmin.HostnameMismatchException e) {
                logger.warning("Hostname does not match with the one the certificate is issued to");
            } catch (MalformedURLException me) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.urlMalformed"),
                                       resources.getString("view.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                logger.warning("Unable to retrieve certificate via SSL connection: " + urlConnTextField.getText().trim());
            }

        } else if (copyAndPasteRadioButton.isSelected()) {
            //todo:

        }


        return rc;

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
                    //todo:
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
        certImportMethodsPane = _2;
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
