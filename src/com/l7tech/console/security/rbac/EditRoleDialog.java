package com.l7tech.console.security.rbac;

import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.awt.*;

import com.l7tech.common.security.rbac.Permission;
import com.l7tech.common.gui.util.Utilities;

public class EditRoleDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField roleName;
    private JButton addPermission;
    private JButton editPermission;
    private JButton removePermission;
    private String roleNameString;
    private JTable permissionsTable;
    private JPanel buttonPanel;
    private JPanel informationPanel;

    private final ActionListener permissionsListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            doPermissionAction(e);
        }
    };

    public EditRoleDialog(String roleName, Dialog parent) {
        super(parent);
        this.roleNameString = roleName;
        inititialize();
    }

    public EditRoleDialog(String roleName, Frame parent) {
        super(parent);
        this.roleNameString = roleName;
        inititialize();
    }

    private void inititialize() {
        String[] columnNames = new String[]{"Type", "Operation", "Applies To"};
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columnNames);
        permissionsTable.setModel(model);


        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        if (StringUtils.isEmpty(roleNameString))
            setTitle("Create new Role");
        else {
            setTitle("Edit \"" + roleNameString + "\" Role");
            roleName.setText(roleNameString);
        }
        setupButtonListeners();
        setupActionListeners();
        pack();

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

        addPermission.addActionListener(permissionsListener);
        editPermission.addActionListener(permissionsListener);
        removePermission.addActionListener(permissionsListener);
    }

    private void doPermissionAction(ActionEvent e) {
        Object src = e.getSource();
        if ((src == null) || !(src instanceof JButton))
            return;

        JButton srcButton = (JButton) src;
        if (srcButton == addPermission)
            showEditPermissionDialog(null);
        else if (srcButton == editPermission)
            showEditPermissionDialog(getSelectedPermission());
        else if (srcButton == removePermission)
            deleteWithConfirm(getSelectedPermission());
        else
            return;
    }

    private void deleteWithConfirm(String selectedPermission) {
        if (selectedPermission == null)
            return;

        int result = JOptionPane.showConfirmDialog(this, "Remove the \"" + selectedPermission + "\" role from the system?", "Confirm Role Removal", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.YES_OPTION) {
//            listModel.removeElement(selectedPermission);
        }
    }

    private void showEditPermissionDialog(String selectedPermission) {
        Permission perm = null;
        EditPermissionsDialog dlg = new EditPermissionsDialog(perm, this);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }

    private String getSelectedPermission() {
        return null;

    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
