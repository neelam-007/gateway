package com.l7tech.client.gui.dialogs;

import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.GuiPasswordCallbackHandler;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.common.io.SslCertificateSniffer;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.client.gui.Gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Dialog displayed if "View Server Cert" is clicked when there is no server cert yet known.
 */
public class NoServerCertDialog extends JDialog {
    private JPanel mainPanel;
    private JLabel headlineLabel;
    private JButton importFromFileButton;
    private JButton closeButton;
    private JButton importFromSslButton;

    private final Ssg ssg;
    private final String ssgName;
    private boolean newCertSaved = false;

    public NoServerCertDialog(Dialog owner, Ssg ssg, String ssgName) {
        super(owner, "Gateway Server Certificate Not Found", true);
        this.ssg = ssg;
        this.ssgName = ssgName;
        initialize();
    }

    private void initialize( ) {
        setContentPane(mainPanel);
        headlineLabel.setText("A certificate for " + ssgName + " was not found.");

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        importFromFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                doImportFromFile();
            }
        });

        importFromSslButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doImportFromSsl();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void doImportFromFile() {
        GuiCertUtil.ImportedData imported = GuiCertUtil.importCertificate(this, false, new GuiPasswordCallbackHandler());
        if (imported == null)
            return;

        X509Certificate cert = imported.getCertificate();
        if (cert == null) {
            final X509Certificate[] chain = imported.getCertificateChain();
            if (chain == null || chain.length < 1 || chain[0] == null) return;
            cert = chain[0];
        }

        try {
            ssg.getRuntime().getSsgKeyStoreManager().saveSsgCertificate(cert);
            newCertSaved = true;
            dispose();
        } catch (Exception e) {
            Gui.errorMessage("Unable to Import Certificate", "Unable to import server certificate", ExceptionUtils.getMessage(e), e);
        }
    }

    private void doImportFromSsl() {
        String url = JOptionPane.showInputDialog(this,
                                                 "Please enter the HTTPS URL of the server whose certificate is to be imported.");
        if (url == null)
            return;
        url = url.trim();
        if (url.length() < 1)
            return;

        try {
            newCertSaved = importFromSslOrThrow(url);
            if (newCertSaved)
                dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to import certificate: " + ExceptionUtils.getMessage(e), "Unable to Import Certificate", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class CertOption {
        private final X509Certificate cert;
        private final String label;

        private CertOption(X509Certificate cert, String label) {
            this.cert = cert;
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }

    private boolean importFromSslOrThrow(String url)
            throws IOException, SslCertificateSniffer.HostnameMismatchException, KeyStoreCorruptException, KeyStoreException, CertificateException
    {
        if (!url.toLowerCase().startsWith("https://"))
            throw new IllegalArgumentException("The URL must start with https://");

        X509Certificate[] serverCerts;
        try {
            serverCerts = SslCertificateSniffer.retrieveCertFromUrl(url, false);
        } catch (SslCertificateSniffer.HostnameMismatchException e) {
            int option = JOptionPane.showConfirmDialog(this,
                                                       "URL contained hostname " + e.getHostname() + " but presented certificate contained " + e.getCertname() + ".  Proceed anyway?",
                                                       "Hostname Mismatch",
                                                       JOptionPane.YES_NO_CANCEL_OPTION,
                                                       JOptionPane.WARNING_MESSAGE);
            if (option != JOptionPane.YES_OPTION)
                return false;

            serverCerts = SslCertificateSniffer.retrieveCertFromUrl(url, true);
        }

        if (serverCerts == null || serverCerts.length < 1)
            throw new IllegalStateException("The server at that URL did present any server certificates."); // can't happen

        final X509Certificate selectedCert;
        if (serverCerts.length < 2) {
            selectedCert = serverCerts[0];
        } else {
            java.util.List<CertOption> options = Functions.map(Arrays.asList(serverCerts), new Functions.Unary<CertOption, X509Certificate>() {
                public CertOption call(X509Certificate cert) {
                    return new CertOption(cert, cert.getSubjectDN().getName());
                }
            });

            int got = JOptionPane.showOptionDialog(this,
                                                   "Please select the server certificate to import",
                                                   "Choose Certificate",
                                                   JOptionPane.OK_CANCEL_OPTION,
                                                   JOptionPane.QUESTION_MESSAGE,
                                                   null,
                                                   options.toArray(new CertOption[options.size()]),
                                                   options.get(0));
            if (got == JOptionPane.CLOSED_OPTION)
                return false;

            selectedCert = options.get(got).cert;
        }

        ssg.getRuntime().getSsgKeyStoreManager().saveSsgCertificate(selectedCert);
        return true;
    }

    /** @return true if a new certificate was imported and saved successfully. */
    public boolean wasNewCertSaved() {
        return newCertSaved;
    }
}
