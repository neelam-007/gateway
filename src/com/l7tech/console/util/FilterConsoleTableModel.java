package com.l7tech.console.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.Category;

import com.l7tech.console.table.ConsoleTableModel;

/**
 * FilterTableModel extends ConsoleTableModel and provides simple
 * filtering over a ConsoleTableModel.
 * In 'patterns' language the class is a variation of decorator
 * (transparent closure) over the ConsoleTableModel passed to the constructor.
 *
 * The instance of the class register itself as a listener for the
 * <code>ConsoleTableModel</code> it decorates, and propagates the events
 * to its own listeners.
 *
 * The class is not thread safe.
 */
public class FilterConsoleTableModel extends ConsoleTableModel {
  /**
   * Creates a new instance of FilteredTableModel encapsulating
   * the root of this model.
   *
   * @param model   the model
   */
  public FilterConsoleTableModel(ConsoleTableModel model) {
    super(model.getColumnNames(), model.getComparator());
    this.model = model;
    loadFilteredRowIndices();

    // Attach as a listener for changes of the underlying model
    model.addTableModelListener(new TableModelListener() {
        /**
         * This fine grain notification tells listeners the exact range
         * of cells, rows, or columns that changed.
         */
        public void tableChanged(TableModelEvent e) {
          // Reload data from the underlying model that has changed
          loadFilteredRowIndices();
          fireTableChanged(e);
        }
      });
  }


  /**
   * Call this method after the filter has been updated.  The method
   * notifies listeners about the change.
   */
  public void refresh() {
    loadFilteredRowIndices();
    fireTableDataChanged();
  }


  /**
   * Associates the filter with this <CODE>FilterTableModel</CODE>
   *
   * @param filter new Filter
   */
  public void setFilter(Filter filter) {
    this.filter = filter;
    loadFilteredRowIndices();
    fireTableDataChanged();
  }


  /**
   * @return the filter associated with this FilterTableModel.
   *         <B>null</B> is returned if filter not set.
   */
  public Filter getFilter() {
    return filter;
  }


  /**
   * Clears the filter associated with this <CODE>FilterTableModel</CODE>
   */
  public void clearFilter() {
    filter = null;
    filteredRowIndices.clear();
    fireTableDataChanged();
  }


  /**
   * Returns the number of rows in this data table;
   * Uses the filter if specified (not null).
   *
   * @return the number of rows in the model
   */
  public int getRowCount() {
    // If filter not specified
    if (null == filter) {
      // Return the underlying row count
      return model.getRowCount();
    } else {
      // Return the filtered count
      return filteredRowIndices.size();
    }
  }


  /**
   * Returns the number of columns in this data table;
   * No filter is used.
   *
   * @return the number of columns in the model
   */
  public int getColumnCount() {
    return model.getColumnCount();
  }


  /**
   * Returns an attribute value for the cell at <code>row</code>
   * and <code>column</code>; The method performs filtering over
   * the <CODE>TableModel</CODE> if Filter has been specified.
   *
   * @param   row             the row whose value is to be queried
   * @param   column          the column whose value is to be queried
   * @return                  the value Object at the specified cell
   * @exception  ArrayIndexOutOfBoundsException  if an invalid row or
   *               column was given
   */
  public Object getValueAt(int row, int column) {
    if (null == filter) {
      return model.getValueAt(row, column);
    } else {
      return model.getValueAt(getRealRowIndex(row), column);
      //return model.getValueAt(((Integer)filteredRowIndices.get(row)).intValue(), column);
    }
  }


  /**
   * Sets the value at the specified row/col indices;
   * if value is null, the specified row is removed
   */
/*  public void setValueAt(Object value, int rowIndex, int colIndex) {
    List row = (List) data.get(rowIndex);
    row.set(colIndex, value);
    fireTableCellUpdated(rowIndex, colIndex);
  }*/


  /**
   * Returns a name of the given column.
   *
   * @return a name for this column
   */
  public String getColumnName(int column) {
    return model.getColumnName(column);
  }


  /** Loads valid (filter accepted) row indices from the underlying model */
  private void loadFilteredRowIndices() {
    if (null != filter) {
      // NOTE: due to the nature of populating the filteredRowIndices array,
      // the array can be considered sorted
      int rowCount = model.getRowCount();
      filteredRowIndices = new ArrayList(rowCount);
      for (int i=0, index=0; index<rowCount; index++) {
        if (filter.accept(model.getValueAt(index, 0))) {
          filteredRowIndices.add(new Integer(index));
        }
      }
    }
  }


  /** Returns the underlying List of rows (Lists) */
  public List asList() {
    if (null == filter) {
      return model.asList();
    } else {
      int size = getRowCount();
      List rows = new ArrayList(size);

      // For each row
      for (int i=0; i<size; i++) {
        // Perform the mapping and add the real value
        rows.add(getRow(i));
      }

      return rows;
    }
  }


  /** Returns the row at the specified index */
  public List getRow(int rowIndex) {
    return model.getRow(getRealRowIndex(rowIndex));
  }


  /** Removes a row at the given index */
  public void removeRow(int rowIndex) {
    model.removeRow(getRealRowIndex(rowIndex));
  }


  /**
   * Returns a real row index based on the filtered one;
   * that is, performs filter->real index mapping
   */
  private int getRealRowIndex(int filteredRowIndex) {
    if (null == filter) {
      return filteredRowIndex;
    } else {
      return ((Integer)filteredRowIndices.get(filteredRowIndex)).intValue();
    }
  }


  private Filter filter;
  private ConsoleTableModel model;
  private List filteredRowIndices; // sorted list of filtered row indices
  private static final Category log = Category.getInstance(FilterConsoleTableModel.class.getName());
}

