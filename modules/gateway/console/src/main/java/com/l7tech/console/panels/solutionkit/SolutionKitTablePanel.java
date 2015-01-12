package com.l7tech.console.panels.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * This panel contains a table that contains solution kits.
 */
public class SolutionKitTablePanel extends JPanel {
    private JPanel mainPanel;
    private JTable solutionKitsTable;

    private SimpleTableModel<SolutionKitHeader> solutionKitsModel;

    /**
     * Create panel.
     */
    public SolutionKitTablePanel() {
        super();
        initialize();
    }

    /**
     * Set solution kits to display in the table.
     *
     * @param solutionKits list of solution kit headers
     */
    public void setData(@NotNull List<SolutionKitHeader> solutionKits) {
        solutionKitsModel.setRows(solutionKits);
    }

    /**
     * Refresh table.
     */
    public void reload() {
        solutionKitsModel.fireTableDataChanged();
    }

    /**
     * Check if a solution kit in the table is selected.
     *
     * @return true if a solution kit selected. false otherwise.
     */
    public boolean isSolutionKitSelected() {
        int rowIndex = solutionKitsTable.getSelectedRow();
        return (rowIndex != -1);
    }

    /**
     * Get the selected solution kit header in the table.
     *
     * @return the selected solution kit header. Null if none is selected.
     */
    public SolutionKitHeader getSelectedSolutionKit() {
        int rowIndex = solutionKitsTable.getSelectedRow();
        if (rowIndex == -1) {
            return null;
        }

        int modelIndex = solutionKitsTable.getRowSorter().convertRowIndexToModel(rowIndex);
        return solutionKitsModel.getRowObject(modelIndex);
    }

    /**
     * Set the button to click when a double-click is performed on the table.
     *
     * @param button the button
     */
    public void setDoubleClickAction(@NotNull JButton button) {
        Utilities.setDoubleClickAction(solutionKitsTable, button);
    }

    /**
     * Add a given list selection listener to the table.
     *
     * @param listener the listener
     */
    public void addListSelectionListener(@NotNull ListSelectionListener listener) {
        solutionKitsTable.getSelectionModel().addListSelectionListener(listener);
    }

    private void initialize() {
        solutionKitsModel = TableUtil.configureTable(solutionKitsTable,
            column("Name", 50, 400, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getName();
                }
            }),
            column("Version", 50, 100, 500, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getSolutionKitVersion();
                }
            }),
            column("Description", 50, 500, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getDescription();
                }
            }),
            column("Last Updated", 50, 300, 1000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return new Date(solutionKitHeader.getLastUpdateTime()).toString();
                }
            })
        );

        solutionKitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.setRowSorter(solutionKitsTable, solutionKitsModel,
            new int[]{0},
            new boolean[]{true},
            new Comparator[]{null});

        setLayout(new BorderLayout());
        add(mainPanel);
    }
}