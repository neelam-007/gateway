package com.l7tech.console.security.rbac;

import com.l7tech.common.security.rbac.Permission;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class EditPermissionsDialog extends JDialog {
    private JPanel contentPane;
    private JPanel informationPanel;
    private JPanel buttonPanel;
    private JButton buttonOK;
    private JButton buttonCancel;

    private JComboBox typeSelection;
    private JComboBox operationSelection;
    private JTextField scopeField;
    private JButton browseForScope;
    private Permission permission;

    public EditPermissionsDialog(Permission permission, Dialog parent) {
        super(parent);
        this.permission = permission;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setupButtonListeners();
        setupActionListeners();

        if (permission == null)
            setTitle("Create new Permission");
        else
            setTitle("Edit Permission");

        pack();
    }

    private void setupButtonListeners() {
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
    }

    private void setupActionListeners() {
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
    }

    private void onOK() {

        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
