package com.l7tech.console.panels;

import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.console.action.GenericUserPropertiesAction;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.UserNode;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;

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

    private JButton createButton = null;

    /** ID text field */
    private SquigglyTextField idTextField = null;
    private JPasswordField passwordField = null;
    private JPasswordField passwordConfirmField = null;
    private JCheckBox additionalPropertiesCheckBox = null;

    private boolean insertSuccess = false;
    private boolean createThenEdit = false;

    /* the user instance */
    private final UserBean user;

    private int MIN_PASSWORD_LENGTH = SyspropUtil.getInteger("com.l7tech.ui.minPasswordLength", 6);
    private EventListenerList listenerList = new EventListenerList();
    private boolean UserIdFieldFilled = false;
    private boolean passwordFieldFilled = false;
    private boolean passwordConfirmFieldFilled = false;

    private JCheckBox forceChangePassword;

    /**
     * Create a new NewInternalUserDialog fdialog for a given Company
     *
     * @param parent  the parent Frame. May be <B>null</B>
     */
    public NewInternalUserDialog(Frame parent) {
        super(parent, true);
        this.user = new UserBean();
        this.user.setProviderId(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
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
        GridBagConstraints constraints = null;
        Container contents = getContentPane();
        JPanel panel = new JPanel();
        panel.setDoubleBuffered(true);
        contents.add(panel);
        panel.setLayout(new GridBagLayout());
        setTitle(resources.getString("dialog.title"));

        Utilities.setEscKeyStrokeDisposes(this);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });


        // user name label
        JLabel userIdLabel = new JLabel();
        userIdLabel.setToolTipText(resources.getString("idTextField.tooltip"));
        userIdLabel.setText(resources.getString("idTextField.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(userIdLabel, constraints);

        // user name text field
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getUserIdTextField(), constraints);

        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setToolTipText(resources.getString("passwordField.tooltip"));
        passwordLabel.setText(resources.getString("passwordField.label"));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(passwordLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getPasswordField(), constraints);

        // password confirm label
        JLabel passwordConfirmLabel = new JLabel();
        passwordConfirmLabel.setToolTipText(resources.getString("passwordConfirmField.tooltip"));
        passwordConfirmLabel.setText(resources.getString("passwordConfirmField.label"));

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 12, 0, 0);
        panel.add(passwordConfirmLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 0.0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(12, 7, 0, 11);
        panel.add(getConfirmPasswordField(), constraints);

        // additional properties
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 12, 0, 0);
        final JCheckBox additionalProperties = getAdditionalProperties();
        additionalProperties.setBorder(null);
        panel.add(additionalProperties, constraints);

        constraints = new GridBagConstraints();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.0;
        constraints.insets = new Insets(12, 9, 0, 0);
        forceChangePassword = new JCheckBox(resources.getString("forceChangePassword.label"));
        forceChangePassword.setSelected(true);
        forceChangePassword.setToolTipText(resources.getString("forceChangePassword.tooltip"));
        forceChangePassword.setHorizontalTextPosition(SwingConstants.LEADING);
        panel.add(forceChangePassword, constraints);


        // button panel
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(12, 0, 12, 21);
        JPanel buttonPanel = createButtonPanel();
        panel.add(buttonPanel, constraints);

        // bottom filler
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        Component filler = Box.createHorizontalStrut(8);
        panel.add(filler, constraints);

        // side filler
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 7;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(0, 0, 0, 0);
        Component filler2 = Box.createHorizontalStrut(8);
        panel.add(filler2, constraints);


        getRootPane().setDefaultButton(createButton);

    } // initComponents()

    /**
     * A method that returns a JTextField containing userId information
     *
     * @return the user ID textfield
     */
    private JTextField getUserIdTextField() {
        if (idTextField != null) return idTextField;

        idTextField = new SquigglyTextField();
        idTextField.setColor(Color.ORANGE);
        idTextField.setPreferredSize(new Dimension(200, 20));
        idTextField.setMinimumSize(new Dimension(200, 20));
        idTextField.setToolTipText(resources.getString("idTextField.tooltip"));

        idTextField.
                setDocument(
                        new FilterDocument(128,
                                new FilterDocument.Filter() {
                                    public boolean accept(String str) {
                                        if (str == null) return false;
                                        return true;
                                    }
                                }));
        idTextField.getDocument().putProperty("name", "identityId");
        idTextField.getDocument().addDocumentListener(documentListener);

        return idTextField;
    }

    /**
     * A method that returns the 1st JPasswordField
     *
     * @return the 1st JPasswordField
     */
    private JPasswordField getPasswordField() {
        // password field
        if (passwordField != null) return passwordField;
        passwordField = new JPasswordField();

        passwordField.setPreferredSize(new Dimension(200, 20));
        passwordField.setMinimumSize(new Dimension(200, 20));
        passwordField.setToolTipText(resources.getString("passwordField.tooltip"));
        Font echoCharFont = new Font("Lucida Sans", Font.PLAIN, 12);
        passwordField.setFont(echoCharFont);
        passwordField.setEchoChar('\u2022');

        passwordField.
                setDocument(
                        new FilterDocument(32,
                                new FilterDocument.Filter() {
                                    public boolean accept(String str) {
                                        if (str == null) return false;
                                        // password shares the same char set rules as id
                                        return true;
                                    }
                                }));

        passwordField.getDocument().putProperty("name", "password");
        passwordField.getDocument().addDocumentListener(documentListener);

        return passwordField;
    }

    /**
     * A method that returns the password confirm field
     *
     * @return the password confirm JPasswordField
     */
    private JPasswordField getConfirmPasswordField() {
        // password confirm field
        if (passwordConfirmField != null)
            return passwordConfirmField;
        passwordConfirmField = new JPasswordField();

        passwordConfirmField.setPreferredSize(new Dimension(200, 20));
        passwordConfirmField.setMinimumSize(new Dimension(200, 20));
        passwordConfirmField.setToolTipText(resources.getString("passwordConfirmField.tooltip"));
        Font echoCharFont = new Font("Lucida Sans", Font.PLAIN, 12);
        passwordConfirmField.setFont(echoCharFont);
        passwordConfirmField.setEchoChar('\u2022');

        passwordConfirmField.
                setDocument(
                        new FilterDocument(32,
                                new FilterDocument.Filter() {
                                    public boolean accept(String str) {
                                        if (str == null) return false;
                                        // password shares the same char set rules as id
                                        return true;
                                    }
                                }));
        passwordConfirmField.getDocument().putProperty("name", "passwordConfirm");
        passwordConfirmField.getDocument().addDocumentListener(documentListener);

        return passwordConfirmField;
    }

    /**
     * A method that returns a JCheckBox that indicates
     * wether the user wishes to define additional properties
     * of the entity
     *
     * @return the CheckBox component
     */
    private JCheckBox getAdditionalProperties() {
        if (additionalPropertiesCheckBox == null) {
            additionalPropertiesCheckBox = new JCheckBox(resources.getString("additionalProperties.label"));
            additionalPropertiesCheckBox.setHorizontalTextPosition(SwingConstants.LEADING);

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
        }
        return additionalPropertiesCheckBox;
    }

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable okButton
     */
    private JPanel createButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // OK button (global variable)
        createButton = new JButton();
        createButton.setText(resources.getString("createButton.label"));
        createButton.setToolTipText(resources.getString("createButton.tooltip"));
        createButton.setActionCommand(CMD_OK);
        createButton.setEnabled(false);
        createButton.
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        windowAction(event.getActionCommand());
                    }
                });
        panel.add(createButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        windowAction(event.getActionCommand());
                    }
                });
        panel.add(cancelButton);

        // equalize buttons
        Utilities.equalizeButtonSizes(new JButton[]{createButton, cancelButton});

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


        if (actionCommand == null) {
            // do nothing
        } else if (actionCommand.equals(CMD_CANCEL)) {
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
            insertUser();
        }
    }


    /** insert user */
    private void insertUser() {
        final String name = idTextField.getText().trim();
        user.setName(name);
        user.setLogin(name);
        user.setCleartextPassword(new String(passwordField.getPassword()));
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            EntityHeader header = new EntityHeader();
                            header.setType(EntityType.USER);
                            header.setName(user.getName());
                            user.setChangePassword(forceChangePassword.isSelected());
                            final String userId =
                                    Registry.getDefault().getIdentityAdmin().saveUser(
                                            IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID,
                                            user, null);
                            header.setStrId( userId);
                            user.setUniqueIdentifier(header.getStrId());
                            fireEventUserAdded(header);
                            insertSuccess = true;
                        } catch (DuplicateObjectException doe) {
                            DialogDisplayer.showMessageDialog(NewInternalUserDialog.this, null, ExceptionUtils.getMessage(doe), null);
                        } catch (ObjectModelException e) {
                            ErrorManager.getDefault().
                              notify(Level.WARNING, e, "Error encountered while adding a user\n"+
                                     "The user has not been created.");
                        }
                        NewInternalUserDialog.this.dispose();
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
        if (password.length < MIN_PASSWORD_LENGTH) {
            JOptionPane.
                    showMessageDialog(this,
                            resources.getString("passwordField.error.empty"),
                            resources.getString("passwordField.error.title"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
