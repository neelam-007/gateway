package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
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
    private EntitiesTableModel dataModel = new EntitiesTableModel(new String[]{"", "Name", "Current Zone"}, new Object[][]{});
    private Map<EntityType, List<SecurityZone>> entityTypes;

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
        filterPanel.registerClearCallback(new Runnable() {
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
                    dataModel.setValueAt(true, i, CHECK_BOX_COL_INDEX);
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
        setBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final SecurityZone selectedZone = getSelectedZone();
                final Map<Integer, EntityHeader> selectedEntities = getSelectedEntities();
                final List<Long> oids = new ArrayList<>(selectedEntities.size());
                for (final EntityHeader selectedHeader : selectedEntities.values()) {
                    oids.add(selectedHeader.getOid());
                }
                try {
                    Registry.getDefault().getRbacAdmin().setSecurityZoneForEntities(selectedZone == null ? null : selectedZone.getOid(), getSelectedEntityType(), oids);
                    for (final Integer selectedIndex : selectedEntities.keySet()) {
                        dataModel.setValueAt(selectedZone == null ? NO_SECURITY_ZONE : selectedZone.getName(), selectedIndex, ZONE_COL_INDEX);
                    }
                } catch (final UpdateException ex) {
                    DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Unable to assign entities to zone.", ex);
                }
            }
        });
    }

    private void initTable() {
        entitiesTable.setModel(dataModel);
        entitiesTable.getColumnModel().getColumn(HEADER_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(final Object value) {
                final EntityHeader header = (EntityHeader) value;
                String name;
                try {
                    name = Registry.getDefault().getEntityNameResolver().getNameForHeader(header);
                } catch (final FindException e) {
                    name = "unknown entity";
                }
                setText(name);
            }
        });
        TableUtil.adjustColumnWidth(entitiesTable, CHECK_BOX_COL_INDEX, 30);
        TableUtil.adjustColumnWidth(entitiesTable, ZONE_COL_INDEX, 60);
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
        Utilities.setRowSorter(entitiesTable, dataModel, new int[]{CHECK_BOX_COL_INDEX, HEADER_COL_INDEX, ZONE_COL_INDEX}, new boolean[]{true, true, true},
                new Comparator[]{null, ComparatorUtils.nullLowComparator(headerComparator), null});
    }

    @Nullable
    private SecurityZone getSelectedZone() {
        SecurityZone selectedZone = null;
        final Object selected = zoneComboBox.getSelectedItem();
        if (selected != null && !SecurityZoneUtil.NULL_ZONE.equals(selected)) {
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
            if (selected == EntityType.SSG_KEY_ENTRY) {
                selected = EntityType.SSG_KEY_METADATA;
            }
            try {
                final EntityHeaderSet<EntityHeader> entities = Registry.getDefault().getRbacAdmin().findEntities(selected);
                final EntityHeader[] headers = entities.toArray(new EntityHeader[entities.size()]);
                for (int i = 0; i < headers.length; i++) {
                    final EntityHeader header = headers[i];
                    final Object[] data = new Object[3];
                    data[CHECK_BOX_COL_INDEX] = Boolean.FALSE;
                    data[HEADER_COL_INDEX] = header;
                    String zone = NO_SECURITY_ZONE;
                    if (header instanceof HasSecurityZoneOid) {
                        final Long securityZoneOid = ((HasSecurityZoneOid) header).getSecurityZoneOid();
                        if (securityZoneOid != null) {
                            zone = Registry.getDefault().getRbacAdmin().findSecurityZoneByPrimaryKey(securityZoneOid).getName();
                        }
                    }
                    data[ZONE_COL_INDEX] = zone;
                    dataModel.addRow(data);
                }
                dataModel.fireTableDataChanged();
            } catch (final FindException ex) {
                final String error = "Error retrieving entities of type " + selected;
                logger.log(Level.WARNING, error, ExceptionUtils.getDebugException(ex));
                DialogDisplayer.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
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
}
