package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class RoleManagementDialog extends JDialog {
    private JButton buttonOK;
    private JButton buttonCancel;;
    private JPanel buttonPane;
    private JPanel informationPane;

    private JTextPane propertiesPane;
    private JList roleList;
    private JButton addRole;
    private JButton editRole;
    private JButton removeRole;
    private JTabbedPane tabs;

    private JList assignmentList;

    private final DefaultListModel listModel = new DefaultListModel();
    private JPanel mainPanel;
    private JButton addAssignment;
    private JButton removeAssignment;
    private JTextField filterText;

    private final ActionListener roleActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doUpdateRoleAction(e);
        }
    };

    public RoleManagementDialog(Dialog parent) throws HeadlessException {
        super(parent);
        initialize();
    }

    public RoleManagementDialog(Frame parent) throws HeadlessException {
        super(parent);
        initialize();
    }

    private void initialize() {

        populateList();
        setupButtonListeners();
        setupActionListeners();

        roleList.setModel(listModel);
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(mainPanel);

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        propertiesPane.setForeground(informationPane.getBackground());
        assignmentList.setForeground(informationPane.getBackground());
        
        pack();
    }

    private void setupActionListeners() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        informationPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void doUpdateRoleAction(ActionEvent e) {
        Object source = e.getSource();
        if (source == null || !(source instanceof JButton)) {
            return;
        }

        JButton srcButton = (JButton) source;
        if (srcButton == addRole) {
            showEditDialog(null);
        } else if (srcButton == editRole) {
            showEditDialog(getSelectedRole());
        } else if (srcButton == removeRole) {
            deleteWithConfirm(getSelectedRole());
        } else {
            return;
        }
    }

    private void setupButtonListeners() {
        editRole.addActionListener(roleActionListener);
        addRole.addActionListener(roleActionListener);
        removeRole.addActionListener(roleActionListener);

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

    private void deleteWithConfirm(String selectedRole) {
        if (selectedRole == null)
            return;

        int result = JOptionPane.showConfirmDialog(this, "Remove the \"" + selectedRole + "\" role from the system?", "Confirm Role Removal", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            listModel.removeElement(selectedRole);
        }
    }

    private String getSelectedRole() {
        int index = roleList.getSelectedIndex();
        Object obj = null;
        try {
            obj = listModel.get(index);
        } catch (ArrayIndexOutOfBoundsException aobe) {
            return null;
        }

        return (String)obj;
    }

    private void showEditDialog(String selectedRole) {
        Dialog dlg = new EditRoleDialog(selectedRole, this);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }

    private void populateList() {

        String[] roles = new String[] {
                "Administrator",
                "Operator",
                "Customer Representative",
                "Logs View Only",
        };

        for (String role : roles) {
            listModel.addElement(role);
        }

        roleList.setSelectedIndex(0);
    }

    private void onOK() {

        dispose();
    }

    private void onCancel() {

        dispose();
    }

    public static void main(String[] args) {
        JFrame parent = new JFrame("Testing");
        RoleManagementDialog dialog = new RoleManagementDialog(parent);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
