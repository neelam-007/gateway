package com.l7tech.console.table;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;


import org.apache.log4j.Category;

/**
 * The TableModel operates in two modes :
 * rows are fully loaded from the source. This is a simple
 * blocking mode, where the data is copied into the internal
 * <CODE>List</CODE>.
 * the initial set of rows is loaded, and the rest of the
 * records is loaded on different thread.
 * Note that loading has to be explicitely stopped (#stop()
 * method) to stop data loading.
 */
public class ContextListTableModel extends AbstractTableModel {
  private static final Category log =
  Category.getInstance(ContextListTableModel.class.getName());

  private final Enumeration enum;
  private Thread th;
  private static final int DEFAULT_BATCH_SIZE = 50;
  private ObjectRowAdapter oAdapter = null;
  private int numberOfCols;
  private String[] colNames;
  private volatile boolean isLoading = false;
  private final List tableRows = Collections.synchronizedList(new LinkedList());

  /**
   * default constructor
   */
  public ContextListTableModel(Enumeration nEnum,
                               int numberOfCols,
                               String[] colNames,
                               ObjectRowAdapter oAdapter) {
    this.enum = nEnum;
    this.numberOfCols = numberOfCols;
    this.colNames = colNames;
    this.oAdapter = oAdapter;
  }

  /**
   * constructor accepting an arbitrary collection
   */
  public ContextListTableModel(Collection c) {
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
    if(oAdapter == null) {
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
   *               or -1 if object not found
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
    isLoading=true;
    load();
  }

  public synchronized void stop() {
    isLoading = false;
  }

  private void load() {
    List list = new ArrayList(DEFAULT_BATCH_SIZE);
    int count = 0;
    while (enum.hasMoreElements()
           && ++count < DEFAULT_BATCH_SIZE) {
      list.add(enum.nextElement());
    }
    addBatch(list);
    if (enum.hasMoreElements()) {
      th = new Thread(contextLoader);
      th.start();
    }
  }

  /**
   * The <CODE>Runnable</CODE> that loads the context list.
   * When batch size records are loaded it adds them to the
   * main TableModel rows list.
   *
   * The data loading can be explicitely stopped by invoking
   * the ContextListTableModel#stop() method.
   */
  private final Runnable
  contextLoader = new Runnable() {
    List list = new ArrayList(DEFAULT_BATCH_SIZE);
    public void run() {
      while (enum.hasMoreElements() && isLoading) {
        list.add(enum.nextElement());
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
   * @param mList  the <CODE>List</CODE> of elements to add to the
   *               <CODE>TableModel</CODE>
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

  public static interface ObjectRowAdapter {
    Object getValue(Object rowObject, int col);
  }
}
