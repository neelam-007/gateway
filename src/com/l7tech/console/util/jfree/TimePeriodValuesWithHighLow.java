package com.l7tech.console.util.jfree;

import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValue;
import org.jfree.data.time.TimePeriod;

/**
 * Same as {@link org.jfree.data.time.TimePeriodValues} but for
 * {@link TimePeriodValueWithHighLow} objects.
 *
 * @author rmak
 */
public class TimePeriodValuesWithHighLow extends TimePeriodValues {
    /**
     * Creates a new (empty) time series.
     *
     * @param name the name of the series
     */
    public TimePeriodValuesWithHighLow(String name) {
        super(name, DEFAULT_DOMAIN_DESCRIPTION, DEFAULT_RANGE_DESCRIPTION);
    }

    /**
     * Adds a new data item to the series, where high and low values are assumed
     * to be the same as the value.
     *
     * @param item the data item
     */
    public void add(TimePeriodValue item) {
        add(item.getPeriod(), item.getValue(), item.getValue(), item.getValue());
    }

    /**
     * Adds a new data item to the series, where high and low values are assumed
     * to be the same as the value.
     *
     * @param period the time period
     * @param value  the value
     */
    public void add(TimePeriod period, double value) {
        add(period, value, value, value);
    }

    /**
     * Adds a new data item to the series, where high and low values are assumed
     * to be the same as the value.
     *
     * @param period the time period
     * @param value  the value
     */
    public void add(TimePeriod period, Number value) {
        add(period, value, value, value);
    }

    /**
     * Adds a data time to the time series
     *
     * @param item the data item
     */
    public void add(TimePeriodValueWithHighLow item) {
        super.add((TimePeriodValue) item);
    }

    /**
     * Adds a new data item to the series.
     *
     * @param period the time period
     * @param value  the value
     * @param high   the high value
     * @param low    the low value
     */
    public void add(TimePeriod period, double value, double high, double low) {
        TimePeriodValueWithHighLow item = new TimePeriodValueWithHighLow(period, value, high, low);
        add(item);
    }

    /**
     * Adds a new data item to the series.
     *
     * @param period the time period
     * @param value  the value
     * @param high   the high value limit
     * @param low    the low value limit
     */
    public void add(TimePeriod period, Number value, Number high, Number low) {
        TimePeriodValueWithHighLow item = new TimePeriodValueWithHighLow(period, value, high, low);
        add(item);
    }

    /**
     * Returns one data item at the specified index.
     *
     * @param index the item index (zero-based)
     * @return the data item at the specified index; actual return type is {@link TimePeriodValueWithHighLow}
     */
    public TimePeriodValue getDataItem(int index) {
        return (TimePeriodValueWithHighLow) super.getDataItem(index);
    }

    /**
     * Returns the high value at the specified index.
     *
     * @param index the item index (zero-based)
     * @return the high value at the specified index
     */
    public Number getHighValue(int index) {
        return ((TimePeriodValueWithHighLow) getDataItem(index)).getHighValue();
    }

    /**
     * Returns the low value at the specified index.
     *
     * @param index the item index (zero-based)
     * @return the low value at the specified index
     */
    public Number getLowValue(int index) {
        return ((TimePeriodValueWithHighLow) getDataItem(index)).getLowValue();
    }

    /**
     * Updates (changes) the value of a data item, where high and low values are
     * assumed to be the same as the value.
     *
     * @param index the index of the data item to update
     * @param value the new value
     */
    public void update(int index, Number value) {
        update(index, value, value, value);
    }

    /**
     * Updates (changes) the value of a data item.
     *
     * @param index the index of the data item to update
     * @param value the new value
     * @param high  the new high value
     * @param low   the new low value
     */
    public void update(int index, Number value, Number high, Number low) {
        TimePeriodValueWithHighLow item = (TimePeriodValueWithHighLow) getDataItem(index);
        item.setValue(value);
        item.setLowValue(low);
        item.setHighValue(high);
        fireSeriesChanged();
    }

    // Note: No need to override equals(), hashCode and clone() methods since
    // there is no additional member fields.
}
