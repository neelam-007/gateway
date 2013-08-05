package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which displays a summary of the permissions that will be added.
 */
public class PermissionSummaryPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionSummaryPanel.class.getName());
    private static final String SUMMARY = "Summary";
    private JPanel contentPanel;
    private JPanel optionsPanel;
    private RolePermissionsPanel permissionsPanel;
    private JLabel applyToLabel;
    private JLabel restrictScopeLabel;
    private JLabel permittedOperationsLabel;

    public PermissionSummaryPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
    }

    @Override
    public String getStepLabel() {
        return SUMMARY;
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof AddPermissionsWizard.PermissionConfig) {
            final AddPermissionsWizard.PermissionConfig config = (AddPermissionsWizard.PermissionConfig) settings;
            if (config.getType() == EntityType.ANY) {
                applyToLabel.setText("All object types");
            } else {
                // TODO
            }

            final Set<String> ops = new HashSet<>(config.getOperations().size());
            for (final OperationType operationType : config.getOperations()) {
                ops.add(operationType.getName().toLowerCase());
            }
            permittedOperationsLabel.setText(StringUtils.join(ops, ", "));

            // start fresh
            config.getGeneratedPermissions().clear();
            if (config.isHasScope()) {
                restrictScopeLabel.setText("Objects matching a set of conditions");
                generateZonePermissions(config);
                generateFolderPermissions(config);
                generateAttributePermissions(config);
            } else {
                restrictScopeLabel.setText("All objects of the specified type");
                generateUnrestrictedPermissions(config);
            }
            permissionsPanel.configure(config.getGeneratedPermissions());
        } else {
            logger.log(Level.WARNING, "Cannot read settings because received invalid settings object: " + settings);
        }
    }

    private void generateAttributePermissions(final AddPermissionsWizard.PermissionConfig config) {
        if (!config.getAttributePredicates().isEmpty()) {
            for (final OperationType op : config.getOperations()) {
                final Permission permission = new Permission(config.getRole(), op, config.getType());
                for (final AttributePredicate attribute : config.getAttributePredicates()) {
                    final AttributePredicate predicate = new AttributePredicate(permission, attribute.getAttribute(), attribute.getValue());
                    predicate.setMode(attribute.getMode());
                    permission.getScope().add(predicate);
                }
                config.getGeneratedPermissions().add(permission);
            }
        }
    }

    private void generateUnrestrictedPermissions(final AddPermissionsWizard.PermissionConfig config) {
        for (final OperationType operationType : config.getOperations()) {
            final Permission unrestricted = new Permission(config.getRole(), operationType, config.getType());
            unrestricted.setScope(Collections.<ScopePredicate>emptySet());
            config.getGeneratedPermissions().add(unrestricted);
        }
    }

    private void generateFolderPermissions(final AddPermissionsWizard.PermissionConfig config) {
        for (final FolderHeader header : config.getSelectedFolders()) {
            try {
                final Folder folder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(header.getGoid());
                for (final OperationType op : config.getOperations()) {
                    final Permission folderPermission = new Permission(config.getRole(), op, config.getType());
                    final FolderPredicate predicate = new FolderPredicate(folderPermission, folder, config.isFolderTransitive());
                    folderPermission.getScope().add(predicate);
                    config.getGeneratedPermissions().add(folderPermission);
                }
                if (config.isFolderAncestry()) {
                    final Permission ancestryPermission = new Permission(config.getRole(), OperationType.READ, config.getType());
                    final EntityFolderAncestryPredicate predicate = new EntityFolderAncestryPredicate(ancestryPermission, EntityType.FOLDER, folder.getGoid());
                    ancestryPermission.getScope().add(predicate);
                    config.getGeneratedPermissions().add(ancestryPermission);
                }
            } catch (final FindException e) {
                logger.log(Level.WARNING, "Unable to retrieve folder from header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    private void generateZonePermissions(final AddPermissionsWizard.PermissionConfig config) {
        for (final SecurityZone zone : config.getSelectedZones()) {
            for (final OperationType op : config.getOperations()) {
                final Permission zonePermission = new Permission(config.getRole(), op, config.getType());
                final SecurityZonePredicate predicate = new SecurityZonePredicate(zonePermission, zone);
                zonePermission.getScope().add(predicate);
                config.getGeneratedPermissions().add(zonePermission);
            }
        }
    }
}
