package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Confirmation dialog for removal of users from a role.
 */
public class RoleAssignmentRemovalDialog extends JDialog {
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(RoleAssignmentRemovalDialog.class.getName());
    private static final int NAME_COL_INDEX = 0;
    private static final int TYPE_COL_INDEX = 1;
    private static final String UNKNOWN = "unknown";
    private static final String CONFIRMATION_LABEL_PROPERTY = "confirmation.label";
    private static final String MAX_NAME_CHARS = "name.max.chars";
    private JPanel contentPanel;
    private JTable assignmentsTable;
    private OkCancelPanel okCancelPanel;
    private JLabel confirmationLabel;
    private Map<RoleAssignment, String[]> toRemove;
    private SimpleTableModel<RoleAssignment> assignmentsTableModel;
    private boolean confirmed = false;

    /**
     * @param owner    owner of the dialog.
     * @param roleName the name of the role which is having role assignments removed from.
     * @param toRemove map of role assignments to remove where key = RoleAssignment and value = string array [nameOfUserOrGroup, nameOfProvider].
     */
    public RoleAssignmentRemovalDialog(@NotNull Window owner, @NotNull final String roleName, @NotNull final Map<RoleAssignment, String[]> toRemove) {
        super(owner, "Remove Assignments", DEFAULT_MODALITY_TYPE);
        for (final String[] names : toRemove.values()) {
            Validate.notNull(names, "names of the user/group and provider cannot be null");
            Validate.isTrue(names.length == 2, "Expected 2 names, received " + names.length);
        }
        setContentPane(contentPanel);
        this.toRemove = toRemove;
        confirmationLabel.setText(MessageFormat.format(RESOURCES.getString(CONFIRMATION_LABEL_PROPERTY),
                TextUtils.truncateStringAtEnd(roleName, Integer.valueOf(RESOURCES.getString(MAX_NAME_CHARS)))));
        initTable();
        initButtons();
    }

    private void initTable() {
        assignmentsTableModel = TableUtil.configureTable(assignmentsTable,
                column("Name", 80, 300, 99999, new Functions.Unary<String, RoleAssignment>() {
                    @Override
                    public String call(final RoleAssignment assignment) {
                        String name = toRemove.get(assignment)[0];
                        if (StringUtils.isBlank(name)) {
                            name = UNKNOWN;
                        }
                        return name;
                    }
                }),
                column("Type", 80, 300, 99999, new Functions.Unary<String, RoleAssignment>() {
                    @Override
                    public String call(final RoleAssignment assignment) {
                        return assignment.getEntityType();
                    }
                }),
                column("Provider", 80, 300, 99999, new Functions.Unary<String, RoleAssignment>() {
                    @Override
                    public String call(final RoleAssignment assignment) {
                        String providerName = toRemove.get(assignment)[1];
                        if (StringUtils.isBlank(providerName)) {
                            providerName = UNKNOWN;
                        }
                        return providerName;
                    }
                }));
        assignmentsTable.setModel(assignmentsTableModel);
        assignmentsTable.getColumnModel().getColumn(NAME_COL_INDEX).setCellRenderer(new RoleAssignmentNameCellRenderer(assignmentsTable, TYPE_COL_INDEX));
        Utilities.setRowSorter(assignmentsTable, assignmentsTableModel);
        assignmentsTableModel.setRows(new ArrayList<>(toRemove.keySet()));
    }

    private void initButtons() {
        okCancelPanel.setOkButtonText("Remove");
        okCancelPanel.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        okCancelPanel.getCancelButton().addActionListener(Utilities.createDisposeAction(this));
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
