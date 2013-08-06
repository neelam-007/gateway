package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
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
                generateScopedPermissions(config);
            } else {
                restrictScopeLabel.setText("All objects of the specified type");
                generateUnrestrictedPermissions(config);
            }
            permissionsPanel.configure(config.getGeneratedPermissions());
        } else {
            logger.log(Level.WARNING, "Cannot read settings because received invalid settings object: " + settings);
        }
    }

    private void generateScopedPermissions(final AddPermissionsWizard.PermissionConfig config) {
        final FolderAdmin folderAdmin = Registry.getDefault().getFolderAdmin();
        for (final OperationType op : config.getOperations()) {
            // permission created for each zone-folder pair
            for (final SecurityZone zone : config.getSelectedZones()) {
                for (final FolderHeader folderHeader : config.getSelectedFolders()) {
                    try {
                        final Folder folder = folderAdmin.findByPrimaryKey(folderHeader.getGoid());
                        if (folder != null) {
                            final Permission permission = new Permission(config.getRole(), op, config.getType());
                            permission.getScope().add(new SecurityZonePredicate(permission, zone));
                            permission.getScope().add(new FolderPredicate(permission, folder, config.isFolderTransitive()));
                            if (config.isFolderAncestry()) {
                                permission.getScope().add(new EntityFolderAncestryPredicate(permission, EntityType.FOLDER, folder.getGoid()));
                            }
                            for (final AttributePredicate attribute : config.getAttributePredicates()) {
                                final AttributePredicate attributePredicate = new AttributePredicate(permission, attribute.getAttribute(), attribute.getValue());
                                attributePredicate.setMode(attribute.getMode());
                                permission.getScope().add(attributePredicate);
                            }
                            config.getGeneratedPermissions().add(permission);
                        } else {
                            throw new FindException("No folder exists with goid: " + folderHeader.getGoid());
                        }
                    } catch (final FindException e) {
                        logger.log(Level.WARNING, "Skipping permission because unable to retrieve folder for header: " + folderHeader);
                    }

                }
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
}
