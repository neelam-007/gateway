/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.internal.audit;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.BuildInfo;

import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Tool to check signatures in a downloaded audit file; with both
 * GUI and command line interfaces.
 *
 * @see <a href="http://sarek/mediawiki/index.php?title=Audit_Signature_Checker">Usage Documentation</a>
 * @since SecureSpan 4.2
 * @author rmak
 */
public class AuditSignatureChecker extends JFrame {
    static {
        Logger.getLogger("").setLevel(Level.WARNING);
    }

    /** Result status. Ordinal is used as exit status. */
    private enum Status {
        /** All records have signatures and they are valid. */
        PASS,
        /** One or more record does not have signature or its signature is invalid. */
        FAIL,
        /** Error occurred to prevent checking. */
        ERROR,
    };

    private static final Preferences _preferences = Preferences.userRoot().node("/com/l7tech/internal/audit/auditsignaturechecker");
    private static final char ESC = '\\';

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

    private static boolean isFilePath(final String path) {
        try {
            new File(path).getCanonicalPath();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static boolean isURL(final String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    private static Certificate[] loadCertFromFile(String file) throws IOException, CertificateException {
        final Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(new FileInputStream(file));
        return certs.toArray(new Certificate[certs.size()]);
    }


    private static Certificate[] loadCertFromURL(String purl) throws IOException {
        URL url = new URL(purl);

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
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
            conn.connect();
            return conn.getServerCertificates();
        } else {
            throw new IOException("URL resulted in a non-HTTPS connection");
        }
    }


    private static Certificate[] loadCert(final String urlOrFile) throws IOException, CertificateException {
        if (isURL(urlOrFile)) {
            return loadCertFromURL(urlOrFile);
        } else if (isFilePath(urlOrFile)) {
            return loadCertFromFile(urlOrFile);
        } else {
            throw new IOException("Neither a file path or URL: " + urlOrFile);
        }
    }

    /**
     * Check signatures of each audit record in an exported file or ZIP archive.
     *
     * @param auditPath     path of audit ZIP file or the uncompressed audit.dat file
     * @param certPath      path of signing certficate file
     * @param out           output writer
     * @param verbose       verbose mode
     * @return {@link Status#PASS}, {@link Status#FAIL}, or {@link Status#ERROR}
     */
    public static Status checkFile(final String auditPath,
                                   final String certPath,
                                   final PrintWriter out,
                                   final boolean verbose) {
        try {
            System.setProperty("com.l7tech.common.security.jceProviderEngineName", "BC");
            JceProvider.init();

            final Certificate[] cert = loadCert(certPath);

            if (isZipFile(auditPath)) {
                // A downloaded audit ZIP file.
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(auditPath);
                    final ZipEntry zipEntry = zipFile.getEntry("audit.dat");
                    if (zipEntry == null) {
                        out.println("\"audit.dat\" not found inside ZIP file.");
                        return Status.ERROR;
                    } else {
                        return checkFile(zipFile.getInputStream(zipEntry), out, cert, verbose);
                    }
                } finally {
                    if (zipFile != null) zipFile.close();
                }
            } else {
                // File is the unZIPed "audit.dat".
                return checkFile(new FileInputStream(auditPath), out, cert, verbose);
            }
        } catch (Exception e) {
            out.println(e);
            return Status.ERROR;
        }
    }

    // reconstitutes records on multi lines
    static String readRecord(BufferedReader in) throws IOException {
        final StringBuilder result = new StringBuilder();
        boolean escaping = false;
        int numRead = 0;
        int intValue;
        while ((intValue = in.read()) != -1) {
            ++numRead;
            final char c = (char)intValue;
            if (escaping) {
                escaping = false;
            } else if (c == ESC) {
                escaping = true;
            } else if (c == '\n') {
                // An unescaped LF marks end of record, and is not part of record.
                break;
            }
            result.append(c);
        }

        if (numRead == 0) {
            return null;
        } else {
            return result.toString();
        }
    }

    /**
     * Check signatures of each audit record in a file.
     *
     * @param is        input stream of audit.dat
     * @param cert      signing certficate
     * @param out       output writer
     * @param verbose   true for print out per record
     * @return {@link Status#PASS}, {@link Status#FAIL}, or {@link Status#ERROR}
     * @throws IOException if I/O error occurs
     */
    private static Status checkFile(final InputStream is,
                                    final PrintWriter out,
                                    final Certificate[] cert,
                                    final boolean verbose)
            throws IOException {
        Status result = Status.PASS;
        final BufferedReader in = new BufferedReader(new InputStreamReader(is,"UTF-8"));
        String readrecord;
        int numValid = 0, numInvalid = 0, numUnsigned = 0, numError = 0, numTotal = 0;
        int i = 0;
        while ((readrecord = readRecord(in)) != null ) {
            i++;
            if (i == 1) continue; // dont do header line
            if (readrecord.length() < 5) continue;
            ++numTotal;

            DownloadedAuditRecordSignatureVerificator rec;
            try {
                rec = DownloadedAuditRecordSignatureVerificator.parse(readrecord);
            } catch (DownloadedAuditRecordSignatureVerificator.InvalidAuditRecordException e) {
                ++numError;
                out.println(e.getMessage());
                out.println(readrecord);
                continue;
            }
            if (rec.isSigned()) {
                try {
                    if (rec.verifySignature((X509Certificate)cert[0])) {
                        ++numValid;
                        if (verbose) out.println(rec.getAuditID() + " has a valid signature");
                    } else {
                        ++numInvalid;
                        out.println(rec.getAuditID() + " has an *INVALID* signature");
                        if (verbose) out.println(rec.getRecordInExportedFormat());
                        if (result == Status.PASS) result = Status.FAIL;
                    }
                } catch (Exception e) {
                    ++numError;
                    out.println(rec.getAuditID() + ": Error validating signature: " + e.toString());
                    result = Status.ERROR;
                }
            } else {
                ++numUnsigned;
                out.println(rec.getAuditID() + " is not signed");
                if (result == Status.PASS) result = Status.FAIL;
            }
        }

        int maxNumDigits = 0;
        maxNumDigits = Math.max(maxNumDigits, Integer.toString(numValid).length());
        maxNumDigits = Math.max(maxNumDigits, Integer.toString(numInvalid).length());
        maxNumDigits = Math.max(maxNumDigits, Integer.toString(numUnsigned).length());
        maxNumDigits = Math.max(maxNumDigits, Integer.toString(numError).length());
        maxNumDigits = Math.max(maxNumDigits, Integer.toString(numTotal).length());
        out.println("--------------------------------------------------------------------------------");
        out.println("Summary:");
        out.printf("    Valid   : %" + maxNumDigits + "d\n", numValid);
        out.printf("    Invalid : %" + maxNumDigits + "d\n", numInvalid);
        out.printf("    Unsigned: %" + maxNumDigits + "d\n", numUnsigned);
        out.printf("    Error   : %" + maxNumDigits + "d\n", numError);
        out.printf("    Total   : %" + maxNumDigits + "d\n", numTotal);
        out.println("--------------------------------------------------------------------------------");

        return result;
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, IllegalAccessException, InstantiationException {
        if (args.length == 0) {
            // Invokes GUI interface.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final Gui frame = new Gui();
                    frame.toFront();
                }
            });
        } else {
            // Invokes command line interface.

            boolean verbose = false;
            int i = 0;
            for (; i < args.length && args[i].startsWith("-"); ++i) {
                if (args[i].equals("--")) {
                    ++i;
                    break;
                } else if (args[i].equals("-h")) {
                    usage(System.out);
                    System.exit(Status.PASS.ordinal());
                } else if (args[i].equals("-v")) {
                    verbose = true;
                } else {
                    System.out.println("!!unknown option: " + args[i]);
                    usage(System.out);
                    System.exit(Status.ERROR.ordinal());
                }
            }

            if (args.length - i != 2) {
                usage(System.out);
                System.exit(Status.ERROR.ordinal());
            }
            final String auditPath = args[i];
            final String certPath = args[++i];

            final Status result = checkFile(auditPath, certPath, new PrintWriter(System.out, true), verbose);
            System.out.println("***** " + result + " *****");
            System.exit(result.ordinal());
        }
    }

    public static void usage(final PrintStream out) {
        out.println("GUI mode:");
        out.println("    java -jar AuditSignatureChecker.jar");
        out.println("Command line mode:");
        out.println("    java -jar AuditSignatureChecker.jar [options] <ZIP file> <Cert file>");
        out.println("    java -jar AuditSignatureChecker.jar [options] audit.dat  <Cert file>");
        out.println("    Options: -h    help (this message)");
        out.println("             -v    verbose mode");
        out.println("             --    stop parsing for options");
        out.println("    Exit status: " + Status.PASS.ordinal() + " " + Status.PASS);
        out.println("                 " + Status.FAIL.ordinal() + " " + Status.FAIL);
        out.println("                 " + Status.ERROR.ordinal() + " " + Status.ERROR);
    }
    /**
     * GUI interface.
     */
    private static class Gui extends JFrame {

        private static final String PREF_WINDOW_X = "window.x";
        private static final String PREF_WINDOW_Y = "window.y";
        private static final String PREF_WINDOW_WIDTH = "window.width";
        private static final String PREF_WINDOW_HEIGHT = "window.height";
        private static final String PREF_AUDITPATH = "audit.path";
        private static final String PREF_CERTPATH = "cert.path";
        private static final String PREF_VERBOSE = "verbose";

        private final JTextField _auditPathTextField = new JTextField();
        private final JTextField _certPathTextField = new JTextField();
        private final JButton _checkButton = new JButton("Check");
        private final JLabel _statusLabel = new JLabel();
        private final JCheckBox _verboseCheckBox = new JCheckBox("verbose");
        private final JTextArea _outputTextArea = new JTextArea();

        private final PrintWriter _outputTextAreaWriter = new PrintWriter(new Writer() {
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
        });

        final PrintStream _outputTextAreaStream = new PrintStream(new OutputStream() {
            public void write(int b) throws IOException {
                _outputTextAreaWriter.print((char)b);
            }
        }, true);

        public Gui() {
            // Redirects all exception stack trace.
            System.setOut(_outputTextAreaStream);
            System.setErr(_outputTextAreaStream);

            setTitle("Audit Signature Checker " + BuildInfo.getProductVersion());

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

            gridBagConstraints.insets = new Insets(3, 10, 3, 10);
            gridBagConstraints.gridx = 0;
            ++gridBagConstraints.gridy;
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
            ++gridBagConstraints.gridy;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            add(subPanel, gridBagConstraints);

            ++gridBagConstraints.gridy;
            add(_verboseCheckBox, gridBagConstraints);

            _outputTextArea.setEditable(false);

            final JScrollPane scrollPane = new JScrollPane(_outputTextArea);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setPreferredSize(new Dimension(600, 400));
            ++gridBagConstraints.gridy;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.CENTER;
            gridBagConstraints.insets = new Insets(3, 10, 10, 10);
            gridBagConstraints.weightx = 1.;
            gridBagConstraints.weighty = 1.;
            add(scrollPane, gridBagConstraints);

            setDefaultCloseOperation(EXIT_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    // Saves user preferences.
                    if ((getExtendedState() & MAXIMIZED_BOTH) != 0) {
                        setExtendedState(NORMAL);
                    }
                    _preferences.putInt(PREF_WINDOW_X, getLocation().x);
                    _preferences.putInt(PREF_WINDOW_Y, getLocation().y);
                    _preferences.putInt(PREF_WINDOW_WIDTH, getSize().width);
                    _preferences.putInt(PREF_WINDOW_HEIGHT, getSize().height);
                    _preferences.put(PREF_AUDITPATH, _auditPathTextField.getText());
                    _preferences.put(PREF_CERTPATH, _certPathTextField.getText());
                    _preferences.put(PREF_VERBOSE, Boolean.toString(_verboseCheckBox.isSelected()));
                }
            });

            enableOrDisableComponents();
            pack();

            // Applies user preferences.
            int width = _preferences.getInt(PREF_WINDOW_WIDTH, -1);
            int height = _preferences.getInt(PREF_WINDOW_HEIGHT, -1);
            if (width == -1) width = getSize().width;
            if (height == -1) height = getSize().height;
            int x = _preferences.getInt(PREF_WINDOW_X, -1);
            int y = _preferences.getInt(PREF_WINDOW_Y, -1);
            if (x == -1 || y == -1) {
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                if (x == -1) x = screenSize.width > width ? (screenSize.width - width) / 2 : 0;
                if (y == -1) y = screenSize.height > height ? (screenSize.height - height) / 2 : 0;
            }
            setLocation(x, y);
            setSize(width, height);

            _auditPathTextField.setText(_preferences.get(PREF_AUDITPATH, null));
            _certPathTextField.setText(_preferences.get(PREF_CERTPATH, null));
            _verboseCheckBox.setSelected(Boolean.parseBoolean(_preferences.get(PREF_VERBOSE, "false")));

            setVisible(true);
        }

        /** Handles browse audit button click. */
        private void onBrowseAudit() {
            File startingPath = new File(_auditPathTextField.getText());
            if(!startingPath.exists()) {
                startingPath = FileChooserUtil.getStartingDirectory();
            }
            final JFileChooser chooser = new JFileChooser(startingPath);
            FileChooserUtil.addListenerToFileChooser(chooser);
            chooser.setDialogTitle("Select Audit Download File");
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);
            chooser.setMultiSelectionEnabled(false);
            final javax.swing.filechooser.FileFilter fileFilter = new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return (f.isDirectory() || f.getName().toLowerCase().endsWith(".zip"));
                }

                public String getDescription() {
                    return "ZIP archives (*.zip)";
                }
            };
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
            File startingPath = new File(_certPathTextField.getText());
            if(!startingPath.exists()) {
                startingPath = FileChooserUtil.getStartingDirectory();
            }
            final JFileChooser chooser = new JFileChooser(_certPathTextField.getText());
            FileChooserUtil.addListenerToFileChooser(chooser);
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
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            final Status result = checkFile(_auditPathTextField.getText(), _certPathTextField.getText(), _outputTextAreaWriter, _verboseCheckBox.isSelected());
            this.setCursor(null);
            setStatus(result);
        }

        private void setStatus(final Status status) {
            _statusLabel.setOpaque(true);
            _statusLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            _statusLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
            if (status == Status.PASS) {
                _statusLabel.setText(" PASS ");
                _statusLabel.setForeground(Color.BLACK);
                _statusLabel.setBackground(Color.GREEN);
            } else if (status == Status.FAIL) {
                _statusLabel.setText(" FAIL ");
                _statusLabel.setForeground(Color.RED);
                _statusLabel.setBackground(Color.YELLOW);
            } else if (status == Status.ERROR) {
                _statusLabel.setText(" ERROR ");
                _statusLabel.setForeground(Color.RED);
                _statusLabel.setBackground(Color.WHITE);
            } else {
                throw new RuntimeException("Internal Error: Unknown status: " + status);
            }
        }

        private void enableOrDisableComponents() {
            _checkButton.setEnabled(_auditPathTextField.getText().trim().length() > 0 &&
                                    _certPathTextField.getText().trim().length() > 0);
        }
    }
}
