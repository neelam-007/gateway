package com.l7tech.console.table;

import com.l7tech.common.xml.WsdlComposer;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class <code>WsdlOperationsTableModel</code> represents the port
 * type operations.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class WsdlOperationsTableModel extends AbstractTableModel {
    private WsdlComposer composer;
    private PortType portType;

    /**
     * Create the new <code>WsdlMessagesTableModel</code>
     * @param wc the WSDL Composer
     */
    public WsdlOperationsTableModel(WsdlComposer wc, PortType pt) {
        if (wc == null || pt == null) {
            throw new IllegalArgumentException();
        }
        composer = wc;
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
        return portType.getOperations().size() + 1;
    }

    /**
     *  Returns a the name for the columns. There is a part name and the
     * type column
     *
     * @param column  the column being queried
     * @return a string containing the default name of <code>column</code>
     */
    @Override
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
    @Override
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
        if (rowIndex == portType.getOperations().size()) return null;

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
     *
     */
    public Operation addOperation() {
        Operation operation;
        String newOperationName;
        int suffixAdd = 0;

        while (true) {
            newOperationName = "NewOperation" + (getRowCount() + suffixAdd);
            boolean found = true;
            int rows = getRowCount();
            for (int i = 0; i < rows; i++) {
                String name = (String) getValueAt(i, 0);
                if (newOperationName.equals(name)) {
                    found = false;
                    break;
                }
            }
            if (found) {
                operation = addOperation(newOperationName);
                break;
            }
            suffixAdd++;
        }

        return operation;
    }

    /**
     *
     */
    public Operation addOperation(String name) {
        Operation op = composer.createOperation();
        op.setName(name);
        op.setUndefined(false);
        addOperation(op);
        return op;
    }

    /**
     * add the operation <code>Operation</code> to
     * the port type operations table
     * @param operation the message to add
     */
    public void addOperation(Operation operation) {
        ensureInputOutputMessages( operation );
        portType.addOperation(operation);

        this.fireTableStructureChanged();
    }


    /**
     * remove the message by name
     * @param name the message name local part
     */
    public void removeOperation(String name) {
        List operations = portType.getOperations();
        for (Iterator iterator = operations.iterator(); iterator.hasNext();) {
            Operation operation = (Operation)iterator.next();
            if (name.equals(operation.getName())) {
                iterator.remove();
                composer.removeBindingOperationByOperation(operation);
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
        composer.removeBindingOperationByOperation(op);
        this.fireTableStructureChanged();
        return op;
    }

    /**
     *  Retuirns true. This is the default implementation for all cells.
     *
     *  @param  rowIndex  the row being queried
     *  @param  columnIndex the column being queried
     *  @return false
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     *  Set the cell value.
     *
     *  @param  aValue   value to assign to cell
     *  @param  rowIndex   row of cell
     *  @param  columnIndex  column of cell
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex == portType.getOperations().size()) {
            if (aValue != null) {
                Operation operation = null;
                if (columnIndex == 0) {
                    String name = (String) aValue;
                    if (name.length() > 0) operation = addOperation(name);
                } else if (columnIndex == 1) {
                    operation = addOperation();
                    if (operation.getInput() != null)
                        operation.getInput().setMessage((Message)aValue);
                } else if (columnIndex == 2) {
                    operation = addOperation();
                    if (operation.getOutput() != null)
                        operation.getOutput().setMessage((Message)aValue);
                }
                if (operation != null) ensureInputOutputMessages(operation);
            }
        }
        else {
            Operation op = getOperationAt(rowIndex);

            if (columnIndex == 0) {
                if (aValue == null) {
                    throw new IllegalArgumentException(" value == null ");
                }
                op.setName((String)aValue);
            } else if (columnIndex == 1) {
                if (aValue == null) {
                    op.setInput(null);
                } else {
                    setInputMessage(op, (Message) aValue);
                }
            } else if (columnIndex == 2) {
                if (aValue == null) {
                    op.setOutput(null);
                } else {
                    setOutputMessage(op, (Message) aValue);
                }
            } else {
                throw new IndexOutOfBoundsException("" + rowIndex + " > " + portType.getOperations().size());
            }
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
                return (Operation) o;
            }
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + portType.getOperations().size());
    }

    /**
     * Ensure that the input and output messages are set for the
     * operation.
     *
     * @param operation The operation to check
     */
    private void ensureInputOutputMessages(Operation operation) {
        Map messages = composer.getMessages();
        if(messages != null && !messages.isEmpty()) {
            Message defaultMessage = (Message) messages.values().iterator().next();

            if ( operation.getInput() == null ||
                 operation.getInput().getMessage() == null) {
                setInputMessage( operation, defaultMessage );
            }

            if ( operation.getOutput() == null ||
                 operation.getOutput().getMessage() == null ) {
                setOutputMessage( operation, defaultMessage );
            }
        }
    }

    private void setInputMessage( Operation operation, Message message ) {
        // create input if it does not exist
        if ( operation.getInput() == null ) {
            Input input = composer.createInput();
            operation.setInput( input );
        }

        // set input message and also name if available
        operation.getInput().setMessage( message );

        // todo: set name and binding operation input name
        /*if ( message.getQName() != null ) {
            operation.getInput().setName(message.getQName().getLocalPart());
        } else {
            operation.getInput().setName( null ); // name is optional
        }*/
    }

    private void setOutputMessage( Operation operation, Message message ) {
        // create output if it does not exist
        if ( operation.getOutput() == null ) {
            Output output = composer.createOutput();
            operation.setOutput( output );
        }

        // set output message and also name if available
        operation.getOutput().setMessage( message );

        // todo: set name and binding operation output name
        /*if ( message.getQName() != null ) {
            operation.getOutput().setName(message.getQName().getLocalPart());
        } else {
            operation.getOutput().setName( null ); // name is optional
        }*/
    }
}
