package com.l7tech.console.security.rbac;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

/**
 * CRUD dialog for Roles.
 */
public class RoleManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(RoleManagerWindow.class.getName());
    private JPanel contentPanel;
    private JTable rolesTable;
    private JButton closeButton;
    private JButton helpButton;
    private FilterPanel filterPanel;
    private JLabel filterLabel;
    private SimpleTableModel<Role> rolesTableModel;

    public RoleManagerWindow(@NotNull final Window owner) {
        super(owner, "Manage Roles", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        initButtons();
        initTable();
        initFiltering();
    }

    private void initFiltering() {
        filterPanel.registerClearCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        filterPanel.attachRowSorter((TableRowSorter) (rolesTable.getRowSorter()), new int[]{0});
        loadCount();
    }

    private void initTable() {
        rolesTableModel = TableUtil.configureTable(rolesTable,
                column("Name", 80, 400, 99999, propertyTransform(Role.class, "descriptiveName")),
                column("Type", 40, 80, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? "Custom" : "System";
                    }
                }));
        Utilities.setRowSorter(rolesTable, rolesTableModel);
        loadTable();
    }

    private void initButtons() {
        closeButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeButton);
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Actions.invokeHelp(RoleManagerWindow.this);
            }
        });
    }

    private void loadCount() {
        final int visible = rolesTable.getRowCount();
        final int total = rolesTableModel.getRowCount();
        filterLabel.setText("showing " + visible + " of " + total + " items");
    }

    private void loadTable() {
        try {
            final ArrayList<Role> roles = new ArrayList<>(Registry.getDefault().getRbacAdmin().findAllRoles());
            Collections.sort(roles, new NamedEntityComparator());
            rolesTableModel.setRows(roles);
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve rows: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }
}
