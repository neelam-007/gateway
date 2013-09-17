package com.l7tech.external.assertions.whichmodule.console;

import com.l7tech.console.panels.ServiceComboBox;
import com.l7tech.external.assertions.whichmodule.DemoGenericEntity;
import com.l7tech.gui.NumberField;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

public class DemoGenericEntityDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox playsTromboneCheckBox;
    private JTextField nameField;
    private JTextField ageField;
    private JCheckBox enabledCheckBox;
    private ServiceComboBox serviceComboBox;
    private JCheckBox enableServiceSelection;

    final DemoGenericEntity entity;
    boolean confirmed = false;
    private boolean readOnly = false;

    public DemoGenericEntityDialog(Window owner, DemoGenericEntity entity) {
        super(owner);
        this.entity = entity;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        enableServiceSelection.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                serviceComboBox.setEnabled(enableServiceSelection.isSelected() && !readOnly);
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        ageField.setDocument(new NumberField(3));

        nameField.setText(entity.getName());
        ageField.setText(String.valueOf(entity.getAge()));
        playsTromboneCheckBox.setSelected(entity.isPlaysTrombone());
        enableServiceSelection.setSelected(entity.getServiceId() != null);
        serviceComboBox.populateAndSelect(entity.getServiceId() != null ? true : false, entity.getServiceId());
        serviceComboBox.setEnabled(entity.getServiceId() != null);
        enabledCheckBox.setSelected(entity.isEnabled());
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        nameField.setEnabled(!readOnly);
        ageField.setEnabled(!readOnly);
        playsTromboneCheckBox.setEnabled(!readOnly);
        enableServiceSelection.setEnabled(!readOnly);
        serviceComboBox.setEnabled(enableServiceSelection.isSelected() && !readOnly);
        enabledCheckBox.setEnabled(!readOnly);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public DemoGenericEntity getEntity() {
        return entity;
    }

    private void onOK() {
        entity.setName(nameField.getText());
        entity.setAge(Integer.valueOf(ageField.getText()));
        entity.setPlaysTrombone(playsTromboneCheckBox.isSelected());
        entity.setServiceId((enableServiceSelection.isSelected() && serviceComboBox.getSelectedPublishedService() != null) ? serviceComboBox.getSelectedPublishedService().getGoid() : null);
        entity.setEnabled(enabledCheckBox.isSelected());
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
