package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import java.awt.*;
import java.security.cert.*;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertDetailsPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private X509Certificate cert;

    public CertDetailsPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
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

            if (settings instanceof CertInfo) {

                CertInfo ci = (CertInfo) settings;
                InputStream is = null;
                CertificateFactory cf = null;

                try {

                    cf = CertificateFactory.getInstance("X.509");

                } catch (CertificateException e) {
                    //todo:
                }


                if (ci.getCertDataSource() instanceof File) {

                    try {
                        is = new FileInputStream(((File) ci.getCertDataSource()).getAbsolutePath());

                        cert = (X509Certificate) cf.generateCertificate(is);

                        is.close();
                    } catch (FileNotFoundException fne) {

                    } catch (CertificateException ce) {

                    } catch (IOException ioe) {

                    }
                } else if (ci.getCertDataSource() instanceof URL) {
                    try {

                        URL url = (URL) ci.getCertDataSource();

                        String urlStr = url.getProtocol() + ":" + "//" + url.getHost();

                        if(url.getPort() > 0) {
                            urlStr = urlStr +  ":" + url.getPort();
                        }

                        if(url.getPath() != null) {
                            urlStr += url.getPath();
                        }

                        System.out.println("Retriving cert from URL: " + urlStr);

                        X509Certificate[] certs = getTrustedCertAdmin().retrieveCertFromUrl(urlStr);
                        cert = certs[0];

                        // todo: ignore the rest?

                    } catch (IOException e) {
                        //todo:
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                } else if (ci.getCertDataSource() instanceof String) {

                }
            }
        }

        try {
            JComponent certView = getCertView();
            if(certView == null) {
                 certView = new JLabel();
            } else {
                //certScrollPane.setViewportBorder(BorderFactory.createLineBorder(Color.black));
                certScrollPane.setViewportView(certView);;
            }

            revalidate();
            repaint();

        } catch (CertificateEncodingException ee) {
            //todo:
        } catch (NoSuchAlgorithmException ae) {
            //todo:
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

            if (settings instanceof CertInfo) {
                CertInfo ci = (CertInfo) settings;

                try {
                    ci.getTrustedCert().setCertificate(cert);
                    ci.getTrustedCert().setName(cert.getSubjectDN().getName());
                    ci.getTrustedCert().setSubjectDn(cert.getSubjectDN().getName());

                } catch (CertificateEncodingException e) {
                    //todo:
                }
            }
        }
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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, 0, 3, 3, 3, null, null, null));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("Certificate Details");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _4;
        _4 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final com.intellij.uiDesigner.core.Spacer _5;
        _5 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final JScrollPane _6;
        _6 = new JScrollPane();
        certScrollPane = _6;
        _1.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 3, 7, 7, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _7;
        _7 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_7, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 2, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
    }

}
