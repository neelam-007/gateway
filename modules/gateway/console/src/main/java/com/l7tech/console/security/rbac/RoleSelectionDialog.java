package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Dialog which allows selection of roles for which a user/group should be assigned.
 */
public class RoleSelectionDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(RoleSelectionDialog.class.getName());
    private static final String NAME = "Name";
    private static final String TYPE = "Type";
    private static final String DESCRIPTION = "Description";
    private static final int CHECK_COL_INDEX = 0;
    private static final int NAME_COL_INDEX = 1;
    private static final int MAX_WIDTH = 99999;
    private static final int CHECK_BOX_WIDTH = 30;
    private static final String ROLES = "roles";
    private JPanel contentPanel;
    private OkCancelPanel okCancelPanel;
    private SelectableFilterableTablePanel tablePanel;
    private boolean confirmed;
    private CheckBoxSelectableTableModel<Role> rolesModel;

    public RoleSelectionDialog(@NotNull final Window owner, @NotNull final String identityName) {
        super(owner, "Add Roles to " + identityName, DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        initButtons();
        initTable();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public java.util.List<Role> getSelectedRoles() {
        if (isConfirmed()) {
            return rolesModel.getSelected();
        }
        throw new IllegalStateException("Dialog is not yet confirmed");
    }

    private void initButtons() {
        okCancelPanel.setOkButtonText("Add");
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
        rolesModel = TableUtil.configureSelectableTable(tablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, Role>() {
                    @Override
                    public Boolean call(final Role role) {
                        return rolesModel.isSelected(role);
                    }
                }),
                column(NAME, 30, 200, MAX_WIDTH, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        String name = "unavailable";
                        try {
                            name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return name;
                    }
                }),
                column(TYPE, 30, 200, MAX_WIDTH, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? BasicRolePropertiesPanel.CUSTOM : BasicRolePropertiesPanel.SYSTEM;
                    }
                }),
                column(DESCRIPTION, 30, 400, MAX_WIDTH, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return RbacUtilities.getDescriptionString(role, false);
                    }
                }));
        try {
            rolesModel.setSelectableObjects(new ArrayList<>(Registry.getDefault().getRbacAdmin().findAllRoles()));
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve roles: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            rolesModel.setSelectableObjects(Collections.<Role>emptyList());
        }
        tablePanel.configure(rolesModel, new int[]{NAME_COL_INDEX}, ROLES);
    }
}
