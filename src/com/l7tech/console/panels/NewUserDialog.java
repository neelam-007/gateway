package com.l7tech.console.panels;

import com.l7tech.console.text.FilterDocument;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.imp.UserImp;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * New User dialog.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class NewUserDialog extends JDialog {
    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private String CMD_CANCEL = "cmd.cancel";

    private String CMD_OK = "cmd.ok";

    private JButton createButton = null;

    /** ID text field */
    private JTextField idTextField = null;
    private JPasswordField passwordField = null;
    private JPasswordField passwordConfirmField = null;
    private JCheckBox additionalPropertiesCheckBox = null;
    private JFrame parent;

    private boolean insertSuccess = false;
    private boolean createThenEdit = false;

    /* the user instance */
    private User user = new UserImp();

    /* new user Password */
    private char[] newPassword;

    /* the panel listener */
    private PanelListener panelListener;

    private int MIN_PASSWORD_LENGTH = 6;

    /**
     * Create a new NewUserDialog fdialog for a given Company
     *
     * @param parent  the parent Frame. May be <B>null</B>
     */
    public NewUserDialog(JFrame parent) {
        super(parent, true);
        this.parent = parent;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * set the PanelListener
     *
     * @param listener the PanelListener
     */
    public void setPanelListener(PanelListener listener) {
        this.panelListener = listener;
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
        panel.add(getAdditionalProperties(), constraints);

        // button panel
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 5;
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
        constraints.gridy = 6;
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

        idTextField = new JTextField();
        idTextField.setPreferredSize(new Dimension(200, 20));
        idTextField.setMinimumSize(new Dimension(200, 20));
        idTextField.setToolTipText(resources.getString("idTextField.tooltip"));

        idTextField.
                setDocument(
                        new FilterDocument(24,
                                new FilterDocument.Filter() {
                                    public boolean accept(String str) {
                                        if (str == null) return false;
                                        return true;
                                    }
                                }));
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
        createButton.
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        windowAction(event);
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
                        windowAction(event);
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
    private void windowAction(Object actionCommand) {
        String cmd = null;

        if (actionCommand != null) {
            if (actionCommand instanceof ActionEvent) {
                cmd = ((ActionEvent)actionCommand).getActionCommand();
            } else {
                cmd = actionCommand.toString();
            }
        }
        if (cmd == null) {
            // do nothing
        } else if (cmd.equals(CMD_CANCEL)) {
            this.dispose();
        } else if (cmd.equals(CMD_OK)) {
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
            insertUser();
        }
    }


    /** insert user */
    private void insertUser() {
        user.setName(idTextField.getText());
        user.setPassword(new String(passwordField.getPassword()));
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            EntityHeader header = new EntityHeader();
                            header.setType(EntityType.USER);
                            header.setName(user.getName());
                            Registry.getDefault().getInternalUserManager().save(user);
                            panelListener.onInsert(header);
                            insertSuccess = true;
                        } catch (SaveException e) {
                            e.printStackTrace();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                        NewUserDialog.this.dispose();
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
                                EntityEditorPanel panel = PanelFactory.getPanel(EntityType.USER, panelListener);
                                if (panel == null) return;
                                EntityHeader header = new EntityHeader();
                                header.setType(EntityType.USER);
                                header.setName(user.getName());
                                header.setOid(user.getOid());
                                panel.edit(header);

                                EditorDialog dialog = new EditorDialog(parent, panel);
                                dialog.pack();
                                Utilities.centerOnScreen(dialog);
                                dialog.show();
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

}
