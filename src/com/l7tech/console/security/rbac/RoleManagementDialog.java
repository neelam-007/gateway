package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class RoleManagementDialog extends JDialog {
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel informationPane;

    private JList roleList;
    private JButton addRole;
    private JButton editRole;
    private JButton removeRole;

    private JPanel mainPanel;
    private JTextPane propertiesPane;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");
    private final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
    private final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();

    private final ActionListener roleActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doUpdateRoleAction(e);
        }
    };

    public RoleManagementDialog(Dialog parent) throws HeadlessException {
        super(parent, resources.getString("manageRoles.title"));
        initialize();
    }

    public RoleManagementDialog(Frame parent) throws HeadlessException {
        super(parent, resources.getString("manageRoles.title"));
        initialize();
    }

    private void initialize() {
        populateList();
        setupButtonListeners();
        setupActionListeners();



        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(mainPanel);

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        enableEditRemoveButtons();

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

        roleList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1)
                    enableEditRemoveButtons();
                else if (e.getClickCount() >= 2)
                    showEditDialog(getSelectedRole());
            }
        });

        roleList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableEditRemoveButtons();
            }
        });
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

    private void enableEditRemoveButtons() {
        boolean enabled = roleList.getModel().getSize() != 0 &&
                roleList.getSelectedValue() != null;

        removeRole.setEnabled(enabled);
        editRole.setEnabled(enabled);
    }

    private void doUpdateRoleAction(ActionEvent e) {
        Object source = e.getSource();
        if (source == null || !(source instanceof JButton)) {
            return;
        }

        JButton srcButton = (JButton) source;
        if (srcButton == addRole) {
            Role newRole = showEditDialog(new Role());
            if (newRole != null) populateList();
        } else if (srcButton == editRole) {
            Role r = showEditDialog(getSelectedRole());
            if (r != null) populateList();
        } else if (srcButton == removeRole) {
            final Role selectedRole = getSelectedRole();
            if (selectedRole == null) return;
            Utilities.doWithConfirmation(
                this,
                resources.getString("manageRoles.deleteTitle"),
                MessageFormat.format(resources.getString("manageRoles.deleteMessage"), selectedRole.getName()),
                new Runnable() {
                    public void run() {
                        try {
                            rbacAdmin.deleteRole(selectedRole);
                            populateList();
                        } catch (Exception e1) {
                            throw new RuntimeException("Couldn't delete Role", e1);
                        }
                    }
                });
        }
    }

    private Role getSelectedRole() {
        return (Role)roleList.getSelectedValue();
    }

    private Role showEditDialog(Role selectedRole) {
        if (selectedRole == null) return null;

        EditRoleDialog dlg = new EditRoleDialog(selectedRole, this);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        Role updated = dlg.getRole();
        if (updated != null) {
            Role sel = (Role) roleList.getSelectedValue();
            populateList();
            roleList.setSelectedValue(sel, true);
        }
        return updated;
    }

    private void populateList() {
        try {
            Role[] roles = rbacAdmin.findAllRoles().toArray(new Role[0]);
            roleList.setModel(new DefaultComboBoxModel(roles));
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get initial list of Roles", e);
        }
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
