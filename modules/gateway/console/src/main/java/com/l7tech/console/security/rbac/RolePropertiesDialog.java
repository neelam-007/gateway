package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.BasicPropertiesPanel;
import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RolePropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(RolePropertiesDialog.class.getName());
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(RolePropertiesDialog.class.getName());
    private static final String NAME_MAX_CHARS = "name.max.chars";
    private static final String DESCRIPTION_MAX_CHARS = "description.max.chars";
    private static final String ERROR_UNIQUE_NAME = "error.unique.name";
    private JPanel contentPanel;
    private BasicPropertiesPanel basicPropertiesPanel;
    private OkCancelPanel okCancelPanel;
    private RolePermissionsPanel permissionsPanel;
    private Role role;
    private boolean readOnly;
    private InputValidator inputValidator;
    private Set<String> reservedNames;
    private String operation;
    private Functions.UnaryVoidThrows<Role, SaveException> afterEditListener;
    private boolean confirmed;

    public RolePropertiesDialog(@NotNull final Window owner, @NotNull final Role role, final boolean readOnly, @NotNull final Set<String> reservedNames, @NotNull final Functions.UnaryVoidThrows<Role, SaveException> afterEditListener) {

        super(owner, readOnly ? "Role Properties" : role.isUnsaved() ? "Create Role" : "Edit Role", DEFAULT_MODALITY_TYPE);
        this.role = role;
        this.operation = role.isUnsaved() ? "Create" : "Edit";
        this.reservedNames = new HashSet<>();
        for (final String reservedName : reservedNames) {
            // for case-insensitive checks
            this.reservedNames.add(reservedName.toLowerCase());
        }
        this.role = role;
        this.readOnly = readOnly;
        this.afterEditListener = afterEditListener;
        setContentPane(contentPanel);
        initComponents();
        initValidation();
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
    }

    public void setDataOnRole(@NotNull final Role role) {
        role.setName(basicPropertiesPanel.getNameText());
        role.setDescription(basicPropertiesPanel.getDescriptionText());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initComponents() {
        okCancelPanel.setOkButtonText(operation);
        okCancelPanel.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                RoleManagerWindow.restorePermissions(role);
                try {
                    afterEditListener.call(null);
                } catch (final SaveException ex) {
                    logger.log(Level.WARNING, "Error cancelling save: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
                }
                dispose();
            }
        });
        if (readOnly || basicPropertiesPanel.getNameField().getText().trim().isEmpty()) {
            okCancelPanel.getOkButton().setEnabled(false);
        }
        basicPropertiesPanel.getNameField().setText(role.getName());
        basicPropertiesPanel.getDescriptionTextArea().setText(role.getDescription());
        permissionsPanel.configure(role);
    }

    private void initValidation() {
        inputValidator = basicPropertiesPanel.configureValidation(this, getTitle(), okCancelPanel.getOkButton(),
                new OkButtonActionListener(), getResourceInt(NAME_MAX_CHARS), getResourceInt(DESCRIPTION_MAX_CHARS));
        inputValidator.isValid();
    }

    private Integer getResourceInt(final String property) {
        Integer ret = null;
        final String val = RESOURCES.getString(property);
        try {
            ret = Integer.valueOf(val);
        } catch (final NumberFormatException e) {
            logger.log(Level.WARNING, "Property " + property + " is an invalid number: " + val, ExceptionUtils.getDebugException(e));
        }
        return ret;
    }

    private void save() {
        setDataOnRole(role);
        try {
            afterEditListener.call(role);
            dispose();
        } catch (final SaveException e) {
            if (e instanceof DuplicateObjectException && StringUtils.containsIgnoreCase(e.getMessage(), "name")) {
                // name conflict with role that the user cannot read
                showDuplicateNameError();
            } else {
                logger.log(Level.WARNING, "Unable to save role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                DialogDisplayer.showMessageDialog(this, "Unable to save.", "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private void createUIComponents() {
        permissionsPanel = new RolePermissionsPanel(readOnly);
    }

    private class OkButtonActionListener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            // don't display name validation errors until after user clicks ok
            if (reservedNames.contains(basicPropertiesPanel.getNameField().getText().trim().toLowerCase())) {
                showDuplicateNameError();
            } else {
                save();
                confirmed = true;
            }
        }
    }

    private void showDuplicateNameError() {
        final String error = MessageFormat.format(RESOURCES.getString(ERROR_UNIQUE_NAME), operation.toLowerCase());
        DialogDisplayer.showMessageDialog(this, error,
                operation + " Role", JOptionPane.ERROR_MESSAGE, null);
    }
}
