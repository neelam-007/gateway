package com.l7tech.console.panels.solutionkit.install;

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
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

/**
 *
 */
public class SolutionKitMappingsPanel extends JPanel {
    private JPanel mainPanel;
    private JTable mappingsTable;

    private SimpleTableModel<Mapping> mappingsModel;
    private Map<String, Item> bundleItems; // key = bundle reference item id. value = bundle reference item.

    public SolutionKitMappingsPanel() {
        super();
        initialize();
    }

    public void setData(@NotNull Mappings mappings, @NotNull Map<String, Item> bundleItems) {
        this.bundleItems = bundleItems;
        mappingsModel.setRows(mappings.getMappings());
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

    private void initialize() {
        mappingsModel = TableUtil.configureTable(mappingsTable,
            TableUtil.column("Name", 25, 200, 99999, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    if ("0000000000000000ffffffffffffec76".equals(mapping.getSrcId()) &&
                        EntityType.FOLDER.toString().equals(mapping.getType())) {
                        return "Root";
                    }

                    Item item = bundleItems.get(mapping.getSrcId());
                    if (item != null) {
                        return item.getName();
                    }

                    return "Unknown";
                }
            }),
            TableUtil.column("Type", 25, 200, 99999, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    return mapping.getType();
                }
            }),
            TableUtil.column("Action", 25, 200, 99999, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.Action action = mapping.getAction();
                    if (action != null) {
                        return action.toString();
                    }
                    return "None";
                }
            }),
            TableUtil.column("Action Taken", 25, 200, 99999, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ActionTaken actionTaken = mapping.getActionTaken();
                    if (actionTaken != null) {
                        return actionTaken.toString();
                    }
                    return "None";
                }
            }),
            TableUtil.column("Error Type", 25, 200, 99999, new Functions.Unary<String, Mapping>() {
                @Override
                public String call(Mapping mapping) {
                    Mapping.ErrorType errorType = mapping.getErrorType();
                    if (errorType != null) {
                        return errorType.toString();
                    }
                    return "None";
                }
            })
        );
        mappingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.setRowSorter(mappingsTable, mappingsModel,
            new int[]{0, 1, 2, 3, 4},
            new boolean[]{true, true, true, true, true},
            new Comparator[]{null, null, null, null, null});

        setLayout(new BorderLayout());
        add(mainPanel);
    }
}