package com.l7tech.console.panels;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Reusable filter panel.
 */
public class FilterPanel extends JPanel {
    private static final String CASE_INSENSITIVE_FLAG = "(?i)";
    private JPanel contentPanel;
    private JTextField filterTextField;
    private JButton filterButton;
    private JButton clearButton;
    private JPanel btnPanel;
    private TableRowSorter rowSorter;
    private int[] columnsToFilter = new int[]{0};
    private Runnable clearCallback;
    private Runnable filterCallback;

    public FilterPanel() {
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rowSorter != null) {
                    rowSorter.setRowFilter(RowFilter.regexFilter(CASE_INSENSITIVE_FLAG + getFilterText(), columnsToFilter));
                }
                if (filterCallback != null) {
                    filterCallback.run();
                }
            }
        });
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterTextField.setText(StringUtils.EMPTY);
                enableDisableBtns();
                if (rowSorter != null) {
                    rowSorter.setRowFilter(null);
                }
                if (clearCallback != null) {
                    clearCallback.run();
                }
            }
        });
        filterTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                enableDisableBtns();
            }
        });
        enableDisableBtns();
    }

    /**
     * Register a callback to execute after the clear filter button has been clicked.
     *
     * @param clearCallback the callback to execute after the clear filter button has been clicked.
     */
    public void registerClearCallback(@NotNull final Runnable clearCallback) {
        this.clearCallback = clearCallback;
    }

    /**
     * Register a callback to execute after the apply filter button has been clicked.
     *
     * @param filterCallback the callback to execute after the apply filter button has been clicked.
     */
    public void registerFilterCallback(@NotNull final Runnable filterCallback) {
        this.filterCallback = filterCallback;
    }

    /**
     * @return the trimmed text from the filter text field.
     */
    public String getFilterText() {
        return filterTextField.getText().trim();
    }

    /**
     * Disable/enable the filter text field and buttons. Does not remove any applied filters to the TableRowSorter.
     *
     * @param allowFiltering false to disable the filter text field and buttons. True to enable them (buttons will only be enabled if the text field is non-empty).
     */
    public void allowFiltering(boolean allowFiltering) {
        filterTextField.setEnabled(allowFiltering);
        enableDisableBtns();
    }

    public boolean isFiltered() {
        return rowSorter.getRowFilter() != null;
    }

    /**
     * Attach a TableRowSorter to this filter panel such that when the apply filter button is clicked, a case insensitive row filter will be applied to the TableRowSorter.
     *
     * @param rowSorter       the TableRowSorter to apply/clear a case insensitive row filter when the filter button is clicked.
     * @param columnsToFilter the column indices which can be filtered.
     */
    public void attachRowSorter(@NotNull final TableRowSorter rowSorter, @NotNull final int[] columnsToFilter) {
        this.rowSorter = rowSorter;
        this.columnsToFilter = columnsToFilter;
    }

    private void enableDisableBtns() {
        filterButton.setEnabled(filterTextField.isEnabled() && !getFilterText().isEmpty());
        clearButton.setEnabled(filterTextField.isEnabled() && !getFilterText().isEmpty());
    }
}
