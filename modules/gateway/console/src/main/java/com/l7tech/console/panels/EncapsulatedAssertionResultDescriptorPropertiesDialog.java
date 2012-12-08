package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionDataType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;

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
        super(owner, "Argument Properties", ModalityType.APPLICATION_MODAL);
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

        typeComboBox.setModel(new DefaultComboBoxModel(EncapsulatedAssertionDataType.values()));

        cancelButton.addActionListener(Utilities.createDisposeAction(this));


        updateGui(bean);
    }

    private void updateGui(EncapsulatedAssertionResultDescriptor bean) {
        nameField.setText(bean.getResultName());
        typeComboBox.setSelectedItem(bean.getResultType() == null ? null : EncapsulatedAssertionDataType.valueOf(bean.getResultType()));
    }

    private EncapsulatedAssertionResultDescriptor updateBean(EncapsulatedAssertionResultDescriptor bean) {
        bean.setResultName(nameField.getText());
        EncapsulatedAssertionDataType type = (EncapsulatedAssertionDataType) typeComboBox.getSelectedItem();
        bean.setResultType(type == null ? null : type.name());
        return bean;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
