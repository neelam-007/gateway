package com.l7tech.console.table;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.Iterator;
import java.util.Collections;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

import com.l7tech.wsdl.MimePartInfo;

/**
 * Extra mime parts table.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ExtraMimePartsTable extends JTable {

    //- PUBLIC

    public ExtraMimePartsTable() {

        setModel(getMimePartsTableModel());
        getColumnModel().getColumn(ExtraMimePartsTableModel.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX).setMinWidth(180);
        getColumnModel().getColumn(ExtraMimePartsTableModel.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX).setPreferredWidth(220);
        getColumnModel().getColumn(ExtraMimePartsTableModel.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX).setMinWidth(100);
        getColumnModel().getColumn(ExtraMimePartsTableModel.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX).setPreferredWidth(120);
        getTableHeader().setReorderingAllowed(false);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // REPLACE default editors with our own bug fixed versions
        setDefaultEditor(Object.class, new GenericEditor());
        setDefaultEditor(Number.class, new NumberEditor());

        TableCellEditor cellEditor = getDefaultEditor(Number.class);
        if(cellEditor instanceof DefaultCellEditor) {
            ((DefaultCellEditor) cellEditor).setClickCountToStart(1);
        }

        TableCellEditor cellEditor2 = getDefaultEditor(Object.class);
        if(cellEditor2 instanceof DefaultCellEditor) {
            ((DefaultCellEditor) cellEditor2).setClickCountToStart(1);
        }

        // this will force the cell editor stop editing
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        addMouseListenerToHeaderInTable();
    }

    public void clear() {
        TableCellEditor editor = getCellEditor();
        if (editor != null) editor.cancelCellEditing();
        tableModel.setData(Collections.EMPTY_LIST);
    }

    //- PRIVATE

    private ExtraMimePartsTableModel tableModel = null;

    /**
     *
     */
    private ExtraMimePartsTableModel getMimePartsTableModel() {

        if (tableModel == null) {
            Object[][] rows = new Object[][]{};

            String[] cols = new String[]{
                "MIME Part Content Type", "MIME Part Length Max. (KB)"
            };

            tableModel = new ExtraMimePartsTableModel(new DefaultTableModel(rows, cols));
        }

        return tableModel;
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (rowIndex==0 && columnIndex==0) {
            // Ensure empty row exists
            java.util.List mimePartInfos = tableModel.getData();
            int selectRow = mimePartInfos.size();
            boolean hasEmptyRow = false;
            for (Iterator iterator = mimePartInfos.iterator(); iterator.hasNext();) {
                MimePartInfo mimePartInfo = (MimePartInfo) iterator.next();
                if (mimePartInfo.retrieveAllContentTypes().length()==0) {
                    hasEmptyRow = true;
                    break;
                }
            }
            if (!hasEmptyRow) {
                tableModel.addEmptyRow();
                rowIndex = selectRow;
            }

            // fill in any missing data [unless currently selected row]
            for (Iterator iterator = mimePartInfos.iterator(); iterator.hasNext();) {
                MimePartInfo mimePartInfo = (MimePartInfo) iterator.next();
                if (mimePartInfo.retrieveAllContentTypes().length()>0) {
                    if (iterator.hasNext() || rowIndex!=selectRow-1) {
                        if (mimePartInfo.getMaxLength() < 0) mimePartInfo.setMaxLength(1000);
                    }
                }
            }
        }

        super.changeSelection(rowIndex, columnIndex, toggle, extend);
    }

    /**
     * Add a mouse listener to the Table to trigger a table sort
     * when a column heading is clicked in the JTable.
     */
    private void addMouseListenerToHeaderInTable() {
        this.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = ExtraMimePartsTable.this.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = ExtraMimePartsTable.this.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {
                    ((ExtraMimePartsTableModel) ExtraMimePartsTable.this.getModel()).sortData(column, true);
                    ((ExtraMimePartsTableModel) ExtraMimePartsTable.this.getModel()).fireTableDataChanged();
                    ExtraMimePartsTable.this.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = ExtraMimePartsTable.this.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    /**
     * REPLACE THE DEFAULT EDITOR TO FIX BUG
     */
    static class GenericEditor extends DefaultCellEditor {

	Class[] argTypes = new Class[]{String.class};
	java.lang.reflect.Constructor constructor;
	Object value;

	public GenericEditor() {
            super(new JTextField());
            getComponent().setName("Table.editor");
        }

	public boolean stopCellEditing() {
	    String s = (String)super.getCellEditorValue();
	    // Here we are dealing with the case where a user
	    // has deleted the string value in a cell, possibly
	    // after a failed validation. Return null, so that
	    // they have the option to replace the value with
	    // null or use escape to restore the original.
	    // For Strings, return "" for backward compatibility.
	    if ("".equals(s)) {
		    if (constructor.getDeclaringClass() == String.class) {
		        value = s;
		    }
		    return super.stopCellEditing(); // FIX FOR JDK BUG HERE (add return)
	    }

	    try {
		value = constructor.newInstance(new Object[]{s});
	    }
	    catch (Exception e) {
		((JComponent)getComponent()).setBorder(new LineBorder(Color.red));
		return false;
	    }
	    return super.stopCellEditing();
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
						 boolean isSelected,
						 int row, int column) {
	    this.value = null;
            ((JComponent)getComponent()).setBorder(new LineBorder(Color.black));
	    try {
		Class type = table.getColumnClass(column);
		// Since our obligation is to produce a value which is
		// assignable for the required type it is OK to use the
		// String constructor for columns which are declared
		// to contain Objects. A String is an Object.
		if (type == Object.class) {
		    type = String.class;
		}
		constructor = type.getConstructor(argTypes);
	    }
	    catch (Exception e) {
		return null;
	    }
	    return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	public Object getCellEditorValue() {
	    return value;
	}
    }

    static class NumberEditor extends GenericEditor {

	public NumberEditor() {
	    ((JTextField)getComponent()).setHorizontalAlignment(JTextField.RIGHT);
	}
    }
}
