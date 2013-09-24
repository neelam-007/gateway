package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.FontUtil;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

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
import java.security.KeyStoreException;
import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
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
    private JRadioButton base64PEMRadioButton;
    private JRadioButton base64RadioButton;
    private JRadioButton trustedCertRadioButton;
    private JRadioButton privateKeyRadioButton;
    private JComboBox trustedCertsComboBox;
    private PrivateKeysComboBox privateKeysComboBox;
    private SecurityZoneWidget zoneControl;
    private X509Certificate[] certChain = null;
    private boolean defaultToSslOption;
    private boolean allowSecurityZoneSelection = false;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertImportMethodsPanel.class.getName());

    /**
     * Constructor
     *
     * @param next  The next step panel
     * @param defaultToSslOption if true, defaults to "read from SSL connection"
     */
    public CertImportMethodsPanel(WizardStepPanel next, boolean defaultToSslOption) {
        this(next, defaultToSslOption, false);
    }

    /**
     *
     * @param next The next step panel
     * @param defaultToSslOption if true, defaults to "read from SSL connection"
     * @param allowSecurityZoneSelection if true, shows security zone widget for zone selection.
     */
    public CertImportMethodsPanel(WizardStepPanel next, boolean defaultToSslOption, boolean allowSecurityZoneSelection) {
        super(next);
        this.defaultToSslOption = defaultToSslOption;
        this.allowSecurityZoneSelection = allowSecurityZoneSelection;
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
            @Override
            public void actionPerformed(ActionEvent event) {
                //Create a file chooser
                SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
                    @Override
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
        bg.add(trustedCertRadioButton);
        bg.add(privateKeyRadioButton);

        if (!Registry.getDefault().isAdminContextPresent()) {
            fileRadioButton.setSelected(true);
            trustedCertRadioButton.setEnabled(false);
            privateKeyRadioButton.setEnabled(false);
        }

        privateKeysComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        privateKeysComboBox.setIncludeDefaultSslKey(false);
        privateKeysComboBox.setIncludeRestrictedAccessKeys(true);
        privateKeysComboBox.repopulate();

        trustedCertsComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        repopulateTrustedCertsComboBox();

        // urlConnection as the default
        if (defaultToSslOption) {
            urlConnRadioButton.setSelected(true);
        } else {
            fileRadioButton.setSelected(true);
        }

        updateEnableDisable();

        final ChangeListener enableListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateEnableDisable();
            }
        };
        copyAndPasteRadioButton.addChangeListener(enableListener);
        fileRadioButton.addChangeListener(enableListener);
        urlConnRadioButton.addChangeListener(enableListener);
        trustedCertRadioButton.addChangeListener(enableListener);
        privateKeyRadioButton.addChangeListener(enableListener);
        if (allowSecurityZoneSelection) {
            zoneControl.configure(EntityType.TRUSTED_CERT, OperationType.CREATE, null);
        } else {
            zoneControl.setVisible(false);
        }
    }

    private static class CertComboEntry {
        final TrustedCert cert;
        final String name;

        private CertComboEntry(TrustedCert cert) {
            this.cert = cert;
            this.name = cert.getCertificate().getSubjectDN().getName();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void repopulateTrustedCertsComboBox() {
        List<CertComboEntry> certs = new ArrayList<CertComboEntry>();
        if (Registry.getDefault().isAdminContextPresent()) {
            try {
                List<TrustedCert> tcs = Registry.getDefault().getTrustedCertManager().findAllCerts();
                for (TrustedCert tc : tcs) {
                    certs.add(new CertComboEntry(tc));
                }
            } catch (FindException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, "Unable to read trusted certs: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        trustedCertsComboBox.setModel(new DefaultComboBoxModel(certs.toArray()));
    }

    /**
     * Enable/disable the fields according the current selection.
     */
    private void updateEnableDisable() {
        final boolean copyPaste = copyAndPasteRadioButton.isSelected();
        copyAndPasteTextArea.setEnabled(copyPaste);
        base64PEMRadioButton.setEnabled(copyPaste);
        base64RadioButton.setEnabled(copyPaste);

        final boolean file = fileRadioButton.isSelected();
        browseButton.setEnabled(file);
        certFileName.setEnabled(file);

        urlConnTextField.setEnabled(urlConnRadioButton.isSelected());

        privateKeysComboBox.setEnabled(privateKeyRadioButton.isSelected());
        trustedCertsComboBox.setEnabled(trustedCertRadioButton.isSelected());
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Enter Certificate Info";
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    @Override
    public String getDescription() {
        return "Enter the HTTPS URL of the certificate, import the certificate file, or " +
                "cut and paste the certificate in PEM format into the window.";
    }

    /**
     * Perform the task upon the Next button being clicked.
     *
     * @return true if it is ok to proceed to the next panel. otherwise, false.
     */
    @Override
    public boolean onNextButton() {

        InputStream is = null;

        if (fileRadioButton.isSelected()) {
            try {
                is = AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                    @Override
                    public InputStream run() {
                        try {
                            return new FileInputStream(new File(certFileName.getText().trim()));
                        } catch (FileNotFoundException fne) {
                            showViewError("view.error.filenotfound");
                            return null;
                        } catch (AccessControlException ace) {
                            TopComponents.getInstance().showNoPrivilegesErrorMessage();
                            return null;
                        }
                    }
                });
                if (is == null) return false;
                Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(is);
                //noinspection SuspiciousToArrayCall
                certChain = certs.toArray(new X509Certificate[certs.size()]);


            } catch (CertificateException ce) {
                final String msg = resources.getString("view.error.cert.generate") + " " + ExceptionUtils.getMessage(ce);
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
                    showViewError("view.error.urlNotSsl");
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
                showViewError("view.error.urlMalformed");
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
            String cn = null;
            try {
                cn = CertUtils.extractSingleCommonNameFromCertificate(cert);
            } catch (CertUtils.MultipleCnValuesException e) {
                // Fallthrough and use subject name
            }

            // use the subjectDN name if a single CN attribute isn't found
            if (cn == null || cn.length() == 0) {
                cn = subjectName;
            }

            if (!hostnameURL.equals(cn)) {
                Object[] options = {"Accept", "Cancel"};

                //Adding this message to a scrollPane to better show large Subject DN's [SSM-4313]
                JLabel textArea = new JLabel("<html>The hostname in URL does not match with the certificate's subject name. " +
                        "<br>" + "Hostname in URL: " + hostnameURL + "</br>" +
                        "<br>" + "Subject DN in Certificate: " + cn + "</br>" +
                        "<br>" + "Do you want to accept the certificate?" + "</br></html>");
                JScrollPane scrollPane = new JScrollPane(textArea);
                //Always have the Horizontal scrollbar so that the scrollPane gets properly sized.
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                //Get the dimensions of the scrollPane and change its width only.
                Dimension scrollPaneDimensions = scrollPane.getPreferredSize();
                scrollPaneDimensions.width = 450;
                scrollPane.setPreferredSize(scrollPaneDimensions);
                int result = JOptionPane.showOptionDialog(null, scrollPane,
                                                          "Hostname Mismatch",
                                                          0, JOptionPane.WARNING_MESSAGE,
                                                          null, options, options[1]);

                // abort if the user does not accept the hostname mismatch
                if (result != 0) {
                    return false;
                }
            }

        } else if (copyAndPasteRadioButton.isSelected()) {

            String certPem = copyAndPasteTextArea.getText();
            if (certPem == null || certPem.trim().isEmpty()) {
                showViewError("view.error.pem.cert.content");
                return false;
            }
            certPem = certPem.trim();

            final byte [] bytes;
            if(base64PEMRadioButton.isSelected()){
                bytes = certPem.getBytes(Charsets.UTF8);
            } else {
                bytes = HexUtils.decodeBase64(certPem);
            }

            try {
                is = new ByteArrayInputStream(bytes);
                Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(is);
                certChain = CertUtils.asX509CertificateArray(certs.toArray(new Certificate[certs.size()]));
            } catch (Exception e) {
                showViewError("view.error.cert.generate", e);
                return false;
            }

        } else if (privateKeyRadioButton.isSelected()) {
            String alias = privateKeysComboBox.getSelectedKeyAlias();
            Goid keystoreId = privateKeysComboBox.getSelectedKeystoreId();
            if (alias == null) {
                showViewError("view.error.privatekey.noneselected");
                return false;
            }

            try {
                SsgKeyEntry entry = Registry.getDefault().getTrustedCertManager().findKeyEntry(alias, keystoreId);
                certChain = entry.getCertificateChain();
            } catch (FindException e) {
                showViewError("view.error.privatekey.bad", e);
                return false;
            } catch (KeyStoreException e) {
                showViewError("view.error.privatekey.bad", e);
                return false;
            }
        } else if (trustedCertRadioButton.isSelected()) {
            Object item = trustedCertsComboBox.getSelectedItem();
            if (!(item instanceof CertComboEntry)) {
                showViewError("view.error.trustedcert.nonselected");
                return false;
            }

            CertComboEntry entry = (CertComboEntry) item;
            certChain = new X509Certificate[] { entry.cert.getCertificate() };
        }

        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                showViewError("view.error.close.InputStream");
                // continue regardless of this error.
            }
        }

        return true;
    }

    private void showViewError(String msg) {
        JOptionPane.showMessageDialog(this, resources.getString(msg), resources.getString("view.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    private void showViewError(String msg, Exception e) {
        JOptionPane.showMessageDialog(this, resources.getString(msg) + " " + ExceptionUtils.getMessage(e), resources.getString("view.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    @Override
    public void storeSettings(Object settings) {
        if (settings == null || certChain == null || certChain.length < 1 || certChain[0] == null)
            return;

        if (settings instanceof TrustedCert) {
            TrustedCert tc = (TrustedCert) settings;
            tc.setCertificate(certChain[0]);
            if (allowSecurityZoneSelection) {
                tc.setSecurityZone(zoneControl.getSelectedZone());
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
    @Override
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
