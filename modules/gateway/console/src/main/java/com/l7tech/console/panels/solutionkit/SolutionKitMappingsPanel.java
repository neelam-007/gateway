package com.l7tech.console.panels.solutionkit;

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
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

/**
 *
 */
public class SolutionKitMappingsPanel extends JPanel {
    private JPanel mainPanel;
    private JTable mappingsTable;

    private TableColumn nameColumn;
    private TableColumn errorTypeColumn;
    private TableColumn resolvedColumn;
    private TableColumn targetIdColumn;

    private SimpleTableModel<Mapping> mappingsModel;
    private Map<String, Item> bundleItems;          // key = bundle reference item id. value = bundle reference item.
    private Map<String, String> resolvedEntityIds;  // key = from id. value  = to id.

    public SolutionKitMappingsPanel() {
        super();
        initialize();
    }

    public void setData(@NotNull Mappings mappings, @NotNull Map<String, Item> bundleItems, @NotNull Map<String, String> resolvedEntityIds) {
        this.bundleItems = bundleItems;
        this.resolvedEntityIds = resolvedEntityIds;
        mappingsModel.setRows(mappings.getMappings());
    }

    public void setData(@NotNull Mappings mappings) {
        this.bundleItems = null;
        this.resolvedEntityIds = null;
        mappingsModel.setRows(mappings.getMappings());
    }

    public void reload() {
        mappingsModel.fireTableDataChanged();
    }

    public Mapping getSelectedMapping() {
        int rowIndex = mappingsTable.getSelectedRow();
        if (rowIndex == -1) {
            return null;
        }

        int modelIndex = mappingsTable.getRowSorter().convertRowIndexToModel(rowIndex);
        return mappingsModel.getRowObject(modelIndex);
    }

    public void setDoubleClickAction(@NotNull JButton button) {
        Utilities.setDoubleClickAction(mappingsTable, button);
    }

    public void addListSelectionListener(@NotNull ListSelectionListener listener) {
        mappingsTable.getSelectionModel().addListSelectionListener(listener);
    }

    public void hideNameColumn() {
        mappingsTable.removeColumn(nameColumn);
    }

    public void hideErrorTypeColumn() {
        mappingsTable.removeColumn(errorTypeColumn);
    }

    public void hideResolvedColumn() {
        mappingsTable.removeColumn(resolvedColumn);
    }

    public void hideTargetIdColumn() {
        mappingsTable.removeColumn(targetIdColumn);
    }

    private void initialize() {
        mappingsModel = TableUtil.configureTable(mappingsTable,
            TableUtil.column("Name", 200, 200, 1000, new Functions.Unary<String, Mapping>() {
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
            TableUtil.column("Type", 150, 150, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    return mapping.getType();
                }
            }),
            TableUtil.column("Action", 100, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.Action action = mapping.getAction();
                    if (action != null) {
                        return action.toString();
                    }
                    return "---";
                }
            }),
            TableUtil.column("Action Taken", 100, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ActionTaken actionTaken = mapping.getActionTaken();
                    if (actionTaken != null) {
                        return actionTaken.toString();
                    }
                    return "---";
                }
            }),
            TableUtil.column("Error Type", 100, 100, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ErrorType errorType = mapping.getErrorType();
                    if (errorType != null) {
                        return errorType.toString();
                    }
                    return "---";
                }
            }),
            TableUtil.column("Resolved", 100, 100, 500, new Functions.Unary<String, Mapping>() {
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
            TableUtil.column("Source ID", 200, 200, 500, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    return mapping.getSrcId();
                }
            }),
            TableUtil.column("Target ID", 200, 200, 500, new Functions.Unary<String, Mapping>() {
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
            new int[]{0},
            new boolean[]{true},
            new Comparator[]{null});

        setLayout(new BorderLayout());
        add(mainPanel);
    }
}