package com.l7tech.console.table;

import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

/**
 * The <CODE>TableModel</CODE> backed by <CODE>Map</CODE>. Every
 * class implementing the Map interface may be used as a source
 * for the <CODE>MapBackedTableModel</CODE>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 *
 * @see javax.swing.table.DefaultTableModel
 * @see java.util.Properties
 * @see java.util.Hashtable
 * @see java.util.SortedMap
 * @see java.util.HashMap
 * @see java.util.TreeMap
 * @see java.util.Map
 */
public class MapBackedTableModel extends DefaultTableModel {

  private boolean isEditable = false;
  private String keyColumnName = null;
  private String valueColumnName = null;

  public MapBackedTableModel(Vector data, Vector colNames){
		super(data, colNames);
	}
	
	/**
   * Full constructor
   * 
   * @param keyColumnName
   *               key column name
   * @param valueColumnName
   *               value column name
   * @param map    the Map to load the TableModel from.
   */
  public MapBackedTableModel(String keyColumnName, String valueColumnName, Map map) {
    super();
    setData(keyColumnName, valueColumnName, map);
  }

  /**
   * Default constructor
   */
  public MapBackedTableModel() {
    super();
  }

  /**
   * Set the data and column names.
   * 
   * @param keyColumnName
   *               key column name
   * @param valueColumnName
   *               value column name
   * @param map    the Map to load the TableModel from.
   */
  public void setData(String keyColumnName, String valueColumnName, Map map) {
    clearTable();
    this.keyColumnName = keyColumnName;
    this.valueColumnName = valueColumnName;
    addColumn(keyColumnName);
    addColumn(valueColumnName);

    Set set = map.keySet();
    Iterator iterator = set.iterator();

    while (iterator.hasNext()) {
      Object key = iterator.next();
      addRow(new Object[]{key,map.get(key)});
    }
    // politely notify listeners
    fireTableRowsInserted(0,getRowCount());
  }

  /**
   * Toggle the cell editing
   * 
   * @param toggle  true allows cell editing, false otherwise
   */
  public synchronized void allowEditing(boolean toggle) {
    isEditable = toggle;
  }

  /**
   * 
   * @param row    cell row
   * @param col    cell column
   * @return if the cell is editable
   */
  public boolean isCellEditable(int row,int col) {
    return isEditable;
  }

  /**
   * clear/reset the table model.
   * Method is synchronized
   */
  public synchronized void clearTable() {
    columnIdentifiers.clear();
    dataVector.clear();
  }
}
