package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Cell renderer for the role assignment name column which displays the name with an icon depending on the role assignment type.
 */
public class RoleAssignmentNameCellRenderer extends DefaultTableCellRenderer {
    private Icon GROUP_ICON = ImageCache.getInstance().getIconAsIcon(GroupPanel.GROUP_ICON_RESOURCE);
    private Icon USER_ICON = ImageCache.getInstance().getIconAsIcon(UserPanel.USER_ICON_RESOURCE);
    private JTable assignmentsTable;
    private int typeColIndex;

    /**
     * @param assignmentsTable the JTable holding the role assignments.
     * @param typeColIndex     the index of the column which is holding the assignment type (group or user).
     */
    public RoleAssignmentNameCellRenderer(@NotNull final JTable assignmentsTable, final int typeColIndex) {
        this.assignmentsTable = assignmentsTable;
        this.typeColIndex = typeColIndex;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        if (value instanceof String) {
            final String name = (String) value;
            final Object type = assignmentsTable.getValueAt(row, typeColIndex);
            final JLabel label = new JLabel(name);
            if (type instanceof String && type.equals(EntityType.USER.getName())) {
                label.setIcon(USER_ICON);
            } else if (type instanceof String && type.equals(EntityType.GROUP.getName())) {
                label.setIcon(GROUP_ICON);
            }
            label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            label.setOpaque(isSelected);
            return label;
        } else {
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
