package com.l7tech.console.panels.solutionkit;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.Component;
import java.util.*;

/**
 * This panel contains a table that contains the mappings.
 */
public class SolutionKitMappingsPanel extends JPanel {
    private JPanel mainPanel;
    private JTable mappingsTable;

    private TableColumn nameColumn;
    private TableColumn errorTypeColumn;
    private TableColumn resolvedColumn;
    private TableColumn targetIdColumn;

    private SimpleTableModel<Mapping> mappingsModel;
    private Map<String, Item> bundleItems = new HashMap<>();    // key = bundle reference item id. value = bundle reference item.
    private Map<String, String> resolvedEntityIds;              // key = from id. value  = to id.

    /**
     * Create panel.
     */
    public SolutionKitMappingsPanel() {
        super();
        initialize();
    }

    /**
     * Set mappings to display in the table.
     *
     * @param mappings the mappings
     * @param bundle the bundle
     * @param resolvedEntityIds map of resolved entity IDs. Key is source ID, value is the resolved ID.
     */
    public void setData(@NotNull Mappings mappings, @NotNull Bundle bundle, @NotNull Map<String, String> resolvedEntityIds) {
        bundleItems.clear();
        for (Item aItem : bundle.getReferences()) {
            bundleItems.put(aItem.getId(), aItem);
        }

        this.resolvedEntityIds = resolvedEntityIds;
        if (mappings.getMappings() != null) {
            mappingsModel.setRows(mappings.getMappings());
        }
    }

    /**
     * Set mappings to display in the table.
     *
     * @param mappings the mappings
     */
    public void setData(@NotNull Mappings mappings) {
        this.bundleItems = null;
        this.resolvedEntityIds = null;
        if (mappings.getMappings() != null) {
            mappingsModel.setRows(mappings.getMappings());
        }
    }

    /**
     * Refresh the table.
     */
    public void reload() {
        mappingsModel.fireTableDataChanged();
    }

    /**
     * Get the selected mapping in the table.
     *
     * @return the selected mapping. Null if none is selected.
     */
    public Mapping getSelectedMapping() {
        int rowIndex = mappingsTable.getSelectedRow();
        if (rowIndex == -1) {
            return null;
        }

        int modelIndex = mappingsTable.getRowSorter().convertRowIndexToModel(rowIndex);
        return mappingsModel.getRowObject(modelIndex);
    }

    public java.util.List<Mapping> getAllMappings() {
        return mappingsModel.getRows();
    }

    /**
     * Set the button to click when a double-click is performed on the table.
     *
     * @param button the button
     */
    public void setDoubleClickAction(@NotNull JButton button) {
        Utilities.setDoubleClickAction(mappingsTable, button);
    }

    /**
     * Add a given list selection listener to the table.
     *
     * @param listener the listener
     */
    public void addListSelectionListener(@NotNull ListSelectionListener listener) {
        mappingsTable.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Hide the "Name" column from the table.
     */
    public void hideNameColumn() {
        mappingsTable.removeColumn(nameColumn);
    }

    /**
     * Hide the "Error Type" column from the table.
     */
    public void hideErrorTypeColumn() {
        mappingsTable.removeColumn(errorTypeColumn);
    }

    /**
     * Hide the "Resolved" column from the table.
     */
    public void hideResolvedColumn() {
        mappingsTable.removeColumn(resolvedColumn);
    }

    /**
     * Hide the "Target ID" column from the table.
     */
    public void hideTargetIdColumn() {
        mappingsTable.removeColumn(targetIdColumn);
    }

    @NotNull
    public Map<String, Item> getBundleItems() {
        return bundleItems;
    }

    @NotNull
    public Map<String, String> getResolvedEntityIds() {
        return resolvedEntityIds;
    }

    private void initialize() {
        mappingsModel = TableUtil.configureTable(mappingsTable,
            TableUtil.column("Name", 50, 200, 1000, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    if ("0000000000000000ffffffffffffec76".equals(mapping.getSrcId()) &&
                        EntityType.FOLDER.toString().equals(mapping.getType())) {
                        return "Root";
                    }

                    if (bundleItems != null) {
                        Item item = bundleItems.get(mapping.getSrcId());
                        if (item != null) {
                            return item.getName();
                        } else {
                            return "Unknown";
                        }

                    }
                    return "---";
                }
            }),
            TableUtil.column("Type", 50, 150, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    return mapping.getType();
                }
            }),
            TableUtil.column("Action", 50, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.Action action = mapping.getAction();
                    if (action != null) {
                        return action.toString();
                    }
                    return "---";
                }
            }),
            TableUtil.column("Action Taken", 50, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ActionTaken actionTaken = mapping.getActionTaken();
                    if (actionTaken != null) {
                        return actionTaken.toString();
                    }
                    return "---";
                }
            }),
            TableUtil.column("Error Type", 50, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ErrorType errorType = mapping.getErrorType();

                    class CustomRenderer extends DefaultTableCellRenderer {
                        Mapping mapping;

                        CustomRenderer(Mapping m) {
                            super();
                            mapping = m;
                        }

                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                            if ((mapping.getErrorType() != null) && (resolvedEntityIds.get(mapping.getSrcId()) == null)){
                                cellComponent.setBackground(Color.PINK);
                                cellComponent.setForeground(Color.RED);
                            }
                            return cellComponent;
                        }
                    }

                    resolvedColumn.setCellRenderer(new CustomRenderer(mapping));

                    if (errorType != null) {
                        return errorType.toString();
                    }
                    return "---";
                }
            }),
            TableUtil.column("Resolved", 50, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ErrorType errorType = mapping.getErrorType();
                    if (errorType == null) {
                        return "---";
                    }
                    if (resolvedEntityIds.get(mapping.getSrcId()) == null) {
                        return "No";
                    }
                    return "Yes";
                }
            }),
            TableUtil.column("Source ID", 50, 200, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    return mapping.getSrcId();
                }
            }),
            TableUtil.column("Target ID", 50, 200, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    return mapping.getTargetId();
                }
            })
        );

        nameColumn = mappingsTable.getColumnModel().getColumn(0);
        errorTypeColumn = mappingsTable.getColumnModel().getColumn(4);
        resolvedColumn = mappingsTable.getColumnModel().getColumn(5);
        targetIdColumn = mappingsTable.getColumnModel().getColumn(7);

        mappingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.setRowSorter(mappingsTable, mappingsModel,
            new int[]{5},
            new boolean[]{false},
            new Comparator[]{null});

        hideTargetIdColumn();
        setLayout(new BorderLayout());
        add(mainPanel);
    }
}