package com.l7tech.console.panels;

import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.adminws.VersionException;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.action.ImportCertificateAction;
import com.l7tech.util.Locator;
import com.l7tech.common.util.CertificateDownloader;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import javax.net.ssl.SSLHandshakeException;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.CertificateException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;

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

    /** True if "remember id" pref is enabled and a remembered ID was found. */
    private static boolean rememberUser = false;

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    /* Command string for a cancel action (e.g.,a button or menu item). */
    private String CMD_CANCEL = "cmd.cancel";


    /* Command string for a login action (e.g.,a button or menu item). */
    private String CMD_LOGIN = "cmd.login";

    /* the listener for logon events */
    private LogonListener listener;

    private JButton loginButton = null;

    /** the logo above the Login Dialogue **/
    private JLabel companyLogoJLabel = null;

    /** username text field */
    private JTextField userNameTextField = null;
    /** password text field */
    private JPasswordField passwordField = null;

    /**
     * VM property (-D) that triggers off the server check
     * */
    private static final String DISABLE_SERVER_CHECK = "disable.server.check";
    private Frame parentFrame;

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

        constraints = new GridBagConstraints();

        companyLogoJLabel = new JLabel();
        companyLogoJLabel.setText(resources.getString("dialog.title"));
        companyLogoJLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        //companyLogoJLabel.setBorder(BorderFactory.createLoweredBevelBorder());

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(10, 10, 10, 10);
        contents.add(companyLogoJLabel, constraints);

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

        userNameLabel.setDisplayedMnemonic(
          resources.getString("userNameTextField.label").charAt(0));
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
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(userNameTextField, constraints);


        // last ID logic
        String lastID = null;
        try {
            rememberUser = false;
            final Preferences prefs = Preferences.getPreferences();
            lastID = prefs.rememberLoginId() ?
              prefs.getString(Preferences.LAST_LOGIN_ID) :
              null;

            if (prefs.rememberLoginId()) {
                userNameTextField.setText(lastID);
                if (lastID != null)
                    rememberUser = true;
            }
        } catch (IOException e) {
            ; //swallow, problem loading prefs, continue login attmept
        }
        final String fLastID = lastID; // anon class requires final
        userNameTextField.
          addFocusListener(new FocusAdapter() {
              private boolean hasbeenInvoked = false;

              /** Invoked when a component gains the keyboard focus.*/
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
        passwordLabel.setDisplayedMnemonic(
          resources.getString("passwordField.mnemonic").charAt(0));
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
        constraints.gridwidth = 2;
        constraints.insets = new Insets(5, 5, 0, 10);
        contents.add(passwordField, constraints);


        /* context label
        JLabel contextLabel = new JLabel();
        contextLabel.setDisplayedMnemonic(
                                          resources.getString("contextField.mnemonic").charAt(0));
        contextLabel.setToolTipText(resources.getString("contextField.tooltip"));
        contextLabel.setText(resources.getString("contextField.label"));
        contextLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11,12,0,0);
        contents.add(contextLabel, constraints);

        // context field
        contextField = new JTextField();
        contextField.setToolTipText(resources.getString("contextField.tooltip"));
        constraints.gridx=1;
        constraints.gridy=2;
        constraints.weightx=1.0;
        constraints.gridwidth=2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(12,7,0,11);
        contents.add(contextField, constraints); */


        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(10, 0, 10, 10);
        JPanel buttonPanel = createButtonPanel(); // sets global loginButton
        contents.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(loginButton);

    } // initComponents()


    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable loginButton
     *
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

        Utilities.
          equalizeButtonSizes(new JButton[]{cancelButton, loginButton});
        return panel;
    } // createButtonPanel()

    /**
     * The user has selected an option. Here we close and dispose
     * the dialog.
     * If actionCommand is an ActionEvent, getCommandString() is
     * called, otherwise toString() is used to get the action command.
     *
     * @param actionCommand
     *               may be null
     */
    private void windowAction(String actionCommand) {

        authentication = new PasswordAuthentication("", new char[]{});
        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            aborted = true;
        } else if (actionCommand.equals(CMD_LOGIN)) {
            if (!validateInput(userNameTextField.getText())) {
                return;
            }
            /*String context = contextField.getText() == null ? "" : contextField.getText();

            if (!"".equals(context)) {
              if (context.charAt(0) != '.') {
                context = "."+context;
              }
            } */

            authentication =
              new PasswordAuthentication(userNameTextField.getText(),
                passwordField.getPassword());
            aborted = false;
        }
        setVisible(false);
    }

    /**
     *
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
        // service URL
        String serviceURL = null;
        try {
            serviceURL = Preferences.getPreferences().getServiceUrl();
        } catch (IOException e) {
            ; // swallow
        }

        if (serviceURL == null || "".equals(serviceURL)) {
            String msg =
              MessageFormat.format(
                dialog.resources.getString("logon.connect.error"),
                new Object[]{"Invalid service url.\nPlease update your preferences."});
            JOptionPane.
              showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
            dialog.aborted = true;
        }


        while (!dialog.isAborted() && !authenticated) {
            try {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                pw = dialog.getAuthentication();
                if (dialog.isAborted()) break;
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                ClientCredentialManager credentialManager = getCredentialManager();

                // if the service is not avail, format the message and show to te client
                if (!dialog.isServiceAvailable(serviceURL)) {
                    String msg = MessageFormat.format(
                      dialog.resources.getString("logon.connect.error"),
                      new Object[]{getHostPart(serviceURL)});
                    JOptionPane.
                      showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    break;
                }

                credentialManager.login(pw);
                authenticated = true;
                break;
            } catch (VersionException e) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                log.log(Level.WARNING, "logon()", e);
                String msg =
                  MessageFormat.format(
                    dialog.resources.getString("logon.version.mismatch"),
                    new Object[]{e.getExpectedVersion()});
                JOptionPane.
                  showMessageDialog(dialog, msg, "Warning", JOptionPane.ERROR_MESSAGE);
            } catch (LoginException e) { // on invalid credentials
                log.log(Level.WARNING, "logon()", e);
                dialog.showInvalidCredentialsMessage();
            } catch (Exception e) { // everything else, generic error message
                log.log(Level.WARNING, "logon()", e);
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                String msg =
                  MessageFormat.format(
                    dialog.resources.getString("logon.connect.error"),
                    new Object[]{getHostPart(serviceURL)});
                JOptionPane.
                  showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
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

    private void showUpdatedSsgCertificateMessage() {
        parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        JOptionPane.
          showMessageDialog(this,
            resources.getString("logon.updated.ssg.certificate"),
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
     * @param serviceUrl    service url
     * @return true if service available, false otherwise
     */
    private boolean isServiceAvailable(String serviceUrl) {
        boolean serviceAvailable;
        boolean b = Boolean.getBoolean(DISABLE_SERVER_CHECK);
        if (b) return true;

        // try to connect and read the HTTP(S) code. We are ok if it
        // is 200, 401, 403, 302.
        // if it is everything else we assume the connection cannot
        // be established
        HttpClient client = new HttpClient();
        GetMethod method = null;
        URL url = null;
        try {
            url = new URL(serviceUrl);

            method = new GetMethod(serviceUrl);
            int code = client.executeMethod(method);
            if (code == 403 || code == 401 || code == 200 || code == 302) {
                serviceAvailable = true;
            } else {
                serviceAvailable = false;
            }
        } catch (SSLHandshakeException e) {
            serviceAvailable = false;
            log.log(Level.INFO,  "LogonDialog: Attempting to update SSG certificate");
            log.log(Level.INFO,  "LogonDialog", e);
            try {
                importCertificate(url);
            } catch (CertificateException e1) {
                log.log(Level.INFO, "LogonDialog", e);
            } catch (IOException e1) {
                log.log(Level.INFO, "LogonDialog", e);
            } catch (NoSuchAlgorithmException e1) {
                log.log(Level.INFO, "LogonDialog", e);
            } catch (KeyStoreException e1) {
                log.log(Level.INFO, "LogonDialog", e);
            }
        } catch (HttpException e) {
            serviceAvailable = false;
            log.log(Level.INFO, "LogonDialog", e);
        } catch (IOException e) {
            serviceAvailable = false;
            log.log(Level.INFO, "LogonDialog", e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return serviceAvailable;
    }

    /** Try to download the SSG certificate automatically. */
    private void importCertificate(URL surl)
            throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException
    {
        // TODO: Fix hardcoded SSG non-SSL port here
        URL url = new URL("http", surl.getHost(), 8080, surl.getPath());
        CertificateDownloader cd = new CertificateDownloader(url,
                                                             userNameTextField.getText(),
                                                             passwordField.getPassword());
        if (cd.downloadCertificate()) {
            ImportCertificateAction.importSsgCertificate(cd.getCertificate(), url.getHost());
            showUpdatedSsgCertificateMessage();
            System.exit(0);
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
     * logoff from the
     */
    public static void logoff() {
        try {
            // logoff here
        } catch (Exception e) {
            log.log(Level.WARNING, "logoff()", e);
        } finally {
        }
    }


    /**
     *
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
     * validate the username and context
     *
     * @param userName user name entered
     * @return true validated, false othwerwise
     */
    private boolean validateInput(String userName) {
        if (null == userName || "".equals(userName)) {
            JOptionPane.
              showMessageDialog(this,
                resources.getString("userNameTextField.error.empty"),
                resources.getString("userNameTextField.error.title"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;

    }


    /**
     * the LogonListener interface, implementations receive the logon
     * events.
     */
    public static interface LogonListener {
        /**
         * invoked on successful authentication
         *
         * @param id     the id of the authenticated user
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
            new LogonDialog(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
