package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.policy.ConsoleAssertionRegistry;
import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.objectmodel.folder.HasFolderOid;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for bulk assigning entities to security zones.
 */
public class AssignSecurityZonesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(AssignSecurityZonesDialog.class.getName());
    private static final int HEADER_COL_INDEX = 1;
    private static final int ZONE_COL_INDEX = 2;
    private static final int CHECK_BOX_COL_INDEX = 0;
    private static final int PATH_COL_INDEX = 3;
    private static final String NO_SECURITY_ZONE = "(no security zone)";
    private JPanel contentPanel;
    private JComboBox typeComboBox;
    private JPanel entitiesPanel;
    private JTable entitiesTable;
    private JButton selectAllBtn;
    private JButton selectNoneBtn;
    private JPanel btnPanel;
    private JComboBox zoneComboBox;
    private JButton setBtn;
    private JButton closeBtn;
    private JLabel filterLabel;
    private JLabel selectedLabel;
    private FilterPanel filterPanel;
    private JPanel noEntitiesPanel;
    private JLabel noEntitiesLabel;
    private JScrollPane scrollPane;
    private EntitiesTableModel dataModel = new EntitiesTableModel(new String[]{"", "Name", "Current Zone", "Path"}, new Object[][]{});
    private Map<EntityType, List<SecurityZone>> entityTypes;
    private TableColumn pathColumn;
    // key = assertion access oid, value = class name
    private Map<Long, String> assertionNames = new HashMap<>();

    /**
     * @param owner       owner of this Dialog.
     * @param entityTypes map where key = entity types that contain at least one modifiable entity and value = list of zones that can be set for the entity type
     */
    public AssignSecurityZonesDialog(@NotNull final Window owner, @NotNull final Map<EntityType, List<SecurityZone>> entityTypes) {
        super(owner, "Assign Security Zones");
        setContentPane(contentPanel);
        this.entityTypes = entityTypes;
        initBtns();
        initComboBoxes();
        setPanelTitle();
        initTable();
        loadTable();
        loadCount();
        initFiltering();
        enableDisable();
    }

    private void initFiltering() {
        filterPanel.attachRowSorter(((TableRowSorter) entitiesTable.getRowSorter()), new int[]{HEADER_COL_INDEX});
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
                setButtonTexts();
            }
        });
    }

    private void setButtonTexts() {
        final String label = filterPanel.isFiltered() ? "visible" : "all";
        selectAllBtn.setText("select " + label);
        selectNoneBtn.setText("clear " + label);
    }

    private void loadCount() {
        final EntityType selectedEntityType = getSelectedEntityType();
        if (selectedEntityType != null) {
            final int showCount = entitiesTable.getRowCount();
            final int total = entitiesTable.getModel().getRowCount();
            filterLabel.setText("showing " + showCount + " of " + total + " " + selectedEntityType.getPluralName().toLowerCase());
        } else {
            filterLabel.setText(StringUtils.EMPTY);
        }
    }

    private void initComboBoxes() {
        typeComboBox.setModel((new DefaultComboBoxModel<EntityType>(entityTypes.keySet().toArray(new EntityType[entityTypes.keySet().size()]))));
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
                setPanelTitle();
                loadTable();
                loadCount();
                reloadZoneComboBox();
                enableDisable();
            }
        });
        typeComboBox.setSelectedItem(null);
        zoneComboBox.setModel(new DefaultComboBoxModel<>(new SecurityZone[]{}));
        zoneComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof SecurityZone) {
                    value = ((SecurityZone) value).getName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    private void reloadZoneComboBox() {
        final EntityType selectedEntityType = getSelectedEntityType();
        if (selectedEntityType != null) {
            final List<SecurityZone> zonesForEntityType = entityTypes.get(selectedEntityType);
            zoneComboBox.setModel(new DefaultComboBoxModel<SecurityZone>(zonesForEntityType.toArray(new SecurityZone[zonesForEntityType.size()])));
        }
    }

    private void setPanelTitle() {
        final EntityType selectedEntityType = getSelectedEntityType();
        if (selectedEntityType != null) {
            entitiesPanel.setBorder(BorderFactory.createTitledBorder("Available " + selectedEntityType.getPluralName()));
        } else {
            entitiesPanel.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void initBtns() {
        getRootPane().setDefaultButton(closeBtn);
        Utilities.setEscAction(this, closeBtn);
        Utilities.buttonToLink(selectAllBtn);
        Utilities.buttonToLink(selectNoneBtn);
        selectAllBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                for (int i = 0; i < dataModel.getRowCount(); i++) {
                    // check all visible rows
                    if (!filterPanel.isFiltered() || entitiesTable.getRowSorter().convertRowIndexToView(i) >= 0) {
                        dataModel.setValueAt(true, i, CHECK_BOX_COL_INDEX);
                    }
                }
            }
        });
        selectNoneBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                for (int i = 0; i < dataModel.getRowCount(); i++) {
                    dataModel.setValueAt(false, i, CHECK_BOX_COL_INDEX);
                }
            }
        });
        closeBtn.addActionListener(Utilities.createDisposeAction(this));
        setBtn.addActionListener(new BulkUpdateActionListener());
    }

    private void initTable() {
        entitiesTable.setModel(dataModel);
        entitiesTable.getColumnModel().getColumn(HEADER_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(final Object value) {
                setText(((EntityHeader) value).getName());
            }
        });
        entitiesTable.getColumnModel().getColumn(ZONE_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(final Object value) {
                if (value instanceof SecurityZone) {
                    final SecurityZone zone = (SecurityZone) value;
                    setText(zone.equals(SecurityZoneUtil.NULL_ZONE) ? NO_SECURITY_ZONE : zone.getName());
                }
            }
        });
        setColumnWidths(CHECK_BOX_COL_INDEX, 30, 30);
        setColumnWidths(PATH_COL_INDEX, 60, 99999);
        setColumnWidths(ZONE_COL_INDEX, 80, 99999);
        pathColumn = entitiesTable.getColumnModel().getColumn(PATH_COL_INDEX);
        dataModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                final EntityType selectedEntityType = getSelectedEntityType();
                if (selectedEntityType != null) {
                    final int numSelected = getSelectedEntities().size();
                    selectedLabel.setText(numSelected + " " + selectedEntityType.getPluralName().toLowerCase() + " selected");
                } else {
                    selectedLabel.setText(StringUtils.EMPTY);
                }
            }
        });
        final Comparator<EntityHeader> headerComparator = new Comparator<EntityHeader>() {
            @Override
            public int compare(final EntityHeader o1, final EntityHeader o2) {
                String o1Name = o1.getName() == null ? StringUtils.EMPTY : o1.getName();
                String o2Name = o2.getName() == null ? StringUtils.EMPTY : o2.getName();
                return o1Name.compareToIgnoreCase(o2Name);
            }
        };
        Utilities.setRowSorter(entitiesTable, dataModel, new int[]{CHECK_BOX_COL_INDEX, HEADER_COL_INDEX, ZONE_COL_INDEX, PATH_COL_INDEX}, new boolean[]{true, true, true, true},
                new Comparator[]{null, ComparatorUtils.nullLowComparator(headerComparator), new NamedEntityComparator(), null});
    }

    private void setColumnWidths(final int index, final int min, final int max) {
        entitiesTable.getColumnModel().getColumn(index).setPreferredWidth(min);
        entitiesTable.getColumnModel().getColumn(index).setMinWidth(min);
        entitiesTable.getColumnModel().getColumn(index).setMaxWidth(max);
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

    /**
     * @return a map of selected entities where key = row index and value = entity header.
     */
    private Map<Integer, EntityHeader> getSelectedEntities() {
        final Map<Integer, EntityHeader> selectedEntities = new HashMap<>();
        for (int i = 0; i < dataModel.getRowCount(); i++) {
            final boolean selected = (Boolean) dataModel.getValueAt(i, CHECK_BOX_COL_INDEX);
            if (selected) {
                selectedEntities.put(i, ((EntityHeader) dataModel.getValueAt(i, HEADER_COL_INDEX)));
            }
        }
        return selectedEntities;
    }

    private void loadTable() {
        final int rowCount = dataModel.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            dataModel.removeRow(i);
        }
        EntityType selected = getSelectedEntityType();
        if (selected != null) {
            selected = convertToBackingEntityType(selected);
            final EntityNameResolver entityNameResolver = Registry.getDefault().getEntityNameResolver();
            try {
                final EntityHeaderSet<EntityHeader> entities = getEntities(selected);
                boolean atLeastOnePath = false;
                final EntityHeader[] headers = entities.toArray(new EntityHeader[entities.size()]);
                for (int i = 0; i < headers.length; i++) {
                    final EntityHeader header = headers[i];
                    if (header instanceof PolicyHeader) {
                        final PolicyHeader policy = (PolicyHeader) header;
                        if (PolicyType.PRIVATE_SERVICE == policy.getPolicyType() || !policy.getPolicyType().isSecurityZoneable()) {
                            // don't show service policies or non-zoneable policies
                            continue;
                        }
                    }
                    if (header.getType() == EntityType.ASSERTION_ACCESS) {
                        final String assertionClassName = header.getName();
                        assertionNames.put(header.getOid(), assertionClassName);
                    }
                    try {
                        final String displayName = entityNameResolver.getNameForHeader(header, false);
                        header.setName(displayName);
                        final Object[] data = new Object[4];
                        data[CHECK_BOX_COL_INDEX] = Boolean.FALSE;
                        data[HEADER_COL_INDEX] = header;
                        final String path = getPathForHeader(header);
                        if (StringUtils.isNotBlank(path)) {
                            data[PATH_COL_INDEX] = path;
                            atLeastOnePath = true;
                        }
                        data[ZONE_COL_INDEX] = getZoneForHeader(header);
                        dataModel.addRow(data);
                    } catch (final PermissionDeniedException e) {
                        logger.log(Level.WARNING, "Skipping row because unable to resolve display info for header " + header + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }

                showHidePathColumn(atLeastOnePath);
                dataModel.fireTableDataChanged();
            } catch (final FindException ex) {
                final String error = "Error retrieving entities of type " + selected;
                logger.log(Level.WARNING, error, ExceptionUtils.getDebugException(ex));
                DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private SecurityZone getZoneForHeader(final EntityHeader header) {
        SecurityZone zone = SecurityZoneUtil.NULL_ZONE;
        if (header instanceof HasSecurityZoneOid) {
            final Long securityZoneOid = ((HasSecurityZoneOid) header).getSecurityZoneOid();
            if (securityZoneOid != null) {
                zone = SecurityZoneUtil.getSecurityZoneByOid(securityZoneOid);
            }
        }
        return zone;
    }

    private String getPathForHeader(final EntityHeader header) throws FindException {
        String path = StringUtils.EMPTY;
        final EntityNameResolver entityNameResolver = Registry.getDefault().getEntityNameResolver();
        final ConsoleAssertionRegistry assertionRegistry = TopComponents.getInstance().getAssertionRegistry();
        try {
            if (header instanceof HasFolderOid) {
                path = entityNameResolver.getPath((HasFolderOid) header);
            } else if (header.getType() == EntityType.ASSERTION_ACCESS) {
                final Assertion assertion = assertionRegistry.findByClassName(assertionNames.get(header.getOid()));
                if (assertion != null) {
                    path = entityNameResolver.getPath(assertion);
                }
            }
        } catch (final PermissionDeniedException e) {
            logger.log(Level.WARNING, "Unable to determine path for header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            path = "path unavailable";
        }
        return path;
    }

    private EntityHeaderSet<EntityHeader> getEntities(@NotNull final EntityType selected) throws FindException {
        final EntityHeaderSet<EntityHeader> entities;
        if (selected == EntityType.ASSERTION_ACCESS) {
            // get assertion access from registry as they may not all be persisted in the database
            entities = new EntityHeaderSet<>();
            final ConsoleAssertionRegistry assertionRegistry = TopComponents.getInstance().getAssertionRegistry();
            final Collection<AssertionAccess> assertions = assertionRegistry.getPermittedAssertions();
            long nonPersistedAssertions = 0L;
            for (final AssertionAccess assertionAccess : assertions) {
                long oid = assertionAccess.getOid();
                if (oid == PersistentEntity.DEFAULT_OID) {
                    // this assertion access is not yet persisted
                    // give it some negative dummy oid so that it won't be considered 'equal' to the other headers
                    oid = --nonPersistedAssertions;
                }
                final ZoneableEntityHeader assertionHeader = new ZoneableEntityHeader(oid, EntityType.ASSERTION_ACCESS, assertionAccess.getName(), null, assertionAccess.getVersion());
                assertionHeader.setSecurityZoneOid(assertionAccess.getSecurityZone() == null ? null : assertionAccess.getSecurityZone().getOid());
                entities.add(assertionHeader);
            }
        } else {
            entities = Registry.getDefault().getRbacAdmin().findEntities(selected);
        }
        return entities;
    }

    private EntityType convertToBackingEntityType(final EntityType selected) {
        EntityType ret = selected;
        if (ret == EntityType.SSG_KEY_ENTRY) {
            ret = EntityType.SSG_KEY_METADATA;
        }
        return ret;
    }

    private void showHidePathColumn(final boolean show) {
        final boolean pathColumnVisible = entitiesTable.getColumnCount() > PATH_COL_INDEX;
        if (!show && pathColumnVisible) {
            entitiesTable.getColumnModel().removeColumn(pathColumn);
        } else if (show && !pathColumnVisible) {
            entitiesTable.getColumnModel().addColumn(pathColumn);
        }
    }

    private void enableDisable() {
        final boolean hasRows = entitiesTable.getModel().getRowCount() > 0;
        scrollPane.setVisible(hasRows);
        setBtn.setEnabled(hasRows && getSelectedZone() != null);
        filterPanel.allowFiltering(hasRows);
        noEntitiesPanel.setVisible(!hasRows);
        if (noEntitiesPanel.isVisible()) {
            final EntityType selectedEntityType = getSelectedEntityType();
            noEntitiesLabel.setText(selectedEntityType == null ? "no entities are available" : "no " + selectedEntityType.getPluralName().toLowerCase() + " are available");
        }
    }

    private class EntitiesTableModel extends DefaultTableModel {
        private EntitiesTableModel(@NotNull final String[] columnNames, @NotNull final Object[][] data) {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return col == CHECK_BOX_COL_INDEX;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return columnIndex == CHECK_BOX_COL_INDEX ? Boolean.class : String.class;
        }
    }

    private class BulkUpdateActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final EntityType selectedEntityType = convertToBackingEntityType(getSelectedEntityType());
            SecurityZone selectedZone = getSelectedZone();
            if (selectedZone != null && selectedZone.equals(SecurityZoneUtil.NULL_ZONE)) {
                selectedZone = null;
            }
            final Map<Integer, EntityHeader> selectedEntities = getSelectedEntities();
            if (selectedEntityType == EntityType.ASSERTION_ACCESS) {
                final Set<EntityHeader> assertionAccessToUpdate = new HashSet<>();
                try {
                    final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                    for (final Map.Entry<Integer, EntityHeader> entry : selectedEntities.entrySet()) {
                        final EntityHeader header = entry.getValue();
                        if (header.getOid() < 0) {
                            // save a new assertion access
                            final String assertionClassName = assertionNames.get(header.getOid());
                            final AssertionAccess assertionAccess = new AssertionAccess(assertionClassName);
                            assertionAccess.setSecurityZone(selectedZone);
                            final long savedOid = rbacAdmin.saveAssertionAccess(assertionAccess);
                            assertionNames.remove(header.getOid());
                            assertionNames.put(savedOid, assertionClassName);
                            header.setOid(savedOid);
                            dataModel.setValueAt(header, entry.getKey(), HEADER_COL_INDEX);
                        } else {
                            // update an existing assertion access
                            assertionAccessToUpdate.add(header);
                        }
                    }
                    doBulkUpdate(EntityType.ASSERTION_ACCESS, selectedZone, selectedEntities.keySet(), assertionAccessToUpdate);
                    TopComponents.getInstance().getAssertionRegistry().updateAssertionAccess();
                } catch (final UpdateException ex) {
                    DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Error assigning entities to zone.", ex);
                }
            } else {
                doBulkUpdate(selectedEntityType, selectedZone, selectedEntities.keySet(), selectedEntities.values());
            }
        }

        /**
         * @param selectedEntityType the selected EntityType
         * @param selectedZone       the selected SecurityZone or null if no zone selected
         * @param tableRowIndices    the row indices which require zone value to be updated
         * @param entitiesToUpdate   the entities to update in the database
         */
        private void doBulkUpdate(@NotNull final EntityType selectedEntityType, @Nullable final SecurityZone selectedZone, @NotNull final Collection<Integer> tableRowIndices, @NotNull final Collection<EntityHeader> entitiesToUpdate) {
            final Long securityZoneOid = selectedZone == null ? null : selectedZone.getOid();
            try {
                final BulkZoneUpdater updater = new BulkZoneUpdater(Registry.getDefault().getRbacAdmin(),
                        Registry.getDefault().getUDDIRegistryAdmin(), Registry.getDefault().getJmsManager(), Registry.getDefault().getPolicyAdmin());
                updater.bulkUpdate(securityZoneOid, selectedEntityType, entitiesToUpdate);
                for (final Integer selectedIndex : tableRowIndices) {
                    dataModel.setValueAt(selectedZone == null ? SecurityZoneUtil.NULL_ZONE : selectedZone, selectedIndex, ZONE_COL_INDEX);
                }
            } catch (final FindException ex) {
                DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Error assigning entities to zone.", ex);
            } catch (final UpdateException ex) {
                DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Unable to assign entities to zone.", ex);
            }
        }
    }
}
