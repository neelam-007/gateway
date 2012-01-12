package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.objectmodel.folder.Folder;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

    private boolean confirmed = false;
    private final JDialog dialog;

    private static final int MAX_FOLDER_NAME_LENGTH = EntityUtil.getMaxFieldLength( Folder.class, "name", 128 );

    /**
     * Creates a new instance of PolicyFolderPropertiesDialog. The name field in the dialog
     * will be set from the provided value.
     *
     * @param owner The owner of this dialog window
     * @param folderName The name of the policy/service folder
     */
    public PolicyFolderPropertiesDialog(Window owner, String folderName) throws HeadlessException {
        super(owner, TITLE, DEFAULT_MODALITY_TYPE);
        initialize(folderName);
        dialog = this;
    }

    /**
     * Loads locale-specific resources: strings, images, etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.PolicyFolderPropertyDialog", locale);
    }

    /**
     * Initializes this dialog window and sets fields with the given values.
     */
    private void initialize(String folderName) {
        setContentPane(contentPanel);
        getRootPane().setDefaultButton( okButton );

        initResources();

        nameField.setDocument(new PlainDocument(){
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if(getLength() >= MAX_FOLDER_NAME_LENGTH){
                     DialogDisplayer.showMessageDialog(dialog,
                                                      "Folder name cannot exceed "+MAX_FOLDER_NAME_LENGTH+" characters",
                                                      "Folder name too long",
                                                      JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                super.insertString(offs, str, a);
            }
        });

        nameField.setText( folderName );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if ( nameField.isEditable() ) {
                    nameField.requestFocusInWindow();
                    nameField.selectAll();
                }
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        Utilities.equalizeButtonSizes( okButton, cancelButton );

        pack();
        Utilities.centerOnParentWindow(this);
    }

    public String getName() {
        return nameField.getText().trim();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    private void ok() {
        if (nameField.getText() == null || nameField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(PolicyFolderPropertiesDialog.this,
                    resources.getString("settings.name.dialog.text"),
                    resources.getString("settings.name.dialog.title"),
                    JOptionPane.ERROR_MESSAGE);
        } else {
            confirmed = true;
            dispose();
        }
    }
}
