/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.table;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * The class represents the {@link TableCellEditor} implementation that renders
 * the string value as a string value and the <code>JButton</code> in the single
 * cell. The button is accessed with {@link ButtonCellEditor#getButton()} where
 * an action can be attached to it, button label ( default "...") can be changed
 * etc.
 * <p/>
 * Here is the example usage that attaches the cell editor to the first column,
 * and then attaches the editing action to the cell editor button.
 * <p/>
 * The action is responsible for finishing editing by invoking one of
 * {@link javax.swing.CellEditor#stopCellEditing()}  to accept edits or
 * {@link javax.swing.CellEditor#cancelCellEditing()} to cancel edits.
 * <p/>
 * <pre>
 * final JTable table = new JTable(dm);
 * <p/>
 * final ButtonCellEditor cellEditor = ButtonCellEditor.attach(table, 0);
 * <p/>
 * cellEditor.getButton().addActionListener(new ActionListener() {
 *      public void actionPerformed(ActionEvent e) {
 *          JOptionPane.showMessageDialog(table, cellEditor.getCellEditorValue());
 *          cellEditor.setCellEditorValue("bleeee");
 *          cellEditor.stopCellEditing();
 *      }
 * });
 * <p/>
 * </pre>
 * <p/>
 * Sharing the same <code>CellEditor</code> instance between multiple columns
 * at the  same time is not supported.
 *
 * @author emil
 * @version Oct 18, 2004
 */
public class ButtonCellEditor extends JPanel implements TableCellEditor {
    protected JLabel textLabel = new JLabel();
    protected JButton button = new JButton("...");
    protected JTable table;
    private DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

    /**
     * Attach the <code>ButtonCellEditor</code> to the table column
     * specified by column index.
     *
     * @param table       the table that hosts the column
     * @param columnIndex the column index
     * @return the <code>ButtonCellEditor</code> attached
     */
    public static ButtonCellEditor attach(JTable table, int columnIndex) {
        if (table == null || columnIndex < 0) {
            throw new IllegalArgumentException();
        }
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        ButtonCellEditor cellEditor = new ButtonCellEditor(table);
        column.setCellEditor(cellEditor);
        column.setCellRenderer(new ButtonEditorCellRender(table));
        return cellEditor;
    }


    public ButtonCellEditor(JTable table) {
        this.table = table;
        setLayout(new BorderLayout());
        textLabel.setBorder(null);
        textLabel.setFont(table.getFont());
        add(textLabel, BorderLayout.WEST);
        add(button, BorderLayout.EAST);
        setForeground(table.getSelectionForeground());
        setBackground(table.getSelectionBackground());
    }

    // implements javax.swing.table.TableCellEditor
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {

        String s = value.toString();
        textLabel.setText(s);
        Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);

        Dimension d = c.getPreferredSize();
        Dimension bd = button.getPreferredSize();
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        Dimension td = new Dimension(columnWidth - (bd.width + 2), d.height);
        textLabel.setPreferredSize(td);
        return this;
    }

    public Object getCellEditorValue() {
        return textLabel.getText();
    }


    public void setCellEditorValue(String s) {
        textLabel.setText(s);
    }

    public JButton getButton() {
        return button;
    }

    public boolean isCellEditable(EventObject anEvent) {
        if (anEvent instanceof MouseEvent) {
            final MouseEvent me = (MouseEvent)anEvent;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    MouseEvent me2 = SwingUtilities.convertMouseEvent(table, me, button);
                    if (!button.contains(me2.getPoint())) {
                        cancelCellEditing();
                    }
                }
            });
            return me.getClickCount() >= 1;
        }
        return false;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    protected void fireEditingStopped() {
        ChangeEvent changeEvent = null;

        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((CellEditorListener)listeners[i + 1]).editingStopped(changeEvent);
            }
        }
    }

    protected void fireEditingCanceled() {
        ChangeEvent changeEvent = null;
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((CellEditorListener)listeners[i + 1]).editingCanceled(changeEvent);
            }
        }
    }

    /**
     * The renderer to use
     */
    protected static class ButtonEditorCellRender extends JPanel implements TableCellRenderer {

        protected JLabel textLabel = new JLabel();
        protected JButton button = new JButton("...");
        protected JTable table;
        private DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();

        public ButtonEditorCellRender(JTable table) {
            this.table = table;
            setLayout(new BorderLayout());
            add(textLabel, BorderLayout.WEST);
            textLabel.setFont(table.getFont());
            add(button, BorderLayout.EAST);
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            Component c = defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            Dimension d = c.getPreferredSize();
            Dimension bd = button.getPreferredSize();
            int columnWidth = table.getColumnModel().getColumn(column).getWidth();
            Dimension td = new Dimension(columnWidth - (bd.width + 2), d.height);
            textLabel.setPreferredSize(td);
            textLabel.setText(value.toString());
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
                textLabel.setForeground(table.getSelectionForeground());
                textLabel.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
                textLabel.setForeground(table.getForeground());
                textLabel.setBackground(table.getBackground());
            }
            return this;
        }
    }

//    public static void main(String[] args) {
//        JDialog d = new JDialog((JFrame)null, true);
//        MimePartsTable table = new MimePartsTable();
//        ButtonCellEditor be = ButtonCellEditor.attach(table, 1);
//
//        Vector pv = new Vector();
//
//        final MimePartInfo mp = new MimePartInfo("portfolioData", "application/x-zip-compressed");
//        pv.add(mp);
//        //table.removeAll();
//        table.getTableSorter().setData(pv);
//
//        Object[][] rows = new Object[][]{
//            {"param", "text/xml", new Integer(101) }
//        };
//
//        String[] cols = new String[]{
//            "Parameter Name", "MIME Part Content Type", "MIME Part Length Max. (KB)"
//        };
//        // JTable table = new JTable();
//        DefaultTableModel model = new DefaultTableModel(rows, cols);
//        //table.setModel(model);
//
//
//        Container cp = d.getContentPane();
//        cp.setLayout(new BorderLayout());
//        TableCellRenderer br = table.getColumnModel().getColumn(1).getCellRenderer();
//        //cp.add(br.getTableCellRendererComponent(table, mp.retrieveAllContentTypes(), false, false, 1, 1), BorderLayout.CENTER);
//        cp.add(new JScrollPane(table));
//        d.pack();
//        d.show();
//    }
}
