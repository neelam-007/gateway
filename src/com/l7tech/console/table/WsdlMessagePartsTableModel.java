package com.l7tech.console.table;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Class <code>WsdlMessagePartsTableModel</code> is an implementation
 * of <code>TableModel</code> that holds the wsdl message parts.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class WsdlMessagePartsTableModel extends AbstractTableModel {
    private Message message;
    private Definition definition;

    /**
     * Create the new <code>WsdlMessagePartsTableModel</code>
     * @param m the message that this model represents
     * @param d the definition that the message belongs to
     */
    public WsdlMessagePartsTableModel(Message m, Definition d) {
        message = m;
        definition = d;
    }

    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    public int getColumnCount() {
        return 2;
    }

    /**
     *  Returns the column class.
     *
     *  @param columnIndex  the column being queried
     *  @return the column class
     */
    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else if (columnIndex == 1) {
            return QName.class;
        }
        throw new IndexOutOfBoundsException("column may be 1 or 0. received " + columnIndex);
    }

    /**
     *  add the value to the 
     *  this method if their data model is not editable.
     *
     *  @param  aValue   value to assign to cell
     *  @param  rowIndex   row of cell
     *  @param  columnIndex  column of cell
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null) {
            throw new IllegalArgumentException("value is null");
        }
        Part p = getPartAt(rowIndex);
        if (columnIndex == 0) {
            removePart(p.getName());
            p.setName(aValue.toString());
            addPart(p);
        } else if (columnIndex == 1){
            if (aValue instanceof QName) {
                p.setTypeName((QName)aValue);
            } else {
                throw new IllegalArgumentException(
                  "Expected "+QName.class.getName() +
                  "Received "+aValue.getClass());
            }
        } else {
            throw new IndexOutOfBoundsException("" + columnIndex + " >= " +getColumnCount());
        }
    }


    /**
     * Returns the number of rows in the model, that is the
     * number of message parts.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    public int getRowCount() {
        return message.getParts().size();
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     *                      (1 based)
     * @param	columnIndex the column whose value is to be queried
     *                      this field is ignored as
     * @return	the value Object at the specified cell
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        Part p = getPartAt(rowIndex);
        if (columnIndex == 0) {
            return p.getName();
        } else if (columnIndex == 1){
            return p.getTypeName();
        }
        throw new IndexOutOfBoundsException("" + columnIndex + " >= " +getColumnCount());
    }

    /**
     * create and add an empty part  by name
     * to the message parts table
     * @param name the <code>Part</code> name local part
     * @return the newly created message
     */
    public Part addPart(String name) {
        Part p = definition.createPart();
        p.setName(name);
        addPart(p);
        return p;
    }

    /**
     * add the part <code>Part</code> to the
     * message table
     * @param p the part to add
     */
    public void addPart(Part p) {
        message.addPart(p);
        this.fireTableStructureChanged();
    }

    /**
     * remove the message by name
     * @param name the message name local part
     */
    public void removePart(String name) {
        Object removed = message.getParts().remove(name);
        if (removed != null) {
            this.fireTableStructureChanged();
        }
    }

    /**
     * remove the message by <code>index</code>
     * @param index the message index
     */
    public Part removePart(int index) {
        Part p = getPartAt(index);
        Part removed = (Part)message.getParts().remove(p.getName());
        this.fireTableStructureChanged();
        return removed;
    }

    /**
     *  Returns false.  This is the default implementation for all cells.
     *
     *  @param  rowIndex  the row being queried
     *  @param  columnIndex the column being queried
     *  @return false
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     *  Returns a the name for the columns. There is a part name and the
     * type column
     *
     * @param column  the column being queried
     * @return a string containing the default name of <code>column</code>
     */
    public String getColumnName(int column) {
        if (column == 0) {
            return "Name";
        } else if (column == 1) {
            return "Type";
        }
        throw new IndexOutOfBoundsException("column may be 1 or 0. received " + column);
    }

    /**
     * Returns the Part at the  row <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     *                      (1 based)
     * @return	the Part at the specified row
     */
    public Part getPartAt(int rowIndex) {
        Iterator it = message.getParts().values().iterator();
        int row = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if (row++ == rowIndex) {
                Part p = (Part)o;
                return p;
            }
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + message.getParts().size());
    }

}
