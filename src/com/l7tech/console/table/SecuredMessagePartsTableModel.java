package com.l7tech.console.table;

import com.l7tech.common.security.xml.ElementSecurity;

import javax.swing.table.AbstractTableModel;
import javax.wsdl.BindingOperation;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>SecuredMessagePartsTableModel</code> represents the
 * secure (signed/encrypted elements) from the wsdl.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SecuredMessagePartsTableModel extends AbstractTableModel {
    private List securedMessageParts = new ArrayList();

    public static class SecuredMessagePart {

        public String getOperationName() {
            if (operation != null) {
                return operation.getName();
            }
            return "*"; // all operations
        }

        public BindingOperation getOperation() {
            return operation;
        }

        public void setOperation(BindingOperation operation) {
            this.operation = operation;
        }

        public String getXpathExpression() {
            return xpathExpression;
        }

        public void setXpathExpression(String xpathExpression) {
            this.xpathExpression = xpathExpression;
        }

        public boolean isEncrypt() {
            return encrypt;
        }

        public void setEncrypt(boolean encrypt) {
            this.encrypt = encrypt;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public int getKeyLength() {
            return keyLength;
        }

        public void setKeyLength(int keyLength) {
            this.keyLength = keyLength;
        }

        private BindingOperation operation;
        private String xpathExpression;
        private boolean encrypt;
        private String algorithm = ElementSecurity.DEFAULT_CIPHER; // default
        private int keyLength = ElementSecurity.DEFAULT_KEYBITS; // defaault

        /**
         * Tests whether this message part implies the part specified by the
         * parameter p.
         *
         * @param p the part to test agains
         * @return true if the part is implied, false otherwise
         */
        public boolean implies(SecuredMessagePart p) {
            if (equals(p)) return true;
            if (operation != null && p.operation != null) {
                if (operation.getName().equals(p.getOperationName())) {
                    if (xpathExpression != null && p.xpathExpression != null) {
                        return xpathExpression.startsWith(p.xpathExpression);
                    }
                }
            } else if (operation == null && p.operation == null) {
                if (xpathExpression != null && p.xpathExpression != null) {
                    return xpathExpression.startsWith(p.xpathExpression);
                }
            }
            return false;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SecuredMessagePart)) return false;

            final SecuredMessagePart securedMessagePart = (SecuredMessagePart)o;

            if (operation != null ? !operation.equals(securedMessagePart.operation) : securedMessagePart.operation != null) return false;
            if (xpathExpression != null ? !xpathExpression.equals(securedMessagePart.xpathExpression) : securedMessagePart.xpathExpression != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (operation != null ? operation.hashCode() : 0);
            result = 29 * result + (xpathExpression != null ? xpathExpression.hashCode() : 0);
            return result;
        }
    }

    /**
     * Create the new <code>SecuredMessagePartsTableModel</code>
     */
    public SecuredMessagePartsTableModel() {
    }

    /**
     * Return the list of <code>SecuredMessagePart</code> that his
     * table model is backed by.
     * <p/>
     * Note that any changes made 'outside' are not automatically
     * visible by the model.
     * 
     * @return the list of secured message parts
     */
    public List getSecuredMessageParts() {
        return securedMessageParts;
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
        return securedMessageParts.size();
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
            return "Operation";
        } else if (column == 1) {
            return "XPath Expression";
        } else if (column == 2) {
            return "Encrypt";
        } else if (column == 3) {
            return "Algorithm";
        } else if (column == 4) {
            return "Key length";
        }
        throw new IndexOutOfBoundsException("column " + column);
    }

    /**
     * Returns the class for the <code>columnIndex</code>.
     * 
     * @param column the column being queried
     * @return the corresponding class
     */
    public Class getColumnClass(int column) {
        if (column == 0) {
            return String.class;
        } else if (column == 1) {
            return String.class;
        } else if (column == 2) {
            return Boolean.class;
        } else if (column == 3) {
            return String.class;
        } else if (column == 4) {
            return Integer.class;
        }
        throw new IndexOutOfBoundsException("column " + column);
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
    public Object getValueAt(int rowIndex, int columnIndex) {
        SecuredMessagePart sp = (SecuredMessagePart)securedMessageParts.get(rowIndex);
        if (columnIndex == 0) {
            return sp.getOperationName();
        } else if (columnIndex == 1) {
            return sp.getXpathExpression();
        } else if (columnIndex == 2) {
            return new Boolean(sp.isEncrypt());
        } else if (columnIndex == 3) {
            return sp.getAlgorithm();
        } else if (columnIndex == 4) {
            return new Integer(sp.getKeyLength());
        }
        throw new IndexOutOfBoundsException("column " + columnIndex);
    }

    /**
     * Returns the value for the <code>SecuredMessagePart</code> at
     * <code>rowIndex</code>.
     * 
     * @param	rowIndex	the row whose value is to be queried
     * @return	the value Object at the specified cell
     */
    public SecuredMessagePart getPartAt(int rowIndex) {
        return (SecuredMessagePart)securedMessageParts.get(rowIndex);
    }

    /**
     * add the part <code>SecuredMessagePart</code> to
     * the table
     * 
     * @param part the part to add
     */
    public void addPart(SecuredMessagePart part) {
        securedMessageParts.add(part);
        int rowCount = getRowCount();
        this.fireTableRowsInserted(rowCount, rowCount);
    }


    /**
     * remove the part by name
     */
    public void removePart(SecuredMessagePart part) {
        if (securedMessageParts.remove(part)) {
            this.fireTableStructureChanged();
        }
    }

    /**
     * Retuirns true. This is the default implementation for all cells.
     * 
     * @param rowIndex    the row being queried
     * @param columnIndex the column being queried
     * @return false
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    /**
     * Set the cell value.
     * 
     * @param aValue      value to assign to cell
     * @param rowIndex    row of cell
     * @param columnIndex column of cell
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue == null) {
            throw new IllegalArgumentException(" value == null ");
        }
        SecuredMessagePart sp = (SecuredMessagePart)securedMessageParts.get(rowIndex);

        if (columnIndex == 0) {
            if (aValue == null) {
                sp.setOperation(null);
            }
            if (!(aValue instanceof BindingOperation)) {
                throw new IllegalArgumentException("Unsupported type " + aValue.getClass());
            }
            sp.setOperation((BindingOperation)aValue);
        } else if (columnIndex == 1) {
            if (!(aValue instanceof String)) {
                throw new IllegalArgumentException("Unsupported type " + aValue.getClass() + " expected " + String.class);
            }
            sp.setXpathExpression(aValue.toString());
        } else if (columnIndex == 2) {
            if (!(aValue instanceof Boolean)) {
                throw new IllegalArgumentException("Unsupported type " + aValue.getClass() + " expected " + Boolean.class);
            }
            sp.setEncrypt(((Boolean)aValue).booleanValue());
        } else if (columnIndex == 3) {
            if (!(aValue instanceof String)) {
                throw new IllegalArgumentException("Unsupported type " + aValue.getClass() + " expected " + String.class);
            }
            sp.setAlgorithm(aValue.toString());
        } else if (columnIndex == 4) {
            if (!(aValue instanceof Integer)) {
                throw new IllegalArgumentException("Unsupported type " + aValue.getClass() + " expected " + Integer.class);
            }
            sp.setKeyLength(((Integer)aValue).intValue());
        } else {
            throw new IndexOutOfBoundsException("" + columnIndex + " > 4");
        }
    }

}
