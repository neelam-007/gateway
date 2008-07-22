package com.l7tech.console.util.jfree;

import org.jfree.data.time.TimePeriodValue;
import org.jfree.data.time.TimePeriod;

/**
 * An extension of {@link org.jfree.data.time.TimePeriodValue} with high and low values.
 *
 * @author rmak
 */
public class TimePeriodValueWithHighLow extends TimePeriodValue {
    /** The high value. */
    private Number _high;

    /** The low value. */
    private Number _low;

    public TimePeriodValueWithHighLow(TimePeriod period, Number value, Number high, Number low) {
        super(period, value);
        _high = high;
        _low = low;
    }

    public TimePeriodValueWithHighLow(TimePeriod period, double value, double high, double low) {
        this(period, new Double(value), new Double(high), new Double(low));
    }

    public Number getHighValue() {
        return _high;
    }

    public void setHighValue(Number high) {
        _low = high;
    }

    public Number getLowValue() {
        return _low;
    }

    public void setLowValue(Number low) {
        _low = low;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final TimePeriodValueWithHighLow that = (TimePeriodValueWithHighLow) o;

        if (_high != null ? !_high.equals(that._high) : that._high != null)
            return false;
        if (_low != null ? !_low.equals(that._low) : that._low != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (_high != null ? _high.hashCode() : 0);
        result = 29 * result + (_low != null ? _low.hashCode() : 0);
        return result;
    }

    // Note: No need to override clone() method since the additional member fields are immutable.
}
