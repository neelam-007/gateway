package com.l7tech.console.panels;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.VersionException;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.security.InvalidHostCertificateException;
import com.l7tech.console.security.InvalidHostNameException;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.History;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.BadCredentialsException;

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
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the SSG console Logon dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class LogonDialog extends JDialog {
    private static final Logger log = Logger.getLogger(LogonDialog.class.getName());

    /** Preconfigured credentials for applet. */
    private static String preconfiguredGatewayHostname;
    private static String preconfiguredUsername;
    private static String preconfiguredPassword;

    /* the PasswordAuthentication instance with user supplied credentials */
    private PasswordAuthentication authenticationCredentials = null;

    /**
     * True if "remember id" pref is enabled and a remembered ID was found.
     */
    private boolean rememberUser = false;

    /**
     *
     */
    private Set acceptedInvalidHosts = new HashSet();

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
    private SsmPreferences preferences;
    private LogonListener logonListener;

    // Cache version info here so we don't do needless repeat calls to get it again   TODO move this somewhere more reasonable
    public static String remoteProtocolVersion;
    public static String remoteSoftwareVersion;

    /**
     * Create a new LogonDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public LogonDialog(Frame parent) {
        super(parent, true);
        if (!TopComponents.getInstance().getMainWindow().isApplet())
            this.setAlwaysOnTop(true);
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
        preferences = TopComponents.getInstance().getMainWindow().getPreferences();
        lastID = preferences.rememberLoginId() ?
                 preferences.getString(SsmPreferences.LAST_LOGIN_ID) :
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

        serverUrlHistory = preferences.getHistory(SsmPreferences.SERVICE_URL);

        Runnable runnable = new Runnable() {
            public void run() {
                Object[] urls = serverUrlHistory.getEntries();
                for (int i = 0; i < urls.length; i++) {
                    String surl = urls[i].toString();
                    String hostName = surl;
                    try {
                        URL url = new URL(surl);
                        hostName = url.getHost();
                    } catch (MalformedURLException e) {
                    }
                    serverComboBox.addItem(hostName);
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

        authenticationCredentials = new PasswordAuthentication("", new char[]{});
        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            dispose();
        } else if (actionCommand.equals(CMD_LOGIN)) {
            setVisible(false);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    doLogon();
                }
            });
        }
    }

    private void doLogon() {
        authenticationCredentials = new PasswordAuthentication(userNameTextField.getText(), passwordField.getPassword());
        Container parentContainer = getParent();
        // service URL
        String selectedHost = (String) serverComboBox.getSelectedItem();
        if(selectedHost!=null) selectedHost = selectedHost.trim();
        final String sHost = selectedHost;

        try {
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            final SecurityProvider securityProvider = getCredentialManager();

            // fla change: remember this url even if the login wont be successfull (requirement #729)
            serverUrlHistory.add(sHost);

            final LogonInProgressDialog progressDialog = buildLogonInProgressDialog((Frame)getOwner(), sHost);
            final SwingWorker sw =
              new SwingWorker() {
                  private Throwable memoException = null;

                  public Object construct() {
                      try {
                          securityProvider.getAuthenticationProvider().login(authenticationCredentials, sHost, !acceptedInvalidHosts.contains(sHost));
                      } catch (Throwable e) {
                          if (!Thread.currentThread().isInterrupted()) {
                              memoException = e;
                          }
                      }
                      finally {
                        progressDialog.setVisible(false);
                      }
                      if (!Thread.currentThread().isInterrupted() && memoException == null) {
                          return new Boolean(true);
                      }
                      return null;
                  }

                  public void finished() {
                      if (memoException != null) {
                          handleLogonThrowable(memoException, sHost);
                      }
                      if (get() != null) {
                          dispose();
                          try {
                              preferences.store();
                          } catch (IOException e) {
                              log.log(Level.WARNING, "error saving properties", e);
                          }
                          preferences.updateSystemProperties();
                          // invoke the listener
                          if (logonListener != null) {
                              logonListener.onAuthSuccess(authenticationCredentials.getUserName(), sHost);
                          }
                      } else {
                          if (!progressDialog.isCancelled()) {
                              if (logonListener != null) {
                                  logonListener.onAuthFailure();
                              }
                              setVisible(true);
                          }
                      }
                  }
              };

            sw.start();
            progressDialog.setSwingWorker(sw);
            progressDialog.setVisible(true);
        } catch (Exception e) {
            handleLogonThrowable(e, sHost);
        } finally {
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Set a preconfigured Gateway hostname.  If one of these is specified, the Gateway drop-down will
     * not be displayed on the logon form.  Used only by the Applet version of the Manager.
     *
     * @param gatewayHostname  the hostname to use, or null to display the drop-down.
     */
    public static void setPreconfiguredGatewayHostname(String gatewayHostname) {
        preconfiguredGatewayHostname = gatewayHostname;
    }

    /**
     * Set preconfigured credentials to use instead of popping up the logon dialog.  These will be cleared
     * if there is a logon failure.  Used only by the Applet version of the manager.
     *
     * @param username
     * @param password
     */
    public static void setPreconfiguredCredentials(String username, String password) {
        preconfiguredUsername = username;
        preconfiguredPassword = password;
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
        dialog.setVisible(true);
    }

    private static LogonInProgressDialog buildLogonInProgressDialog(Frame parent, String url) {
        LogonInProgressDialog ld = new LogonInProgressDialog(parent, url);
        ld.pack();
        Utilities.centerOnScreen(ld);
        return ld;
    }

    private void showInvalidCredentialsMessage() {
        parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        JOptionPane.
          showMessageDialog(parentFrame,
                            resources.getString("logon.invalid.credentials"),
                            "Warning",
                            JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Before displaying dialog, ensure that correct fields are selected.
     */
    public void setVisible(boolean visible) {
        if (visible && preconfiguredGatewayHostname != null) {
            serverComboBox.setSelectedItem(preconfiguredGatewayHostname);
            serverComboBox.setEnabled(false);
            if (preconfiguredUsername != null && preconfiguredPassword != null) {
                userNameTextField.setText(preconfiguredUsername);
                passwordField.setText(preconfiguredPassword);
                doLogon();
                return;
            }
        }

        if(visible) {
            if (rememberUser) {
                passwordField.requestFocus();
                passwordField.selectAll();
            } else {
                userNameTextField.requestFocus();
                userNameTextField.selectAll();
            }
        }
        super.setVisible(visible);
    }

    private static SecurityProvider getCredentialManager() {
        SecurityProvider credentialManager = Registry.getDefault().getSecurityProvider();
        return credentialManager;
    }

    public static void setLastRemoteProtocolVersion(String rv) {
        remoteProtocolVersion = rv;
    }

    public static void setLastRemoteSoftwareVersion(String rv) {
        remoteSoftwareVersion = rv;
    }

    /** @return the remote protocol version, ie "". */
    public static String getLastRemoteProtocolVersion() {
        return remoteProtocolVersion;
    }

    /** @return the remote software version, ie "4.0", or null if this is a logoff event. */
    public static String getLastRemoteSoftwareVersion() {
        return remoteSoftwareVersion;
    }

    private static class LogonInProgressDialog extends JDialog {
        private SwingWorker worker;
        private String serviceUrl;
        private volatile boolean cancelled = false;

        public LogonInProgressDialog(Frame owner, String url)
          throws HeadlessException {
            super(owner, true);
            setTitle("Logon in progress");
            if (!TopComponents.getInstance().getMainWindow().isApplet())
                setAlwaysOnTop(true);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            setResizable(false);
            serviceUrl = url;
            layoutComponents();
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setSwingWorker(SwingWorker sw) {
            worker = sw;
        }

        private void layoutComponents() {
            Container contentPane = getContentPane();
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5));
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            contentPane.setLayout(new BorderLayout());
            String labelText = "Gateway " + serviceUrl;
            if (labelText.length() > 64) {
                labelText = labelText.substring(0, 60) + "...";
            }
            JLabel label = new JLabel(labelText);
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
            log.finest("Empty user name, returning false");
            return false;
        }
        if (serverComboBox == null) return false;

        JTextField editor = (JTextField)serverComboBox.getEditor().getEditorComponent();
        String surl = editor.getText();
        if (surl == null || "".equals(surl)) {
            log.finest("Empty server name, returning false");
            return false;
        }
        return true;
    }

    /**
     * handle the logon throwable. Unwrap the exception
     *
     * @param e
     */
    private void handleLogonThrowable(Throwable e, String host) {
        preconfiguredPassword = null;
        Throwable cause = ExceptionUtils.unnestToRoot(e);
        if (cause instanceof VersionException) {
            VersionException versionex = (VersionException)cause;
            log.log(Level.WARNING, "logon()", e);
            String msg = null;
            if (versionex.getExpectedVersion() != null &&
                versionex.getExpectedVersion().equals(BuildInfo.getProductVersion())) {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch3"),
                                           new Object[]{
                                               "'" + versionex.getReceivedVersion() + "'",
                                               "'" + versionex.getExpectedVersion() + "'",
                                               BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber()
                                           });
            }
            else if (versionex.getExpectedVersion() != null && versionex.getReceivedVersion() != null) {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch2"),
                                           new Object[]{
                                               "'" + versionex.getReceivedVersion() + "'",
                                               "'" + versionex.getExpectedVersion() + "'",
                                               BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber()
                                           });
            } else {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch"),
                                           new Object[]{
                                               BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber()});
            }
            JOptionPane.showMessageDialog(parentFrame, msg, "Warning", JOptionPane.ERROR_MESSAGE);
        }
        else if (cause instanceof ConnectException ||
          cause instanceof UnknownHostException) {
            log.log(Level.WARNING, "Could not connect, '"+cause.getMessage()+"'");
            String msg = MessageFormat.format(resources.getString("logon.connect.error"), new Object[]{
                host});
            JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
        else if (cause instanceof LoginException || cause instanceof FailedLoginException || cause instanceof BadCredentialsException || cause instanceof AuthenticationException) {
            log.log(Level.WARNING, "Could not connect, authentication error.");
            showInvalidCredentialsMessage();
        }
        else if (cause instanceof MalformedURLException) {
            String msg = resources.getString("logon.invalid.service.url");
            JOptionPane.showMessageDialog(parentFrame, msg, "Warning", JOptionPane.WARNING_MESSAGE);
        }
        else if (cause instanceof RemoteException || cause instanceof IOException) {
            log.log(Level.WARNING, "Could not connect to admin service server", e);
            String msg = MessageFormat.format(resources.getString("service.unavailable.error"), new Object[]{
                host});
            JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
        else if (cause instanceof InvalidHostNameException) {
            InvalidHostNameException ihne = (InvalidHostNameException) cause;
            String msg = MessageFormat.format(resources.getString("logon.hostname.mismatch"),
                                              new Object[]{ihne.getExpectedHost(), ihne.getActualHost()});
            JOptionPane.showMessageDialog(parentFrame, msg, "Warning", JOptionPane.WARNING_MESSAGE);
            acceptedInvalidHosts.add(ihne.getExpectedHost());
        }
        else if (cause instanceof InvalidHostCertificateException) {
            String msg = MessageFormat.format(resources.getString("logon.certificate.problem"),
                                              new Object[]{host});
            JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
        else {
            log.log(Level.WARNING, "logon()", e);
            String msg = MessageFormat.format(resources.getString("logon.connect.error"), new Object[]{host});
            JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
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
