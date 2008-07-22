package com.l7tech.console.table;

import com.l7tech.console.util.WsdlComposer;
import com.l7tech.xml.XmlSchemaConstants;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * Class <code>WsdlMessagePartsTableModel</code> is an implementation
 * of <code>TableModel</code> that holds the wsdl message parts.
 * <p/>
 * The parts are maintained in the internal linked collection,
 * to maintain predictable traversal order.
 * Note that the class is not aware of the external modifications
 * doen to the <code>Message</code> instance.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WsdlMessagePartsTableModel extends AbstractTableModel {
    private final WsdlComposer composer;
    private final List<Part> parts;

    /**
     * Create the new <code>WsdlMessagePartsTableModel</code>
     * 
     * @param parts the Parts that this model represents
     * @param composer the WsdlComposer object that this panel manipulates
     */
    public WsdlMessagePartsTableModel(final List<Part> parts, final WsdlComposer composer) {
        if (parts == null || composer == null) {
            throw new IllegalArgumentException();
        }

        this.composer = composer;
        this.parts = parts;
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
     * Returns the column class.
     * 
     * @param columnIndex the column being queried
     * @return the column class
     */
    @Override
    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else if (columnIndex == 1) {
            return QName.class;
        }
        throw new IndexOutOfBoundsException("column may be 1 or 0. received " + columnIndex);
    }

    /**
     * add the value to the
     * this method if their data model is not editable.
     * 
     * @param aValue      value to assign to cell
     * @param rowIndex    row of cell
     * @param columnIndex column of cell
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex == parts.size()) {
            if (aValue == null) return;
            if (columnIndex == 0) {
                String partName = (String) aValue;
                if (partName.trim().length() > 0) {
                    addPart(partName.trim());
                }
            }
            else if (columnIndex == 1) {
                Part newPart = addPart();
                newPart.setTypeName((QName)aValue);
            }
        }
        else {
            Part p = getPartAt(rowIndex);
            if (columnIndex == 0) {
                if (aValue == null) {
                    throw new IllegalArgumentException("value is null");
                }
                final String newName = aValue.toString();
                if ( newName.trim().length() == 0 ) {
                    removePart( rowIndex );
                    return;
                } else {
                    p.setName(newName.trim());
                }
            } else if (columnIndex == 1) {
                p.setTypeName((QName)aValue);
            }
            this.fireTableDataChanged();
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
        return parts.size() + 1;
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     * 
     * @param	rowIndex	the row whose value is to be queried
     * (1 based)
     * @param	columnIndex the column whose value is to be queried
     * this field is ignored as
     * @return	the value Object at the specified cell
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex == parts.size()) return null;
        Part p = getPartAt(rowIndex);
        if (columnIndex == 0) {
            return p.getName();
        } else if (columnIndex == 1) {
            return p.getTypeName();
        }
        throw new IndexOutOfBoundsException("" + columnIndex + " >= " + getColumnCount());
    }

    /**
     * create and add an empty part  by name
     * to the message parts table
     * 
     * @param name the <code>Part</code> name local part
     * @return the newly created message
     */
    public Part addPart(String name) {
        Part p = composer.createPart();
        p.setName(name);
        p.setTypeName(XmlSchemaConstants.QNAME_TYPE_STRING);
        addPart(p);
        return p;
    }

    public Part addPart() {
        return addPart(getNewMessagePartArgumentName());
    }

    /**
     * add the part <code>Part</code> to the
     * message table
     * 
     * @param p the part to add
     */
    public void addPart(Part p) {
        final int i = parts.size();
        p.setName(p.getName());
        parts.add(p);
        this.fireTableStructureChanged();
        this.fireTableRowsInserted(i,i);
    }

    /**
     * remove the message by name
     * 
     * @param name the message name local part
     */
    private Part removePart(String name) {
        Part toRemove = null;

        if (name != null) {
            for (Part part : parts) {
                if (part.getName().equals(name)) {
                    toRemove = part;
                    break;
                }
            }

            if (toRemove != null) {
                int index = parts.indexOf(toRemove);
                parts.remove(toRemove);
                this.fireTableRowsDeleted(index,index);
            }
        }

        return toRemove;
    }

    /**
     * remove the message by <code>index</code>
     * 
     * @param index the message index
     */
    public Part removePart(int index) {
        Part p = getPartAt(index);
        if (p != null) {
            return removePart(p.getName());
        }
        return null;
    }

    /**
     * Returns false.  This is the default implementation for all cells.
     * 
     * @param rowIndex    the row being queried
     * @param columnIndex the column being queried
     * @return false
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     * Returns a the name for the columns. There is a part name and the
     * type column
     * 
     * @param column the column being queried
     * @return a string containing the default name of <code>column</code>
     */
    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Name";
        } else if (column == 1) {
            return "Type";
        }
        throw new IndexOutOfBoundsException("column may be 1 or 0. received " + column);
    }

    /**
     * Returns the Part at the given <code>modelIndex</code>.
     * 
     * @param	modelIndex	the item whose value is to be queried
     * (1 based)
     * @return	the Part at the specified row
     */
    private Part getPartAt(int modelIndex) {
        return parts.get(modelIndex);
    }

    private String getNewMessagePartArgumentName() {
        String newMessagePartName;
        boolean found;
        int suffixAdd = 0;
        while (true) {
            int partNameSuffix = getRowCount() + suffixAdd;
            newMessagePartName = "arg" + partNameSuffix;
            found = true;
            int rows = getRowCount();
            for (int i = 0; i < rows; i++) {
                String name = (String) getValueAt(i, 0);
                if (newMessagePartName.equals(name)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                break;
            }
            suffixAdd++;
        }
        return newMessagePartName;
    }
}
