package com.l7tech.console.table;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * Class WsdlMessagesTableModel.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class WsdlMessagesTableModel extends AbstractTableModel {
    private Definition definition;

    /**
     * Create the new <code>WsdlMessagesTableModel</code>
     * @param def
     */
    public WsdlMessagesTableModel(Definition def) {
        definition = def;
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
        return definition.getMessages().size();
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
        Iterator it = definition.getMessages().values().iterator();
        int row = 0;
        while (it.hasNext()) {
            Object m = it.next();
            if (row++ == rowIndex) return m;
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + definition.getMessages().size());
    }

    /**
     * create and add an empty message by name 
     * to the message table 
     * @param name the message name local part
     * @return the newly created message
     */
    public Message addMessage(String name) {
        return addMessage(new QName(definition.getTargetNamespace(), name));
    }

    /**
     * create then add the message by <code>QName</code> 
     * to the message table
     * @param name the message <code>QName</code>
     * @return the newly created message
     */
    public Message addMessage(QName name) {
        Message m = definition.createMessage();
        m.setQName(name);
        addMessage(m);
        return m;
    }

    /**
     * add the message <code>Message</code>  to 
     * the message table
     * @param message the message to add
     */
    public void addMessage(Message message) {
        message.setUndefined(false);
        definition.addMessage(message);
        this.fireTableStructureChanged();
    }


    /**
     * remove the message by name
     * @param name the message name local part
     */
    public void removeMessage(String name) {
        removeMessage(new QName(definition.getTargetNamespace(), name));
    }

    /**
     * remove the message by <code>QName</code>
     * @param name the message name local part
     */
    public void removeMessage(QName name) {
        definition.removeMessage(name);
        this.fireTableStructureChanged();
    }

    /**
     * remove the message by <code>index</code>
     * @param index the message index
     */
    public Message removeMessage(int index) {
        Iterator it = definition.getMessages().keySet().iterator();
        int row = 0;
        while (it.hasNext()) {
            Object key = it.next();
            if (row++ == index) {
                Message m = (Message)definition.getMessages().remove(key);
                this.fireTableStructureChanged();
                return m;
            }
        }
        throw new IndexOutOfBoundsException("" + index + " > " + definition.getMessages().size());
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
