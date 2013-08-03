package com.l7tech.console.panels;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Pattern;

/**
 * Reusable filter panel.
 */
public class FilterPanel extends JPanel {
    private static final String CASE_INSENSITIVE_FLAG = "(?i)";
    private JPanel contentPanel;
    private JTextField filterTextField;
    private JLabel filterOnLabel;
    private TableRowSorter rowSorter;
    // default is filter on first column
    private int[] columnsToFilter = new int[]{0};
    private Runnable filterCallback;

    public FilterPanel() {
        filterTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (rowSorter != null) {
                    final String filterText = getFilterText();
                    // special characters are treated literally
                    final String escaped = Pattern.quote(filterText);
                    if (StringUtils.isNotBlank(filterText)) {
                        rowSorter.setRowFilter(RowFilter.regexFilter(CASE_INSENSITIVE_FLAG + escaped, columnsToFilter));
                    } else {
                        rowSorter.setRowFilter(null);
                    }
                }
                if (filterCallback != null) {
                    filterCallback.run();
                }
            }
        });
    }

    /**
     * Register a callback to execute after the filtering has been applied or removed.
     *
     * @param filterCallback the callback to execute after filtering has been applied or removed.
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
     * Disable/enable the filter text field. Does not remove any applied filters to the TableRowSorter.
     *
     * @param allowFiltering false to disable the filter text field. True to enable it.
     */
    public void allowFiltering(boolean allowFiltering) {
        filterTextField.setEnabled(allowFiltering);
    }

    public boolean isFiltered() {
        return rowSorter == null ? false : rowSorter.getRowFilter() != null;
    }

    /**
     * Attach a TableRowSorter to this filter panel such that when the filter is applied, a case insensitive row filter will be applied to the TableRowSorter.
     *
     * @param rowSorter       the TableRowSorter to apply/clear a case insensitive row filter when the filter is applied.
     * @param columnsToFilter the column indices which can be filtered.
     */
    public void attachRowSorter(@NotNull final TableRowSorter rowSorter, @NotNull final int[] columnsToFilter) {
        this.rowSorter = rowSorter;
        this.columnsToFilter = columnsToFilter;
    }

    public void setFilterLabel(@NotNull final String labelText) {
        filterOnLabel.setText(labelText);
    }
}
