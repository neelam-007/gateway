package com.l7tech.console.table;


import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * The <code>DynamicTableModel</code> class supports loading
 * the lists on the spearate thread, helping UI to be repsonsive.
 * It operates in two modes :
 * <p/>
 * <ul>
 * <li>rows are fully loaded from the source. This is a simple<br>
 * blocking mode, where the data is copied into the internal</br>
 * <CODE>List</CODE>.
 * <li>the initial set of rows is loaded, and the rest of the<br>
 * records is loaded on different thread.<br>
 * </ul>
 * <p/>
 * The loading may be explicitely stopped using {@link#stop()} method.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DynamicTableModel extends AbstractTableModel {
    private final Enumeration enumeration;
    private Thread th;
    private static final int DEFAULT_BATCH_SIZE = 50;
    private ObjectRowAdapter oAdapter = null;
    private int numberOfCols;
    private String[] colNames;
    private volatile boolean isLoading = false;
    private List tableRows = Collections.synchronizedList(new LinkedList());

    /**
     * default constructor
     */
    public DynamicTableModel(Enumeration nEnum,
                             int numberOfCols,
                             String[] colNames,
                             ObjectRowAdapter oAdapter) {
        this.enumeration = nEnum;
        this.numberOfCols = numberOfCols;
        this.colNames = colNames;
        this.oAdapter = oAdapter;
    }

    /**
     * constructor accepting an arbitrary collection
     */
    public DynamicTableModel(Collection c) {
        this(Collections.enumeration(c), 1, null, null);
    }

    /**
     * @return int number of rows
     */
    public int getRowCount() {
        return tableRows.size();
    }

    /**
     * @return int number of columns
     */
    public int getColumnCount() {
        return numberOfCols;
    }

    public String getColumnName(int column) {
        return colNames[column];
    }

    /**
     * Returns an attribute value for the cell at row and column.
     * 
     * @param row    the row whose value is to be queried
     * @param column the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    public Object getValueAt(int row, int column) {
        if (oAdapter == null) {
            return tableRows.get(row);
        }
        return oAdapter.getValue(tableRows.get(row), column);
    }

    public Object getValueAt(int row) {
        return tableRows.get(row);
    }

    public void addRow(Object o) {
        tableRows.add(o);
        this.fireTableRowsInserted(tableRows.size(), tableRows.size());
    }

    public void removeRow(int row) {
        tableRows.remove(row);
        fireTableRowsDeleted(row, row);
    }


    /**
     * Returns a row that corresponds to the given object.
     * 
     * @param o - the object whose row is to be found
     * @return int - the row that corresponds to the given object
     *         or -1 if object not found
     */
    public int getRow(Object o) {
        return tableRows.indexOf(o);
    }

    public void setValueAt(Object o, int row) {
        tableRows.set(row, o);
        fireTableRowsUpdated(row, row);
    }

    public synchronized void start()
      throws InterruptedException {
        if (isLoading) {
            throw new IllegalStateException("already started");
        }
        if (th != null) {
            if (th.isAlive()) {
                th.join();
                th = null;
            }
        }
        isLoading = true;
        load();
    }

    public synchronized void stop() throws InterruptedException {
        isLoading = false;
        if (th != null) {
            if (th.isAlive()) {
                th.join();
                th = null;
            }
        }
    }

    public synchronized void clear() throws InterruptedException {
        stop();
        tableRows = Collections.synchronizedList(new LinkedList());
        fireTableDataChanged();
    }

    private void load() {
        List list = new ArrayList(DEFAULT_BATCH_SIZE);
        int count = 0;
        while (enumeration.hasMoreElements()
          && ++count < DEFAULT_BATCH_SIZE && isLoading) {
            list.add(enumeration.nextElement());
        }
        addBatch(list);
        if (enumeration.hasMoreElements() && isLoading) {
            th = new Thread(contextLoader);
            th.start();
        }
    }

    /**
     * The <CODE>Runnable</CODE> that loads the context list.
     * When batch size records are loaded it adds them to the
     * main TableModel rows list.
     * <p/>
     * The data loading can be explicitely stopped by invoking
     * the ContextListTableModel#stop() method.
     */
    private final Runnable
      contextLoader = new Runnable() {
          List list = new ArrayList(DEFAULT_BATCH_SIZE);

          public void run() {
              while (enumeration.hasMoreElements() && isLoading) {
                  list.add(enumeration.nextElement());
                  if (list.size() == DEFAULT_BATCH_SIZE) {
                      addBatch(list);
                      list = new ArrayList(DEFAULT_BATCH_SIZE);
                  }
              }
              addBatch(list);
          }
      };

    /**
     * add a <CODE>List</CODE> of rows to the TableModel on
     * the Swing event thread.
     * Notify the TableModel listeners about the data change.
     * 
     * @param mList the <CODE>List</CODE> of elements to add to the
     *              <CODE>TableModel</CODE>
     */
    private void addBatch(List mList) {
        final List list = new ArrayList(mList);
        SwingUtilities.
          invokeLater(new Runnable() {
              public void run() {
                  int firstRow = tableRows.size();
                  tableRows.addAll(list);
                  int lastRow = tableRows.size();
                  fireTableRowsInserted(firstRow, lastRow);
              }
          });
    }

    /**
     * The arbitrary object to row translator
     */
    public static interface ObjectRowAdapter {
        Object getValue(Object rowObject, int col);
    }
}
