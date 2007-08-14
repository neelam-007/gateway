package com.l7tech.internal.audit;

import com.l7tech.common.util.CertUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.Collection;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;

/**
 * Tool to check individual signatures of downloaded audit events with both
 * GUI and command line interfaces.
 *
 * @since SecureSpan 4.2
 * @rmak
 */
public class AuditSignatureChecker extends JFrame {

    private static boolean isZipFile(final String path) throws IOException {
        boolean result = false;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(path);
            result = true;
        } catch (ZipException e) {
        } finally {
            if (zipFile != null) zipFile.close();
        }
        return result;
    }

    private static Certificate[] loadCertFromFile(String file) throws IOException, CertificateException {
        final File auditFile = new File(file);
        if (! auditFile.exists()) {
            return null;
        }
        if (! auditFile.isFile()) {
            return null;
        }
        final File certFile = new File(file);

        if (! certFile.exists()) {
            return null;
        }
        if (! certFile.isFile()) {
            return null;
        }

        Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(new FileInputStream(file));

        return certs.toArray(new X509Certificate[0]);
    }


    private static Certificate[] loadCertFromURL(String purl) throws IOException {
        URL url = new URL(purl);

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null,
              new X509TrustManager[]{new X509TrustManager() {
                  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                      return new java.security.cert.X509Certificate[0];
                  }

                  public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {
                  }

                  public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {
                  }
              }},
              null);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        } catch (KeyManagementException e) {
            throw new IOException(e.getMessage());
        }

        URLConnection gconn = url.openConnection();
        if (gconn instanceof HttpsURLConnection) {
            HttpsURLConnection conn = (HttpsURLConnection)gconn;
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            final String[] sawHost = new String[] { null };
            conn.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    sawHost[0] = s;
                    return true;
                }
            });

            try {
                conn.connect();
            } catch (IOException e) {
                throw e;
            }

            try {
                return conn.getServerCertificates();
            } catch (IOException e) {
                throw e;
            }
        } else
            throw new IOException("URL resulted in a non-HTTPS connection");
    }


    private static Certificate[] loadCert(String urlOrFile, final PrintWriter out) {
        String errormsg = null;
        Certificate[] output = new Certificate[0];
        try {
            output = loadCertFromFile(urlOrFile);
        } catch (IOException e) {
            errormsg = e.getMessage();
        } catch (CertificateException e) {
            errormsg = e.getMessage();
        }
        if (output == null) {
            try {
                output = loadCertFromURL(urlOrFile);
            } catch (IOException e) {
                errormsg = e.getMessage();
            }
        }
        if (output == null) {
            out.println("Could not load cert from " + urlOrFile);
            out.println(errormsg);
            return null;
        }
        out.println("Certificate loaded OK");
        return output;
    }

    public static boolean checkFile(final String auditPath, final String certPath, final PrintWriter out) {
        Certificate[] cert = loadCert(certPath, out);
        if (cert == null) {
            return false;
        }

        try {
            if (isZipFile(auditPath)) {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(auditPath);
                    final ZipEntry zipEntry = zipFile.getEntry("audit.dat");
                    if (zipEntry == null) {
                        out.println("\"audit.dat\" not found inside ZIP file.");
                        return false;
                    } else {
                        return checkFile(zipFile.getInputStream(zipEntry), out, cert);
                    }
                } finally {
                    if (zipFile != null) zipFile.close();
                }
            } else {
                // Treat auditFile as the "audit.dat" content.
                return checkFile(new FileInputStream(auditPath), out, cert);
            }
        } catch (Exception e) {
            e.printStackTrace(out);
            return false;
        }
    }

    private static boolean checkFile(final InputStream is, final PrintWriter out, Certificate[] cert) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        int i = 0;
        while ((line = in.readLine()) != null ) {
            i++;
            if (i == 1) continue; // dont do header line
            if (line.length() < 5) continue;

            DownloadedAuditRecordSignatureVerificator rec;
            try {
                rec = DownloadedAuditRecordSignatureVerificator.parse(line);
            } catch (DownloadedAuditRecordSignatureVerificator.InvalidAuditRecordException e) {
                out.println(e.getMessage());
                out.println(line);
                continue;
            }
            if (!rec.isSigned()) {
                out.println(rec.getAuditID() + " is not signed");
            } else {
                boolean res;
                try {
                    res = rec.verifySignature(cert[0]);
                } catch (Exception e) {
                    out.println("Error validating signature " + e.getMessage());
                    res = false;
                }
                if (res) {
                    out.println(rec.getAuditID() + " has a valid signature");
                } else {
                    out.println(rec.getAuditID() + " has an *INVALID* signature");
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        if (args.length == 0) {
            // Invokes GUI interface.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final Gui frame = new Gui();
                    final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    final Dimension frameSize = frame.getSize();
                    final int x = screenSize.width > frameSize.width ? (screenSize.width - frameSize.width) / 2 : 0;
                    final int y = screenSize.height > frameSize.height ? (screenSize.height - frameSize.height) / 2 : 0;
                    frame.setLocation(x, y);
                    frame.toFront();
                }
            });
        } else if (args.length == 2) {
            // Invokes command line interface.
            final boolean result = checkFile(args[0], args[1], new PrintWriter(System.out, true));
            System.out.println("*****" + (result ? "PASS" : "FAIL") + "*****");
        } else {
            System.out.println("usage: (Help)         java -h");
            System.out.println("       (GUI)          java " + AuditSignatureChecker.class.getName());
            System.out.println("       (Command line) java " + AuditSignatureChecker.class.getName() + " {ZIP file} {Cert file}");
            System.out.println("                      java " + AuditSignatureChecker.class.getName() + " audit.dat  {Cert file}");
            System.exit(1);
        }
    }

    /**
     * GUI interface.
     */
    private static class Gui extends JFrame {

        private final JTextField _auditPathTextField = new JTextField();
        private final JTextField _certPathTextField = new JTextField();
        private final JButton _checkButton = new JButton("Check");
        private final JLabel _statusLabel = new JLabel();
        private final JTextArea _outputTextArea = new JTextArea();

        public Gui() {
            setTitle("Audit Signature Checker");

            setLayout(new GridBagLayout());
            final GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.WEST;

            gridBagConstraints.insets = new Insets(10, 10, 3, 10);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            add(new JLabel("Audit file:"), gridBagConstraints);

            _auditPathTextField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
                public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
                public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            });
            _auditPathTextField.setToolTipText("Path of downloaded audit ZIP file or the uncompressed audit.dat file");
            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.;
            add(_auditPathTextField, gridBagConstraints);
            gridBagConstraints.weightx = 0.;
            gridBagConstraints.fill = GridBagConstraints.NONE;

            final JButton browseAuditButton = new JButton("Browse");
            browseAuditButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onBrowseAudit();
                }
            });
            gridBagConstraints.gridx = 2;
            add(browseAuditButton, gridBagConstraints);

            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            add(new JLabel("Certificate (path or URL):"), gridBagConstraints);

            _certPathTextField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
                public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
                public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            });
            _certPathTextField.setToolTipText("Path of certificate file or HTTPS URL");
            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.;
            add(_certPathTextField, gridBagConstraints);
            gridBagConstraints.weightx = 0.;
            gridBagConstraints.fill = GridBagConstraints.NONE;

            final JButton browseCertButton = new JButton("Browse");
            browseCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onBrowseCert();
                }
            });
            gridBagConstraints.gridx = 2;
            add(browseCertButton, gridBagConstraints);

            _checkButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCheck();
                }
            });
            final JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
            subPanel.add(_checkButton);
            subPanel.add(Box.createHorizontalStrut(20));
            subPanel.add(_statusLabel);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.insets = new Insets(3, 10, 3, 10);
            add(subPanel, gridBagConstraints);

            _outputTextArea.setEditable(false);

            final JScrollPane scrollPane = new JScrollPane(_outputTextArea);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            gridBagConstraints.gridy = 3;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.CENTER;
            gridBagConstraints.insets = new Insets(3, 10, 10, 10);
            gridBagConstraints.weightx = 1.;
            gridBagConstraints.weighty = 1.;
            add(scrollPane, gridBagConstraints);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            enableOrDisableComponents();
            pack();
            setVisible(true);
        }

        /** Handles browse audit button click. */
        private void onBrowseAudit() {
            final JFileChooser chooser = new JFileChooser(_auditPathTextField.getText());
            chooser.setDialogTitle("Select Audit Download File");
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            final FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("ZIP archives (*.zip)", "zip");
            chooser.setFileFilter(fileFilter);
            final int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION)
                return;

            File filePath = chooser.getSelectedFile();
            if (filePath == null)
                return;

            _auditPathTextField.setText(filePath.getAbsolutePath());
        }

        /** Handles browse cert button click. */
        private void onBrowseCert() {
            final JFileChooser chooser = new JFileChooser(_certPathTextField.getText());
            chooser.setDialogTitle("Select Certificate File");
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            final int result = chooser.showOpenDialog(this);
            if (result != JFileChooser.APPROVE_OPTION)
                return;

            File filePath = chooser.getSelectedFile();
            if (filePath == null)
                return;

            _certPathTextField.setText(filePath.getAbsolutePath());
        }

        /** Handles check button click. */
        private void onCheck() {
            _outputTextArea.setText(null);
            final boolean result = checkFile(_auditPathTextField.getText(), _certPathTextField.getText(), new PrintWriter(new Writer() {
                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }

                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    _outputTextArea.append(new String(cbuf, off, len));
                }
            }));
            setStatus(result);
        }

        private void setStatus(final boolean pass) {
            _statusLabel.setOpaque(true);
            _statusLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            _statusLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            if (pass) {
                _statusLabel.setText("PASS");
                _statusLabel.setForeground(Color.BLACK);
                _statusLabel.setBackground(Color.GREEN);
            } else {
                _statusLabel.setText("FAIL");
                _statusLabel.setForeground(Color.RED);
                _statusLabel.setBackground(Color.YELLOW);
            }
        }

        private void enableOrDisableComponents() {
            _checkButton.setEnabled(_auditPathTextField.getText().trim().length() > 0 &&
                                    _certPathTextField.getText().trim().length() > 0);
        }
    }
}
