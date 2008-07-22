package com.l7tech.console.table;

import com.l7tech.console.util.ArrowIcon;
import com.l7tech.gui.util.JTableColumnResizeMouseListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.util.EventObject;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssociatedLogsTable extends JTable {

    private static int[] DEFAULT_COLUMN_WIDTHS ={175, 80, 80, 40, 400};

    private AssociatedLogsTableSorter associatedLogsTableModel = null;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    public AssociatedLogsTable() {
        setModel(getAssociatedLogsTableModel());
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnModel(getLogColumnModel());
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        addMouseListenerToHeaderInTable();
    }

    /**
     * Get table sorter which is the table model being used in this table.
     *
     * @return AssociatedLogsTableSorter  The table model with column sorting.
     */
    public AssociatedLogsTableSorter getTableSorter() {
        return associatedLogsTableModel;
    }

    private JButton buildButton() {
        JButton button = new JButton("...");
        button.setFont(button.getFont().deriveFont(9));
        return button;
    }

    private JComponent buildDetailComponent(JButton button) {
        JPanel detailPanel = new JPanel(){
            public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
            public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
            public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
            public boolean isOpaque() { return true; }
        };
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.X_AXIS));
        detailPanel.add(Box.createHorizontalGlue());
        detailPanel.add(button);
        return detailPanel;
    }

    /**
     * Return LogColumnModel property value
     * @return  DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        final DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[0]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_SECURITY_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[1]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[2]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_CODE_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[3]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[4]));

        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer(){
            JButton detailRenderButton = buildButton();
            JComponent detailRenderComponent = buildDetailComponent(detailRenderButton);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (comp instanceof JLabel) {
                    ((JLabel)comp).setText("");
                }
                String detailText = (String) value;
                if (detailText != null && detailText.trim().length() > 0) {
                    detailRenderComponent.setBackground(comp.getBackground());
                    detailRenderComponent.setBorder(comp.getBorder());
                    comp = detailRenderComponent;
                }
                return comp;
            }
        });

        CellEditorWithButton cellEditorWithButton = new CellEditorWithButton();
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX).setCellEditor(cellEditorWithButton);
        this.getSelectionModel().addListSelectionListener(cellEditorWithButton);

        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(comp instanceof JLabel) {
                    String detailText = ((JLabel)comp).getText();
                    if (detailText == null || detailText.trim().length() == 0) {
                        ((JComponent)comp).setToolTipText(null);
                    } else {
                        ((JComponent)comp).setToolTipText(detailText);
                    }
                }
                return comp;
            }
        });

        for(int i = 0; i < columnModel.getColumnCount(); i++){
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getAssociatedLogsTableModel().getColumnName(i));
        }

        return columnModel;
    }


    /**
     * create the table model with log fields
     *
     * @return DefaultTableModel
     *
     */
    private AssociatedLogsTableSorter getAssociatedLogsTableModel() {
        if (associatedLogsTableModel != null) {
            return associatedLogsTableModel;
        }

        String[] cols = {"Time", "Severity", "Detail", "Code", "Message"};
        String[][] rows = new String[][]{};

        associatedLogsTableModel = new AssociatedLogsTableSorter(new DefaultTableModel(rows, cols)) {
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };

        return associatedLogsTableModel;
    }

    /**
     * Add a mouse listener to the Table to trigger a table sort
     * when a column heading is clicked in the JTable.
     */
    private void addMouseListenerToHeaderInTable() {

        final JTable tableView = this;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int viewColumn = tableView.columnAtPoint(e.getPoint());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    ((AssociatedLogsTableSorter) tableView.getModel()).sortData(column, true);
                    ((AssociatedLogsTableSorter) tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
        th.addMouseListener(new JTableColumnResizeMouseListener(tableView, DEFAULT_COLUMN_WIDTHS));
    }

    // This customized renderer can render objects of the type TextandIcon
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Inherit the colors and font from the header component
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                    setHorizontalTextPosition(SwingConstants.LEFT);
                }
            }

            setText((String) value);

            if (getTableSorter().getSortedColumn() == column) {

                if (getTableSorter().isAscending()) {
                    setIcon(upArrowIcon);
                } else {
                    setIcon(downArrowIcon);
                }
            }
            else{
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

    private static class DetailViewDialog extends JDialog {
        private DetailViewDialog(final String detail) {
            super((Frame)null/*(Frame)SwingUtilities.getWindowAncestor(AssociatedLogsTable.this)*/, true);
            setTitle("Associated Log - Detail");
            JPanel panel = new JPanel(new BorderLayout());

            // configure text display component
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setText(detail);
            JScrollPane sp = new JScrollPane(textArea);
            panel.add(sp, BorderLayout.CENTER);

            add(panel, BorderLayout.CENTER);
            Utilities.setEscKeyStrokeDisposes(this);
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            pack();
            Utilities.centerOnScreen(this);
        }
    }

    private class CellEditorWithButton extends AbstractCellEditor implements TableCellEditor, ListSelectionListener {
        private final JButton detailEditButton;
        private final JComponent detailEditComponent;
        private int row;
        private String value;

        private CellEditorWithButton() {
            detailEditButton = buildButton();
            detailEditComponent = buildDetailComponent(detailEditButton);

            // When the color is exactly the same as the row bg the panel
            // does not get painted correctly, not sure why.
            Color selColor = AssociatedLogsTable.this.getSelectionBackground();
            Color color = selColor.getRed()<255 && selColor.getGreen()<255 && selColor.getBlue()<255 ?
                    new Color(selColor.getRed()+1, selColor.getGreen()+1, selColor.getBlue()+1) :
                    new Color(selColor.getRed(), selColor.getGreen(), selColor.getBlue());

            detailEditComponent.setBackground(color);
            detailEditButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (value != null) {
                        DialogDisplayer.display(new DetailViewDialog(value), new Runnable() {
                            public void run() {
                                // Make the renderer reappear.
                                fireEditingStopped();
                            }
                        });
                    }
                }
            });

            detailEditButton.setNextFocusableComponent(AssociatedLogsTable.this);
            detailEditComponent.setNextFocusableComponent(AssociatedLogsTable.this);
        }

        public void valueChanged(ListSelectionEvent e) {
            if(AssociatedLogsTable.this.isEditing() && row!=AssociatedLogsTable.this.getSelectedRow())
                this.fireEditingCanceled(); // stop edit when another row is selected
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = (String) value;
            this.row = row;
            return detailEditComponent;
        }

        public Object getCellEditorValue() {
            return value;
        }

        public boolean isCellEditable(EventObject anEvent) {
            boolean editable = false;

            String value = getValue(anEvent);
            if (value != null && value.trim().length() > 0) {
                editable = true;
            }

            return editable;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                detailEditComponent.dispatchEvent((MouseEvent)anEvent);
            }
            return true;
        }

        public boolean stopCellEditing() {
            return false;
        }

        private String getValue(EventObject anEvent) {
            String value = null;

            if (anEvent instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) anEvent;
                int row    = AssociatedLogsTable.this.rowAtPoint(me.getPoint());
                int column = AssociatedLogsTable.this.columnAtPoint(me.getPoint());

                value = (String) AssociatedLogsTable.this.getValueAt(row, column);
            }

            return value;
        }
    }
}
