package com.l7tech.external.assertions.whichmodule.console;

import com.l7tech.external.assertions.whichmodule.DemoGenericEntity;
import com.l7tech.gui.NumberField;

import javax.swing.*;
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

    final DemoGenericEntity entity;
    boolean confirmed = false;

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
        enabledCheckBox.setSelected(entity.isEnabled());
    }

    public void setReadOnly(boolean readOnly) {
        nameField.setEnabled(!readOnly);
        ageField.setEnabled(!readOnly);
        playsTromboneCheckBox.setEnabled(!readOnly);
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
        entity.setEnabled(enabledCheckBox.isSelected());
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
