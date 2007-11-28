package com.l7tech.common.gui;

import com.l7tech.common.util.Functions.Unary;

/**
 * A simple store of information about a single column in a {@link SimpleTableModel}.
 */
public class SimpleColumn<RT> {
    private String name;
    private Unary<Object,RT> valueGetter;

    /**
     * Create a SimpleColumn with no special behavior configured.
     */
    public SimpleColumn() {
    }

    /**
     * Create a SimpleColumn that will use the specified transform to map row objects to the values
     * for cells in this column.
     *
     * @param valueGetter the transform, or null. See {@link #setValueGetter} for more information.
     */
    public SimpleColumn(Unary<Object, RT> valueGetter) {
        this.valueGetter = valueGetter;
    }

    /**
     * Create a SimpleColumn with the specified column name that will use the specified
     * transform to map row objects to the values for cells in this column.
     *
     * @param name the name of the column.  Required.
     * @param valueGetter the transform, or null.  See {@link #setValueGetter} for more information.
     */
    public SimpleColumn(String name, Unary<Object, RT> valueGetter) {
        this.name = name;
        this.valueGetter = valueGetter;
    }

    /**
     * Get the name of this column.  This may be used as the default table header.
     *
     * @return the name of this column, or null.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this column.  This may be used as the default table header.
     *
     * @param name the name to use for this column, or null.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the transform that will be used to map row object to cell values for this column, if any.
     *
     * @return  the transform, or null.  See {@link #setValueGetter} for more information.
     */
    public Unary<Object, RT> getValueGetter() {
        return valueGetter;
    }

    /**
     * Configure a transform to map row objects to cell values for this column.
     *
     * @param valueGetter a Unary which, when invoked with on an instance of our Row Type, returns the corresponding
     *                    cell value for this column.
     *                    <p/>
     *                    If this is null, no transform will be used and the row instance will be returned
     *                    as-is for cell values in this column.
     * @see com.l7tech.common.util.Functions#propertyTransform(Class, String) for an easy way to generate a valueGetter for any bean property of RT
     * @see com.l7tech.common.util.Functions#getterTransform(java.lang.reflect.Method) for an easy way to generate a valueGetter for any nullary method of RT
     */
    public void setValueGetter(Unary<Object, RT> valueGetter) {
        this.valueGetter = valueGetter;
    }

    /**
     * Get the value of a cell for this column, given the specified row backing object.
     * <p/>
     * This method invokes the valueGetter if there is one; otherwise it just returns row unmodified.
     *
     * @param row an object repesenting a row in this table model.  May be null if the model permits this.
     * @return the value of the cell.  May be null if the model permits this.
     */
    public Object getValue(RT row) {
        return valueGetter == null ? row : valueGetter.call(row);
    }

    /**
     * Get the value of a cell for this column, possibly taking into account its location in the table.
     * <p/>
     * This method ignores the coordinates and just calls {@link #getValue(Object)}.
     * Subclasses of SimpleColumn may choose to take note of rowIndex and columnIndex.
     *
     * @param row an object repesenting a row in this table model.  May be null if the model permits this.
     * @param rowIndex a specific row index
     * @param columnIndex a specific column index
     * @return the value of the cell.  May be null if the model permits this.
     */
    public Object getValue(RT row, int rowIndex, int columnIndex) {
        return getValue(row);
    }
}
