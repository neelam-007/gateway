package com.l7tech.external.assertions.policybundleexporter.console;

import com.l7tech.external.assertions.policybundleexporter.ComponentInfo;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import java.awt.event.*;

/**
 * Dialog used to add or edit a component within a bundle.
 */
public class ComponentPropertiesDialog extends JDialog{
    private JPanel contentPanel;
    private JTextField nameTextField;
    private JTextField descriptionTextField;
    private JTextField versionTextField;
    private JTextField idTextField;
    private JCheckBox autoGenerateIdCheckBox;
    private FolderSelectionPanel folderSelectionPanel;
    private JButton okButton;
    private JButton cancelButton;

    private boolean isConfirmed = false;

    public ComponentPropertiesDialog(JDialog owner, Goid topFolderId) {
        super(owner, "Component Properties", true);
        initialize(topFolderId);
        // todo (kpak) - need to check for existing component names.
    }

    public boolean getIsConfirmed() {
        return isConfirmed;
    }

    public void setComponentInfo(ComponentInfo componentInfo) {
        nameTextField.setText(componentInfo.getName());
        descriptionTextField.setText(componentInfo.getDescription());
        versionTextField.setText(componentInfo.getVersion());
        boolean autoGenerateId = componentInfo.isAutoGenerateId();
        autoGenerateIdCheckBox.setSelected(autoGenerateId);
        if (!autoGenerateId) {
            idTextField.setText(componentInfo.getId());
        } else {
            idTextField.setText("");
        }
        folderSelectionPanel.setSelectedFolder(componentInfo.getFolderHeader());
    }

    public ComponentInfo getComponentInfo() {
        boolean autoGenerateId = autoGenerateIdCheckBox.isSelected();
        return new ComponentInfo(
            nameTextField.getText().trim(),
            descriptionTextField.getText().trim(),
            versionTextField.getText().trim(),
            autoGenerateId,
            autoGenerateId ? null : idTextField.getText().trim(),
            folderSelectionPanel.getSelectedFolder());
    }

    private void initialize(Goid topFolderId) {
        autoGenerateIdCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean isChecked = e.getStateChange() == ItemEvent.SELECTED;
                idTextField.setEnabled(!isChecked);
            }
        });

        autoGenerateIdCheckBox.setSelected(true);
        idTextField.setEnabled(false);

        folderSelectionPanel.populateFolders(topFolderId, true, true);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        getRootPane().setDefaultButton(okButton);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        contentPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setContentPane(contentPanel);
        pack();

        // todo (kpak) - remove populate with default values.
        //
        nameTextField.setText("My Component");
        descriptionTextField.setText("My Component Description");
        versionTextField.setText("1.0");
    }

    private void onOk() {
        // todo (kpak) - Check that text are valid in xml.
        StringBuilder sb = new StringBuilder();

        if (nameTextField.getText().trim().isEmpty()) {
            sb.append("The Component Name field must not be empty.\n");
        }

        if (descriptionTextField.getText().trim().isEmpty()) {
            sb.append("The Component Description field must not be empty.\n");
        }

        if (versionTextField.getText().trim().isEmpty()) {
            sb.append("The Component Version field must not be empty.\n");
        }

        if (!autoGenerateIdCheckBox.isSelected() && idTextField.getText().trim().isEmpty()) {
            sb.append("The Component ID field must not be empty.\n");
        }

        sb.append(folderSelectionPanel.isFolderSelected());

        String error = sb.toString();
        if (!error.isEmpty()) {
            DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
        } else {
            isConfirmed = true;
            dispose();
        }
    }
}