package com.l7tech.console.panels.encass;

import com.l7tech.console.util.ContextVariableTextComponentValidationRule;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.policy.variable.DataType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static com.l7tech.console.panels.encass.EncapsulatedAssertionConstants.MAX_CHARS_FOR_NAME;

public class EncapsulatedAssertionArgumentDescriptorPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox guiPromptCheckBox;
    private JComboBox typeComboBox;
    private JTextField nameField;
    private JTextField guiLabelField;

    private final EncapsulatedAssertionArgumentDescriptor encapsulatedAssertionArgumentDescriptor;
    private InputValidator inputValidator;
    private boolean confirmed = false;
    private final Set<String> usedVariableNames;

    public EncapsulatedAssertionArgumentDescriptorPropertiesDialog(@NotNull Window owner, @NotNull EncapsulatedAssertionArgumentDescriptor bean, @NotNull Set<String> reservedVariableNames) {
        super(owner, "Argument Properties", ModalityType.APPLICATION_MODAL);
        Utilities.setEscKeyStrokeDisposes(this);
        this.usedVariableNames = reservedVariableNames;
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

        inputValidator.addRule(new ContextVariableTextComponentValidationRule("Name", nameField, false, false));
        inputValidator.constrainTextFieldToMaxChars("Name", nameField, MAX_CHARS_FOR_NAME, null);
        inputValidator.constrainTextFieldToMaxChars("Label", guiLabelField, MAX_CHARS_FOR_NAME, null);
        inputValidator.addRule(new InputValidator.ComponentValidationRule(nameField) {
            @Override
            public String getValidationError() {
                if (usedVariableNames.contains(nameField.getText()))
                    return "The specified variable name is already used by a different argument.";
                return null;
            }
        });

        typeComboBox.setModel(new DefaultComboBoxModel(DataType.GUI_EDITABLE_VALUES));
        inputValidator.ensureComboBoxSelection("Type", typeComboBox);

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
        typeComboBox.setSelectedItem(bean.getArgumentType() == null ? typeComboBox.getItemAt(0) : DataType.forName(bean.getArgumentType()));
        guiPromptCheckBox.setSelected(bean.isGuiPrompt());
        guiLabelField.setText(bean.getGuiLabel());
    }

    private EncapsulatedAssertionArgumentDescriptor updateBean(EncapsulatedAssertionArgumentDescriptor bean) {
        bean.setArgumentName(nameField.getText());
        bean.setGuiPrompt(guiPromptCheckBox.isSelected());
        DataType type = (DataType) typeComboBox.getSelectedItem();
        bean.setArgumentType(type == null ? DataType.UNKNOWN.getShortName() : type.getShortName());
        if (guiLabelField.isEnabled()) {
            bean.setGuiLabel(guiLabelField.getText());
        }
        return bean;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
