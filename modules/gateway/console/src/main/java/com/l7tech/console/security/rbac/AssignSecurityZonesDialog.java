package com.l7tech.console.security.rbac;

import com.l7tech.console.policy.ConsoleAssertionRegistry;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Dialog for bulk assigning entities to security zones.
 */
public class AssignSecurityZonesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(AssignSecurityZonesDialog.class.getName());
    private static ResourceBundle RESOURCES = ResourceBundle.getBundle(AssignSecurityZonesDialog.class.getName());
    private static final String MAX_CHAR_NAME_DISPLAY = "max.char.name.display";
    private static final int CHECK_BOX_COL_INDEX = 0;
    private static final int NAME_COL_INDEX = 1;
    private static final int ZONE_COL_INDEX = 2;
    private static final int PATH_COL_INDEX = 3;
    private static final String NO_SECURITY_ZONE = RESOURCES.getString("no.zone.label");
    private JPanel contentPanel;
    private JComboBox typeComboBox;
    private JComboBox zoneComboBox;
    private JButton setBtn;
    private JButton closeBtn;
    private SelectableFilterableTablePanel tablePanel;
    private JPanel borderPanel;
    private JLabel setLabel;
    private CheckBoxSelectableTableModel<EntityHeader> dataModel;
    private Map<EntityType, List<SecurityZone>> entityTypes;
    private TableColumn pathColumn;
    // key = assertion access oid, value = class name
    private Map<Goid, String> assertionNames = new HashMap<>();

    /**
     * @param owner       owner of this Dialog.
     * @param entityTypes map where key = entity types that contain at least one modifiable entity and value = list of zones that can be set for the entity type
     */
    public AssignSecurityZonesDialog(@NotNull final Window owner, @NotNull final Map<EntityType, List<SecurityZone>> entityTypes) {
        super(owner, "Assign Security Zones", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        this.entityTypes = entityTypes;
        initBtns();
        initComboBoxes();
        setTypeSpecificLabels();
        initTable();
        loadTable();
        enableDisable();
    }

    private void initComboBoxes() {
        typeComboBox.setModel((new DefaultComboBoxModel<>(entityTypes.keySet().toArray(new EntityType[entityTypes.keySet().size()]))));
        typeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                if (value instanceof EntityType) {
                    value = ((EntityType) value).getName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setTypeSpecificLabels();
                loadTable();
                reloadZoneComboBox();
                enableDisable();
                if (dataModel != null) {
                    final EntityType type = getSelectedEntityType();
                    tablePanel.configure(dataModel, new int[]{NAME_COL_INDEX}, new int[]{NAME_COL_INDEX}, new boolean[]{true}, new Comparator[]{null}, type == null ? null : type.getPluralName().toLowerCase());
                }
            }
        });
        typeComboBox.setSelectedItem(null);
        zoneComboBox.setModel(new DefaultComboBoxModel<>(new SecurityZone[]{}));
        zoneComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof SecurityZone) {
                    value = SecurityZoneUtil.getSecurityZoneName((SecurityZone) value, SecurityZoneUtil.getIntFromResource(RESOURCES, MAX_CHAR_NAME_DISPLAY));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    private void reloadZoneComboBox() {
        final EntityType selectedEntityType = getSelectedEntityType();
        if (selectedEntityType != null) {
            final List<SecurityZone> zonesForEntityType = entityTypes.get(selectedEntityType);
            zoneComboBox.setModel(new DefaultComboBoxModel<>(zonesForEntityType.toArray(new SecurityZone[zonesForEntityType.size()])));
        }
    }

    private void setTypeSpecificLabels() {
        final EntityType selectedEntityType = getSelectedEntityType();
        final String type;
        if (selectedEntityType != null) {
            type = selectedEntityType.getPluralName().toLowerCase();
        } else {
            type = "entities";
        }
        borderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Available " + type));
        setLabel.setText("Set zone on selected " + type);
    }

    private void initBtns() {
        getRootPane().setDefaultButton(closeBtn);
        closeBtn.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeBtn);
        setBtn.addActionListener(new BulkUpdateActionListener());
    }

    private void initTable() {
        dataModel = TableUtil.configureSelectableTable(tablePanel.getSelectableTable(), CHECK_BOX_COL_INDEX,
                column(StringUtils.EMPTY, 30, 30, 99999, new Functions.Unary<Boolean, EntityHeader>() {
                    @Override
                    public Boolean call(final EntityHeader header) {
                        return dataModel.isSelected(header);
                    }
                }),
                column("Name", 30, 250, 99999, new Functions.Unary<String, EntityHeader>() {
                    @Override
                    public String call(final EntityHeader header) {
                        String name = "unavailable";
                        try {
                            name = Registry.getDefault().getEntityNameResolver().getNameForHeader(header, false);
                        } catch (final FindException | PermissionDeniedException e) {
                            logger.log(Level.WARNING, "Error resolving name for header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return name;
                    }
                }),
                column("Current Zone", 30, 250, 99999, new Functions.Unary<String, EntityHeader>() {
                    @Override
                    public String call(final EntityHeader header) {
                        String zoneName = StringUtils.EMPTY;
                        if (header instanceof HasSecurityZoneId) {
                            final HasSecurityZoneId zoneable = (HasSecurityZoneId) header;
                            final SecurityZone zone = SecurityZoneUtil.getSecurityZoneByGoid(zoneable.getSecurityZoneId());
                            if (zone != null) {
                                zoneName = zone.getName();
                            } else {
                                zoneName = NO_SECURITY_ZONE;
                            }
                        }
                        return zoneName;
                    }
                }),
                column("Path", 30, 150, 99999, new Functions.Unary<String, EntityHeader>() {
                    @Override
                    public String call(final EntityHeader header) {
                        String path = "unavailable";
                        try {
                            path = getPathForHeader(header);
                        } catch (final FindException | PermissionDeniedException e) {
                            logger.log(Level.WARNING, "Error resolving path for header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return path;
                    }
                }));
        pathColumn = tablePanel.getSelectableTable().getColumnModel().getColumn(PATH_COL_INDEX);
        dataModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                enableDisable();
            }
        });
        tablePanel.configure(dataModel, new int[]{NAME_COL_INDEX}, null);
    }

    @Nullable
    private SecurityZone getSelectedZone() {
        SecurityZone selectedZone = null;
        final Object selected = zoneComboBox.getSelectedItem();
        if (selected != null) {
            selectedZone = (SecurityZone) selected;
        }
        return selectedZone;
    }

    @Nullable
    private EntityType getSelectedEntityType() {
        final Object selected = typeComboBox.getSelectedItem();
        if (selected != null) {
            return (EntityType) selected;
        }
        return null;
    }

    private void loadTable() {
        EntityType selected = getSelectedEntityType();
        if (selected != null) {
            selected = convertToBackingEntityType(selected);
            try {
                final EntityHeaderSet<EntityHeader> entities = EntityUtils.getEntities(selected);
                final List<EntityHeader> headers = new ArrayList<>();
                for (final EntityHeader header : entities) {
                    if (header instanceof PolicyHeader) {
                        final PolicyHeader policy = (PolicyHeader) header;
                        if (PolicyType.PRIVATE_SERVICE == policy.getPolicyType() || !policy.getPolicyType().isSecurityZoneable()) {
                            // don't show service policies or non-zoneable policies
                            continue;
                        }
                    }

                    if (header.getType() == EntityType.ASSERTION_ACCESS) {
                        final String assertionClassName = header.getName();
                        if (EncapsulatedAssertion.class.getName().equals(assertionClassName)) {
                            // encapsulated assertions are handled by their config entity type
                            continue;
                        }
                        assertionNames.put(header.getGoid(), assertionClassName);
                    }
                    headers.add(header);
                }
                dataModel.setSelectableObjects(headers);
                boolean showPath = !headers.isEmpty() && (headers.get(0) instanceof HasFolderId || headers.get(0).getType() == EntityType.ASSERTION_ACCESS);
                showHidePathColumn(showPath);
            } catch (final FindException ex) {
                final String error = "Error retrieving entities of type " + selected;
                logger.log(Level.WARNING, error, ExceptionUtils.getDebugException(ex));
                DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private String getPathForHeader(final EntityHeader header) throws FindException {
        String path = StringUtils.EMPTY;
        final EntityNameResolver entityNameResolver = Registry.getDefault().getEntityNameResolver();
        final ConsoleAssertionRegistry assertionRegistry = TopComponents.getInstance().getAssertionRegistry();
        try {
            if (header instanceof HasFolderId) {
                path = entityNameResolver.getPath((HasFolderId) header);
            } else if (header.getType() == EntityType.ASSERTION_ACCESS) {
                final String assname = assertionNames.get(header.getGoid());
                final Assertion assertion = assname == null ? null : assertionRegistry.findByClassName(assname);
                if (assertion != null) {
                    path = entityNameResolver.getPaletteFolders(assertion);
                }
            }
        } catch (final PermissionDeniedException e) {
            logger.log(Level.WARNING, "Unable to determine path for header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            path = "path unavailable";
        }
        return path;
    }

    private EntityType convertToBackingEntityType(final EntityType selected) {
        EntityType ret = selected;
        if (ret == EntityType.SSG_KEY_ENTRY) {
            ret = EntityType.SSG_KEY_METADATA;
        }
        return ret;
    }

    private void showHidePathColumn(final boolean show) {
        final boolean pathColumnVisible = tablePanel.getSelectableTable().getColumnCount() > PATH_COL_INDEX;
        if (!show && pathColumnVisible) {
            tablePanel.getSelectableTable().getColumnModel().removeColumn(pathColumn);
        } else if (show && !pathColumnVisible) {
            tablePanel.getSelectableTable().getColumnModel().addColumn(pathColumn);
        }
    }

    private void enableDisable() {
        final boolean hasRows = tablePanel.getSelectableTable().getModel().getRowCount() > 0;
        setBtn.setEnabled(hasRows && getSelectedZone() != null && !dataModel.getSelected().isEmpty());
    }

    private class BulkUpdateActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final EntityType selectedEntityType = convertToBackingEntityType(getSelectedEntityType());
            SecurityZone selectedZone = getSelectedZone();
            if (selectedZone != null && selectedZone.equals(SecurityZoneUtil.getNullZone())) {
                selectedZone = null;
            }
            final List<EntityHeader> selectedEntities = dataModel.getSelected();
            if (selectedEntityType == EntityType.ASSERTION_ACCESS) {
                final Set<EntityHeader> assertionAccessToUpdate = new HashSet<>();
                try {
                    final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                    for (final EntityHeader header : selectedEntities) {
                        if (PersistentEntity.DEFAULT_GOID.equals(header.getGoid()) || GoidRange.WRAPPED_OID.isInRange(header.getGoid())) {
                            final int rowIndex = dataModel.getRowIndexForSelectableObject(header);
                            // save a new assertion access
                            final String assertionClassName = assertionNames.get(header.getGoid());
                            final AssertionAccess assertionAccess = new AssertionAccess(assertionClassName);
                            assertionAccess.setSecurityZone(selectedZone);
                            final Goid savedGoid = rbacAdmin.saveAssertionAccess(assertionAccess);
                            assertionNames.remove(header.getGoid());
                            assertionNames.put(savedGoid, assertionClassName);
                            header.setGoid(savedGoid);
                            setZoneOnHeader(selectedZone, header);
                            dataModel.fireTableRowsUpdated(rowIndex, rowIndex);
                        } else {
                            // update an existing assertion access
                            assertionAccessToUpdate.add(header);
                        }
                    }
                    doBulkUpdate(EntityType.ASSERTION_ACCESS, selectedZone, assertionAccessToUpdate);
                } catch (final UpdateException ex) {
                    DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Error assigning entities to zone.", ex);
                }
            } else if (selectedEntityType == EntityType.SSG_KEY_METADATA) {
                final Set<EntityHeader> metadataToUpdate = new HashSet<>();
                try {
                    final TrustedCertAdmin trustedCertManager = Registry.getDefault().getTrustedCertManager();
                    for (final EntityHeader header : selectedEntities) {
                        if (GoidRange.ZEROED_PREFIX.isInRange(header.getGoid()) && header.getGoid().getLow() < 0) {
                            final int rowIndex = dataModel.getRowIndexForSelectableObject(header);
                            // save new key metadata
                            if (header instanceof KeyMetadataHeaderWrapper) {
                                final KeyMetadataHeaderWrapper keyHeader = (KeyMetadataHeaderWrapper) header;
                                final SsgKeyMetadata metadata = new SsgKeyMetadata(keyHeader.getKeystoreOid(), keyHeader.getAlias(), selectedZone);
                                final Goid savedGoid = trustedCertManager.saveOrUpdateMetadata(metadata);
                                header.setGoid(savedGoid);
                                setZoneOnHeader(selectedZone, header);
                                dataModel.fireTableRowsUpdated(rowIndex, rowIndex);
                            }
                        } else {
                            // update an existing key metadata
                            metadataToUpdate.add(header);
                        }
                    }
                    doBulkUpdate(EntityType.SSG_KEY_METADATA, selectedZone, metadataToUpdate);
                } catch (final SaveException ex) {
                    DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Error assigning entities to zone.", ex);
                }
            } else {
                doBulkUpdate(selectedEntityType, selectedZone, selectedEntities);
            }
        }

        /**
         * @param selectedEntityType the selected EntityType
         * @param selectedZone       the selected SecurityZone or null if no zone selected
         * @param entitiesToUpdate   the entities to update in the database
         */
        private void doBulkUpdate(@NotNull final EntityType selectedEntityType, @Nullable final SecurityZone selectedZone, @NotNull final Collection<EntityHeader> entitiesToUpdate) {
            final Goid securityZoneGoid = selectedZone == null ? null : selectedZone.getGoid();
            try {
                final BulkZoneUpdater updater = new BulkZoneUpdater(Registry.getDefault().getRbacAdmin(),
                        Registry.getDefault().getUDDIRegistryAdmin(), Registry.getDefault().getJmsManager(), Registry.getDefault().getPolicyAdmin());
                updater.bulkUpdate(securityZoneGoid, selectedEntityType, entitiesToUpdate);

                for (final EntityHeader header : entitiesToUpdate) {
                    setZoneOnHeader(selectedZone, header);
                }
                dataModel.fireTableDataChanged();

                final RootNode rootNode = TopComponents.getInstance().getRootNode();
                switch (selectedEntityType) {
                    case ASSERTION_ACCESS:
                        TopComponents.getInstance().getAssertionRegistry().updateAssertionAccess();
                        break;
                    case ID_PROVIDER_CONFIG:
                        IdentityProvidersTree providersTree = (IdentityProvidersTree) TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
                        providersTree.refresh(providersTree.getRootNode());
                        break;
                    case ENCAPSULATED_ASSERTION:
                        TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
                        break;
                    case FOLDER:
                        for (final EntityHeader header : entitiesToUpdate) {
                            if (Folder.ROOT_FOLDER_ID.equals(header.getGoid())) {
                                rootNode.setSecurityZone(selectedZone);
                                break;
                            }
                        }
                    case SERVICE:
                    case POLICY:
                        final ServicesAndPoliciesTree servicesAndPoliciesTree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                        servicesAndPoliciesTree.refresh(rootNode);
                    default:
                        // no reload necessary
                }
            } catch (final FindException ex) {
                DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Error assigning entities to zone.", ex);
            } catch (final UpdateException ex) {
                DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Unable to assign entities to zone.", ex);
            }
        }

        private void setZoneOnHeader(final SecurityZone selectedZone, final EntityHeader header) {
            if (header instanceof HasSecurityZoneId) {
                final HasSecurityZoneId zoneable = (HasSecurityZoneId) header;
                zoneable.setSecurityZoneId(selectedZone == null ? null : selectedZone.getGoid());
            }
        }
    }
}
