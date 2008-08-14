package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog for setting the name of a service/policy folder.
 */
public class PolicyFolderPropertiesDialog extends JDialog {
    public static final String TITLE = "Folder Properties";

    /** Resource bundle with default locale */
    private ResourceBundle resources = null;

    private JTextField nameField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel contentPanel;

    private InputValidator inputValidator;
    private boolean confirmed = false;

    /**
     * Creates a new instance of PolicyFolderPropertiesDialog. The name field in the dialog
     * will be set from the provided value.
     *
     * @param owner The owner of this dialog window
     * @param folderName The name of the policy/service folder
     */
    public PolicyFolderPropertiesDialog(Dialog owner, String folderName) throws HeadlessException {
        super(owner, TITLE, true);
        initialize(folderName);
    }

    /**
     * Creates a new instance of PolicyFolderPropertiesDialog. The name field in the dialog
     * will be set from the provided value.
     *
     * @param owner The owner of this dialog window
     * @param folderName The name of the policy/service folder
     */
    public PolicyFolderPropertiesDialog(Frame owner, String folderName) throws HeadlessException {
        super(owner, TITLE, true);
        initialize(folderName);
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.PolicyFolderPropertyDialog", locale);
    }

    /**
     * Initializes this dialog window and sets all of the fields based on the provided EmailListener
     * object.
     */
    private void initialize(String folderName) {
        setContentPane(contentPanel);

        initResources();

        nameField.setText(folderName);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(nameField.getText() == null || nameField.getText().length() == 0) {
                    JOptionPane.showMessageDialog(PolicyFolderPropertiesDialog.this,
                            resources.getString("settings.name.dialog.text"),
                            resources.getString("settings.name.dialog.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    confirmed = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new AbstractButton[] { okButton, cancelButton });

        pack();
        Utilities.centerOnScreen(this);
    }

    public String getName() {
        return nameField.getText();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }
}
