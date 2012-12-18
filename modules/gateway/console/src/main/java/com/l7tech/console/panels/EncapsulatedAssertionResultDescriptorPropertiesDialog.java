package com.l7tech.console.panels;

import com.l7tech.console.util.ContextVariableTextComponentValidationRule;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.variable.DataType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EncapsulatedAssertionResultDescriptorPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox typeComboBox;
    private JTextField nameField;

    private final EncapsulatedAssertionResultDescriptor encapsulatedAssertionResultDescriptor;
    private InputValidator inputValidator;
    private boolean confirmed = false;

    public EncapsulatedAssertionResultDescriptorPropertiesDialog(Window owner, EncapsulatedAssertionResultDescriptor bean) {
        super(owner, "Result Properties", ModalityType.APPLICATION_MODAL);
        encapsulatedAssertionResultDescriptor = bean;
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
