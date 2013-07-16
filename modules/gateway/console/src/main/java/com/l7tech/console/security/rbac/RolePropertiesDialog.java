package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.BasicPropertiesPanel;
import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
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
    private Role role;
    private boolean readOnly;
    private InputValidator inputValidator;
    private Set<String> reservedNames;
    private String operation;
    private Functions.Unary<Boolean, Role> afterEditListener;

    public RolePropertiesDialog(@NotNull final Window owner, @NotNull final Role role, final boolean readOnly, @NotNull final Set<String> reservedNames, @NotNull final Functions.Unary<Boolean, Role> afterEditListener) {
        super(owner, readOnly ? "Role Properties" : role.getOid() == Role.DEFAULT_OID ? "Create Role" : "Edit Role", DEFAULT_MODALITY_TYPE);
        this.role = role;
        this.operation = role.getOid() == Role.DEFAULT_OID ? "Create" : "Edit";
        this.reservedNames = reservedNames;
        this.role = role;
        this.readOnly = readOnly;
        this.afterEditListener = afterEditListener;
        setContentPane(contentPanel);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
        initComponents();
        initValidation();
    }

    public void setDataOnRole(@NotNull final Role role) {
        role.setName(basicPropertiesPanel.getNameText());
        role.setDescription(basicPropertiesPanel.getDescriptionText());
    }

    private void initComponents() {
        okCancelPanel.setOkButtonText(operation);
        okCancelPanel.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                afterEditListener.call(null);
                dispose();
            }
        });
        if (readOnly || basicPropertiesPanel.getNameField().getText().trim().isEmpty()) {
            okCancelPanel.getOkButton().setEnabled(false);
        }
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
        final boolean successful = afterEditListener.call(role);
        if (successful) {
            dispose();
        }
    }

    private class OkButtonActionListener implements ActionListener {
        public void actionPerformed(final ActionEvent e) {
            // don't display name validation errors until after user clicks ok
            if (reservedNames.contains(basicPropertiesPanel.getNameField().getText().trim().toLowerCase())) {
                final String error = MessageFormat.format(RESOURCES.getString(ERROR_UNIQUE_NAME), operation.toLowerCase());
                DialogDisplayer.showMessageDialog(RolePropertiesDialog.this, error,
                        operation + " Role", JOptionPane.ERROR_MESSAGE, null);
            } else {
                save();
            }
        }
    }
}
