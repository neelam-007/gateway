package com.l7tech.console.table;

import com.ibm.wsdl.MessageImpl;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>WsdlMessagesTableModel</code> is the TableModel
 * that hadles the WSDL message elemnts.
 * The message names are internally maintained in the linked
 * hashmap to keep the predictable iteration order.
 * Note that the class is not aware of the external modifications
 * of the  <code>Definition</code> instance that is used.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WsdlMessagesTableModel extends AbstractTableModel {
    private Definition definition;
    List messageList = new ArrayList();

    /**
     * Create the new <code>WsdlMessagesTableModel</code>
     * 
     * @param def 
     */
    public WsdlMessagesTableModel(Definition def) {
        definition = def;
        if (def == null) {
            throw new IllegalArgumentException();
        }
        messageList.addAll(definition.getMessages().values());

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
     * Returns the number of rows in the model.
     * 
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    public int getRowCount() {
        // return definition.getMessages().size();
        return messageList.size();
    }

    /**
     * Returns the rows from the table model. Note that this
     * operation returns the list that the table model is backed
     * and not the copy.
     * 
     * @return the list of rows in the model
     */
    public List getRows() {
        return messageList;
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
        // Iterator it = definition.getMessages().values().iterator();
        Iterator it = messageList.iterator();
        int row = 0;
        while (it.hasNext()) {
            Object m = it.next();
            if (row++ == rowIndex) return m;
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + messageList.size());
    }

    /**
     * create and add an empty message by name
     * to the message table
     * 
     * @param name the message name local part
     * @return the newly created message
     */
    public Message addMessage(String name) {
        return addMessage(new QName(definition.getTargetNamespace(), name));
    }

    /**
     * create then add the message by <code>QName</code>
     * to the message table
     * 
     * @param name the message <code>QName</code>
     * @return the newly created message
     */
    public Message addMessage(QName name) {
        Message m = new MutableMessage();
        m.setQName(name);
        addMessage(m);
        return m;
    }

    /**
     * add the message <code>Message</code>  to
     * the message table
     * 
     * @param message the message to add
     */
    public void addMessage(Message message) {
        message.setUndefined(false);
        definition.addMessage(message);
        messageList.add(message);
        this.fireTableStructureChanged();
    }


    /**
     * remove the message by name
     * 
     * @param name the message name local part
     */
    public void removeMessage(String name) {
        removeMessage(new QName(definition.getTargetNamespace(), name));
    }

    /**
     * remove the message by <code>QName</code>
     * 
     * @param name the message name local part
     */
    public Message removeMessage(QName name) {
        Message removed = definition.removeMessage(name);
        if (removed != null) {
            messageList.remove(removed);
            this.fireTableStructureChanged();
        }
        return removed;
    }

    /**
     * remove the message by <code>index</code>
     * 
     * @param index the message index
     */
    public Message removeMessage(int index) {
        // Iterator it = definition.getMessages().keySet().iterator();
        Iterator it = messageList.iterator();
        int row = 0;
        while (it.hasNext()) {
            Object key = it.next();
            if (row++ == index) {
                Message m = (Message)messageList.remove(index);
                definition.removeMessage(m.getQName());
                this.fireTableStructureChanged();
                return m;
            }
        }
        throw new IndexOutOfBoundsException("" + index + " > " + messageList.size());
    }

    /**
     * Returns false.  This is the default implementation for all cells.
     * 
     * @param rowIndex    the row being queried
     * @param columnIndex the column being queried
     * @return false
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!(aValue instanceof Message)) {
            throw new IllegalArgumentException("value is not a Message. Received " + aValue.getClass());
        }
        Message m = getMessageAt(rowIndex);
        if (columnIndex == 0) {
            replaceMessage(m, (Message)aValue);
        } else {
            throw new IndexOutOfBoundsException("" + columnIndex + " >= " + getColumnCount());
        }
    }

    /**
     * replace the message om with the new message nm.
     * 
     * @param om the
     * @param nm 
     */
    private void replaceMessage(Message om, Message nm) {
        int size = messageList.size();
        for (int i = 0; i < size; i++) {
            Message m = (Message)messageList.get(i);
            if (m.getQName().equals(om.getQName())) {
                Message rm = removeMessage(m.getQName());
                addMessage(nm);
                if (rm != null) {
                    Iterator pi = om.getParts().values().iterator();
                    while (pi.hasNext()) {
                        Part p = (Part)pi.next();
                        nm.addPart(p);
                    }
                    messageList.set(i, nm);
                }
                this.fireTableRowsUpdated(i, i);
                break;
            }
        }
    }

    /**
     * Returns a the name for the columns. There is a part name and the
     * type column
     * 
     * @param column the column being queried
     * @return a string containing the default name of <code>column</code>
     */
    public String getColumnName(int column) {
        if (column == 0) {
            return "Name";
        }
        throw new IndexOutOfBoundsException("column may be 0 only. received " + column);
    }


    /**
     * Returns the Message at the  row <code>rowIndex</code>.
     * 
     * @param	rowIndex	the row whose value is to be queried
     * (1 based)
     * @return	the Message at the specified row
     */
    private Message getMessageAt(int rowIndex) {
        //Iterator it = message.getParts().values().iterator();
        Iterator it = messageList.iterator();
        int row = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if (row++ == rowIndex) {
                Message m = (Message)o;
                return m;
            }
        }
        throw new IndexOutOfBoundsException("" + rowIndex + " > " + messageList.size());
    }

    // hack so MessageImpl is editable. Make the internal list
    // accessible.
    public static class MutableMessage extends MessageImpl {
        public List getadditionOrderOfParts() {
            return additionOrderOfParts;
        }

        /**
         * Replace the message part.
         * 
         * @param part the part to be added
         */
        public void replacePart(String name, Part part) {
            parts.remove(name);

            final int size = additionOrderOfParts.size();
            for (int i = 0; i < size; i++) {
                String s = (String)additionOrderOfParts.get(i);
                if (s.equals(name)) {
                    additionOrderOfParts.set(i, part.getName());
                    break;
                }
            }
            parts.put(part.getName(), part);
        }
    }

}
