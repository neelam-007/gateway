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

    private static int[] DEFAULT_COLUMN_WIDTHS = {175, 80, 80, 40, 400};

    private AssociatedLogsTableSorter associatedLogsTableModel = null;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    private DefaultTableColumnModel columnModel;

//    int width = DEFAULT_COLUMN_WIDTHS[4] - 45;//45 ~ approx size of button

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

    private JComponent buildButtonComponent(JButton button) {
        JPanel detailPanel = new JPanel() {
            public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
            }

            public void firePropertyChange(String propertyName, char oldValue, char newValue) {
            }

            public void firePropertyChange(String propertyName, int oldValue, int newValue) {
            }

            public boolean isOpaque() {
                return true;
            }
        };
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.X_AXIS));
        detailPanel.add(Box.createHorizontalGlue());
        detailPanel.add(button);
        return detailPanel;
    }

    /**
     * Return LogColumnModel property value
     *
     * @return DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        if(columnModel != null){
            return columnModel;
        }
        columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[0]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_SECURITY_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[1]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[2]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_CODE_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[3]));
        columnModel.addColumn(new TableColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX, DEFAULT_COLUMN_WIDTHS[4]));

        //detail column
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            JButton detailRenderButton = buildButton();
            JComponent detailRenderComponent = buildButtonComponent(detailRenderButton);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setText("");
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

        CellEditorWithButton detailCellEditorWithButton = new CellEditorWithButton("Detail");
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_DETAIL_COLUMN_INDEX).setCellEditor(detailCellEditorWithButton);
        this.getSelectionModel().addListSelectionListener(detailCellEditorWithButton);

        //message column
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            JButton messageRenderButton = buildButton();
            JComponent messageRenderComponent = buildButtonComponent(messageRenderButton);

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                String messageText = (String) value;
                if (messageText != null && messageText.trim().length() > 0 && (messageText.length() > 200 || messageText.contains("\n"))) {
                    JLabel textLabel = new JLabel(messageText, SwingConstants.LEFT);
                    textLabel.setPreferredSize(new Dimension(columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).getWidth() - 45, 25));

                    messageRenderComponent.setBackground(comp.getBackground());
                    messageRenderComponent.setBorder(comp.getBorder());

                    JPanel messagePane = new JPanel();
                    messagePane.setBackground(comp.getBackground());
                    messagePane.setLayout(new BorderLayout());
                    messagePane.add(textLabel, BorderLayout.WEST);
                    messagePane.add(messageRenderComponent, BorderLayout.EAST);
                    comp = messagePane;
                }

                //set tooltip
                if (messageText == null || messageText.trim().length() == 0) {
                    comp.setToolTipText(null);
                } else {
                    if (messageText.length() < 4096) {
                        comp.setToolTipText("<html><pre>" + messageText + "</pre</html>");//sanitize in case messageText contains html?
                    }
                }

                return comp;
            }
        });

        CellEditorWithButton messageCellEditorWithButton = new CellEditorWithButton("Message");
        columnModel.getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).setCellEditor(messageCellEditorWithButton);
        this.getSelectionModel().addListSelectionListener(messageCellEditorWithButton);


        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getAssociatedLogsTableModel().getColumnName(i));
        }

        return columnModel;
    }


    /**
     * create the table model with log fields
     *
     * @return DefaultTableModel
     */
    private AssociatedLogsTableSorter getAssociatedLogsTableModel() {
        if (associatedLogsTableModel != null) {
            return associatedLogsTableModel;
        }

        String[] cols = {"Time", "Severity", "Detail", "Code", "Message"};
        String[][] rows = new String[][]{};

        associatedLogsTableModel = new AssociatedLogsTableSorter(new DefaultTableModel(rows, cols)) {
            public boolean isCellEditable(int row, int col) {
                return col == 2 || col == 4;
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
            // Inherit the colors and font from the header buttonComponent
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
            } else {
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

    private static class ViewDialog extends JDialog {
        private ViewDialog(final String title, final String detail) {
            super((Frame) null, true);
            setTitle("Associated Log - " + title);
            JPanel panel = new JPanel(new BorderLayout());

            // configure text display buttonComponent
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
        private final JButton button;
        private final JComponent buttonComponent;
        private int row;
        private String value;

        private CellEditorWithButton(final String columnTitle) {
            button = buildButton();
            buttonComponent = buildButtonComponent(button);
            buttonComponent.setBackground(getColour());

            this.button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (value != null) {
                        DialogDisplayer.display(new ViewDialog(columnTitle, value), new Runnable() {
                            public void run() {
                                // Make the renderer reappear.
                                fireEditingStopped();
                            }
                        });
                    }
                }
            });

            this.button.setNextFocusableComponent(AssociatedLogsTable.this);
            this.buttonComponent.setNextFocusableComponent(AssociatedLogsTable.this);
        }

        public void valueChanged(ListSelectionEvent e) {
            if (AssociatedLogsTable.this.isEditing() && row != AssociatedLogsTable.this.getSelectedRow()) {
                this.fireEditingCanceled(); // stop edit when another row is selected
            }
        }

        // When the color is exactly the same as the row bg the panel
        // does not get painted correctly, not sure why.
        private Color getColour() {
            Color selColor = AssociatedLogsTable.this.getSelectionBackground();
            return selColor.getRed() < 255 && selColor.getGreen() < 255 && selColor.getBlue() < 255 ?
                    new Color(selColor.getRed() + 1, selColor.getGreen() + 1, selColor.getBlue() + 1) :
                    new Color(selColor.getRed(), selColor.getGreen(), selColor.getBlue());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = (String) value;
            this.row = row;
            fireEditingStopped();
            // Case 1: for "Message" tab in the 5th Column (index = 4)
            if (column == 4) {
                JComponent tempButtonComponent = buildButtonComponent(button);
                JLabel textLabel = new JLabel(this.value, SwingConstants.LEFT);
                textLabel.setPreferredSize(new Dimension(getLogColumnModel().getColumn(AssociatedLogsTableSorter.ASSOCIATED_LOG_MSG_COLUMN_INDEX).getWidth() - 45, 25));
                JPanel messagePane = new JPanel();
                messagePane.setBackground(getColour());
                messagePane.setLayout(new BorderLayout());
                messagePane.add(textLabel, BorderLayout.WEST);

                if (this.value != null && this.value.trim().length() > 0 && (this.value.length() > 200 || this.value.contains("\n"))) {
                    messagePane.add(tempButtonComponent, BorderLayout.EAST);
                }

                return messagePane;
            }

            // Case 2: for other tabs, for example, "Detail" tab in the 3rd Column (index = 2)
            return buttonComponent;
        }

        public Object getCellEditorValue() {
            return value;
        }

        public boolean isCellEditable(EventObject anEvent) {
            int column;
            if (anEvent instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) anEvent;
                column = AssociatedLogsTable.this.columnAtPoint(me.getPoint());
                String value = getValue(anEvent);
                if (column == 4 && value != null && value.length() > 0 && (value.length() > 200 || value.contains("\n"))) {
                    return true;
                } else if (column == 2 && value != null && value.length() > 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                buttonComponent.dispatchEvent((MouseEvent)anEvent);
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
                int row = AssociatedLogsTable.this.rowAtPoint(me.getPoint());
                int column = AssociatedLogsTable.this.columnAtPoint(me.getPoint());

                value = (String) AssociatedLogsTable.this.getValueAt(row, column);
            }

            return value;
        }
    }
}
