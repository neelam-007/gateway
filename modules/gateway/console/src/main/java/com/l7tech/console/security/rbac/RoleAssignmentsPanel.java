package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoleAssignmentsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RoleAssignmentsPanel.class.getName());
    private JPanel assignmentsContentPanel;
    private JTextField assignmentsRoleTextField;
    private JTable assignmentsTable;
    private FilterPanel assignmentsFilterPanel;
    private JLabel assignmentsFilterLabel;
    private JButton addButton;
    private JButton removeButton;
    private Role role;
    private RoleAssignmentTableModel assignmentsTableModel;

    public RoleAssignmentsPanel() {
        loadTable();
    }

    public void configure(@Nullable final Role role) {
        this.role = role;
        if (this.role != null) {
            String name = "name unavailable";
            try {
                name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
            } catch (final FindException e) {
                logger.log(Level.WARNING, "Unable to retrieve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            assignmentsRoleTextField.setText(name);
        }
        loadTable();
    }

    private void loadTable() {
        try {
            assignmentsTableModel = new RoleAssignmentTableModel(role);
            assignmentsTable.setModel(assignmentsTableModel);
            Utilities.setRowSorter(assignmentsTable, assignmentsTableModel, new int[]{0, 1}, new boolean[]{true, true},
                    new Comparator[]{null, RoleAssignmentTableModel.USER_GROUP_COMPARATOR});
            TableColumn tC = assignmentsTable.getColumn(RoleAssignmentTableModel.USER_GROUPS);
            tC.setCellRenderer(new UserGroupTableCellRenderer(assignmentsTable));
        } catch (final FindException | DuplicateObjectException e) {
            logger.log(Level.WARNING, "Unable to populate role assignments: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this, "Unable to populate role assignments", "Error", JOptionPane.ERROR_MESSAGE, null);
        }
    }
}
