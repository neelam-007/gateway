package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog for bulk assigning entities to security zones.
 */
public class AssignSecurityZonesDialog extends JDialog {
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
    private EntitiesTableModel dataModel = new EntitiesTableModel(new String[]{"", "Name", "Current Zone"}, new Object[][]{});

    public AssignSecurityZonesDialog(@NotNull final Window owner, @NotNull final Collection<EntityType> entityTypes, @NotNull final Collection<SecurityZone> securityZones) {
        super(owner);
        setContentPane(contentPanel);
        initBtns();
        initComboBoxes(entityTypes, securityZones);
        setPanelTitle();
        initTable();
        loadTable();
        loadCount();
        initFiltering();
    }

    private void initFiltering() {
        filterPanel.attachRowSorter(((TableRowSorter) entitiesTable.getRowSorter()), new int[]{1});
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        filterPanel.registerClearCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
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

    private void initComboBoxes(final Collection<EntityType> entityTypes, final Collection<SecurityZone> securityZones) {
        typeComboBox.setModel((new DefaultComboBoxModel<EntityType>(entityTypes.toArray(new EntityType[entityTypes.size()]))));
        typeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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
            }
        });
        typeComboBox.setSelectedItem(null);
        zoneComboBox.setModel(new DefaultComboBoxModel<SecurityZone>(securityZones.toArray(new SecurityZone[securityZones.size()])));
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
                    dataModel.setValueAt(true, i, 0);
                }
            }
        });
        selectNoneBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                for (int i = 0; i < dataModel.getRowCount(); i++) {
                    dataModel.setValueAt(false, i, 0);
                }
            }
        });
        closeBtn.addActionListener(Utilities.createDisposeAction(this));
        setBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final SecurityZone selectedZone = getSelectedZone();
                final List<EntityHeader> selectedEntities = getSelectedEntities();
                final List<Long> oids = new ArrayList<>(selectedEntities.size());
                for (EntityHeader header : selectedEntities) {
                    oids.add(header.getOid());
                }
                try {
                    Registry.getDefault().getRbacAdmin().setSecurityZoneForEntities(selectedZone == null ? null : selectedZone.getOid(), getSelectedEntityType(), oids);
                    loadTable();
                } catch (final UpdateException ex) {
                    DialogDisplayer.showMessageDialog(AssignSecurityZonesDialog.this, "Error", "Unable to assign entities to zone.", ex);
                }
            }
        });
    }

    private void initTable() {
        entitiesTable.setModel(dataModel);
        entitiesTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof EntityHeader ? ((EntityHeader) value).getName() : StringUtils.EMPTY);
            }
        });
        TableUtil.adjustColumnWidth(entitiesTable, 0, 30);
        TableUtil.adjustColumnWidth(entitiesTable, 2, 60);
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
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        };
        Utilities.setRowSorter(entitiesTable, dataModel, new int[]{0, 1, 2}, new boolean[]{true, true, true},
                new Comparator[]{null, ComparatorUtils.nullLowComparator(headerComparator), null});
    }

    @Nullable
    private SecurityZone getSelectedZone() {
        final Object selected = zoneComboBox.getSelectedItem();
        if (selected != null) {
            return (SecurityZone) selected;
        }
        return null;
    }

    @Nullable
    private EntityType getSelectedEntityType() {
        final Object selected = typeComboBox.getSelectedItem();
        if (selected != null) {
            return (EntityType) selected;
        }
        return null;
    }

    private java.util.List<EntityHeader> getSelectedEntities() {
        final java.util.List<EntityHeader> selectedEntities = new ArrayList<>();
        for (int i = 0; i < dataModel.getRowCount(); i++) {
            final boolean selected = (Boolean) dataModel.getValueAt(i, 0);
            if (selected) {
                selectedEntities.add((EntityHeader) dataModel.getValueAt(i, 1));
            }
        }
        return selectedEntities;
    }

    private void loadTable() {
        final int rowCount = dataModel.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            dataModel.removeRow(i);
        }
        final EntityType selected = getSelectedEntityType();
        if (selected != null) {
            try {
                final EntityHeaderSet<EntityHeader> entities = Registry.getDefault().getRbacAdmin().findEntities(selected);
                final EntityHeader[] headers = entities.toArray(new EntityHeader[entities.size()]);
                for (int i = 0; i < headers.length; i++) {
                    final EntityHeader header = headers[i];
                    final Object[] data = new Object[3];
                    data[0] = Boolean.FALSE;
                    data[1] = header;
                    String zone = "(no security zone)";
                    if (header instanceof ZoneableEntityHeader) {
                        final Long securityZoneOid = ((ZoneableEntityHeader) header).getSecurityZoneOid();
                        if (securityZoneOid != null) {
                            zone = Registry.getDefault().getRbacAdmin().findSecurityZoneByPrimaryKey(securityZoneOid).getName();
                        }
                    }
                    data[2] = zone;
                    dataModel.addRow(data);
                }
                dataModel.fireTableDataChanged();
            } catch (final FindException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class EntitiesTableModel extends DefaultTableModel {
        private EntitiesTableModel(@NotNull final String[] columnNames, @NotNull final Object[][] data) {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return col == 0;
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }
    }
}
