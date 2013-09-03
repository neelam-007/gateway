package com.l7tech.console.security.rbac;

import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only properties dialog for a PermissionGroup.
 */
public class PermissionGroupPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(PermissionGroupPropertiesDialog.class.getName());
    private static final String PERMISSION_GROUP_PROPERTIES = "Permission Group Properties";
    private static final String ALL = "<ALL>";
    private static final String SEPARATOR = ", ";
    private JPanel contentPanel;
    private JTextArea scopeTextArea;
    private JButton closeButton;
    private JLabel typeLabel;
    private JLabel opsLabel;

    public PermissionGroupPropertiesDialog(@NotNull final Window owner, @NotNull final PermissionGroup group) {
        super(owner, PERMISSION_GROUP_PROPERTIES, DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscAction(this, closeButton);
        typeLabel.setText(group.getEntityType() == EntityType.ANY ? ALL : group.getEntityType().getName());
        scopeTextArea.setText(getScopeDescription(group));
        final Set<String> operations = new TreeSet<>();
        boolean containsOther = false;
        for (final OperationType operationType : group.getOperations()) {
            if (operationType != OperationType.OTHER) {
                operations.add(operationType.getName().toLowerCase());
            } else {
                containsOther = true;
            }
        }
        if (containsOther) {
            operations.addAll(group.getOtherOperations());
        }
        opsLabel.setText(StringUtils.join(operations, SEPARATOR));
    }

    /**
     * @param group the PermissionGroup for which to get a description of its scope.
     * @return a comma-separated description of each scope predicate in the PermissionGroup.
     */
    public static String getScopeDescription(@NotNull final PermissionGroup group) {
        final String scopeDescription;
        final Set<ScopePredicate> scope = group.getScope();
        if (!scope.isEmpty()) {
            final Set<String> predicateDescriptions = new TreeSet<>();
            final EntityNameResolver nameResolver = Registry.getDefault().getEntityNameResolver();
            for (final ScopePredicate predicate : scope) {
                try {
                    predicateDescriptions.add(nameResolver.getNameForEntity(predicate, true));
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Unable to determine name for predicate " + predicate + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
            scopeDescription = StringUtils.join(predicateDescriptions.toArray(), SEPARATOR);
        } else {
            scopeDescription = ALL;
        }
        return scopeDescription;
    }
}
