package com.l7tech.console.panels;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.VersionException;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.action.ImportCertificateAction;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.History;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;
import com.l7tech.spring.remoting.rmi.NamingURL;
import com.l7tech.spring.remoting.rmi.ssl.SSLTrustFailureHandler;
import com.l7tech.spring.remoting.rmi.ssl.SslRMIClientSocketFactory;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.EventListener;
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
     * the dialog title label *
     */
    private JLabel dialogTitleLabel = null;

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

    private Frame parentFrame;
    private History serverUrlHistory;
    private Preferences preferences;
    private LogonListener logonListener;

    /**
     * holds the scheme://host:port the user dialog is attempting the connection to
     */
    private NamingURL adminServiceNamingURL;
    private X509Certificate[] serverCertificateChain = null;

    /** context field (company.realm)
     private JTextField contextField = null;*/

    /**
     * Create a new LogonDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public LogonDialog(Frame parent) {
        super(parent, false);
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

        dialogTitleLabel = new JLabel();
        dialogTitleLabel.setText(resources.getString("dialog.title"));
        dialogTitleLabel.setFont(new Font("Dialog", Font.BOLD, 12));

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(10, 10, 10, 10);
        contents.add(dialogTitleLabel, constraints);

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
        constraints.insets = new Insets(5, 10, 0, 0);
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
        constraints.insets = new Insets(5, 10, 0, 0);

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
        constraints.insets = new Insets(20, 10, 0, 0);
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
                    String surl = urls[i].toString();
                    String hostNamePort = surl;
                    try {
                        URL url = new URL(surl);
                        hostNamePort = url.getHost() + ":" + url.getPort();
                    } catch (MalformedURLException e) {
                    }
                    serverComboBox.addItem(hostNamePort);
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
            dispose();
        } else if (actionCommand.equals(CMD_LOGIN)) {
            hide();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    doLogon();
                }
            });
        }
    }

    private void doLogon() {
        authentication = new PasswordAuthentication(userNameTextField.getText(),
          passwordField.getPassword());
        Container parentContainer = getParent();
        // service URL
        final String selectedURL = (String)serverComboBox.getSelectedItem();
        final String sNamingUrl = selectedURL;
        try {
            adminServiceNamingURL = NamingURL.parse(NamingURL.DEFAULT_SCHEME + "://" + selectedURL + "/AdminLogin");
        } catch (MalformedURLException e) {
            String msg = resources.getString("logon.invalid.service.url");
            JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            initializeSslTrustHandler();
            sslHostNameMismatchUserNotified = false;
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            final SecurityProvider securityProvider = getCredentialManager();

            // fla change: remember this url even if the login wont be successfull (requirement #729)
            serverUrlHistory.add(sNamingUrl);


            final SwingWorker sw =
              new SwingWorker() {
                  private Exception memoException = null;
                  private LogonInProgressDialog progressDialog = showLogonInProgressDialog(this, LogonDialog.this, sNamingUrl);

                  public Object construct() {
                      try {
                          securityProvider.getAuthenticationProvider().login(authentication, adminServiceNamingURL.toString());
                      } catch (Exception e) {
                          if (!Thread.currentThread().isInterrupted()) {
                              memoException = e;
                          }
                      }
                      if (!Thread.currentThread().isInterrupted() && memoException == null) {
                          return new Boolean(true);
                      }
                      return null;
                  }

                  public void finished() {
                      progressDialog.dispose();
                      if (memoException != null) {
                          handleLogonThrowable(memoException, adminServiceNamingURL);
                      }
                      if (get() != null) {
                          dispose();
                          try {
                              preferences.store();
                          } catch (IOException e) {
                              log.log(Level.WARNING, "error saving properties", e);
                          }
                          preferences.updateSystemProperties();
                          try {
                              sslPostLogin();
                          } catch (Exception e) {
                              handleLogonThrowable(memoException, adminServiceNamingURL);
                          }
                          // invoke the listener
                          if (logonListener != null) {
                              logonListener.onAuthSuccess(authentication.getUserName(), sNamingUrl);
                          }
                      } else {
                          serverCertificateChain = null; // reset if we recorded something
                          if (logonListener != null) {
                              logonListener.onAuthFailure();
                          }
                          if (!progressDialog.isCancelled()) {
                              show();
                          }
                      }
                  }
              };

            sw.start();
        } catch (Exception e) {
            handleLogonThrowable(e, adminServiceNamingURL);
        } finally {
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * invoke logon dialog
     *
     * @param frame
     */
    public static void logon(JFrame frame, LogonListener listener) {
        final LogonDialog dialog = new LogonDialog(frame);
        dialog.logonListener = listener;
        dialog.setResizable(false);
        dialog.setSize(300, 275);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();

    }

    private static LogonInProgressDialog showLogonInProgressDialog(SwingWorker sw, JDialog parent, String url) {
        LogonInProgressDialog ld = new LogonInProgressDialog(parent, sw, url);
        ld.pack();
        Utilities.centerOnScreen(ld);
        ld.show();
        return ld;
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

    private static SecurityProvider getCredentialManager() {
        SecurityProvider credentialManager = Registry.getDefault().getSecurityProvider();
        return credentialManager;
    }

    /**
     * Initialize the SSL logic arround login. This register the trust failure handler
     * that will be invoked if the server cert is not yet present. We just record it here
     * as we do not know yet whether this is is ssg. Only after the successful login, the
     * sslPostLogin() handler will import the cert and reset the ssl.
     */
    private void initializeSslTrustHandler() {
        SslRMIClientSocketFactory.resetSocketFactory();
        SslRMIClientSocketFactory.setTrustFailureHandler(new SSLTrustFailureHandler() {
            public boolean handle(CertificateException e, X509Certificate[] chain, String authType) {
                if (chain == null) {
                    return false;
                }
                serverCertificateChain = chain;
                return true;
            }
        });
    }

    /**
     * Invoked after the successful login
     */
    private void sslPostLogin()
      throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException {
        if (serverCertificateChain == null) {
            return; // nothing to do, cert has been imported earlier
        }
        ImportCertificateAction.importSsgCertificate(serverCertificateChain[0]);
        SslRMIClientSocketFactory.resetSocketFactory();

    }

    private static class LogonInProgressDialog extends JDialog {
        private SwingWorker worker;
        private String serviceUrl;
        private boolean cancelled = false;

        public LogonInProgressDialog(JDialog owner, SwingWorker sw, String url)
          throws HeadlessException {
            super(owner, false);
            setTitle("Logon in progress");
            worker = sw;
            serviceUrl = url;
            layoutComponents();
        }

        public boolean isCancelled() {
            return cancelled;
        }

        private void layoutComponents() {
            Container contentPane = getContentPane();
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5));
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            contentPane.setLayout(new BorderLayout());
            JLabel label = new JLabel("Gateway " + serviceUrl);
            panel.add(label);
            panel.add(Box.createHorizontalStrut(100));
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    worker.interrupt();
                    dispose();
                    cancelled = true;
                }
            });

            panel.add(cancelButton);
            getRootPane().setDefaultButton(cancelButton);

            contentPane.add(panel);
        }
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
        if (serverComboBox == null) return false;

        JTextField editor = (JTextField)serverComboBox.getEditor().getEditorComponent();
        String surl = editor.getText();
        if (surl == null || "".equals(surl)) {
            return false;
        }
        return true;
    }

    /**
     * handle the logon throwable. Unwrap the exception
     *
     * @param e
     */
    private void handleLogonThrowable(Throwable e, NamingURL serviceUrl) {
        if (sslHostNameMismatchUserNotified) return;
        Throwable cause = ExceptionUtils.unnestToRoot(e);
        if (cause instanceof VersionException) {
            VersionException versionex = (VersionException)cause;
            log.log(Level.WARNING, "logon()", e);
            String msg = null;
            if (versionex.getExpectedVersion() != null && versionex.getReceivedVersion() != null) {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch2"),
                  new Object[]{
                      "'" + versionex.getReceivedVersion() + "'",
                      "'" + versionex.getExpectedVersion() + "'",
                      BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber()
                  });
            } else {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch"),
                  new Object[]{BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber()});
            }
            JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof ConnectException ||
          cause instanceof UnknownHostException) {
            log.log(Level.WARNING, "logon()", e);
            String msg = MessageFormat.format(resources.getString("logon.connect.error"), new Object[]{serviceUrl.getHost()});
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof LoginException || cause instanceof FailedLoginException) {
            log.log(Level.WARNING, "logon()", e);
            showInvalidCredentialsMessage();
        } else if (cause instanceof RemoteException || cause instanceof IOException) {
            log.log(Level.WARNING, "Could not connect to admin service server", e);
            String msg = MessageFormat.format(resources.getString("service.unavailable.error"), new Object[]{serviceUrl.getHost()});
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            log.log(Level.WARNING, "logon()", e);
            String msg =
              MessageFormat.format(resources.getString("logon.connect.error"), new Object[]{serviceUrl.getHost()});
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * the LogonListener interface, implementations receive the logon
     * events.
     */
    public static interface LogonListener extends EventListener {
        /**
         * invoked on successful authentication
         *
         * @param id the id of the authenticated user
         */
        void onAuthSuccess(String id, String serverURL);

        /**
         * invoked on authentication failure
         */
        void onAuthFailure();
    }

}
