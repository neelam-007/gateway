package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.security.cert.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.util.logging.Logger;
import java.util.ResourceBundle;
import java.util.Locale;

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
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
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
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings != null) {

            if (settings instanceof TrustedCert) {
                TrustedCert tc = (TrustedCert) settings;

                if (tc != null) {
                    try {
                        cert = tc.getCertificate();

                    } catch (CertificateException e) {
                        logger.warning(resources.getString("cert.decode.error"));
                        JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.decode.error"),
                                           resources.getString("save.error.title"),
                                           JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e) {
                       logger.warning(e.getMessage());
                        JOptionPane.showMessageDialog(mainPanel, e.getMessage(),
                                           resources.getString("save.error.title"),
                                           JOptionPane.ERROR_MESSAGE);
                    }

                    if (cert != null) {

                        // retrieve the value of cn
                        String subjectName = cert.getSubjectDN().getName();
                        String cn = CertUtils.extractUsernameFromClientCertificate(cert);

                        if(cn.length() > 0) {
                             certNameTextField.setText(cn);
                        }
                        else {
                            // cn NOT found, use the subject name
                            certNameTextField.setText(subjectName);
                        }

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

                if (cert != null) {
                    try {
                        tc.setCertificate(cert);
                        tc.setName(certNameTextField.getText().trim());
                        tc.setSubjectDn(cert.getSubjectDN().getName());

                    } catch (CertificateEncodingException e) {
                        logger.warning("Unable to decode the certificate issued to: " + cert.getSubjectDN().getName());
                        cert = null;
                    }
                }
            }
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "View Certificate Details";
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
        mainPanel.setLayout(new GridLayoutManager(4, 3, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Certificate Name:");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        certNameTextField = new JTextField();
        panel1.add(certNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Details:");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        certMainPanel = new JPanel();
        certMainPanel.setLayout(new GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane1.setViewportView(certMainPanel);
        certPanel = new JPanel();
        certMainPanel.add(certPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final Spacer spacer1 = new Spacer();
        certMainPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
    }
}
