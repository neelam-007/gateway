package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCert;
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
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertDetailsPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
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

    public void readSettings(Object settings) throws IllegalArgumentException {
        if (settings != null) {

            if (settings instanceof TrustedCert) {
                TrustedCert tc = (TrustedCert) settings;

                if (tc != null) {
                    try {
                        cert = tc.getCertificate();

                        if (cert != null) {

                            // strip out the first 3 characters (ie. "cn=" )
                            certNameTextField.setText(cert.getSubjectDN().getName().substring(3, cert.getSubjectDN().getName().length()));
                        }
                    } catch (CertificateException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }


                    // remove the old view
                    if(certView != null) {
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
                    } catch (NoSuchAlgorithmException ae) {
                        logger.warning("Unable to decode the certificate issued to: " + cert.getSubjectDN().getName() + ", Algorithm is not supported:" + cert.getSigAlgName());
                    }
                } else {
                    //todo:
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
        _1.setLayout(new GridLayoutManager(4, 3, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new GridConstraints(0, 1, 1, 1, 0, 3, 3, 0, null, null, null));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("Certificate Name");
        _2.add(_3, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _4;
        _4 = new JTextField();
        certNameTextField = _4;
        _2.add(_4, new GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_5, new GridConstraints(1, 1, 1, 1, 0, 3, 3, 0, null, null, null));
        final JLabel _6;
        _6 = new JLabel();
        _6.setText("Details:");
        _5.add(_6, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _7;
        _7 = new JPanel();
        _7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_7, new GridConstraints(2, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JScrollPane _8;
        _8 = new JScrollPane();
        certScrollPane = _8;
        _7.add(_8, new GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JPanel _9;
        _9 = new JPanel();
        certMainPanel = _9;
        _9.setLayout(new GridLayoutManager(1, 2, new Insets(5, 5, 5, 5), -1, -1));
        _8.setViewportView(_9);
        final JPanel _10;
        _10 = new JPanel();
        certPanel = _10;
        _9.add(_10, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final Spacer _11;
        _11 = new Spacer();
        _9.add(_11, new GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
    }

}
