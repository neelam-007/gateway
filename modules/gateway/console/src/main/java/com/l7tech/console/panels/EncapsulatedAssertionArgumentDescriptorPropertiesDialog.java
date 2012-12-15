package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.policy.variable.DataType;

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
    private JTextField guiLabelField;

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

        typeComboBox.setModel(new DefaultComboBoxModel(DataType.GUI_EDITABLE_VALUES));

        cancelButton.addActionListener(Utilities.createDisposeAction(this));

        Utilities.enableGrayOnDisabled(guiLabelField);
        guiPromptCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableThings();
            }
        });

        updateGui(bean);
        enableOrDisableThings();
    }

    private void enableOrDisableThings() {
        guiLabelField.setEnabled(guiPromptCheckBox.isSelected());
    }

    private void updateGui(EncapsulatedAssertionArgumentDescriptor bean) {
        nameField.setText(bean.getArgumentName());
        defaultValueField.setText(bean.getDefaultValue());
        typeComboBox.setSelectedItem(bean.getArgumentType() == null ? null : DataType.forName(bean.getArgumentType()));
        guiPromptCheckBox.setSelected(bean.isGuiPrompt());
        guiLabelField.setText(bean.getGuiLabel());
    }

    private EncapsulatedAssertionArgumentDescriptor updateBean(EncapsulatedAssertionArgumentDescriptor bean) {
        bean.setArgumentName(nameField.getText());
        bean.setDefaultValue(defaultValueField.getText());
        bean.setGuiPrompt(guiPromptCheckBox.isSelected());
        DataType type = (DataType) typeComboBox.getSelectedItem();
        bean.setArgumentType(type == null ? DataType.UNKNOWN.getShortName() : type.getShortName());
        bean.setGuiLabel(guiLabelField.getText());
        return bean;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
