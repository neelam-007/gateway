package com.l7tech.console.panels;

import com.l7tech.console.action.ChangePasswordAction;
import com.l7tech.gateway.common.admin.AdminLogin;
import com.l7tech.identity.*;
import com.l7tech.util.BuildInfo;
import com.l7tech.gateway.common.VersionException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.security.*;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.TrustCertificateDialog;
import com.l7tech.console.util.*;
import com.l7tech.objectmodel.InvalidPasswordException;

import javax.net.ssl.SSLHandshakeException;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AccountLockedException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * This class is the SSG console Logon dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class LogonDialog extends JDialog {
    private static final Logger log = Logger.getLogger(LogonDialog.class.getName());

    /* the PasswordAuthentication instance with user supplied credentials */
    private PasswordAuthentication authenticationCredentials = null;

    /**
     * The component to focus when the dialog is shown
     */
    private JComponent focusComponent = null;

    /**
     *
     */
    private Set<String> acceptedInvalidHosts = new HashSet<String>();

    /**
     * Resource bundle with default locale
     */
    private ResourceBundle resources = null;

    /* Command string for a cancel action (e.g.,a button or menu item). */
    private String CMD_CANCEL = "cmd.cancel";


    /* Command string for a login action (e.g.,a button or menu item). */
    private String CMD_LOGIN = "cmd.login";

    private String CMD_MANAGE_CERT = "cmd.manage.cert";

    private JButton loginButton = null;

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

    private JRadioButton useCreds = null;
    private JRadioButton useCert = null;
    private JComboBox certSelection = null;
    private JButton manageCertBtn = null;
    private JLabel certLabel = null;
    private JLabel passwordLabel = null;
    private JLabel userNameLabel = null;
    private Hashtable<String, X509Certificate> certsHash = new Hashtable<String, X509Certificate>();

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
        GridBagConstraints constraints;

        Container contents = getContentPane();
        contents.setLayout(new GridBagLayout());
        //setTitle (resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            @Override
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
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        windowAction(CMD_CANCEL);
                    }
                });

        constraints = new GridBagConstraints();

        JLabel dialogTitleLabel = new JLabel();
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

        useCreds = new JRadioButton(resources.getString("logon.radio.usernamePassword"));
        useCreds.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnabledState();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        contents.add(useCreds, constraints);


        userNameTextField = new JTextField(); //needed below

        // user name label
        userNameLabel = new JLabel();
        userNameLabel.setToolTipText(resources.getString("userNameTextField.tooltip"));
        userNameTextField.setDocument(new FilterDocument(IdentityProviderLimits.MAX_ID_LENGTH.getValue(),
                new FilterDocument.Filter() {
                    @Override
                    public boolean accept(String str) {
                        return str != null;
                    }
                }));

        DocumentListener inputValidDocumentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (loginButton != null)
                    loginButton.setEnabled(isInputValid());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (loginButton != null)
                    loginButton.setEnabled(isInputValid());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (loginButton != null)
                    loginButton.setEnabled(isInputValid());
            }
        };

        userNameLabel.setDisplayedMnemonic(resources.getString("userNameTextField.label").charAt(0));
        userNameLabel.setLabelFor(userNameTextField);
        userNameLabel.setText(resources.getString("userNameTextField.label"));

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 25, 0, 0);
        contents.add(userNameLabel, constraints);

        // user name text field
        userNameTextField.setToolTipText(resources.getString("userNameTextField.tooltip"));
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 1.0;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 15, 0, 10);
        contents.add(userNameTextField, constraints);

        constraints.gridx = 3;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contents.add(Box.createHorizontalStrut(100), constraints);

        final JPanel buttonPanel = createButtonPanel(); // sets global loginButton
        passwordField = new JPasswordField(); // needed below

        // last ID logic
        focusComponent = userNameTextField;
        preferences = TopComponents.getInstance().getPreferences();
        final String lastID;
        final boolean lastIDWasCert;
        if (preferences.rememberLoginId()) {
            lastID = preferences.getString(SsmPreferences.LAST_LOGIN_ID);
            lastIDWasCert = "certificate".equals(preferences.getString(SsmPreferences.LAST_LOGIN_TYPE));

            if (!lastIDWasCert)
                userNameTextField.setText(lastID);
            if (lastID != null) {
                focusComponent = lastIDWasCert ?
                        loginButton :
                        passwordField;
            }
        } else {
            lastID = null;
            lastIDWasCert = false;
        }
        userNameTextField.getDocument().addDocumentListener(inputValidDocumentListener);

        // password label
        passwordLabel = new JLabel();
        passwordLabel.setToolTipText(resources.getString("passwordField.tooltip"));
        passwordLabel.setDisplayedMnemonic(resources.getString("passwordField.mnemonic").charAt(0));
        passwordLabel.setText(resources.getString("passwordField.label"));
        passwordLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 25, 0, 0);

        contents.add(passwordLabel, constraints);

        // password field
        passwordField.setToolTipText(resources.getString("passwordField.tooltip"));
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(5, 15, 0, 10);
        contents.add(passwordField, constraints);

        constraints.gridx = 3;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contents.add(Box.createHorizontalStrut(100), constraints);

        useCert = new JRadioButton(resources.getString("logon.radio.clientCert"));
        useCert.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnabledState();
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15, 0, 0, 0);
        contents.add(useCert, constraints);

        ButtonGroup selection = new ButtonGroup();
        selection.add(useCreds);
        selection.add(useCert);
        useCreds.setSelected(!lastIDWasCert);
        useCert.setSelected(lastIDWasCert);

        certLabel = new JLabel(resources.getString("logon.certificateLabel"));
        certLabel.setEnabled(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(2, 25, 0, 0);
        contents.add(certLabel, constraints);

        certSelection = new JComboBox();
        populateCertificateChoices();
        certSelection.setEnabled(false);
        certSelection.setEditable(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(2, 15, 0, 10);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contents.add(certSelection, constraints);
        if (lastID != null && lastIDWasCert && certSelection.getModel().getSize() > 0) {
            certSelection.setSelectedItem(lastID);
            if (!lastID.equals(certSelection.getSelectedItem())) {
                certSelection.setSelectedIndex(0);
            }
        }

        JPanel certBtnPanel = new JPanel();
        certBtnPanel.setLayout(new BoxLayout(certBtnPanel, 0));
        manageCertBtn = new JButton(resources.getString("logon.importCert"));
        manageCertBtn.setActionCommand(CMD_MANAGE_CERT);
        manageCertBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });
        manageCertBtn.setEnabled(false);
        certBtnPanel.add(manageCertBtn);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 5;
        constraints.insets = new Insets(5, 15, 0, 10);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        contents.add(certBtnPanel, constraints);

        JSeparator sep = new JSeparator();
        constraints = new GridBagConstraints();
        constraints.gridwidth = 5;
        constraints.gridy = 7;
        constraints.insets = new Insets(10, 10, 0, 10);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contents.add(sep, constraints);

        //url label
        JLabel serverLabel = new JLabel();
        serverLabel.setDisplayedMnemonic(resources.getString("serverField.mnemonic").charAt(0));
        serverLabel.setToolTipText(resources.getString("serverField.tooltip"));
        serverLabel.setText(resources.getString("serverField.label"));
        serverLabel.setLabelFor(passwordField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 8;
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
        JTextField jtf = (JTextField) basicComboBoxEditor.getEditorComponent();
        jtf.getDocument().addDocumentListener(inputValidDocumentListener);
        serverComboBox.setEditor(basicComboBoxEditor);
        serverComboBox.setToolTipText(resources.getString("serverField.tooltip"));
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.weightx = 1.0;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(20, 5, 0, 10);
        contents.add(serverComboBox, constraints);

        serverUrlHistory = preferences.getHistory(SsmPreferences.SERVICE_URL);
        String sMaxSize = preferences.getString(SsmPreferences.NUM_SSG_HOSTS_HISTORY, "5");
        if (sMaxSize != null && !sMaxSize.equals("")) {
            try {
                serverUrlHistory.setMaxSize((new Integer(sMaxSize)));
            } catch (NumberFormatException nfe) {
                //Swallow - incorrectly set property
                //don't need to set, it's has an internal default value
                log.log(Level.FINE, "Ignoring invalid url history size ''{0}''.", sMaxSize);
            }
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateServerUrlHistory();
            }
        };

        SwingUtilities.invokeLater(runnable);
        serverComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginButton.setEnabled(isInputValid());
            }
        });

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(10, 0, 10, 10);
        contents.add(buttonPanel, constraints);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (focusComponent != null) {
                    focusComponent.requestFocus();
                    if (focusComponent instanceof JTextField) {
                        ((JTextField) focusComponent).selectAll();
                    }
                    focusComponent = null;
                }
            }
        });

        updateEnabledState();
        loginButton.setEnabled(isInputValid());
        getRootPane().setDefaultButton(loginButton);
    }

    private void updateEnabledState() {
        final boolean isCredentials = useCreds.isSelected();

        userNameLabel.setEnabled(isCredentials);
        userNameTextField.setEnabled(isCredentials);
        passwordLabel.setEnabled(isCredentials);
        passwordField.setEnabled(isCredentials);

        useCert.setSelected(!isCredentials);
        certLabel.setEnabled(!isCredentials);
        certSelection.setEnabled(!isCredentials);
        manageCertBtn.setEnabled(!isCredentials);
        if (!!isCredentials)
            populateCertificateChoices();

        loginButton.setEnabled(isInputValid());
    }

    /*
    * Caller thread should be event thread only
    * Factored out code to build up Server host list as will get updated while user is interacting
    * with the log on component
    * */
    private void updateServerUrlHistory() {
        Object[] urls = serverUrlHistory.getEntries();
        serverComboBox.removeAllItems();
        for (int i = 0; i < urls.length; i++) {
            String surl = urls[i].toString();
            String hostName = surl;
            try {
                URL url = new URL(surl);
                hostName = url.getHost();
            } catch (MalformedURLException e) {
                // can't happen, but omit from list
            }
            serverComboBox.addItem(hostName);
            if (i == 0) {
                serverComboBox.setSelectedIndex(0);
            }
        }
    }

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     * <p/>
     * Sets the variable loginButton
     *
     * @return the new button panel
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
            @Override
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
            @Override
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
            if (parentFrame instanceof MainWindow)
                ((MainWindow)parentFrame).enableOrDisableConnectionComponents(true);
        } else if (actionCommand.equals(CMD_LOGIN)) {
            setVisible(false);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    doLogon(null, null);
                }
            });
        } else if (actionCommand.equals(CMD_MANAGE_CERT)) {
            UserIdentificationRequestDialog certManager = new UserIdentificationRequestDialog(certsHash);
            Utilities.centerOnScreen(certManager);
            DialogDisplayer.display(certManager, new Runnable() {
                @Override
                public void run() {
                    populateCertificateChoices();
                    loginButton.setEnabled(certSelection.getItemCount() > 0 &&
                            (serverComboBox.getSelectedItem() != null && !((String) serverComboBox.getSelectedItem()).equalsIgnoreCase("")));
                }
            });
        }
    }

    /**
     * Populates the certificate selection combo box with possible certificates.
     */
    private void populateCertificateChoices() {
        try {

            Set<X509Certificate> certs = preferences.getKeys();
            String[] items = new String[certs.size()];
            certSelection.removeAllItems(); //clear any old ones
            certsHash.clear();
            int i = 0;
            for (X509Certificate cert : certs) {
                certsHash.put(cert.getSubjectDN().getName(), cert);
                items[i++] = cert.getSubjectDN().getName();
                //certSelection.addItem((String) cert.getSubjectDN().getName());
            }

            Arrays.sort(items);
            for (String item : items) {
                certSelection.addItem(item);
            }

        } catch (Exception e) {
            //do nothing
            log.finest("Unable to load list of possible certificate for selection.");
        }
    }

    /**
     * Does the actual logon process.
     * If both the parameters are set to NULL, then it proceeds as a normal logon.  If both fields are not NULL, then
     * it will treat as a change password and logon process.
     *
     * @param newPassword The new password that should be changed for the login user
     * @param authCreds   The credentials to be used for changing the password and logon
     */
    private void doLogon(final String newPassword, final PasswordAuthentication authCreds) {
        authenticationCredentials = new PasswordAuthentication(userNameTextField.getText(), passwordField.getPassword());
        final Container parentContainer = getParent();
        // service URL
        String selectedHost = (String) serverComboBox.getSelectedItem();
        if (selectedHost != null) selectedHost = selectedHost.trim();
        final String sHost = selectedHost;

        focusComponent = useCert.isSelected() ? certSelection : passwordField;

        LogonInProgressDialog progressDialog = null;
        try {
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            final SecurityProvider securityProvider = getCredentialManager();

            // fla change: remember this url even if the login wont be successful (requirement #729)
            serverUrlHistory.add(sHost);

            progressDialog = buildLogonInProgressDialog((Frame) getOwner(), sHost);
            final LogonInProgressDialog progressDialog1 = progressDialog;
            final SwingWorker sw =
                    new SwingWorker() {
                        private Throwable memoException = null;

                        @Override
                        public Object construct() {
                            try {
                                AuthenticationProvider authProv = securityProvider.getAuthenticationProvider();
                                //need to set the selected certificate if the user has selected to use client-cert login
                                if (useCert.isSelected()) {
                                    String selected = (String) certSelection.getSelectedItem();
                                    setSelectedCertificateByDn(selected);
                                    authenticationCredentials = new PasswordAuthentication("", "".toCharArray());
                                } else {
                                    setSelectedCertificateByDn(""); //clear selected certificate
                                }

                                //determine which approach to logon based on the parameters
                                if (newPassword == null && authCreds == null) {
                                    //normal logon process
                                    authProv.login(authenticationCredentials, sHost, !acceptedInvalidHosts.contains(sHost), null);
                                } else {
                                    //require to change the password and logon
                                    authProv.login(authCreds, sHost, !acceptedInvalidHosts.contains(sHost), newPassword);
                                }
                            } catch (Throwable e) {
                                if (!progressDialog1.isCancelled()) {
                                    memoException = e;
                                }
                            } finally {
                                if (progressDialog1.isCancelled()) {
                                    // if cancelled, clear the interrupted status so disposal runs cleanly
                                    Thread.interrupted();
                                }
                                progressDialog1.dispose();
                            }
                            if (memoException == null && !progressDialog1.isCancelled()) {
                                return Boolean.TRUE;
                            }
                            return null;
                        }

                        @Override
                        public void finished() {
                            boolean showLogin = true;
                            if (memoException != null) {
                                showLogin = !handleLogonThrowable(memoException, sHost);
                            }
                            try {
                                preferences.store();
                            } catch (IOException e) {
                                log.log(Level.WARNING, "error saving properties", e);
                            }
                            if (get() != null) {
                                dispose();
                                preferences.updateSystemProperties();
                                if (logonListener != null) {
                                    if (useCert.isSelected()) {
                                        String selected = (String) certSelection.getSelectedItem();
                                        logonListener.onAuthSuccess(certsHash.get(selected).getSubjectDN().getName(), true);
                                    } else {
                                        logonListener.onAuthSuccess(authenticationCredentials.getUserName(), false);
                                    }
                                }
                            } else {
                                if (!progressDialog1.isCancelled()) {
                                    if (logonListener != null) {
                                        logonListener.onAuthFailure();
                                    }

                                    setVisible(showLogin);
                                }
                            }
                        }
                    };

            sw.start();
            progressDialog.setSwingWorker(sw);
            DialogDisplayer.display(progressDialog, new Runnable() {
                @Override
                public void run() {
                    parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    progressDialog1.dispose();
                    if (progressDialog1.isCancelled() && parentFrame instanceof MainWindow)
                        ((MainWindow)parentFrame).enableOrDisableConnectionComponents(true);
                }
            });
        } catch (Exception e) {
            parentContainer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (progressDialog != null) progressDialog.dispose();

            handleLogonThrowable(e, sHost);
        } finally {
            //Update the host drop down with any host just entered in
            updateServerUrlHistory();
        }
    }

    /**
     * Parses through the list of certificate's dn and compares the DN provided by the parameter.  If a match is found
     * it will set that certificate into the SSM preferences.
     *
     * @param dn The DN that will be used for comparison against list of certificates.
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws CertificateException
     */
    private void setSelectedCertificateByDn(String dn) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        preferences.setClientCertificate(certsHash.get(dn));
    }

    /**
     * invoke logon dialog
     *
     * @param frame    parent frame for the logon dialog
     * @param listener listener to invoke with the result
     */
    public static void logon(Frame frame, LogonListener listener) {
        final LogonDialog dialog = new LogonDialog(frame);
        dialog.logonListener = listener;
        dialog.setResizable(false);
        dialog.setSize(300, 275);
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        dialog.setVisible(true);
    }

    private static LogonInProgressDialog buildLogonInProgressDialog(Frame parent, String url) {
        LogonInProgressDialog ld = new LogonInProgressDialog(parent, url);
        ld.pack();
        Utilities.centerOnParentWindow(ld);
        return ld;
    }

    private void showInvalidCredentialsMessage() {
        parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        JOptionPane.
                showMessageDialog(parentFrame,
                        useCert.isSelected() ?
                                resources.getString("logon.invalid.certificate") :
                                resources.getString("logon.invalid.credentials"),
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
    }

    private void showRequireClientCertificateMessage() {
        parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        JOptionPane.
                showMessageDialog(parentFrame,
                        resources.getString("login.require.client.cert"),
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
    }

    private void showLockAccountMessage(AccountLockedException ex) {
        parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        JOptionPane.
                showMessageDialog(parentFrame,
                        ex.getMessage(),
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
    }

    private static SecurityProvider getCredentialManager() {
        return Registry.getDefault().getSecurityProvider();
    }

    public static void setLastRemoteProtocolVersion(String rv) {
        remoteProtocolVersion = rv;
    }

    public static void setLastRemoteSoftwareVersion(String rv) {
        remoteSoftwareVersion = rv;
    }

    /**
     * @return the remote protocol version, ie "".
     */
    public static String getLastRemoteProtocolVersion() {
        return remoteProtocolVersion;
    }

    /**
     * @return the remote software version, ie "4.0", or null if this is a logoff event.
     */
    public static String getLastRemoteSoftwareVersion() {
        return remoteSoftwareVersion;
    }

    private static class LogonInProgressDialog extends JDialog {
        private SwingWorker worker;
        private String serviceUrl;
        private volatile boolean cancelled = false;

        LogonInProgressDialog(Frame owner, String url)
                throws HeadlessException {
            super(owner, true);
            setTitle("Logon in progress");
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
                @Override
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
        if (null == userName || "".equals(userName) && useCreds.isSelected()) {
            log.finest("Empty user name, returning false");
            return false;
        }

        if (useCert.isSelected() && certSelection.getSelectedItem() == null) {
            log.finest("No certificate selected, returning false");
            return false;
        }

        if (serverComboBox == null) return false;

        JTextField editor = (JTextField) serverComboBox.getEditor().getEditorComponent();
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
     * @param e    the throwable to handle
     * @param host the host we were trying to connect to
     * @return true if this error is handled and the login dialog should not be displayed
     */
    private boolean handleLogonThrowable(Throwable e, String host) {
        boolean handled = false;

        Throwable cause = ExceptionUtils.unnestToRoot(e);
        if (cause instanceof VersionException) {
            VersionException versionex = (VersionException) cause;
            log.log(Level.WARNING, "logon()", e);
            String msg;
            if (versionex.getExpectedVersion() != null &&
                    versionex.getExpectedVersion().equals(BuildInfo.getProductVersion())) {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch3"),
                        "'" + versionex.getReceivedVersion() + "'",
                        "'" + versionex.getExpectedVersion() + "'",
                        BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber());
            } else if (versionex.getExpectedVersion() != null && versionex.getReceivedVersion() != null) {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch2"),
                        "'" + versionex.getReceivedVersion() + "'",
                        "'" + versionex.getExpectedVersion() + "'",
                        BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber());
            } else {
                msg = MessageFormat.format(resources.getString("logon.version.mismatch"),
                        BuildInfo.getProductVersion() + " build " + BuildInfo.getBuildNumber());
            }
            JOptionPane.showMessageDialog(parentFrame, msg, "Warning", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof ConnectException ||
                cause instanceof UnknownHostException) {
            log.log(Level.WARNING, "Could not connect, '" + cause.getMessage() + "'");
            String msg = MessageFormat.format(resources.getString("logon.connect.error"), host);
            JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof AccountLockedException) {
            log.log(Level.WARNING, "Lock account," + cause.getMessage());
            showLockAccountMessage((AccountLockedException) cause);
        } else if (cause instanceof InvalidPasswordException) {
            //problems with the new password (most likely not password policy compliant), re-ask the user to enter a better password
            ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog(
                    this,
                    ChangePasswordAction.formatErrors( (InvalidPasswordException)cause ),
                    userNameTextField.getText(),
                    false);
            changePasswordDialog.setPasswordPolicyDescription(((InvalidPasswordException) cause).getPasswordPolicyDescription());
            changePasswordDialog.setLoggedIn(false);
            changePasswordDialog.setVisible(true);
            if (changePasswordDialog.wasOk()) {
                handled = true;
                String newPassword = new String(changePasswordDialog.getNewPasswordAuthentication().getPassword());
                PasswordAuthentication newPasswordAuth = changePasswordDialog.getCurrentPasswordAuthentication();
                changePasswordDialog.setVisible(false);
                changePasswordDialog.dispose();
                doLogon(newPassword, newPasswordAuth);  //try logon again
            }
        } else if (cause instanceof LoginException || cause instanceof BadCredentialsException || cause instanceof AuthenticationException) {
            log.log(Level.WARNING, "Could not connect, authentication error.");
            if (cause instanceof LoginRequireClientCertificateException) {
                showRequireClientCertificateMessage();
            } else if (ExceptionUtils.causedBy(cause, CredentialExpiredPasswordDetailsException.class)) {
                //the credential was expired, we'll need to provide them a way to enter their new password
                ChangePasswordDialog changePasswordDialog =
                        new ChangePasswordDialog(this, "Password expired, please change your password.", userNameTextField.getText(), false);
                CredentialExpiredPasswordDetailsException ex = ExceptionUtils.getCauseIfCausedBy(cause, CredentialExpiredPasswordDetailsException.class);
                changePasswordDialog.setPasswordPolicyDescription(ex.getPasswordPolicyDescription());
                changePasswordDialog.setLoggedIn(false);
                changePasswordDialog.setVisible(true);
                if (changePasswordDialog.wasOk()) {
                    handled = true;
                    String newPassword = new String(changePasswordDialog.getNewPasswordAuthentication().getPassword());
                    PasswordAuthentication newPasswordAuth = changePasswordDialog.getCurrentPasswordAuthentication();
                    changePasswordDialog.setVisible(false);
                    changePasswordDialog.dispose();
                    doLogon(newPassword, newPasswordAuth);  //try again
                }
            } else if (AdminLogin.ERR_MSG_USERNAME_PSWD_BOTH_REQUIRED.equals(cause.getMessage())) {
                JOptionPane.showMessageDialog(parentFrame, AdminLogin.ERR_MSG_USERNAME_PSWD_BOTH_REQUIRED, "Warning", JOptionPane.WARNING_MESSAGE);
            } else {
                showInvalidCredentialsMessage();
            }
        } else if (ExceptionUtils.causedBy(cause, MalformedURLException.class) || ExceptionUtils.causedBy(cause, NumberFormatException.class)) {
            String msg = resources.getString("logon.invalid.service.url");
            JOptionPane.showMessageDialog(parentFrame, msg, "Warning", JOptionPane.WARNING_MESSAGE);
        } else if (cause instanceof SSLHandshakeException) {
            log.log(Level.WARNING, "Handshake failure: " + cause.getMessage(), ExceptionUtils.getDebugException(e));
            JOptionPane.showMessageDialog(parentFrame, resources.getString("logon.handshake.error"), "Error", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof IOException) {
            log.log(Level.WARNING, "Could not connect to admin service server", e);
            String msg = MessageFormat.format(resources.getString("service.unavailable.error"), host);
            JOptionPane.showMessageDialog(parentFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (cause instanceof InvalidHostNameException) {
            InvalidHostNameException ihne = (InvalidHostNameException) cause;
            String msg = MessageFormat.format(resources.getString("logon.hostname.mismatch"),
                    ihne.getExpectedHost(), ihne.getActualHost());
            int option = JOptionPane.showOptionDialog(parentFrame, msg, "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {
                acceptedInvalidHosts.add(ihne.getExpectedHost());
                doLogon(null, null);
            }
        } else if (cause instanceof InvalidHostCertificateException) {
            X509Certificate cert = ((InvalidHostCertificateException) cause).getCertificate();
            TrustCertificateDialog dialog = new TrustCertificateDialog((JFrame) TopComponents.getInstance().getTopParent(),
                    cert,
                    resources.getString("logon.ssg.untrustedCert.title"),
                    resources.getString("logon.ssg.untrustedCert.question"));
            handled = true;
            dialog.setVisible(true);
            if (dialog.isTrusted()) {
                // import new certificate to trust store
                boolean imported = false;
                try {
                    TopComponents.getInstance().getPreferences().importSsgCert(cert, cert.getSubjectDN().getName());
                    SecurityProvider securityProvider = getCredentialManager();
                    securityProvider.getAuthenticationProvider().acceptServerCertificate(cert);
                    imported = true;
                } catch (Exception ex) {
                    log.log(Level.WARNING, "Error importing new certifiate.", ex);
                }

                if (imported) {
                    // try again
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            doLogon(null, null);
                        }
                    });
                }
            }
        } else {
            ErrorManager.getDefault().notify(Level.WARNING, e, MessageFormat.format(resources.getString("logon.connect.error"), host));
        }

        return handled;
    }

    /**
     * the LogonListener interface, implementations receive the logon
     * events.
     */
    public static interface LogonListener extends EventListener {
        /**
         * invoked on successful authentication
         *
         * @param id              the id of the authenticated user
         * @param usedCertificate true if an X.509 certificate was used
         */
        void onAuthSuccess(String id, boolean usedCertificate);

        /**
         * invoked on authentication failure
         */
        void onAuthFailure();
    }
}