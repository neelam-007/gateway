package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionDataType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EncapsulatedAssertionArgumentDescriptorPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox guiPromptCheckBox;
    private JComboBox typeComboBox;
    private JTextField nameField;
    private JTextField defaultValueField;
    private JPanel defaultValuePanel;

    private final EncapsulatedAssertionArgumentDescriptor encapsulatedAssertionArgumentDescriptor;
    private InputValidator inputValidator;
    private boolean confirmed = false;

    public EncapsulatedAssertionArgumentDescriptorPropertiesDialog(Window owner, EncapsulatedAssertionArgumentDescriptor bean) {
        super(owner, "Argument Properties", ModalityType.APPLICATION_MODAL);
        encapsulatedAssertionArgumentDescriptor = bean;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        inputValidator = new InputValidator(this, "Error");
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                updateBean(encapsulatedAssertionArgumentDescriptor);
                dispose();
            }
        });

        typeComboBox.setModel(new DefaultComboBoxModel(EncapsulatedAssertionDataType.values()));

        cancelButton.addActionListener(Utilities.createDisposeAction(this));


        updateGui(bean);
    }

    private void updateGui(EncapsulatedAssertionArgumentDescriptor bean) {
        nameField.setText(bean.getArgumentName());
        defaultValueField.setText(bean.getDefaultValue());
        typeComboBox.setSelectedItem(bean.getArgumentType() == null ? null : EncapsulatedAssertionDataType.valueOf(bean.getArgumentType()));
        guiPromptCheckBox.setSelected(bean.isGuiPrompt());
    }

    private EncapsulatedAssertionArgumentDescriptor updateBean(EncapsulatedAssertionArgumentDescriptor bean) {
        bean.setArgumentName(nameField.getText());
        bean.setDefaultValue(defaultValueField.getText());
        bean.setGuiPrompt(guiPromptCheckBox.isSelected());
        EncapsulatedAssertionDataType type = (EncapsulatedAssertionDataType) typeComboBox.getSelectedItem();
        bean.setArgumentType(type == null ? null : type.name());
        return bean;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
