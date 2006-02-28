package com.l7tech.console.util.jfree;

import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimePeriodValues;

/**
 * Same as {@link org.jfree.data.time.TimePeriodValuesCollection} but for
 * {@link TimePeriodValuesWithHighLow} objects.
 *
 * @author rmak
 */
public class TimePeriodValuesWithHighLowCollection extends TimePeriodValuesCollection {
    /** Constructs an empty collection. */
    public TimePeriodValuesWithHighLowCollection() {
        this((TimePeriodValuesWithHighLow) null);
    }

    /**
     * Constructs a collection containing a single series. Additional series can
     * be added.
     *
     * @param series the series
     */
    public TimePeriodValuesWithHighLowCollection(TimePeriodValuesWithHighLow series) {
        super(series);
    }

    /**
     * Returns a series.
     *
     * @param series the index of the series (zero-based)
     * @return the series; actual return type is {@link TimePeriodValuesWithHighLow}
     */
    public TimePeriodValues getSeries(int series) {
        return (TimePeriodValuesWithHighLow) super.getSeries(series);
    }

    /**
     * This inherited method is replaced by {@link #addSeries(TimePeriodValuesWithHighLow)}.
     *
     * @throws UnsupportedOperationException always
     */
    public void addSeries(TimePeriodValues series) {
        throw new UnsupportedOperationException("Use addSeries(TimePeriodValuesWithHighLow) instead.");
    }

    /**
     * Adds a series to the collection.  A
     * {@link org.jfree.data.general.DatasetChangeEvent} is sent to all
     * registered listeners.
     *
     * @param series the time series
     */
    public void addSeries(TimePeriodValuesWithHighLow series) {
        super.addSeries((TimePeriodValues) series);
    }

    /**
     * This inherited method is replaced by {@link #removeSeries(TimePeriodValuesWithHighLow)}.
     *
     * @throws UnsupportedOperationException always
     */
    public void removeSeries(TimePeriodValues series) {
        throw new UnsupportedOperationException("Use removeSeries(TimePeriodValuesWithHighLow) instead.");
    }

    /**
     * Removes the specified series from the collection.
     *
     * @param series the series to remove (<code>null</code> not permitted)
     */
    public void removeSeries(TimePeriodValuesWithHighLow series) {
        super.removeSeries((TimePeriodValues) series);
    }

    /**
     * Returns the low value for the specified series and item.
     *
     * @param series the series (zero-based index)
     * @param item   the item (zero-based index)
     * @return the low value for the specified series and item
     */
    public Number getStartY(int series, int item) {
        TimePeriodValuesWithHighLow ts = (TimePeriodValuesWithHighLow) getSeries(series);
        TimePeriodValueWithHighLow dp = (TimePeriodValueWithHighLow) ts.getDataItem(item);
        return dp.getLowValue();
    }

    /**
     * Returns the high value for the specified series and item.
     *
     * @param series the series (zero-based index)
     * @param item   the item (zero-based index)
     * @return the high value for the specified series and item
     */
    public Number getEndY(int series, int item) {
        TimePeriodValuesWithHighLow ts = (TimePeriodValuesWithHighLow) getSeries(series);
        TimePeriodValueWithHighLow dp = (TimePeriodValueWithHighLow) ts.getDataItem(item);
        return dp.getHighValue();
    }

    // Note: No need to override equals(), hashCode and clone() methods since
    // there is no additional member fields.
}
