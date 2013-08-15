package com.l7tech.console.panels;

import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.identity.IdentityProviderLimits;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.action.GenericUserPropertiesAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;

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
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * New User dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class NewInternalUserDialog extends JDialog {
    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private String CMD_CANCEL = "cmd.cancel";

    private String CMD_OK = "cmd.ok";
    private String CMD_HELP = "cmd.help";

    private JButton createButton;
    private JButton passwordRulesButton ;

    /** ID text field */
    private SquigglyTextField idTextField;
    private JPasswordField passwordField;
    private JPasswordField passwordConfirmField;
    private JCheckBox additionalPropertiesCheckBox;

    private boolean insertSuccess = false;
    private boolean createThenEdit = false;


    private EventListenerList listenerList = new EventListenerList();
    private boolean UserIdFieldFilled = false;
    private boolean passwordFieldFilled = false;
    private boolean passwordConfirmFieldFilled = false;
    private JButton cancelButton;
    private JPanel contentPanel;

    /* the user instance */
    private final UserBean user;

    /**
     * Create a new NewInternalUserDialog fdialog for a given Company
     *
     * @param parent  the parent Frame. May be <B>null</B>
     */
    public NewInternalUserDialog(Frame parent) {
        super(parent, true);
        this.user = new UserBean();
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
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

    /**
     * notfy the listeners
     * @param header
     */
    private void fireEventUserAdded(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener)listeners[i]).entityAdded(event);
        }
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.NewUserDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        setContentPane(contentPanel);
        setTitle(resources.getString("dialog.title"));
        getRootPane().setDefaultButton(createButton);
                            
        Utilities.setEscKeyStrokeDisposes(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        idTextField.setColor(Color.ORANGE);
        idTextField.setToolTipText(resources.getString("idTextField.tooltip"));

        idTextField.
                setDocument(
                        new FilterDocument(IdentityProviderLimits.MAX_ID_LENGTH.getValue(),
                                new FilterDocument.Filter() {
                                    public boolean accept(String str) {
                                        if (str == null) return false;
                                        return true;
                                    }
                                }));
        idTextField.getDocument().putProperty("name", "identityId");
        idTextField.getDocument().addDocumentListener(documentListener);

        passwordField.setToolTipText(resources.getString("passwordField.tooltip"));
        Font echoCharFont = new Font("Lucida Sans", Font.PLAIN, 12);
        passwordField.setFont(echoCharFont);
        passwordField.setEchoChar('\u2022');

        passwordField.setDocument(
                new FilterDocument(IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                // password shares the same char set rules as id
                                return true;
                            }
                        }));

        passwordField.getDocument().putProperty("name", "password");
        passwordField.getDocument().addDocumentListener(documentListener);

        passwordConfirmField.setToolTipText(resources.getString("passwordConfirmField.tooltip"));
        passwordConfirmField.setFont(echoCharFont);
        passwordConfirmField.setEchoChar('\u2022');

        passwordConfirmField.setDocument(
                new FilterDocument(IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),
                        new FilterDocument.Filter() {
                            public boolean accept(String str) {
                                if (str == null) return false;
                                // password shares the same char set rules as id
                                return true;
                            }
                        }));
        passwordConfirmField.getDocument().putProperty("name", "passwordConfirm");
        passwordConfirmField.getDocument().addDocumentListener(documentListener);

        additionalPropertiesCheckBox.addItemListener(new ItemListener() {
            /**
             * Invoked when an item has been selected or deselected.
             * The code written for this method performs the operations
             * that need to occur when an item is selected (or deselected).
             */
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    createThenEdit = true;
                } else {
                    createThenEdit = false;
                }
            }
        });

        passwordRulesButton.setActionCommand(CMD_HELP);
        passwordRulesButton.
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    windowAction(event.getActionCommand());
                }
            });
        createButton.setActionCommand(CMD_OK);
        createButton.
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    windowAction(event.getActionCommand());
                }
            });
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    windowAction(event.getActionCommand());
                }
            });

    } // initComponents()


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


        if (actionCommand == null) {
            // do nothing
        } else if(actionCommand.equals(CMD_HELP)){
            final PasswordHelpDialog dialog = new PasswordHelpDialog(NewInternalUserDialog.this,null);
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog, new Runnable() {
                    public void run() {
                        if (!dialog.isOk()) {
                            dispose();
                        }
                    }
                });
        }else if (actionCommand.equals(CMD_CANCEL)) {
            this.dispose();
        } else if (actionCommand.equals(CMD_OK)) {
            if (!validateInput()) {
                idTextField.requestFocus();
                passwordField.setText(null);
                passwordConfirmField.setText(null);
                return;
            }
            if (!validatePassword(passwordField.getPassword(), passwordConfirmField.getPassword())) {
                passwordField.requestFocus();
                passwordField.setText(null);
                passwordConfirmField.setText(null);
                return;
            }
            if (!validateIdTextField()) {
                int result = JOptionPane.showConfirmDialog(this,
                                              resources.getString("idTextField.warning.dialog.badCertChars"),
                                              resources.getString("idTextField.warning.title"),
                                              JOptionPane.OK_CANCEL_OPTION);
                if (result != JOptionPane.OK_OPTION)
                    return;
            }
            boolean success = insertUser();
            if(!success)
            {
                passwordField.requestFocus();
                passwordField.setText(null);
                passwordConfirmField.setText(null);
            }
        }
    }


    /** insert user */
    private boolean insertUser() {
        final String name = idTextField.getText().trim();
        final String password = new String(passwordField.getPassword());

        user.setProviderId(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        user.setName( name );
        user.setLogin( name );

        try {
            EntityHeader header = new EntityHeader();
            header.setType(EntityType.USER);
            header.setName(name);
            final String userId =
                Registry.getDefault().getIdentityAdmin().saveUser(
                    IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID,
                    user, null, password);
            header.setStrId(userId);
            user.setUniqueIdentifier(header.getStrId());
            fireEventUserAdded(header);
            insertSuccess = true;
        } catch (DuplicateObjectException doe) {
            DialogDisplayer.showMessageDialog(NewInternalUserDialog.this, null, ExceptionUtils.getMessage(doe), null);
            return false;
        } catch (InvalidPasswordException e) {
            DialogDisplayer.showMessageDialog(NewInternalUserDialog.this, null, ExceptionUtils.getMessage(e), null);
            return false;
        } catch (ObjectModelException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error encountered while adding a user\nThe user has not been created.");
            return false;
        }
        NewInternalUserDialog.this.dispose();
        return true;
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
                final String name = idTextField.getText().trim();

                SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                EntityHeader header = new EntityHeader();
                                header.setType(EntityType.USER);
                                header.setName(user.getName());
                                header.setStrId(user.getId());
                                GenericUserPropertiesAction ua =
                                  new GenericUserPropertiesAction((UserNode)TreeNodeFactory.asTreeNode(header, null));
                                // only internal provider currently
                                ua.setIdProviderConfig(Registry.getDefault().getInternalProviderConfig());
                                ua.invoke();
                                insertSuccess = false;
                            }
                        });
            }
        }
    }

    /**
     * validate the username and context
     *
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        return true;
    }

    /**
     * validate the passwords
     *
     * @param password user password
     * @param confirmPassword the password to compare for confirmation
     * @return true validated, false othwerwise
     */
    private boolean validatePassword(char[] password, char[] confirmPassword) {
        if (!((new String(password)).equals(new String(confirmPassword)))) {
            JOptionPane.
                    showMessageDialog(this,
                            resources.getString("passwordConfirmField.error"),
                            resources.getString("passwordConfirmField.error.title"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public void updateCreateButtonState(DocumentEvent e) {
        Document field = e.getDocument();
        if (field.getProperty("name").equals("identityId")) {
            if (field.getLength() > 0) {
                if (validateIdTextField()) {
                    UserIdFieldFilled = true;
                } else {
                    UserIdFieldFilled = false;
                }
            } else {
                UserIdFieldFilled = false;
                idTextField.setModelessFeedback(null);
            }
        } else if (field.getProperty("name").equals("password")) {
            if (field.getLength() > 0) {
                passwordFieldFilled = true;
            } else {
                passwordFieldFilled = false;
            }
        } else if (field.getProperty("name").equals("passwordConfirm")) {
            if (field.getLength() > 0) {
                passwordConfirmFieldFilled = true;
            } else {
                passwordConfirmFieldFilled = false;
            }

        } else {
            // do nothing
        }

        if (UserIdFieldFilled && passwordFieldFilled && passwordConfirmFieldFilled) {
            // enable the Create button
            createButton.setEnabled(true);
        } else {
            // disbale the button
            createButton.setEnabled(false);
        }
    }

    static final Pattern CERT_NAME_CHECKER = Pattern.compile("[#,+\"\\\\<>;]");

    private boolean validateIdTextField() {
        String t = idTextField.getText();
        if (CERT_NAME_CHECKER.matcher(t).find()) {
            idTextField.setModelessFeedback(resources.getString("idTextField.warning.tooltip.badCertChars"));
            return false;
        } else if (t.trim().length() == 0) {
            idTextField.setModelessFeedback(resources.getString("idTextField.error.badchar"));
            return false;
        } else {
            idTextField.setModelessFeedback(null);
            return true;
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

}
