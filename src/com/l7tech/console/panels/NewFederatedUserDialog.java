package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.FederatedUserPropertiesAction;
import com.l7tech.console.action.UserPropertiesAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * This class provides a dialog for adding a new federated user.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class NewFederatedUserDialog extends JDialog {

    private static int USER_NAME_NOT_YET_UPDATED = -1;
    private static int USER_NAME_UPDATED_WITH_X509DN = 0;
    private static int USER_NAME_UPDATED_WITH_LOGIN = 1;
    private static int USER_NAME_UPDATED_WITH_EMAIL = 2;
    private JPanel mainPanel;
    private JLabel loginLabel;
    private JLabel emailLabel;
    private JTextField userNameTextField;
    private JTextField x509SubjectDNTextField;
    private JTextField loginTextField;
    private JTextField emailTextField;
    private JCheckBox additionalPropertiesCheckBox;
    private JButton createButton;
    private JButton cancelButton;
    private boolean insertSuccess = false;
    private boolean createThenEdit = false;
    private String CMD_CANCEL = "cmd.cancel";
    private String CMD_OK = "cmd.ok";
    private EventListenerList listenerList = new EventListenerList();
    private IdentityProviderConfig ipc;
    private int userNameTextFieldUpdated = USER_NAME_NOT_YET_UPDATED;
    
    /* the user instance */
    private UserBean user = new UserBean();
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewUserDialog", Locale.getDefault());

    /**
     * Constructor
     * @param parent  the owner of the component
     * @param ipc   the config of the identity provider the new user belongs to
     */
    public NewFederatedUserDialog(JFrame parent, IdentityProviderConfig ipc) {
        super(parent, true);
        this.ipc = ipc;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Initialize the components of the dialog
     */
    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        setTitle(resources.getString("federated.user.dialog.title"));

        Actions.setEscKeyStrokeDisposes(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        createButton.setText(resources.getString("createButton.label"));
        createButton.setToolTipText(resources.getString("createButton.tooltip"));
        createButton.setActionCommand(CMD_OK);
        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });

        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });

        userNameTextField.setToolTipText(resources.getString("idTextField.tooltip"));
        userNameTextField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if(userNameTextField.getText().length() > 0) {
                            x509SubjectDNTextField.getDocument().removeDocumentListener(documentListener);
                            loginTextField.getDocument().removeDocumentListener(documentListener);
                            emailTextField.getDocument().removeDocumentListener(documentListener);
                        }
                    }
                });

            }
        });

        x509SubjectDNTextField.setToolTipText(resources.getString("x509SubjectDNTextField.tooltip"));
        x509SubjectDNTextField.setDocument(new FilterDocument(255,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));
        x509SubjectDNTextField.getDocument().putProperty("name", "x509DN");
        x509SubjectDNTextField.getDocument().addDocumentListener(documentListener);

        emailTextField.setToolTipText(resources.getString("emailTextField.tooltip"));
        emailTextField.setDocument(new FilterDocument(128,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));
        emailTextField.getDocument().putProperty("name", "email");
        emailTextField.getDocument().addDocumentListener(documentListener);

        loginTextField.setToolTipText(resources.getString("loginTextField.tooltip"));
        loginTextField.setDocument(new FilterDocument(32,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));
        loginTextField.getDocument().putProperty("name", "login");
        loginTextField.getDocument().addDocumentListener(documentListener);

        additionalPropertiesCheckBox.addItemListener(new ItemListener() {
            /**
             * Invoked when an item has been selected or deselected.
             * The code written for this method performs the operations
             * that need to occur when an item is selected (or deselected).
             */
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    createThenEdit = true;
                }
            }
        });

        // Bugzilla #1090 - disable the fields that cannot be tested in rel 3.0        
        //emailTextField.setEnabled(false);
        //loginTextField.setEnabled(false);
        //loginLabel.setEnabled(false);
        //emailLabel.setEnabled(false);
    }

    /**
     * override the Dialogue method so tha we can open and editor
     * panel if requested
     */
    public void dispose() {
        super.dispose();

        // maybe cancel was pressed, this ensures that
        // an object has been inserted
        if (insertSuccess) {
            if (createThenEdit) {
                // yes the additional properties are to be defined
                createThenEdit = false;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        EntityHeader header = new EntityHeader();
                        header.setType(EntityType.USER);
                        header.setName(user.getName());
                        header.setStrId(user.getUniqueIdentifier());
                        UserPropertiesAction ua =
                                new FederatedUserPropertiesAction((UserNode) TreeNodeFactory.asTreeNode(header));
                        try {
                            ua.setIdProviderConfig(ipc);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        ua.invoke();
                        insertSuccess = false;
                    }
                });
            }
        }
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

        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
            this.dispose();
        } else if (actionCommand.equals(CMD_OK)) {

            if(validateInput()) {
                insertUser();
            }
        }
    }

    /**
     * validate the user inputs
     *
     * @return true if the input data are valid. false otherwise.
     */
    private boolean validateInput() {

        if(userNameTextField.getText().length() < 3) {
            JOptionPane.showMessageDialog(this, resources.getString("idTextField.error.empty"),
                            resources.getString("idTextField.error.title"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * insert user
     */
    private void insertUser() {
        user.setName(userNameTextField.getText());
        user.setLogin(loginTextField.getText());
        user.setSubjectDn(x509SubjectDNTextField.getText());
        user.setEmail(emailTextField.getText());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    EntityHeader header = new EntityHeader();
                    header.setType(EntityType.USER);
                    header.setName(user.getName());

                    final String userId =
                                    Registry.getDefault().getIdentityAdmin().saveUser(
                                            ipc.getOid(),
                                            user, null);
                            header.setStrId( userId);
                            user.setUniqueIdentifier(header.getStrId());

                    fireEventUserAdded(header);
                    insertSuccess = true;
                } catch (Exception e) {
                    ErrorManager.getDefault().
                            notify(Level.WARNING, e, "Error encountered while adding a user\n" +
                            "The user has not been created.");
                }
                NewFederatedUserDialog.this.dispose();
            }
        });


    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    private void fireEventUserAdded(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener) listeners[i]).entityAdded(event);
        }
    }

    /**
     * Update the user name text field.
     *
     */
    private void updateUserNameField(DocumentEvent e) {
        final Document field = e.getDocument();

       if(field.getProperty("name").equals("x509DN")) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    if(!updateFromX509SubjectDNField()) {
                        if(!updateFromLoginField()) {
                            updateFromEmailField();
                        }
                    }
                }
            });
        }

        if(field.getProperty("name").equals("login")) {
             SwingUtilities.invokeLater(new Runnable() {
                 public void run() {

                     if(userNameTextFieldUpdated != USER_NAME_UPDATED_WITH_X509DN && !updateFromLoginField()) {
                         updateFromEmailField();
                     }
                 }
            });
        }

        if(field.getProperty("name").equals("email")) {
             SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if(userNameTextFieldUpdated != USER_NAME_UPDATED_WITH_X509DN &&
                       userNameTextFieldUpdated != USER_NAME_UPDATED_WITH_LOGIN) {
                        updateFromEmailField();
                    }
                }
            });
        }
    }

    /**
     * Update the user name text field with the CN part of the Subject DN.
     * @return true if the update succeeded, false otherewise.
     */
    private boolean updateFromX509SubjectDNField() {
        String cn = extractCommonName(x509SubjectDNTextField.getText());

        if(cn.length() > 0) {
            if(!userNameTextField.getText().equals(cn)) {
                userNameTextField.setText(cn);
            }
            userNameTextFieldUpdated = USER_NAME_UPDATED_WITH_X509DN;
            return true;
        } else {
            userNameTextField.setText("");
            return false;
        }
    }

   /**
     * Update the user name text field with the login name.
     * @return true if the update succeeded, false otherewise.
     */
    private boolean updateFromLoginField() {
        if(loginTextField.getText().length() > 0) {
            if(!userNameTextField.getText().equals(loginTextField.getText())) {
                userNameTextField.setText(loginTextField.getText());
            }
            userNameTextFieldUpdated = USER_NAME_UPDATED_WITH_LOGIN;
            return true;
        } else {
            userNameTextField.setText("");
            return false;
        }
    }

    /**
     * Update the user name text field with the one in the email.
     * @return true if the update succeeded, false otherewise.
     */
    private boolean updateFromEmailField() {
        String name = extractNameFromEmail(emailTextField.getText());

        if(name.length() > 0) {
            if(!userNameTextField.getText().equals(name)) {
                userNameTextField.setText(name);
            }
            userNameTextFieldUpdated = USER_NAME_UPDATED_WITH_EMAIL;
            return true;
        } else {
            userNameTextField.setText("");
            return false;
        }
    }

    /**
     * Extract the user name from the eamil.
     * @param email  the given email
     * @return  String the username in the email
     */
    private String extractNameFromEmail(String email) {
        if (email == null)
            throw new IllegalArgumentException("Email is NULL");

        int index = -1;
        if ((index = email.indexOf('@')) > 0) {
            return email.substring(0, index);
        } else {
            return email;
        }

    }

    /**
     *
     * Extract the CN part of the DN.
     * @param dn  the given DN
     * @return String the CN part of the DN
     */
    private String extractCommonName(String dn) {

        if (dn == null)
            throw new IllegalArgumentException("DN is NULL");

        String cn = "";
        int index1 = dn.indexOf("cn=");
        int index2 = dn.indexOf("CN=");
        int startIndex = -1;
        int endIndex = -1;

        if (index1 >= 0) {
            startIndex = index1 + 3;
        } else if (index2 >= 0) {
            startIndex = index2 + 3;
        } else {
            return "";
        }

        if (startIndex >= 0) {
            endIndex = dn.indexOf(",", startIndex);
            if (endIndex > 0) {
                cn = dn.substring(startIndex, endIndex);
            } else {
                cn = dn.substring(startIndex, dn.length());
            }
        }

        return cn;
    }

    private final DocumentListener documentListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            updateUserNameField(e);
        }

        public void insertUpdate(DocumentEvent e) {
            updateUserNameField(e);
        }

        public void removeUpdate(DocumentEvent e) {
            updateUserNameField(e);
        }
    };

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Credential Name Identifier  "));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("X509 Subject DN:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        loginLabel = new JLabel();
        loginLabel.setText("Login:");
        panel3.add(loginLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        emailLabel = new JLabel();
        emailLabel.setText("Email:");
        panel3.add(emailLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        x509SubjectDNTextField = new JTextField();
        panel3.add(x509SubjectDNTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null));
        loginTextField = new JTextField();
        loginTextField.setText("");
        panel3.add(loginTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null));
        emailTextField = new JTextField();
        emailTextField.setText("");
        panel3.add(emailTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(10, 10, 10, 10), -1, -1));
        panel1.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        userNameTextField = new JTextField();
        panel4.add(userNameTextField, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("User Name:");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        additionalPropertiesCheckBox = new JCheckBox();
        additionalPropertiesCheckBox.setText("Define Additional Properties");
        panel5.add(additionalPropertiesCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        createButton = new JButton();
        createButton.setText("Create");
        panel6.add(createButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel6.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
    }
}