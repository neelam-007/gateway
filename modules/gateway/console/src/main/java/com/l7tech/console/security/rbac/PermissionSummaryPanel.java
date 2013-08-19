package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.ExceptionUtils;
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
    private static final String PROVIDER_ID = "providerId";
    private static final String ID = "id";
    private static final String ALL_OBJECT_TYPES = "All object types";
    private static final String ALL_OF_TYPE_LABEL = "All objects of the specified type";
    private static final String CONDITIONS_LABEL = "Objects matching a set of conditions";
    private static final String SPECIFIC_OBJECTS_LABEL = "A set of specific objects";
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
                applyToLabel.setText(ALL_OBJECT_TYPES);
            } else {
                applyToLabel.setText(config.getType().getPluralName());
            }

            final Set<String> ops = new HashSet<>(config.getOperations().size());
            for (final OperationType operationType : config.getOperations()) {
                ops.add(operationType.getName().toLowerCase());
            }
            permittedOperationsLabel.setText(StringUtils.join(ops, ", "));

            if (!config.hasScope()) {
                restrictScopeLabel.setText(ALL_OF_TYPE_LABEL);
            } else if (config.getScopeType() == PermissionsConfig.ScopeType.CONDITIONAL) {
                restrictScopeLabel.setText(CONDITIONS_LABEL);
            } else if (config.getScopeType() == PermissionsConfig.ScopeType.SPECIFIC_OBJECTS) {
                restrictScopeLabel.setText(SPECIFIC_OBJECTS_LABEL);
            }
            generatePermissions(config, Registry.getDefault().getFolderAdmin(), Registry.getDefault().getJmsManager());
            permissionsPanel.configure(config.getGeneratedPermissions());
        } else {
            logger.log(Level.WARNING, "Cannot read settings because received invalid settings object: " + settings);
        }
    }

    static void generatePermissions(@NotNull final PermissionsConfig config, @NotNull final FolderAdmin folderAdmin, @NotNull final JmsAdmin jmsAdmin) {
        // start fresh
        config.getGeneratedPermissions().clear();
        if (config.hasScope()) {
            generateScopedPermissions(config, folderAdmin, jmsAdmin);
        } else {
            generateUnrestrictedPermissions(config);
        }
    }

    private static void generateScopedPermissions(final PermissionsConfig config, final FolderAdmin folderAdmin, final JmsAdmin jmsAdmin) {
        final EntityType entityType = config.getType();
        switch (config.getScopeType()) {
            case CONDITIONAL:
                final Map<Goid, Folder> retrievedFolders = new HashMap<>();
                if (config.isGrantReadFolderAncestry()) {
                    // provide read access for ancestry of each selected folder, the selected folder itself and its subfolders
                    for (final FolderHeader folderHeader : config.getSelectedFolders()) {
                        try {
                            final Folder folder = folderAdmin.findByPrimaryKey(folderHeader.getGoid());
                            if (folder == null) {
                                throw new FindException("No folder exists with goid: " + folderHeader.getGoid());
                            } else {
                                retrievedFolders.put(folderHeader.getGoid(), folder);
                                config.getGeneratedPermissions().add(createReadFolderAncestryPermission(config, folderHeader));

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
                                final Permission permission = new Permission(config.getRole(), op, entityType);
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
                for (final EntityHeader header : config.getSelectedEntities()) {
                    for (final OperationType op : config.getOperations()) {
                        final Permission specificEntityPermission = new Permission(config.getRole(), op, entityType);
                        final Set<ScopePredicate> scope = new HashSet<>();
                        if (entityType == EntityType.ASSERTION_ACCESS ||
                                entityType == EntityType.CLUSTER_PROPERTY ||
                                entityType == EntityType.SERVICE_TEMPLATE) {
                            scope.add(new AttributePredicate(specificEntityPermission, NAME, header.getName()));
                        } else if ((entityType == EntityType.USER || entityType == EntityType.GROUP) && header instanceof IdentityHeader) {
                            final IdentityHeader identityHeader = (IdentityHeader) header;
                            scope.add(new AttributePredicate(specificEntityPermission, PROVIDER_ID, identityHeader.getProviderGoid().toHexString()));
                            scope.add(new AttributePredicate(specificEntityPermission, ID, identityHeader.getStrId()));
                        } else {
                            final ObjectIdentityPredicate specificPredicate = new ObjectIdentityPredicate(specificEntityPermission, header.getStrId());
                            specificPredicate.setHeader(header);
                            scope.add(specificPredicate);
                        }
                        specificEntityPermission.getScope().addAll(scope);
                        config.getGeneratedPermissions().add(specificEntityPermission);

                        if (entityType == EntityType.JMS_ENDPOINT && config.isGrantAdditionalJmsAccess() && header instanceof JmsEndpointHeader) {
                            // grant additional access to the connections associated with the selected endpoint
                            final JmsEndpointHeader endpointHeader = (JmsEndpointHeader) header;
                            if (endpointHeader.getConnectionGoid() != null) {
                                final Permission jmsConnectionPermission = new Permission(config.getRole(), op, EntityType.JMS_CONNECTION);
                                final ObjectIdentityPredicate jmsConnectionPredicate = new ObjectIdentityPredicate(jmsConnectionPermission, endpointHeader.getConnectionGoid().toHexString());
                                // header is just for display purposes
                                jmsConnectionPredicate.setHeader(new EntityHeader(endpointHeader.getConnectionGoid().toHexString(), EntityType.JMS_CONNECTION, "connection for endpoint " + endpointHeader.getName(), null));
                                jmsConnectionPermission.getScope().add(jmsConnectionPredicate);
                                config.getGeneratedPermissions().add(jmsConnectionPermission);
                            } else {
                                logger.log(Level.WARNING, "Cannot add permission for jms connection because endpoint's jms connection goid is null.");
                            }
                        }

                        if (entityType == EntityType.JMS_CONNECTION && config.isGrantAdditionalJmsAccess()) {
                            // grant additional access to the endpoints associated with the selected connection
                            try {
                                final JmsEndpoint[] endpoints = jmsAdmin.getEndpointsForConnection(header.getGoid());
                                if (endpoints != null) {
                                    for (final JmsEndpoint endpoint : endpoints) {
                                        final Permission jmsEndpointPermission = new Permission(config.getRole(), op, EntityType.JMS_ENDPOINT);
                                        final ObjectIdentityPredicate jmsEndpointPredicate = new ObjectIdentityPredicate(jmsEndpointPermission, endpoint.getGoid().toHexString());
                                        jmsEndpointPredicate.setHeader(new EntityHeader(endpoint.getGoid(), EntityType.JMS_ENDPOINT, endpoint.getName(), null));
                                        jmsEndpointPermission.getScope().add(jmsEndpointPredicate);
                                        config.getGeneratedPermissions().add(jmsEndpointPermission);
                                    }
                                }
                            } catch (final FindException | PermissionDeniedException e) {
                                logger.log(Level.WARNING, "Unable to retrieve jms endpoints for connection with goid " + header.getGoid() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            }
                        }
                    }
                    if (entityType.isFolderable() && config.isGrantReadSpecificFolderAncestry()) {
                        config.getGeneratedPermissions().add(createReadFolderAncestryPermission(config, header));
                    }
                    if (header instanceof AliasHeader && config.isGrantReadAliasOwningEntities()) {
                        final AliasHeader aliasHeader = (AliasHeader) header;
                        if (aliasHeader.getAliasedEntityId() != null && aliasHeader.getAliasedEntityType() != null) {
                            final Permission readOwningEntityPermission = new Permission(config.getRole(), OperationType.READ, aliasHeader.getAliasedEntityType());
                            final String owningEntityId = aliasHeader.getAliasedEntityId().toHexString();
                            final ObjectIdentityPredicate identityPredicate = new ObjectIdentityPredicate(readOwningEntityPermission, owningEntityId);
                            identityPredicate.setHeader(new EntityHeader(owningEntityId, aliasHeader.getAliasedEntityType(), null, null));
                            readOwningEntityPermission.getScope().add(identityPredicate);
                            config.getGeneratedPermissions().add(readOwningEntityPermission);
                        } else {
                            logger.log(Level.WARNING, "Cannot add read owning entity permission for alias " + aliasHeader + " because either aliased entity id or type is null.");
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Scope type not supported: " + config.getScopeType());
        }
    }

    /**
     * @return a Permission which allows user to read all parent folders of the given entity.
     */
    private static Permission createReadFolderAncestryPermission(final PermissionsConfig config, final EntityHeader header) {
        final Permission ancestryPermission = new Permission(config.getRole(), OperationType.READ, EntityType.FOLDER);
        ancestryPermission.getScope().add(new EntityFolderAncestryPredicate(ancestryPermission, header.getType(), header.getGoid()));
        return ancestryPermission;
    }

    private static void generateUnrestrictedPermissions(final PermissionsConfig config) {
        for (final OperationType operationType : config.getOperations()) {
            final Permission unrestricted = new Permission(config.getRole(), operationType, config.getType());
            unrestricted.setScope(Collections.<ScopePredicate>emptySet());
            config.getGeneratedPermissions().add(unrestricted);
        }
    }
}
