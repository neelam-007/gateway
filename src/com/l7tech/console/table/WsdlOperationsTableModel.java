package com.l7tech.console.table;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.*;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>WsdlOperationsTableModel</code> represents the port
 * type operations.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class WsdlOperationsTableModel extends AbstractTableModel {
    private Definition definition;
    private PortType portType;

    /**
     * Create the new <code>WsdlMessagesTableModel</code>
     * @param def the WSDL definition
     * @param def the port type these operations belong to
     */
    public WsdlOperationsTableModel(Definition def, PortType pt) {
        if (def == null || pt == null) {
            throw new IllegalArgumentException();
        }
        definition = def;
        portType = pt;
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
        return 3;
    }

    /**
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    public int getRowCount() {
        return portType.getOperations().size();
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
            return "Input Message";
        } else if (column == 2) {
            return "Output Message";
        }
        throw new IndexOutOfBoundsException("column " + column);
    }

    /**
     *  Returns the class for the <code>columnIndex</code>.
     *
     *  @param column  the column being queried
     *  @return the corresponding class
     */
    public Class getColumnClass(int column) {
        if (column == 0) {
            return String.class;
        } else if (column == 1) {
            return Input.class;
        } else if (column == 2) {
            return Output.class;
        }
        throw new IndexOutOfBoundsException("column " + column);
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
        Operation op = getOperationAt(rowIndex);
        if (columnIndex == 0) {
            return op.getName();
        } else if (columnIndex == 1) {
            return op.getInput();
        } else if (columnIndex == 2) {
            return op.getOutput();
        } else {
            throw new IndexOutOfBoundsException("" + rowIndex + " > " + portType.getOperations().size());
        }
    }

    /**
     * create and add an empty message by name 
     * to the message table 
     * @param name the message name local part
     * @return the newly created message
     */
    public Operation addOperation(String name) {
        Operation op = definition.createOperation();
        op.setName(name);
        addOperation(op);
        return op;
    }

    /**
     * add the operation <code>Operation</code> to
     * the port type operations table
     * @param operation the message to add
     */
    public void addOperation(Operation operation) {
        portType.addOperation(operation);
        operation.setInput(definition.createInput());
        operation.setOutput(definition.createOutput());

        this.fireTableStructureChanged();
    }


    /**
     * remove the message by name
     * @param name the message name local part
     */
    public void removeOperation(String name) {
        List operations = portType.getOperations();
        operations.iterator();
        for (Iterator iterator = operations.iterator(); iterator.hasNext();) {
            Operation operation = (Operation)iterator.next();
            if (name.equals(operation.getName())) {
                operations.remove(operation);
                this.fireTableStructureChanged();
                break;
            }
        }
    }

    /**
     * remove the message by <code>index</code>
     * @param index the message index
     */
    public Operation removeOperation(int index) {
        Operation op = getOperationAt(index);
        portType.getOperations().remove(op);
        this.fireTableStructureChanged();
        return op;
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
     *  This empty implementation is provided so users don't have to implement
     *  this method if their data model is not editable.
     *
     *  @param  aValue   value to assign to cell
     *  @param  rowIndex   row of cell
     *  @param  columnIndex  column of cell
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null) {
            throw new IllegalArgumentException(" value == null ");
        }
        Operation op = getOperationAt(rowIndex);
        if (columnIndex == 0) {
            if (!(aValue instanceof String)) {
                throw new IllegalArgumentException("Unsupported type "+aValue.getClass());
            }
            op.setName((String)aValue);
        } else if (columnIndex == 1) {
            if (!(aValue instanceof Message)) {
                throw new IllegalArgumentException("Unsupported type "+aValue.getClass()+ " expected "+Message.class);
            }
            op.getInput().setMessage((Message)aValue);
        } else if (columnIndex == 2) {
            if (!(aValue instanceof Message)) {
                throw new IllegalArgumentException("Unsupported type "+aValue.getClass() +" expected "+Message.class);
            }
            op.getOutput().setMessage((Message)aValue);
        } else {
            throw new IndexOutOfBoundsException("" + rowIndex + " > " + portType.getOperations().size());
        }
    }

    /**
     * Returns the Operation at the  row <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     *                      (1 based)
     * @return	the Operation at the specified row
     */
    private Operation getOperationAt(int rowIndex) {
        Iterator it = portType.getOperations().iterator();
        int row = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if (row++ == rowIndex) {
                Operation op = (Operation)o;
                return op;
            }
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + portType.getOperations().size());
    }

}
