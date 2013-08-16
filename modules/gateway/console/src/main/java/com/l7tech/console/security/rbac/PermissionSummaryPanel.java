package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which displays a summary of the permissions that will be added.
 */
public class PermissionSummaryPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionSummaryPanel.class.getName());
    private static final String SUMMARY = "Summary";
    private static final String NAME = "name";
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
        if (settings instanceof PermissionsConfig) {
            final PermissionsConfig config = (PermissionsConfig) settings;
            if (config.getType() == EntityType.ANY) {
                applyToLabel.setText("All object types");
            } else {
                applyToLabel.setText(config.getType().getPluralName());
            }

            final Set<String> ops = new HashSet<>(config.getOperations().size());
            for (final OperationType operationType : config.getOperations()) {
                ops.add(operationType.getName().toLowerCase());
            }
            permittedOperationsLabel.setText(StringUtils.join(ops, ", "));

            if (!config.hasScope()) {
                restrictScopeLabel.setText("All objects of the specified type");
            } else if (config.getScopeType() == PermissionsConfig.ScopeType.CONDITIONAL) {
                restrictScopeLabel.setText("Objects matching a set of conditions");
            } else if (config.getScopeType() == PermissionsConfig.ScopeType.SPECIFIC_OBJECTS) {
                restrictScopeLabel.setText("A set of specific objects");
            }
            generatePermissions(config, Registry.getDefault().getFolderAdmin());
            permissionsPanel.configure(config.getGeneratedPermissions());
        } else {
            logger.log(Level.WARNING, "Cannot read settings because received invalid settings object: " + settings);
        }
    }

    static void generatePermissions(@NotNull final PermissionsConfig config, @NotNull FolderAdmin folderAdmin) {
        // start fresh
        config.getGeneratedPermissions().clear();
        if (config.hasScope()) {
            generateScopedPermissions(config, folderAdmin);
        } else {
            generateUnrestrictedPermissions(config);
        }
    }

    private static void generateScopedPermissions(final PermissionsConfig config, final FolderAdmin folderAdmin) {
        switch (config.getScopeType()) {
            case CONDITIONAL:
                final Map<Goid, Folder> retrievedFolders = new HashMap<>();
                if (config.isFolderAncestry()) {
                    // provide read access for ancestry of each selected folder, the selected folder itself and its subfolders
                    for (final FolderHeader folderHeader : config.getSelectedFolders()) {
                        try {
                            final Folder folder = folderAdmin.findByPrimaryKey(folderHeader.getGoid());
                            if (folder == null) {
                                throw new FindException("No folder exists with goid: " + folderHeader.getGoid());
                            } else {
                                retrievedFolders.put(folderHeader.getGoid(), folder);
                                final Permission ancestryPermission = new Permission(config.getRole(), OperationType.READ, EntityType.FOLDER);
                                ancestryPermission.getScope().add(new EntityFolderAncestryPredicate(ancestryPermission, EntityType.FOLDER, folderHeader.getGoid()));
                                config.getGeneratedPermissions().add(ancestryPermission);

                                final Permission readFolderPermission = new Permission(config.getRole(), OperationType.READ, EntityType.FOLDER);
                                final ObjectIdentityPredicate specificFolderPredicate = new ObjectIdentityPredicate(readFolderPermission, folderHeader.getStrId());
                                specificFolderPredicate.setHeader(folderHeader);
                                readFolderPermission.getScope().add(specificFolderPredicate);
                                config.getGeneratedPermissions().add(readFolderPermission);

                                final Permission readSubfoldersPermission = new Permission(config.getRole(), OperationType.READ, EntityType.FOLDER);
                                readSubfoldersPermission.getScope().add(new FolderPredicate(readSubfoldersPermission, folder, true));
                                config.getGeneratedPermissions().add(readSubfoldersPermission);
                            }
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Skipping ancestry permissions because unable to retrieve folder for header: " + folderHeader);
                        }
                    }
                }
                for (final OperationType op : config.getOperations()) {
                    if (config.getSelectedFolders().isEmpty()) {
                        // no folders were selected
                        config.getSelectedFolders().add(null);
                    }
                    if (config.getSelectedZones().isEmpty()) {
                        // no zones were selected
                        config.getSelectedZones().add(null);
                    }
                    // permission created for each zone-folder pair
                    for (final SecurityZone zone : config.getSelectedZones()) {
                        for (final FolderHeader folderHeader : config.getSelectedFolders()) {
                            try {
                                final Permission permission = new Permission(config.getRole(), op, config.getType());
                                if (zone != null) {
                                    permission.getScope().add(new SecurityZonePredicate(permission, zone.equals(SecurityZoneUtil.getNullZone()) ? null : zone));
                                }
                                if (folderHeader != null) {
                                    final Folder folder;
                                    if (retrievedFolders.containsKey(folderHeader.getGoid())) {
                                        folder = retrievedFolders.get(folderHeader.getGoid());
                                    } else {
                                        folder = folderAdmin.findByPrimaryKey(folderHeader.getGoid());
                                    }
                                    if (folder != null) {
                                        permission.getScope().add(new FolderPredicate(permission, folder, config.isFolderTransitive()));
                                    } else {
                                        throw new FindException("No folder exists with goid: " + folderHeader.getGoid());
                                    }
                                }
                                for (final AttributePredicate attribute : config.getAttributePredicates()) {
                                    final AttributePredicate attributePredicate = new AttributePredicate(permission, attribute.getAttribute(), attribute.getValue());
                                    attributePredicate.setMode(attribute.getMode());
                                    permission.getScope().add(attributePredicate);
                                }
                                config.getGeneratedPermissions().add(permission);
                            } catch (final FindException e) {
                                logger.log(Level.WARNING, "Skipping permission because unable to retrieve folder for header: " + folderHeader);
                            }

                        }
                    }

                }
                break;
            case SPECIFIC_OBJECTS:
                for (final OperationType op : config.getOperations()) {
                    for (final EntityHeader header : config.getSelectedEntities()) {
                        final Permission specificEntityPermission = new Permission(config.getRole(), op, config.getType());
                        final ScopePredicate specificPredicate;
                        if (config.getType() == EntityType.ASSERTION_ACCESS) {
                            specificPredicate = new AttributePredicate(specificEntityPermission, NAME, header.getName());
                        } else {
                            specificPredicate = new ObjectIdentityPredicate(specificEntityPermission, header.getStrId());
                            ((ObjectIdentityPredicate) specificPredicate).setHeader(header);
                        }
                        specificEntityPermission.getScope().add(specificPredicate);
                        config.getGeneratedPermissions().add(specificEntityPermission);
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Scope type not supported: " + config.getScopeType());
        }
    }

    private static void generateUnrestrictedPermissions(final PermissionsConfig config) {
        for (final OperationType operationType : config.getOperations()) {
            final Permission unrestricted = new Permission(config.getRole(), operationType, config.getType());
            unrestricted.setScope(Collections.<ScopePredicate>emptySet());
            config.getGeneratedPermissions().add(unrestricted);
        }
    }
}
