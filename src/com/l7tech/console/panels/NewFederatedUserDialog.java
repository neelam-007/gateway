package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.action.UserPropertiesAction;
import com.l7tech.console.action.FederatedUserPropertiesAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.IdentityProviderConfig;
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
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class NewFederatedUserDialog extends JDialog {

    private JPanel mainPanel;
    private JTextField userNameTextField;
    private JTextField x509SubjectDNTextField;
    private JTextField loginTextField;
    private JTextField emailTextField;
    private JRadioButton subjectDNRadioButton;
    private JRadioButton loginRadioButton;
    private JRadioButton emailRadioButton;
    private JCheckBox additionalPropertiesCheckBox;
    private JButton createButton;
    private JButton cancelButton;
    private boolean insertSuccess = false;
    private boolean createThenEdit = false;
    private String CMD_CANCEL = "cmd.cancel";
    private String CMD_OK = "cmd.ok";
    private boolean UserIdFieldFilled = false;
    private EventListenerList listenerList = new EventListenerList();
    private IdentityProviderConfig ipc;

    /* the user instance */
    private UserBean user = new UserBean();


    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewUserDialog", Locale.getDefault());


    private JFrame parent;

    public NewFederatedUserDialog(JFrame parent, IdentityProviderConfig ipc) {
        super(parent, true);
        this.parent = parent;
        this.ipc = ipc;
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

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

        ButtonGroup bg = new ButtonGroup();
        bg.add(subjectDNRadioButton);
        bg.add(loginRadioButton);
        bg.add(emailRadioButton);
        subjectDNRadioButton.setSelected(true);

        subjectDNRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                updateUserNameLinkedTextField();
            }
        });

        emailRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                updateUserNameLinkedTextField();
            }
        });

        loginRadioButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                updateUserNameLinkedTextField();
            }
        });

        createButton.setText(resources.getString("createButton.label"));
        createButton.setToolTipText(resources.getString("createButton.tooltip"));
        createButton.setActionCommand(CMD_OK);
        createButton.setEnabled(false);
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
        userNameTextField.setDocument(new FilterDocument(24,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));
        userNameTextField.getDocument().putProperty("name", "userId");
        userNameTextField.getDocument().addDocumentListener(documentListener);

        x509SubjectDNTextField.setToolTipText(resources.getString("x509SubjectDNTextField.tooltip"));
        x509SubjectDNTextField.setDocument(new FilterDocument(24,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));
        x509SubjectDNTextField.getDocument().putProperty("name", "x509DN");
        x509SubjectDNTextField.getDocument().addDocumentListener(documentListener);

        emailTextField.setToolTipText(resources.getString("emailTextField.tooltip"));
        emailTextField.setDocument(new FilterDocument(24,
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                return true;
                            }
                        }));
        emailTextField.getDocument().putProperty("name", "email");
        emailTextField.getDocument().addDocumentListener(documentListener);

        loginTextField.setToolTipText(resources.getString("loginTextField.tooltip"));
        loginTextField.setDocument(new FilterDocument(24,
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
                        ua.performAction();
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
            //todo: validate input if necessary

            insertUser();
        }
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

    private final DocumentListener documentListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            updateCreateButtonState(e);
        }

        public void insertUpdate(DocumentEvent e) {
            updateCreateButtonState(e);
        }

        public void removeUpdate(DocumentEvent e) {
            updateCreateButtonState(e);
        }
    };

    public void updateCreateButtonState(DocumentEvent e) {
        final Document field = e.getDocument();
        if (field.getProperty("name").equals("userId")) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateFromUserIdField(field);
                }
            });
        }

       if(field.getProperty("name").equals("x509DN")) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateFromX509SubjectDNField();
                }
            });
        }

        if(field.getProperty("name").equals("email")) {
             SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateFromEmailField();
                }
            });
        }

        if(field.getProperty("name").equals("login")) {
             SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateFromLoginField();
                }
            });
        }
    }


    private void updateFromUserIdField(Document doc) {

        if (doc.getLength() >= 3) {
            UserIdFieldFilled = true;
        } else {
            UserIdFieldFilled = false;
        }

        if (subjectDNRadioButton.isSelected()) {
            x509SubjectDNTextField.setText("CN=" + userNameTextField.getText().trim());
        }
        if (emailRadioButton.isSelected()) {
            emailTextField.setText(userNameTextField.getText() + "@");
        }
        if (loginRadioButton.isSelected()) {
            loginTextField.setText(userNameTextField.getText());
        }

        if (UserIdFieldFilled) {
            // enable the Create button
            createButton.setEnabled(true);
        } else {
            // disbale the button
            createButton.setEnabled(false);
        }
    }

    private void updateFromLoginField() {
        if (loginRadioButton.isSelected()) {
            if(!userNameTextField.getText().equals(loginTextField.getText())) {
                userNameTextField.setText(loginTextField.getText());
            }
        }
    }

    private void updateFromEmailField() {
        if (emailRadioButton.isSelected()) {
            String name = extractNameFromEmail(emailTextField.getText());
            if(!userNameTextField.getText().equals(name)) {
                userNameTextField.setText(name);
            }
        }
    }

    private void updateFromX509SubjectDNField() {
        if (subjectDNRadioButton.isSelected()) {
            String cn = extractCommonName(x509SubjectDNTextField.getText());
            if(!userNameTextField.getText().equals(cn)) {
                userNameTextField.setText(cn);
            }
        }
    }

    private void updateUserNameLinkedTextField() {
        if (userNameTextField.isCursorSet() && userNameTextField.getText().trim().length() > 0) {
            if (subjectDNRadioButton.isSelected()) {
                x509SubjectDNTextField.setText("CN=" + userNameTextField.getText().trim());
            }
            if (emailRadioButton.isSelected()) {
                emailTextField.setText(userNameTextField.getText() + "@");
            }
            if (loginRadioButton.isSelected()) {
                loginTextField.setText(userNameTextField.getText());
            }
        }
    }

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
            throw new IllegalArgumentException("Cert subject DN is not in the format CN=username");
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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _3;
        _3 = new JPanel();
        _3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(10, 0, 10, 10), -1, -1));
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _4;
        _4 = new JLabel();
        _4.setText("User Name:");
        _3.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _5;
        _5 = new JTextField();
        userNameTextField = _5;
        _3.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), new Dimension(200, -1)));
        final JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "  Credential Name Identitier  "));
        final JPanel _7;
        _7 = new JPanel();
        _7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _8;
        _8 = new JLabel();
        _8.setText("X509 Subject DN:");
        _7.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _9;
        _9 = new JLabel();
        _9.setText("Login:");
        _7.add(_9, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("Email:");
        _7.add(_10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _11;
        _11 = new JTextField();
        x509SubjectDNTextField = _11;
        _7.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _12;
        _12 = new JTextField();
        loginTextField = _12;
        _12.setText("");
        _7.add(_12, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _13;
        _13 = new JTextField();
        emailTextField = _13;
        _13.setText("");
        _7.add(_13, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JRadioButton _14;
        _14 = new JRadioButton();
        subjectDNRadioButton = _14;
        _14.setText("User Name as CN");
        _7.add(_14, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _15;
        _15 = new JRadioButton();
        loginRadioButton = _15;
        _15.setText("USer Name as Login");
        _7.add(_15, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _16;
        _16 = new JRadioButton();
        emailRadioButton = _16;
        _16.setText("User Name as First Part of Email");
        _7.add(_16, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 8, 0, 3, 0, null, null, null));
        final JPanel _17;
        _17 = new JPanel();
        _17.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_17, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JCheckBox _18;
        _18 = new JCheckBox();
        additionalPropertiesCheckBox = _18;
        _18.setText("Define Additional Properties");
        _17.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JPanel _19;
        _19 = new JPanel();
        _19.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_19, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _20;
        _20 = new JButton();
        createButton = _20;
        _20.setText("Create");
        _19.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _21;
        _21 = new JButton();
        cancelButton = _21;
        _21.setText("Cancel");
        _19.add(_21, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _22;
        _22 = new com.intellij.uiDesigner.core.Spacer();
        _19.add(_22, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
    }


}
