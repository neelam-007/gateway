package com.l7tech.console.panels;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.common.gui.util.FileChooserUtil;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.util.CertUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.SsmApplication;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.*;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
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

    private JPanel mainPanel;
    private JLabel headerLabel;
    private JRadioButton copyAndPasteRadioButton;
    private JRadioButton fileRadioButton;
    private JRadioButton urlConnRadioButton;
    private JButton browseButton;
    private JTextField certFileName;
    private JTextArea copyAndPasteTextArea;
    private JTextField urlConnTextField;
    private X509Certificate[] certChain = null;
    private boolean defaultToSslOption;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertImportMethodsPanel.class.getName());

    /**
     * Constructor
     *
     * @param next  The next step panel
     * @param defaultToSslOption if true, defaults to "read from SSL connection"
     */
    public CertImportMethodsPanel(WizardStepPanel next, boolean defaultToSslOption) {
        super(next);
        this.defaultToSslOption = defaultToSslOption;
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
                SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
                    public void useFileChooser(JFileChooser fc) {
                        int returnVal = fc.showOpenDialog(CertImportMethodsPanel.this);

                        File file;
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            file = fc.getSelectedFile();

                            certFileName.setText(file.getAbsolutePath());
                        } else {
                            // cancelled by user
                        }
                    }
                });
            }
        });

        ButtonGroup bg = new ButtonGroup();
        bg.add(copyAndPasteRadioButton);
        bg.add(fileRadioButton);
        bg.add(urlConnRadioButton);

        // urlConnection as the default
        if (defaultToSslOption) {
            urlConnRadioButton.setSelected(true);
        } else {
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

        if (fileRadioButton.isSelected()) {
            try {
                is = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                    public InputStream run() {
                        try {
                            return new FileInputStream(new File(certFileName.getText().trim()));
                        } catch (FileNotFoundException fne) {
                            JOptionPane.showMessageDialog(CertImportMethodsPanel.this,
                                                          resources.getString("view.error.filenotfound"),
                                                          resources.getString("view.error.title"),
                                                          JOptionPane.ERROR_MESSAGE);
                            return null;
                        } catch (AccessControlException ace) {
                            TopComponents.getInstance().showNoPrivilegesErrorMessage();
                            return null;
                        }
                    }
                });
                if (is == null) return false;
                Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(is);
                certChain = certs.toArray(new X509Certificate[0]);


            } catch (CertificateException ce) {
                final String msg = resources.getString("view.error.cert.generate");
                logger.log(Level.INFO, msg, ce);
                JOptionPane.showMessageDialog(this, msg,
                                              resources.getString("view.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } else if (urlConnRadioButton.isSelected()) {

            String certURL;
            String hostnameURL;
            try {
                certURL = urlConnTextField.getText().trim();
                if (!certURL.startsWith("https://") && !certURL.startsWith("ldaps://")) {
                    JOptionPane.showMessageDialog(this, resources.getString("view.error.urlNotSsl"),
                                                  resources.getString("view.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                boolean wasLdap = false;
                if (certURL.startsWith("ldaps://")) {
                    wasLdap = true;
                    certURL = "https://" + certURL.substring(8);
                }
                URL url = new URL(certURL);
                hostnameURL = url.getHost();
                if (wasLdap && url.getPort()==-1) {
                    certURL = "https://" + hostnameURL + ":636/";
                    new URL(certURL);
                }
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.urlMalformed"),
                                              resources.getString("view.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }

            try {

                certChain = getTrustedCertAdmin().retrieveCertFromUrl(certURL, true);

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

            if (certChain == null || certChain.length < 1) {
                final String msg = "Certificate chain is missing or empty.";
                logger.log(Level.INFO, msg);
                JOptionPane.showMessageDialog(this, msg,
                                              resources.getString("view.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }

            X509Certificate cert = certChain[0];

            // retrieve the value of cn
            String subjectName = cert.getSubjectDN().getName();
            String cn = CertUtils.extractCommonNameFromClientCertificate(cert);

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

            String certPem = copyAndPasteTextArea.getText();
            if (certPem == null || certPem.trim().length() == 0) {
                JOptionPane.showMessageDialog(this, resources.getString("view.error.pem.cert.content"),
                                              resources.getString("view.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }

            try {
                is = new ByteArrayInputStream(certPem.getBytes("UTF-8"));
                Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(is);
                certChain = certs.toArray(new X509Certificate[0]);
            } catch (Exception e) {
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
        if (settings == null || certChain == null || certChain.length < 1 || certChain[0] == null)
            return;

        if (settings instanceof TrustedCert) {
            TrustedCert tc = (TrustedCert) settings;

            try {
                tc.setCertificate(certChain[0]);
            } catch (CertificateEncodingException e) {
                JOptionPane.showMessageDialog(this, resources.getString("cert.encode.error"),
                                              resources.getString("view.error.title"),
                                              JOptionPane.ERROR_MESSAGE);
            }
        } else if (settings instanceof SsgKeyEntry) {
            SsgKeyEntry ssgKeyEntry = (SsgKeyEntry)settings;
            ssgKeyEntry.setCertificateChain(certChain);
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
        return Registry.getDefault().getTrustedCertManager();
    }


    public X509Certificate[] getCertChain() {
        return certChain;
    }
}
