package com.l7tech.console.panels;


import com.l7tech.console.logging.ErrorManager;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ResourceBundle;
import java.util.Locale;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Oct 21, 2003
 * Time: 10:30:12 AM
 * To change this template use Options | File Templates.
 */
public class EditPolicyTemplateNameDialog extends JDialog {
    static final Logger log = Logger.getLogger(EditPolicyTemplateNameDialog.class.getName());

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    /**
     * Command string for a cancel action (e.g.,a button or menu item).
     * This string is never presented to the user and should
     * not be internationalized.
     */
    private String CMD_CANCEL = "cmd.cancel";

    /**
     * Command string for a login action (e.g.,a button or menu item).
     * This string is never presented to the user and should
     * not be internationalized.
     */
    private String CMD_OK = "cmd.ok";

    private JButton okButton = null;

    /* new template name text field */
    private JTextField newNameField = null;

    private ActionListener listener;

    /**
     * Create a new PasswordDialog
     *
     * @param parent the parent Frame. May be <B>null</B>
     */
    public EditPolicyTemplateNameDialog(Frame parent, ActionListener l) {
        super(parent, true);
        this.listener = l;
        initResources();
        initComponents();
        pack();
        Utilities.centerOnScreen(this);
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.EditPolicyTemplateNameDialog", locale);
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {

        GridBagConstraints constraints = null;

        Container contents = getContentPane();
        contents.setLayout(new GridBagLayout());
        setTitle(resources.getString("dialog.title"));

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                // user hit window manager close button
                windowAction(CMD_CANCEL);
            }
        });

        newNameField = new JTextField(); // needed below

        // password label
        JLabel passwordLabel = new JLabel();
        passwordLabel.setToolTipText(resources.getString("newPolicyTemplateNameField.tooltip"));
        passwordLabel.setText(resources.getString("newPolicyTemplateNameField.label"));
        passwordLabel.setLabelFor(newNameField);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(11, 12, 0, 0);

        contents.add(passwordLabel, constraints);

        // new name field
        newNameField.setToolTipText(resources.getString("newPolicyTemplateNameField.tooltip"));


        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(11, 7, 0, 11);
        contents.add(newNameField, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(17, 12, 11, 11);
        JPanel buttonPanel = createButtonPanel(); // sets global okButton
        contents.add(buttonPanel, constraints);

        getRootPane().setDefaultButton(okButton);
    } // initComponents()

    /**
     * Creates the panel of buttons that goes along the bottom
     * of the dialog
     *
     * Sets the variable okButton
     *
     */
    private JPanel createButtonPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 0));

        // login button (global variable)
        okButton = new JButton();
        okButton.setText(resources.getString("okButton.label"));
        okButton.setToolTipText(resources.getString("okButton.tooltip"));
        okButton.setActionCommand(CMD_OK);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });
        panel.add(okButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        // cancel button
        JButton cancelButton = new JButton();
        cancelButton.setText(resources.getString("cancelButton.label"));
        cancelButton.setActionCommand(CMD_CANCEL);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                windowAction(event);
            }
        });
        panel.add(cancelButton);

        // space
        panel.add(Box.createRigidArea(new Dimension(5, 0)));
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});
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
            dispose();
        } else if (cmd.equals(CMD_OK)) {
            if (validateInput()) {
                changePolicyTemplateName(newNameField.getText());
            } else {
                newNameField.requestFocus();
                return;
            }
        }
    }

    /**
     * validate the username and context
     * @return true validated, false othwerwise
     */
    private boolean validateInput() {
        String newPass = newNameField.getText();

        if (newPass == null || "".equals(newPass)) {
            JOptionPane.
              showMessageDialog(this,
                resources.getString("newPolicyTemplateNameField.error.empty"),
                resources.getString("newPolicyTemplateNameField.error.title"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    /**
     * change policy template name
     *
     * @param newName new policy template name
     */
    private void changePolicyTemplateName(String newName) {

        try {
            dispose();

            if (listener !=null) {
                listener.actionPerformed(new ActionEvent(this, 1, newName));
            }
        } catch (Exception e) {
            ErrorManager.getDefault().notify(Level.WARNING, e,"There was an system error saving the new policy template name" );
            newNameField.requestFocus();
        }
    }
}

