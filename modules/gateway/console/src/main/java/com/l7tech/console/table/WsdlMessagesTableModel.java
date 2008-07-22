package com.l7tech.console.table;

import com.l7tech.console.util.WsdlComposer;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import java.util.ArrayList;
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
    private final WsdlComposer wsdlComposer;
    private final List<MessageInfo> messageList = new ArrayList<MessageInfo>();

    /**
     * Create the new <code>WsdlMessagesTableModel</code>
     * 
     * @param composer The wsdl Definition
     */
    public WsdlMessagesTableModel(final WsdlComposer composer) {
        if (composer == null) {
            throw new IllegalArgumentException();
        }
        this.wsdlComposer = composer;
        populate();
    }

    public void populate() {
        for (Object messageObject : wsdlComposer.getMessages().values()) {
            Message message = (Message) messageObject;
            this.messageList.add(new MessageInfo(message, message.getOrderedParts(null)));
        }
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
        return messageList.size() + 1;
    }

    /**
     * Get the list of messages.
     *
     * <p>The list is not backed by messages from the model.</p>
     * 
     * @return the list of rows in the model
     */
    public List<Message> getMessages() {
        List<Message> messages = new ArrayList<Message>();

        for (MessageInfo messageInfo : messageList) {
            Message message = wsdlComposer.createMessage();
            message.setQName(messageInfo.message.getQName());
            message.setUndefined(messageInfo.message.isUndefined());
            for (Part part : messageInfo.messageParts) {
                message.addPart(part);
            }
            messages.add(message);
        }

        return messages;
    }

    /**
     * Get the list of message Parts.
     *
     * <p>The list is backed by Parts from the model.</p>
     *
     * @return the list of Parts for the index (null for invalid index)
     */
    public List<Part> getMessageParts(int index) {
        List<Part> parts = null;

        if (index >=0 && index < messageList.size()) {
            MessageInfo info = messageList.get(index);
            parts = info.messageParts;
        }

        return parts;
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     * 
     * @param	rowIndex	the row whose value is to be queried
     * @param	columnIndex the column whose value is to be queried
     * this field is ignored as
     * @return	the value Object at the specified cell
     */
    public String getValueAt(int rowIndex, int columnIndex) {
        return rowIndex==messageList.size() ? null : getLocalName(messageList.get(rowIndex).message.getQName());
    }

    /**
     * create and add an empty message by name
     * to the message table
     * 
     * @param name the message name local part
     * @return the newly created message
     */
    public Message addMessage(String name) {
        return addMessage(new QName(wsdlComposer.getTargetNamespace(), name));
    }

    /**
     * create then add the message by <code>QName</code>
     * to the message table
     * 
     * @param name the message <code>QName</code>
     * @return the newly created message
     */
    public Message addMessage(QName name) {
        Message m = wsdlComposer.createMessage();
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
        boolean added = false;
        message.setUndefined(false);

        if (indexOf(message.getQName()) == -1) {
            messageList.add(new MessageInfo(message, message.getOrderedParts(null)));
            added = true;
        }
        
        if (added) {
            int index = messageList.size();
            this.fireTableRowsInserted(index, index);
        }
    }


    /**
     * remove the message by name
     * 
     * @param name the message name local part
     */
    public void removeMessage(String name) {
        removeMessage(new QName(wsdlComposer.getTargetNamespace(), name));
    }

    /**
     * remove the message by <code>QName</code>
     * 
     * @param name the message name local part
     */
    public Message removeMessage(QName name) {
        int index = indexOf(name);
        if (index == -1) return null;

        Message removed = messageList.remove(index).message;
        this.fireTableRowsDeleted(index, index);

        return removed;
    }

    /**
     * remove the message by <code>index</code>
     * 
     * @param index the message index
     */
    public Message removeMessage(int index) {
        MessageInfo messageInfo = messageList.remove(index);
        Message message = null;

        if (messageInfo != null) {
            fireTableRowsDeleted(index, index);
            message = messageInfo.message;
        }

        return message;
    }

    /**
     * Searches for the message with the given <code>QName</code>
     * Returns the index of the message or -1 if the message cannot be found
     * 
     * @param qn the message to search for.
     * @return the index in this table model of the first occurrence of the
     *         specified message, or -1 if this list does not contain this
     *         message.
     */
    public int indexOf(QName qn) {
        for (int i = messageList.size() - 1; i >= 0; --i) {
            MessageInfo messageInfo = messageList.get(i);
            if ( (messageInfo.message.getQName() == null && qn == null) ||
                 (qn != null && qn.equals(messageInfo.message.getQName()))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns true.  This is the default implementation for all cells.
     * 
     * @param rowIndex    the row being queried
     * @param columnIndex the column being queried
     * @return true
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (columnIndex == 0) {
            if (rowIndex == messageList.size()) {
               String value = (String) aValue;
               if (value.length() > 0)
                   addMessage(value);
            }
            else  {
                // Preserve position in the list but remove / re-add to the definition
                // since the key is changed (QName)
                MessageInfo messageInfo = messageList.get(rowIndex);
                messageInfo.message.setQName(new QName(wsdlComposer.getTargetNamespace(), (String) aValue));
                this.fireTableDataChanged();                
            }
        }
        else {
            throw new IndexOutOfBoundsException("" + columnIndex + " >= " + getColumnCount());
        }
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
        }
        throw new IndexOutOfBoundsException("column may be 0 only. received " + column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    private String getLocalName(QName qname) {
        String name = null;

        if ( qname != null ) {
            name = qname.getLocalPart();
        }

        return name;
    }

    private static final class MessageInfo {
        private final Message message;
        private final List<Part> messageParts;

        MessageInfo(Message message, List parts) {
            this.message = message;
            this.messageParts = new ArrayList<Part>();

            for (Object partObj : parts) {
                Part part = (Part) partObj;
                messageParts.add(part);
            }
        }
    }
}
