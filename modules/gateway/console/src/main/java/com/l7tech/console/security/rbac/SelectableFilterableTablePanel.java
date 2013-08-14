package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.util.Utilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableRowSorter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel which contains a filterable JTable with selectable rows.
 */
public class SelectableFilterableTablePanel extends JPanel {
    private static final String ITEMS = "items";
    private JPanel contentPanel;
    private JButton selectAllButton;
    private JButton clearAllButton;
    private JTable selectableTable;
    private JLabel countLabel;
    private JLabel selectionLabel;
    private FilterPanel filterPanel;
    private JLabel noEntitiesLabel;
    private JScrollPane scrollPane;
    private JPanel noEntitiesPanel;
    private CheckBoxSelectableTableModel model;
    private int[] filterableColumns;
    private String selectableObjectLabel = ITEMS;

    public SelectableFilterableTablePanel() {
        loadCount();
        loadSelectionCount();
        initBtns();
        initFiltering();
        setButtonTexts();
        showHide();
    }

    /**
     * Configure the panel.
     *
     * @param model                 the CheckBoxSelectableTableModel which should back the JTable.
     * @param filterableColumns     column indices which are filterable.
     * @param selectableObjectLabel the label to display for the types of objects in the table (ex. no 'selectObjectLabel' are available). Default is 'items'.
     */
    public void configure(@NotNull final CheckBoxSelectableTableModel model, @NotNull final int[] filterableColumns, @Nullable final String selectableObjectLabel) {
        this.model = model;
        this.filterableColumns = filterableColumns;
        this.selectableObjectLabel = selectableObjectLabel == null ? ITEMS : selectableObjectLabel;
        loadCount();
        loadSelectionCount();
        initFiltering();
        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    showHide();
                    loadSelectionCount();
                }
            }
        });
        showHide();
    }

    @NotNull
    public JTable getSelectableTable() {
        return selectableTable;
    }

    private void initBtns() {
        Utilities.buttonToLink(selectAllButton);
        Utilities.buttonToLink(clearAllButton);
        selectAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                selectDeselectVisible(true);
            }
        });
        clearAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                selectDeselectVisible(false);
            }
        });
    }

    private void selectDeselectVisible(final boolean select) {
        if (model != null) {
            if (!filterPanel.isFiltered()) {
                if (select) {
                    model.selectAll();
                } else {
                    model.deselectAll();
                }
            } else {
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (selectableTable.getRowSorter().convertRowIndexToView(i) >= 0) {
                        // it's visible
                        if (select) {
                            model.select(i);
                        } else {
                            model.deselect(i);
                        }
                    }
                }
            }
        }
    }

    private void setButtonTexts() {
        final String label = filterPanel.isFiltered() ? "visible" : "all";
        selectAllButton.setText("select " + label);
        clearAllButton.setText("clear " + label);
    }

    private void initFiltering() {
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
                setButtonTexts();
            }
        });
        if (selectableTable != null) {
            if (model != null) {
                Utilities.setRowSorter(selectableTable, model);
            }
            if (filterableColumns != null) {
                filterPanel.attachRowSorter((TableRowSorter) (selectableTable.getRowSorter()), filterableColumns);
            }
        }
    }

    private void showHide() {
        final boolean hasRows = selectableTable.getModel().getRowCount() > 0;
        scrollPane.setVisible(hasRows);
        filterPanel.allowFiltering(hasRows);
        noEntitiesPanel.setVisible(!hasRows);
        if (noEntitiesPanel.isVisible()) {
            noEntitiesLabel.setText("no " + selectableObjectLabel + " are available");
        }
    }


    private void loadCount() {
        final int visible = selectableTable == null ? 0 : selectableTable.getRowCount();
        final int total = model == null ? 0 : model.getRowCount();
        countLabel.setText("showing " + visible + " of " + total + " " + selectableObjectLabel);
    }

    private void loadSelectionCount() {
        final int numSelected = model == null ? 0 : model.getSelected().size();
        selectionLabel.setText(numSelected + " " + selectableObjectLabel + " selected");
    }
}
