package com.l7tech.console.table;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.Operation;
import java.util.Iterator;

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
        if (def == null || pt == null){
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
        return 1;
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
        Iterator it = portType.getOperations().iterator();
        int row = 0;
        while (it.hasNext()) {
            Object m = it.next();
            if (row++ == rowIndex) return m;
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + portType.getOperations().size());
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
        portType.addOperation(op);
        return op;
    }

    /**
     * add the operation <code>Operation</code> to
     * the port type operations table
     * @param operation the message to add
     */
    public void addOperation(Operation operation) {
        portType.addOperation(operation);
        this.fireTableStructureChanged();
    }


    /**
     * remove the message by name
     * @param name the message name local part
     */
    public void removeOperation(String name) {
        portType.getOperations().iterator();
        for (Iterator iterator = portType.getOperations().iterator(); iterator.hasNext();) {
            Operation operation = (Operation)iterator.next();
            if (name.equals(operation.getName())) {
                portType.getOperations().remove(operation);
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
        Iterator it = portType.getOperations().iterator();
        int row = 0;
        while (it.hasNext()) {
            Operation op = (Operation)it.next();
            if (row++ == index) {
                portType.getOperations().remove(op);
                this.fireTableStructureChanged();
                return op;
            }
        }
        throw new IndexOutOfBoundsException("" + index + " > " + portType.getOperations().size());
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

}
