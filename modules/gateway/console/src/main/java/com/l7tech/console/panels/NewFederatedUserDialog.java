package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.FilterDocument;
import com.l7tech.identity.IdentityProviderLimits;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.action.FederatedUserPropertiesAction;
import com.l7tech.console.action.UserPropertiesAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.common.io.CertUtils;

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
 * <p> @author fpang </p>
 * $Id$
 */
public class NewFederatedUserDialog extends JDialog {

    private static int USER_NAME_NOT_YET_UPDATED = -1;
    private static int USER_NAME_UPDATED_WITH_X509DN = 0;
    private static int USER_NAME_UPDATED_WITH_LOGIN = 1;
    private static int USER_NAME_UPDATED_WITH_EMAIL = 2;
    private JPanel mainPanel;
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
    private final UserBean user;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewUserDialog", Locale.getDefault());

    /**
     * Constructor
     * @param parent  the owner of the component
     * @param ipc   the config of the identity provider the new user belongs to
     */
    public NewFederatedUserDialog(Frame parent, IdentityProviderConfig ipc) {
        super(parent, true);
        this.ipc = ipc;
        this.user = new UserBean();
        this.user.setProviderId(ipc.getGoid());
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

        Utilities.setEscKeyStrokeDisposes(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        createButton.setText(resources.getString("createButton.label"));
        createButton.setToolTipText(resources.getString("createButton.tooltip"));
        createButton.setActionCommand(CMD_OK);
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });

        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                windowAction(event.getActionCommand());
            }
        });

        userNameTextField.setToolTipText(resources.getString("idTextField.tooltip"));
        userNameTextField.setDocument(new FilterDocument(IdentityProviderLimits.MAX_ID_LENGTH.getValue(),
                        new FilterDocument.Filter() {
                            @Override
                            public boolean accept(String str) {
                                return str != null;
                            }
                        }));
        userNameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
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
        x509SubjectDNTextField.setDocument(new FilterDocument(IdentityProviderLimits.MAX_X509_SUBJECT_DN_LENGTH.getValue(),
                        new FilterDocument.Filter() {
                            @Override
                            public boolean accept(String str) {
                                return str != null;
                            }
                        }));
        x509SubjectDNTextField.getDocument().putProperty("name", "x509DN");
        x509SubjectDNTextField.getDocument().addDocumentListener(documentListener);

        emailTextField.setToolTipText(resources.getString("emailTextField.tooltip"));
        emailTextField.setDocument(new FilterDocument(IdentityProviderLimits.MAX_EMAIL_LENGTH.getValue(),
                        new FilterDocument.Filter() {
                            @Override
                            public boolean accept(String str) {
                                return str != null;
                            }
                        }));
        emailTextField.getDocument().putProperty("name", "email");
        emailTextField.getDocument().addDocumentListener(documentListener);

        loginTextField.setToolTipText(resources.getString("loginTextField.tooltip"));
        loginTextField.setDocument( new MaxLengthDocument(255));
        loginTextField.getDocument().putProperty("name", "login");
        loginTextField.getDocument().addDocumentListener(documentListener);

        additionalPropertiesCheckBox.addItemListener(new ItemListener() {
            /**
             * Invoked when an item has been selected or deselected.
             * The code written for this method performs the operations
             * that need to occur when an item is selected (or deselected).
             */
            @Override
            public void itemStateChanged(ItemEvent e) {
                createThenEdit = e.getStateChange() == ItemEvent.SELECTED;
            }
        });
    }

    /**
     * override the Dialogue method so tha we can open and editor
     * panel if requested
     */
    @Override
    public void dispose() {
        super.dispose();

        // maybe cancel was pressed, this ensures that
        // an object has been inserted
        if (insertSuccess) {
            if (createThenEdit) {
                // yes the additional properties are to be defined
                createThenEdit = false;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EntityHeader header = new EntityHeader();
                        header.setType(EntityType.USER);
                        header.setName(user.getName());
                        header.setStrId(user.getId());
                        UserPropertiesAction ua =
                                new FederatedUserPropertiesAction((UserNode) TreeNodeFactory.asTreeNode(header, null));
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

        String dn = x509SubjectDNTextField.getText();
        if ( dn != null && dn.trim().length() > 0 && !CertUtils.isValidDN(dn)) {
            String message = CertUtils.getDNValidationMessage(dn);
            if ( message != null ) {
                message = "\n" + message;

                JOptionPane.showMessageDialog(this,
                        resources.getString("x509SubjectDNTextField.warning.invalid") + message,
                        resources.getString("x509SubjectDNTextField.warning.title"),
                        JOptionPane.ERROR_MESSAGE);
                return false;

            }
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
            @Override
            public void run() {
                boolean duplicate = false;
                try {
                    EntityHeader header = new EntityHeader();
                    header.setType(EntityType.USER);
                    header.setName(user.getName());

                    final String userId =
                                    Registry.getDefault().getIdentityAdmin().saveUser(
                                            ipc.getGoid(),
                                            user, null);
                            header.setStrId( userId);
                            user.setUniqueIdentifier(header.getStrId());

                    fireEventUserAdded(header);
                    insertSuccess = true;
                } catch (DuplicateObjectException doe) {
                    duplicate = true;
                    DialogDisplayer.showMessageDialog(NewFederatedUserDialog.this, null, ExceptionUtils.getMessage(doe), null);
                } catch (ObjectModelException e) {
                    ErrorManager.getDefault().
                            notify(Level.WARNING, e, "Error encountered while adding a user\n" +
                            "The user has not been created.");
                } finally {
                    if (!duplicate) NewFederatedUserDialog.this.dispose();
                }
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
        for (EventListener listener : listeners) {
            ((EntityListener) listener).entityAdded(event);
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
                @Override
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
                 @Override
                 public void run() {

                     if(userNameTextFieldUpdated != USER_NAME_UPDATED_WITH_X509DN && !updateFromLoginField()) {
                         updateFromEmailField();
                     }
                 }
            });
        }

        if(field.getProperty("name").equals("email")) {
             SwingUtilities.invokeLater(new Runnable() {
                @Override
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

        int index;
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
        int startIndex;
        int endIndex;

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
        @Override
        public void changedUpdate(DocumentEvent e) {
            updateUserNameField(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateUserNameField(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateUserNameField(e);
        }
    };

}
