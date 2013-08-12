package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Confirmation dialog for removal of roles from a user/group.
 */
public class IdentityRoleRemovalDialog extends JDialog {
    private static final String CONFIRMATION_FORMAT = "Remove these roles from the {0} ''{1}''?";
    private static final String NAME_UNAVAILABLE = "name unavailable";
    private static final int MAX_NAME_CHARS = 30;
    private JPanel contentPanel;
    private OkCancelPanel okCancelPanel;
    private JTable rolesTable;
    private JLabel confirmationLabel;
    private SimpleTableModel<Role> roleModel;
    private Map<Role, String> toRemove;
    private boolean confirmed;

    /**
     * @param owner        the owner of this dialog.
     * @param entityType   the identity type of the identity this panel is referencing (user/group).
     * @param identityName the display name of the identity that this dialog is referencing.
     * @param toRemove     a map of roles to remove from the identity role assignments where key = role and value = display name for the role.
     */
    public IdentityRoleRemovalDialog(@NotNull final Window owner, @NotNull final EntityType entityType, @NotNull final String identityName, @NotNull final Map<Role, String> toRemove) {
        super(owner, "Remove Roles", DEFAULT_MODALITY_TYPE);
        if (entityType != EntityType.USER && entityType != EntityType.GROUP) {
            throw new IllegalArgumentException("Identity must be a user or group.");
        }
        setContentPane(contentPanel);
        this.toRemove = toRemove;
        this.confirmationLabel.setText(MessageFormat.format(CONFIRMATION_FORMAT, entityType == EntityType.USER ? "user" : "group",
                TextUtils.truncateStringAtEnd(identityName, MAX_NAME_CHARS)));
        initButtons();
        initTable();
    }

    public boolean isConfirmed() {
        return confirmed;
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

    private void initTable() {
        roleModel = TableUtil.configureTable(rolesTable,
                column("Name", 80, 250, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        String name = toRemove.get(role);
                        if (StringUtils.isBlank(name)) {
                            name = NAME_UNAVAILABLE;
                        }
                        return name;
                    }
                }),
                column("Type", 80, 100, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? BasicRolePropertiesPanel.CUSTOM : BasicRolePropertiesPanel.SYSTEM;
                    }
                }));
        roleModel.setRows(new ArrayList<>(toRemove.keySet()));
        Utilities.setRowSorter(rolesTable, roleModel);
    }
}
