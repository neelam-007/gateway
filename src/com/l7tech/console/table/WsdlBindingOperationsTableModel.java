package com.l7tech.console.table;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.*;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>WsdlBindingOperationsTableModel</code> represents the
 * port  * type operations.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class WsdlBindingOperationsTableModel extends AbstractTableModel {
    private Definition definition;
    private Binding binding;

    /**
     * Create the new <code>WsdlMessagesTableModel</code>
     * @param def the WSDL definition
     * @param def the port type these operations belong to
     */
    public WsdlBindingOperationsTableModel(Definition def, Binding b) {
        if (def == null || b == null) {
            throw new IllegalArgumentException();
        }
        definition = def;
        binding = b;
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
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    public int getRowCount() {
        return binding.getBindingOperations().size();
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
            return "Operation";
        } else if (column == 1) {
            return "SOAP Action";
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
        if (column == 0 || column == 1) {
            return String.class;
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
        BindingOperation op = getOperationAt(rowIndex);
        if (columnIndex == 0) {
            return op.getName();
        } else if (columnIndex == 1) {
            String sa = getSoapAction(op);
            if (sa == null) sa = "";
            return sa;
        }
        int size = binding.getBindingOperations().size();
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + size);
    }

    /**
     * create and add an empty message by name 
     * to the message table 
     * @param name the message name local part
     * @return the newly created message
     */
    public BindingOperation addOperation(String name) {
        BindingOperation op = definition.createBindingOperation();
        op.setName(name);
        addOperation(op);
        return op;
    }

    /**
     * add the binding operation <code>Operation</code> to
     * the port type operations table
     * @param operation the message to add
     */
    public void addOperation(BindingOperation operation) {
        binding.addBindingOperation(operation);
        this.fireTableStructureChanged();
    }


    /**
     * remove the message by name
     * @param name the message name local part
     */
    public void removeOperation(String name) {
        List operations = binding.getBindingOperations();
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
    public BindingOperation removeOperation(int index) {
        BindingOperation op = getOperationAt(index);
        binding.getBindingOperations().remove(op);
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
        return columnIndex != 0;
    }

    /**
     *  Set the cell value.
     *
     *  @param  aValue   value to assign to cell
     *  @param  rowIndex   row of cell
     *  @param  columnIndex  column of cell
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null) {
            throw new IllegalArgumentException(" value == null ");
        }
        BindingOperation op = getOperationAt(rowIndex);
        if (columnIndex == 0) {
            op.setName((String)aValue);
        } else if (columnIndex == 1) {
            try {
                setSoapAction(op, (String)aValue);
            } catch (WSDLException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IndexOutOfBoundsException(" column " + columnIndex + "out of range");
        }
    }


    /**
     * Returns the Operation at the  row <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     *                      (1 based)
     * @return	the Operation at the specified row
     */
    private BindingOperation getOperationAt(int rowIndex) {
        List bindingOperations = binding.getBindingOperations();
        Iterator it = bindingOperations.iterator();
        int row = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if (row++ == rowIndex) {
                BindingOperation op = (BindingOperation)o;
                return op;
            }
        }
        throw new IndexOutOfBoundsException(" row " + rowIndex + " > " + bindingOperations.size());
    }

    /**
     * @param operation to get the soap action for
     * @return the soap action or null if none fould
     */
    private String getSoapAction(BindingOperation operation) {
        Iterator eels = operation.getExtensibilityElements().iterator();
        ExtensibilityElement ee;
        while (eels.hasNext()) {
            ee = (ExtensibilityElement)eels.next();
            if (ee instanceof SOAPOperation) {
                SOAPOperation sop = (SOAPOperation)ee;
                return sop.getSoapActionURI();
            }
        }
        return null;
    }

    /**
     * @param operation the binding operation to set the actin for
     * @param action the soap action to set
     */
    private void setSoapAction(BindingOperation operation, String action)
      throws WSDLException {
        List extensibilityElements = operation.getExtensibilityElements();
        Iterator eels = extensibilityElements.iterator();

        while (eels.hasNext()) {
            ExtensibilityElement ee;
            ee = (ExtensibilityElement)eels.next();
            if (ee instanceof SOAPOperation) {
                SOAPOperation sop = (SOAPOperation)ee;
                sop.setSoapActionURI(action);
                return;
            }
        }
        QName qn = new QName(action);
        ExtensibilityElement ee =
          definition.getExtensionRegistry().createExtension(SOAPOperation.class, qn);
        operation.addExtensibilityElement(ee);
    }


}
