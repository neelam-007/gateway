package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import com.l7tech.policy.assertion.JmsMessagePropertyRuleSet;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 2/27/14
 */
public class JmsMsgPropertiesPanel extends JPanel{

    public static final String PASS_THROUGH = "<original value>";

    @SuppressWarnings( { "UnusedDeclaration" } )
    private JPanel mainPanel;       // Not used but required by IntelliJ IDEA.
    private JRadioButton passThroughAllRadioButton;
    private JRadioButton customizeRadioButton;
    private JPanel customPanel;
    private JTable customTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;

    private final JDialog owner;
    private DefaultTableModel customTableModel;

    public JmsMsgPropertiesPanel(JDialog owner) {
        this.owner = owner;
        passThroughAllRadioButton.setSelected(true);//default setting
        passThroughAllRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utilities.setEnabled(customPanel, false);
                customTable.clearSelection();
            }
        });

        customizeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utilities.setEnabled(customPanel, true);
                removeButton.setEnabled(false);
                editButton.setEnabled(false);
            }
        });

        final String[] columnNames = new String[]{"Name", "Value"};
        customTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        customTable.setModel(customTableModel);
        customTable.getTableHeader().setReorderingAllowed(false);
        customTable.setColumnSelectionAllowed(false);
        customTable.setRowSelectionAllowed(true);
        customTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        customTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final int numSelected = customTable.getSelectedRows().length;
                removeButton.setEnabled(numSelected >= 1);
                editButton.setEnabled(numSelected == 1);
            }
        });

        customTable.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    editSelectedRow();
                }
            }
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {}
        });

        customTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editSelectedRow();
            }
        });

        // Provides sorting of the custom table by property name.
        final JTableHeader hdr = customTable.getTableHeader();
        hdr.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent event) {
                final TableColumnModel tcm = customTable.getColumnModel();
                final int viewColumnIndex = tcm.getColumnIndexAtX(event.getX());
                final int modelColumnIndex = customTable.convertColumnIndexToModel(viewColumnIndex);
                if (modelColumnIndex == 0) {
                    sortTable();
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JmsMessagePropertyDialog editor = new JmsMessagePropertyDialog(JmsMsgPropertiesPanel.this.owner, getExistingNames(), null);
                editor.pack();
                Utilities.centerOnScreen(editor);
                editor.setVisible(true);
                if (editor.isOKed()) {
                    JmsMessagePropertyRule newRule = editor.getData();
                    customTableModel.addRow(dataToRow(newRule));
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int[] selectedRows = customTable.getSelectedRows();
                if (selectedRows != null && selectedRows.length > 0) {
                    for (int i = selectedRows.length - 1; i >= 0; --i) {
                        customTableModel.removeRow(selectedRows[i]);
                    }
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                editSelectedRow();
            }
        });
    }

    /**
     * Initialize the view with the given data.
     *
     * @param ruleSet   the JMS message property rule set
     */
    public void setData(JmsMessagePropertyRuleSet ruleSet) {
        if (ruleSet == null || ruleSet.isPassThruAll()) {
            passThroughAllRadioButton.doClick();
        } else {
            customizeRadioButton.doClick();
            for (JmsMessagePropertyRule rule : ruleSet.getRules()) {
                customTableModel.addRow(dataToRow(rule));
            }
            customTable.getSelectionModel().clearSelection();
        }
    }

    /**
     * @return data from the view
     */
    public JmsMessagePropertyRuleSet getData() {
        final int numRows = customTable.getRowCount();
        final JmsMessagePropertyRule[] rules = new JmsMessagePropertyRule[numRows];
        for (int row = 0; row < numRows; ++ row) {
            rules[row] = rowToData(row);
        }
        return new JmsMessagePropertyRuleSet( passThroughAllRadioButton.isSelected(), rules);
    }

    private void editSelectedRow() {
        final int row = customTable.getSelectedRow();
        if (row != -1) {
            final JmsMessagePropertyRule rule = rowToData(row);
            final JmsMessagePropertyDialog editor = new JmsMessagePropertyDialog(owner, getExistingNames(), rule);
            Utilities.centerOnScreen(editor);
            editor.pack();
            editor.setVisible(true);
            if (editor.isOKed()) {
                final Object[] cells = dataToRow(rule);
                customTable.setValueAt(cells[0], row, 0);
                customTable.setValueAt(cells[1], row, 1);
            }
        }
    }

    private Object[] dataToRow(JmsMessagePropertyRule rule) {
        final String name = rule.getName();
        String value;
        if (rule.isPassThru()) {
            value = PASS_THROUGH;
        } else {
            value = rule.getCustomPattern();
        }
        return new Object[]{ name, value };
    }

    private JmsMessagePropertyRule rowToData(int row) {
        final TableModel model = customTable.getModel();
        final String name = (String)model.getValueAt(row, 0);
        final String value = (String)model.getValueAt(row, 1);
        boolean passThrough;
        String pattern;
        if ( PASS_THROUGH.equals(value)) {
            passThrough = true;
            pattern = null;
        } else {
            passThrough = false;
            pattern = value;
        }
        return new JmsMessagePropertyRule(name, passThrough, pattern);
    }

    private Set<String> getExistingNames() {
        final Set<String> existingNames = new HashSet<>(customTable.getRowCount());
        for (int i = 0; i < customTable.getRowCount(); ++ i) {
            existingNames.add((String)customTable.getValueAt(i, 0));
        }
        return existingNames;
    }

    private boolean tableAscending = false;

    /**
     * Sort the rows of the custom table by property name in toggled order.
     */
    private void sortTable() {
        final int rowCount = customTableModel.getRowCount();
        for (int i = 0; i < rowCount; ++ i) {
            for (int j = i + 1; j < rowCount; ++ j) {
                final String name_i = customTable.getValueAt(i, 0).toString();
                final String name_j = customTable.getValueAt(j, 0).toString();
                if (tableAscending) {
                    if (name_i.compareTo(name_j) < 0) {
                        swapRows(i, j);
                    }
                } else {
                    if (name_i.compareTo(name_j) > 0) {
                        swapRows(i, j);
                    }
                }
            }
        }
        tableAscending = !tableAscending;
    }

    /**
     * Swaps the cell contents of two rows in the custom table.
     * @param row1  index of row 1
     * @param row2  index of row 2
     */
    private void swapRows(final int row1, final int row2) {
        for (int column = 0; column < customTable.getColumnCount(); ++ column) {
            final Object value_1 = customTable.getValueAt(row1, column);
            final Object value_2 = customTable.getValueAt(row2, column);
            customTable.setValueAt(value_2, row1, column);
            customTable.setValueAt(value_1, row2, column);
        }
    }

}
