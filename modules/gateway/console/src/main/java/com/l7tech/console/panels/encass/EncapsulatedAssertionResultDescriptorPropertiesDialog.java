package com.l7tech.console.panels.encass;

import com.l7tech.console.util.ContextVariableTextComponentValidationRule;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.variable.DataType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static com.l7tech.console.panels.encass.EncapsulatedAssertionConstants.MAX_CHARS_FOR_NAME;

public class EncapsulatedAssertionResultDescriptorPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox typeComboBox;
    private JTextField nameField;

    private final EncapsulatedAssertionResultDescriptor encapsulatedAssertionResultDescriptor;
    private InputValidator inputValidator;
    private boolean confirmed = false;
    private final Set<String> usedVariableNames;

    public EncapsulatedAssertionResultDescriptorPropertiesDialog(@NotNull Window owner, @NotNull EncapsulatedAssertionResultDescriptor bean, @NotNull Set<String> reserveVariableNames) {
        super(owner, "Result Properties", ModalityType.APPLICATION_MODAL);
        Utilities.setEscKeyStrokeDisposes(this);
        encapsulatedAssertionResultDescriptor = bean;
        usedVariableNames = reserveVariableNames;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        inputValidator = new InputValidator(this, "Error");
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                updateBean(encapsulatedAssertionResultDescriptor);
                dispose();
            }
        });

        inputValidator.addRule(new ContextVariableTextComponentValidationRule("Name", nameField, false, false));
        inputValidator.constrainTextFieldToMaxChars("Name", nameField, MAX_CHARS_FOR_NAME, null);
        inputValidator.addRule(new InputValidator.ComponentValidationRule(nameField) {
            @Override
            public String getValidationError() {
                if (usedVariableNames.contains(nameField.getText()))
                    return "The specified variable name is already used by a different result.";
                return null;
            }
        });

        typeComboBox.setModel(new DefaultComboBoxModel(DataType.VALUES));

        cancelButton.addActionListener(Utilities.createDisposeAction(this));


        updateGui(bean);
    }

    private void updateGui(EncapsulatedAssertionResultDescriptor bean) {
        nameField.setText(bean.getResultName());
        typeComboBox.setSelectedItem(bean.getResultType() == null ? null : DataType.forName(bean.getResultType()));
    }

    private EncapsulatedAssertionResultDescriptor updateBean(EncapsulatedAssertionResultDescriptor bean) {
        bean.setResultName(nameField.getText());
        DataType type = (DataType) typeComboBox.getSelectedItem();
        bean.setResultType(type == null ? DataType.UNKNOWN.getShortName() : type.getShortName());
        return bean;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
