package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * This class displays the details of the trusted certificates. Users can change
 * the certificate name via this dialog.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertDetailsPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private X509Certificate cert;
    private JPanel certPanel;
    private JPanel certMainPanel;
    JComponent certView = null;
    private JTextField certNameTextField;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertDetailsPanel.class.getName());
    private static int maxNameLength = EntityUtil.getMaxFieldLength(TrustedCert.class, "name", 64);

    public CertDetailsPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        certMainPanel.setBackground(Color.white);
        certPanel.setLayout(new FlowLayout());
        certPanel.setBackground(Color.white);
        certNameTextField.setDocument(new MaxLengthDocument(maxNameLength));
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

    @Override
    public boolean canAdvance() {
        return cert != null;
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    @Override
    public String getDescription() {
        return "Enter the certificate name and view certificate details.";
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings != null) {

            if (settings instanceof TrustedCert) {
                TrustedCert tc = (TrustedCert) settings;

                cert = tc.getCertificate();
                if (cert != null) {

                    // retrieve the value of cn
                    String subjectName = cert.getSubjectDN().getName();
                    String cn = null;
                    try {
                        cn = CertUtils.extractSingleCommonNameFromCertificate(cert);
                    } catch (CertUtils.MultipleCnValuesException e) {
                        // Fallthrough and use subject name
                    }

                    if(cn != null && cn.length() > 0) {
                         certNameTextField.setText(truncName(cn));
                    }
                    else {
                        // cn NOT found, use the subject name
                        certNameTextField.setText(truncName(subjectName));
                    }
                    //Set the carat position to the begining of the text field so that long names are easier to read/edit [SSM-4313]
                    certNameTextField.setCaretPosition(0);

                    // remove the old view
                    if (certView != null) {
                        certPanel.remove(certView);
                    }

                    try {
                        certView = getCertView();
                        if (certView == null) {
                            certView = new JLabel();
                        }

                        certPanel.add(certView);

                        revalidate();
                        repaint();

                    } catch (CertificateEncodingException ee) {
                        logger.warning("Unable to decode the certificate issued to: " + cert.getSubjectDN().getName());
                        JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.encode.error"),
                                       resources.getString("save.error.title"),
                                       JOptionPane.ERROR_MESSAGE);
                    } catch (NoSuchAlgorithmException ae) {
                        logger.warning("Unable to decode the certificate issued to: " + cert.getSubjectDN().getName() +
                                ", Algorithm is not supported:" + cert.getSigAlgName());
                    }
                }
            }
        }
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

        if (settings != null) {

            if (settings instanceof TrustedCert) {
                TrustedCert tc = (TrustedCert) settings;

                if (cert != null) {
                    tc.setCertificate(cert);
                    tc.setName(truncName(certNameTextField.getText().trim()));
                }
            }
        }
    }

    private String truncName(String s) {
        return s == null || s.length() < maxNameLength ? s : s.substring(0, maxNameLength);
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "View Certificate Details";
    }

    /*
     * Returns a properties instance filled out with info about the certificate.
     */
    private JComponent getCertView()
            throws CertificateEncodingException, NoSuchAlgorithmException {
        if (cert == null)
            return null;
        com.l7tech.gui.widgets.CertificatePanel i = new com.l7tech.gui.widgets.CertificatePanel(cert);
        i.setCertBorderEnabled(false);
        return i;
    }

    public X509Certificate getCert() {
        return cert;
    }
}
