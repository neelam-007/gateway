package com.l7tech.console.panels;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.VersionException;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.console.MainWindow;
import com.l7tech.console.action.ImportCertificateAction;
import com.l7tech.console.event.ConnectionEvent;
import com.l7tech.console.security.ClientCredentialManager;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.History;
import com.l7tech.console.util.Preferences;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the SSG console Logon dialog.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class LogonDialog extends JDialog {
    static final Logger log = Logger.getLogger(LogonDialog.class.getName());

    /* the PasswordAuthentication instance with user supplied credentials */
    private PasswordAuthentication authentication = null;

    /* was the dialog aborted */
    private boolean aborted = false;

    /**
     * True if "remember id" pref is enabled and a remembered ID was found.
     */
    private boolean rememberUser = false;

    /* was the error handled (to avoid double exception messages) */
    private boolean sslHostNameMismatchUserNotified = false;


    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    /* Command string for a cancel action (e.g.,a button or menu item). */
    private String CMD_CANCEL = "cmd.cancel";


    /* Command string for a login action (e.g.,a button or menu item). */
    private String CMD_LOGIN = "cmd.login";


    private JButton loginButton = null;

    /**
     * the logo above the Login Dialogue *
     */
    private JLabel companyLogoJLabel = null;

    /**
     * username text field
     */
    private JTextField userNameTextField = null;
    /**
     * password text field
     */
    private JPasswordField passwordField = null;
    /**
     * the server combo box *
     */
    private JComboBox serverComboBox = null;

    /**
     * VM property (-D) that triggers off the server check
     */
    private static final String DISABLE_SERVER_CHECK = "disable.server.check";
    private Frame parentFrame;
    private History serverUrlHistory;
    private Preferences preferences;

    /** context field (company.realm)
     private JTextField contextField = null;*/

    /**
     * Create a new LogonDialog
     * 
     * @param parent the parent Frame. May be <B>null</B>
     */
    public LogonDialog(Frame parent) {
        super(parent, true);
        this.parentFrame = parent;
        setTitle("");
        initResources();
        initComponents();
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.LogonDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        setTitle(resources.getString("window.title"));
        GridBagConstraints constraints = null;

        Container contents = getContentPane();
        contents.setLayout(new GridBagLayout());
        //setTitle (resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        JLayeredPane layeredPane = getLayeredPane();
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
          .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-it");
        layeredPane.getActionMap().put("close-it",
          new AbstractAction() {
              public void actionPerformed(ActionEvent evt) {
                  windowAction(CMD_CANCEL);
              }
          });

        constraints = new GridBagConstraints();

        companyLogoJLabel = new JLabel();
        companyLogoJLabel.setText(resources.getString("dialog.title"));
        companyLogoJLabel.setFont(new Font("Dialog", Font.BOLD, 12));

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(10, 10, 10, 10);
        contents.add(companyLogoJLabel, constraints);

        Image icon = ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/layer7_logo_small_32x32.png");
        if (icon != null) {
            ImageIcon imageIcon = new ImageIcon(icon);
            constraints = new GridBagConstraints();
            constraints.gridx = 4;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            constraints.insets = new Insets(10, 10, 10, 10);
            contents.add(new JLabel(imageIcon), constraints);
        }

        userNameTextField = new JTextField(); //needed below

        // user name label
        JLabel userNameLabel = new JLabel();
        userNameLabel.setToolTipText(resources.getString("userNameTextField.tooltip"));
        userNameTextField.setDocument(new FilterDocument(200,
          new FilterDocument.Filter() {
              public boolean accept(String str) {
                  if (str == null) return false;
                  return true;
              }
          }));

        DocumentListener inputValidDocumentListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (loginButton != null)
                    loginButton.setEnabled(isInputValid());
            }

            public void removeUpdate(DocumentEvent e) {
                if (loginButton != null)
                    loginButton.setEnabled(isInputValid());
            }

            public void changedUpdate(DocumentEvent e) {
                if (loginButton != null)
                    loginButton.setEnabled(isInputValid());
            }
        };

        userNameTextField.getDocument().addDocumentListener(inputValidDocumentListener);
        userNameLabel.setDisplayedMnemonic(resources.getString("userNameTextField.label").charAt(0));
        userNameLabel.setLabelFor(userNameTextField);
        userNameLabel.setText(resources.getString("userNameTextField.label"));

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 0, 0);
        contents.add(userNameLabel, constraints);

        // user name text field
        userNameTextField.setToolTipText(resources.getString("userNameTextField.tooltip"));
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(userNameTextField, constraints);

        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contents.add(Box.createHorizontalStrut(100), constraints);
        


        // last ID logic
        String lastID = null;
        rememberUser = false;
        preferences = Preferences.getPreferences();
        lastID = preferences.rememberLoginId() ?
          preferences.getString(Preferences.LAST_LOGIN_ID) :
          null;

        if (preferences.rememberLoginId()) {
            userNameTextField.setText(lastID);
            if (lastID != null)
                rememberUser = true;
        }

        final String fLastID = lastID; // anon class requires final
        userNameTextField.
          addFocusListener(new FocusAdapter() {
              private boolean hasbeenInvoked = false;

              /**
               * Invoked when a component gains the keyboard focus.
               */
              public void focusGained(FocusEvent e) {
                  if (!hasbeenInvoked) {
                      if (fLastID != null) {
                          LogonDialog.this.userNameTextField.transferFocus();
                      }
                  }
                  hasbeenInvoked = true;
              }
          });

        passwordField = new JPasswordField(); // needed below

        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setToolTipText(resources.getString("passwordField.tooltip"));
        passwordLabel.setDisplayedMnemonic(resources.getString("passwordField.mnemonic").charAt(0));
        passwordLabel.setText(resources.getString("passwordField.label"));
        passwordLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field
        passwordField.setToolTipText(resources.getString("passwordField.tooltip"));
//    Font echoCharFont = new Font("Lucida Sans", Font.PLAIN, 12);
//    passwordField.setFont(echoCharFont);
//    passwordField.setEchoChar('\u2022');

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(passwordField, constraints);

        constraints.gridx = 3;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contents.add(Box.createHorizontalStrut(100), constraints);
              


        //url label
        JLabel serverLabel = new JLabel();
        serverLabel.setDisplayedMnemonic(resources.getString("serverField.mnemonic").charAt(0));
        serverLabel.setToolTipText(resources.getString("serverField.tooltip"));
        serverLabel.setText(resources.getString("serverField.label"));
        serverLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(20, 5, 0, 0);
        contents.add(serverLabel, constraints);

        // context field
        serverComboBox = new JComboBox();
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setPreferredSize(new Dimension(200, 130));
        serverComboBox.setEditable(true);
        BasicComboBoxEditor basicComboBoxEditor = new BasicComboBoxEditor();
        JTextField jtf = (JTextField)basicComboBoxEditor.getEditorComponent();
        jtf.getDocument().addDocumentListener(inputValidDocumentListener);
        serverComboBox.setEditor(basicComboBoxEditor);
        serverComboBox.setToolTipText(resources.getString("serverField.tooltip"));
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 1.0;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(20, 5, 0, 10);
        contents.add(serverComboBox, constraints);

        serverUrlHistory = preferences.getHistory(Preferences.SERVICE_URL);

        Runnable runnable = new Runnable() {
            public void run() {
                Object[] urls = serverUrlHistory.getEntries();
                for (int i = 0; i < urls.length; i++) {
                    Object url = urls[i];
                    serverComboBox.addItem(url);
                    if (i == 0) {
                        serverComboBox.setSelectedIndex(0);
                    }
                }
            }
        };
        SwingUtilities.invokeLater(runnable);
        serverComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loginButton.setEnabled(isInputValid());
            }
        });

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(10, 0, 10, 10);
        JPanel buttonPanel = createButtonPanel(); // sets global loginButton
        contents.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(loginButton);
    }


    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     * <p/>
     * Sets the variable loginButton
     */
    private JPanel createButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // login button (global variable)
        loginButton = new JButton();
        loginButton.setText(resources.getString("loginButton.label"));
        loginButton.setToolTipText(resources.getString("loginButton.tooltip"));
        loginButton.setActionCommand(CMD_LOGIN);
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        panel.add(loginButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        panel.add(cancelButton);

        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, loginButton});
        return panel;
    }

    /**
     * The user has selected an option. Here we close and dispose
     * the dialog.
     * If actionCommand is an ActionEvent, getCommandString() is
     * called, otherwise toString() is used to get the action command.
     * 
     * @param actionCommand may be null
     */
    private void windowAction(String actionCommand) {

        authentication = new PasswordAuthentication("", new char[]{});
        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            aborted = true;
        } else if (actionCommand.equals(CMD_LOGIN)) {
            authentication = new PasswordAuthentication(userNameTextField.getText(),
              passwordField.getPassword());
            aborted = false;
        }
        setVisible(false);
    }

    /**
     * @return true if the logon was aborted, false otherwise
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * invoke logon dialog
     * 
     * @param frame 
     */
    public static void logon(JFrame frame, LogonListener listener) {
        final LogonDialog dialog = new LogonDialog(frame);
        dialog.setResizable(false);
        dialog.setSize(300, 275);

        // service available attempt authenticating
        boolean authenticated = false;
        PasswordAuthentication pw = null;


        while (!dialog.isAborted() && !authenticated) {
            String serviceURL = null;
            try {
                dialog.sslHostNameMismatchUserNotified = false;
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                pw = dialog.getAuthentication();
                // service URL
                String serverURL = (String)dialog.serverComboBox.getSelectedItem();
                serviceURL = serverURL + Preferences.SERVICE_URL_SUFFIX;

                if (dialog.isAborted()) break;
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                ClientCredentialManager credentialManager = getCredentialManager();
                getCredentialManager().onDisconnect(new ConnectionEvent(dialog, ConnectionEvent.DISCONNECTED));

                // if the service is not avail, format the message and show to te client (if not already notified)
                if (!dialog.isServiceAvailable(serviceURL)) {
                    if (!dialog.sslHostNameMismatchUserNotified) {
                        String msg = MessageFormat.format(dialog.resources.getString("logon.connect.error"),
                          new Object[]{getHostPart(serviceURL)});
                        JOptionPane.showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    continue;
                }

                dialog.serverUrlHistory.add(serverURL);
                credentialManager.login(pw);
                authenticated = true;
                dialog.preferences.store();
                dialog.preferences.updateSystemProperties();
                break;
            } catch (Exception e) {
                handleLogonThrowable(e, dialog, serviceURL);
            }
        }

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        // invoke the listener
        if (listener != null) {
            if (authenticated) {
                listener.onAuthSuccess(pw.getUserName());
            } else {
                listener.onAuthFailure();
            }
        }
        dialog.dispose();
    }

    private void showInvalidCredentialsMessage() {
        parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        JOptionPane.
          showMessageDialog(this,
            resources.getString("logon.invalid.credentials"),
            "Warning",
            JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Before displaying dialog, ensure that correct fields are selected.
     */
    public void show() {
        if (rememberUser) {
            passwordField.requestFocus();
            passwordField.selectAll();
        } else {
            userNameTextField.requestFocus();
            userNameTextField.selectAll();
        }
        super.show();
    }

    private static ClientCredentialManager getCredentialManager() {
        ClientCredentialManager credentialManager =
          (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        if (credentialManager == null) { // bug
            throw new IllegalStateException("No credential manager configured in services");
        }
        return credentialManager;
    }


    /**
     * verify that the service is avaialble at the given url
     * 
     * @param serviceUrl service url
     * @return true if service available, false otherwise
     */
    private boolean isServiceAvailable(String serviceUrl) {
        boolean serviceAvailable = false;
        boolean b = Boolean.getBoolean(DISABLE_SERVER_CHECK);
        if (b) return true;

        // try to connect and read the HTTP(S) code. We are ok if it
        // is 200, 401, 403, 302.
        // if it is everything else we assume the connection cannot
        // be established
        URL url = null;
        HttpURLConnection conn = null;
        boolean importedCertificate = false;
        for (; ;) {
            try {
                url = new URL(serviceUrl);
                conn = (HttpURLConnection)url.openConnection();
                addSslHostNameVerifier(conn);
                conn.connect();
                int code = conn.getResponseCode();
                if (code == 403 || code == 401 || code == 200 || code == 302) {
                    serviceAvailable = true;
                } else {
                    serviceAvailable = false;
                }

            } catch (SSLHandshakeException e) {
                serviceAvailable = false;

                if (importedCertificate) {
                    log.log(Level.SEVERE, "SSL handshake failed, even after downloading the server certificate", e);
                    break;
                }

                try {
                    log.log(Level.INFO, "LogonDialog: Attempting to update Gateway certificate");
                    importCertificate(url);
                    if (conn != null) {
                        conn.disconnect();
                        conn = null;
                    }

                    // Reinitialize the SSL context
                    reinitializeSsl();

                    importedCertificate = true;

                    // retry
                    continue;

                } catch (CertificateException e1) {
                    log.log(Level.INFO, "LogonDialog", e1);
                } catch (IOException e1) {
                    log.log(Level.INFO, "LogonDialog", e1);
                } catch (NoSuchAlgorithmException e1) {
                    log.log(Level.INFO, "LogonDialog", e1);
                } catch (KeyStoreException e1) {
                    log.log(Level.INFO, "LogonDialog", e1);
                } catch (KeyManagementException e1) {
                    log.log(Level.INFO, "LogonDialog", e1);
                }
            } catch (IOException e) {
                serviceAvailable = false;
                log.log(Level.INFO, "LogonDialog", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            // stop trying now
            break;
        }

        return serviceAvailable;
    }

    /**
     * Reinitialize ssl after the trust store has been updated.
     * Reinitializes the <code>HttpsURLConnection</code> and the jakarta http client.
     * //todo: find a better place for this - em18032004
     *
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     * @throws KeyStoreException
     */
    private void reinitializeSsl()
      throws KeyManagementException, NoSuchAlgorithmException,
      IOException, CertificateException, KeyStoreException {

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        final Preferences preferences = Preferences.getPreferences();
        final String trustStoreFile = preferences.getTrustStoreFile();
        final char[] password = preferences.getTrustStorePassword().toCharArray();
        tmf.init(KeystoreUtils.getKeyStore(trustStoreFile, password));
        final SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(null, tmf.getTrustManagers(), null);
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

        Protocol https = new Protocol("https",
          new SecureProtocolSocketFactory() {
              public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException, UnknownHostException {
                  return ctx.getSocketFactory().createSocket(socket, host, port, autoClose);
              }

              public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort)
                throws IOException, UnknownHostException {
                  return ctx.getSocketFactory().createSocket(host, port, clientAddress, clientPort);
              }

              public Socket createSocket(String host, int port)
                throws IOException, UnknownHostException {
                  return ctx.getSocketFactory().createSocket(host, port);
              }
          }, 443);
        Protocol.registerProtocol("https", https);
    }

    private void addSslHostNameVerifier(HttpURLConnection conn) {
        // support host name verifier from both com.sun.net.ssl. and
        // javax.net.ssl.
        class SsgHostnameVerifier implements HostnameVerifier, com.sun.net.ssl.HostnameVerifier {
            public boolean verify(String host, SSLSession session) {
                String peerHost = session.getPeerHost();
                return verify(host, peerHost);
            }

            public boolean verify(String host, String peerHost) {
                if (host.equals(peerHost)) return true;
                String msg = MessageFormat.format(resources.getString("logon.hostname.mismatch"),
                  new Object[]{host, peerHost});
                sslHostNameMismatchUserNotified = true;
                JOptionPane.showMessageDialog(LogonDialog.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        ;
        final SsgHostnameVerifier hostnameVerifier = new SsgHostnameVerifier();
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection)conn).setHostnameVerifier(hostnameVerifier);
        } else if (conn instanceof com.sun.net.ssl.HttpsURLConnection) {
            ((com.sun.net.ssl.HttpsURLConnection)conn).setHostnameVerifier(hostnameVerifier);
        }
    }

    /**
     * Try to download the SSG certificate automatically.
     */
    private void importCertificate(URL surl)
      throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        // dont assume 8080
        int port = 80;
        if (surl.getPort() == 8443 || surl.getPort() == 8080) port = 8080;
        URL url = new URL("http", surl.getHost(), port, surl.getPath());
        CertificateDownloader cd = new CertificateDownloader(url,
          userNameTextField.getText(),
          passwordField.getPassword());
        if (cd.downloadCertificate()) {
            ImportCertificateAction.importSsgCertificate(cd.getCertificate(), url.getHost());
            //showUpdatedSsgCertificateMessage();
            //System.exit(0);
        } else {
            // auth failure
            showInvalidCredentialsMessage();
        }
    }

    /**
     * extract the protocol://host:port part of the URL
     * 
     * @param serviceUrl the full service URL
     * @return the protocol://host:port part if parsed successfully or the
     *         passed parameter if parsing was unsuccessful
     */
    private static String getHostPart(String serviceUrl) {
        String hostPart = serviceUrl;
        try {
            URL url =
              new URL(serviceUrl);
            String sPort =
              (url.getPort() == -1 ? "" : ":" + Integer.toString(url.getPort()));
            hostPart = url.getProtocol() + "://" + url.getHost() + sPort;

        } catch (MalformedURLException e) {
            ;
        }
        return hostPart;
    }

    /**
     * @return the <CODE>PasswordAuthentication</CODE> collected from
     *         the dialog.
     */
    private PasswordAuthentication getAuthentication() {
        pack();
        Utilities.centerOnScreen(this);
        show();
        return authentication;
    }


    /**
     * validate the username and the gateway url
     * 
     * @return true validated, false othwerwise
     */
    private boolean isInputValid() {
        String userName = userNameTextField.getText();
        if (null == userName || "".equals(userName)) {
            return false;
        }
        try {
            if (serverComboBox == null) return false;

            JTextField editor = (JTextField)serverComboBox.getEditor().getEditorComponent();
            String surl = editor.getText();
            if (surl == null || "".equals(surl)) {
                return false;
            }
            URL url = new URL(surl);
            String host = url.getHost();
            return host != null && !"".equals(host);
        } catch (MalformedURLException e) {
        }
        return false;
    }

    /**
     * todo: refactor exception handling
     * handle the logon throwable. Unwrap the exception
     * 
     * @param e 
     */
    private static void handleLogonThrowable(Throwable e, LogonDialog dialog, String serviceUrl) {
        if (dialog.sslHostNameMismatchUserNotified) return;
        Throwable cause = ExceptionUtils.unnestToRoot(e);
        if (cause instanceof VersionException) {
            log.log(Level.WARNING, "logon()", e);
            String msg = MessageFormat.format(dialog.resources.getString("logon.version.mismatch"),
              new Object[]{BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber()});
            JOptionPane.showMessageDialog(dialog, msg, "Warning", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof ConnectException ||
          cause instanceof UnknownHostException ||
          cause instanceof FileNotFoundException) {
            log.log(Level.WARNING, "logon()", e);
            String msg = MessageFormat.format(dialog.resources.getString("logon.connect.error"), new Object[]{getHostPart(serviceUrl)});
            JOptionPane.showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof LoginException || cause instanceof FailedLoginException) {
            log.log(Level.WARNING, "logon()", e);
            dialog.showInvalidCredentialsMessage();
        } else if (cause instanceof RemoteException || cause instanceof IOException) {
            log.log(Level.WARNING, "Could not connect to admin service server", e);
            String hostname = serviceUrl;
            try {
                URL url = new URL(serviceUrl);
                hostname = url.getHost();
            } catch (MalformedURLException e2) {
                log.log(Level.SEVERE, "this sould not happen", e);
            }
            String msg = MessageFormat.format(dialog.resources.getString("service.unavailable.error"), new Object[]{hostname});
            JOptionPane.showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            log.log(Level.WARNING, "logon()", e);
            String msg =
              MessageFormat.format(dialog.resources.getString("logon.connect.error"), new Object[]{getHostPart(serviceUrl)});
            JOptionPane.showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * the LogonListener interface, implementations receive the logon
     * events.
     */
    public static interface LogonListener {
        /**
         * invoked on successful authentication
         * 
         * @param id the id of the authenticated user
         */
        void onAuthSuccess(String id);

        /**
         * invoked on authentication failure
         */
        void onAuthFailure();
    }

    public static void main(String[] args) {
        try {

            JFrame frame = new JFrame() {
                public Dimension getPreferredSize() {
                    return new Dimension(200, 100);
                }
            };
            frame.setTitle("Debugging frame");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
