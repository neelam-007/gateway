package com.l7tech.service;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A statistical bin for collecting service metrics. Conceptually, a bin has
 * 2 distinct states: an open state to accumulate data (i.e., mutable state),
 * a closed state to read accumulated data (i.e., immutable state).
 * <p/>
 * A bin occupies a time interval. There is the <b>nominal</b> time _interval
 * when the bin was supposed to start and end, according to regular scheduling.
 * But since timers are not exact, the bin also has the <b>actual</b> time
 * _interval when the bin actually starts and ends.
 * <p/>
 * This class is not thread safe for performance reason. Since the typical usage
 * is such that mutable methods are called only during the open state and
 * immutable methods are called only during the closed state, synchronization
 * will be more efficiently handled by the owner of this object, who controls
 * when to change from open to close state.
 *
 * @author rmak
 */
public class MetricsBin implements Serializable, Comparable {
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is fine resolution */
    public static final int RES_FINE = 0;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is hourly resolution */
    public static final int RES_HOURLY = 1;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is daily resolution */
    public static final int RES_DAILY = 2;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /** MAC address of the cluster node from which this bin is collected. */
    private String _clusterNodeId;

    /** Object ID of the {@link PublishedService} for which this bin collects data. */
    private long _serviceOid;

    /**
     * Resolution of this bin.
     * <p/>
     * Must be one of {@link #RES_FINE}, {@link #RES_HOURLY} or {@link #RES_DAILY}.
     */
    private int _resolution = -1;

    /** Nominal time period interval (in milliseconds). */
    private int _interval;

    /** Nominal period start time (as UTC milliseconds from the epoch). */
    private long _periodStart;


    /** Acutal start time (as UTC milliseconds from the epoch). */
    private long _startTime;

    /** Actual end time (as UTC milliseconds from the epoch). */
    private long _endTime;

    private int _numAttemptedRequest;
    private int _numAuthorizedRequest;
    private int _numCompletedRequest;

    /** Minimum frontend response time (in milliseconds) of all attempted requests. */
    private int _minFrontendResponseTime;

    /** Maximum frontend response time (in milliseconds) of all attempted requests. */
    private int _maxFrontendResponseTime;

    /** Sum over frontend response times (in milliseconds) of all attempted requests. */
    private long _sumFrontendResponseTime;

    /** Minimum backend response time (in milliseconds) of all completed requests. */
    private int _minBackendResponseTime;

    /** Maximum backend response time (in milliseconds) of all completed requests. */
    private int _maxBackendResponseTime;

    /** Sum over backend response times (in milliseconds) of all completed requests. */
    private long _sumBackendResponseTime;

    private static void checkResolutionType(int res) {
        if (res != RES_FINE && res != RES_HOURLY && res != RES_DAILY) {
            throw new IllegalArgumentException("Invalid resolution type");
        }
    }

    /**
     * Constructs a statistical bin that is open for accumulating data. Remember
     * to call {@link #setEndTime} when closing it off.
     */
    public MetricsBin(final long startTime, int interval, int resolution,
                      String clusterNodeId, long serviceOid)
    {
        checkResolutionType(resolution);

        long periodStart = -1; // Will never be visible; invariant checked already
        if (resolution == RES_FINE) {
            // Find the beginning of the current fine period
            if (interval == 0) throw new IllegalArgumentException("Fine interval required");
            periodStart = (startTime / interval) * interval;
        } else if (resolution == RES_HOURLY) {
            interval = 60 * 60 * 1000;
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(startTime);
            cal.set(GregorianCalendar.MINUTE, 0);
            cal.set(GregorianCalendar.SECOND, 0);
            cal.set(GregorianCalendar.MILLISECOND, 0);
            periodStart = cal.getTimeInMillis();
        } else if (resolution == RES_DAILY) {
            interval = 24 * 60 * 60 * 1000;
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(startTime);
            cal.set(GregorianCalendar.HOUR_OF_DAY, 0);
            periodStart = cal.getTimeInMillis();
        }

        _clusterNodeId = clusterNodeId;
        _serviceOid = serviceOid;
        _resolution = resolution;
        _interval = interval;
        _periodStart = periodStart;
        _startTime = startTime;
        _endTime = _startTime;  // Signifies end time has not been set.
    }

    /** @deprecated to be used only for serialization and persistence */
    public MetricsBin() {
    }

    public String getClusterNodeId() {
        return _clusterNodeId;
    }

    public long getServiceOid() {
        return _serviceOid;
    }

    public int getResolution() {
        return _resolution;
    }

    public String getResolutionName() {
        return describeResolution(_resolution);
    }

    public static String describeResolution(int resolution) {
        switch (resolution) {
            case RES_FINE:
                return "fine";
            case RES_HOURLY:
                return "hourly";
            case RES_DAILY:
                return "daily";
            default:
                return "Unknown: " + resolution;
        }
    }

    /** Returns the nominal time period interval (in milliseconds). */
    public int getInterval() {
        return _interval;
    }

    /** Returns the nominal period start time (as UTC milliseconds from the epoch). */
    public long getPeriodStart() {
        return _periodStart;
    }

    /**
     * Returns the actual start time of the bin interval.
     *
     * @return start time as UTC milliseconds from the epoch
     */
    public long getStartTime() {
        return _startTime;
    }

    /**
     * Returns the actual end time of the bin interval. The same value as start
     * time will be returned if the end time has not been set explicitly
     * (i.e., the bin is still open).
     *
     * @return end time as UTC milliseconds from the epoch
     */
    public long getEndTime() {
        return _endTime;
    }

    public int getNumAttemptedRequest() {
        return _numAttemptedRequest;
    }

    public int getNumAuthorizedRequest() {
        return _numAuthorizedRequest;
    }

    public int getNumCompletedRequest() {
        return _numCompletedRequest;
    }

    /** Returns the rate of attempted requests (in messages per second) based on actual bin interval. */
    public double getActualAttemptedRate() {
        if (_startTime == _endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numAttemptedRequest / (_endTime - _startTime);
        }
    }

    /** Returns the rate of authorized requests (in messages per second) based on actual bin interval. */
    public double getActualAuthorizedRate() {
        if (_startTime == _endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numAuthorizedRequest / (_endTime - _startTime);
        }
    }

    /** Returns the rate of completed requests (in messages per second) based on actual bin interval. */
    public double getActualCompletedRate() {
        if (_startTime == _endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numCompletedRequest / (_endTime - _startTime);
        }
    }

    /** Returns the rate of attempted requests (in messages per second) based on nominal bin interval. */
    public double getNominalAttemptedRate() {
        return 1000.0 * _numAttemptedRequest / _interval;
    }

    /** Returns the rate of authorized requests (in messages per second) based on nominal bin interval. */
    public double getNominalAuthorizedRate() {
        return 1000.0 * _numAuthorizedRequest / _interval;
    }

    /** Returns the rate of completed requests (in messages per second) based on nominal bin interval. */
    public double getNominalCompletedRate() {
        return 1000.0 * _numCompletedRequest / _interval;
    }

    /** Returns the minimum frontend response time (in milliseconds) of all attempted requests. */
    public int getMinFrontendResponseTime() {
        return _minFrontendResponseTime;
    }

    /** Returns the maximum frontend response time (in milliseconds) of all attempted requests. */
    public int getMaxFrontendResponseTime() {
        return _maxFrontendResponseTime;
    }

    public long getSumFrontendResponseTime() {
        return _sumFrontendResponseTime;
    }

    /** Returns the average frontend response time (in milliseconds) of all attempted requests. */
    public double getAverageFrontendResponseTime() {
        if (_numAttemptedRequest == 0) {
            return 0.0;
        } else {
            return (double) _sumFrontendResponseTime / _numAttemptedRequest;
        }
    }

    /** Returns the minimum backend response time (in milliseconds) of all completed requests. */
    public int getMinBackendResponseTime() {
        return _minBackendResponseTime;
    }

    /** Returns the maximum backend response time (in milliseconds) of all completed requests. */
    public int getMaxBackendResponseTime() {
        return _maxBackendResponseTime;
    }

    public long getSumBackendResponseTime() {
        return _sumBackendResponseTime;
    }

    /** Returns the average backend response time (in milliseconds) of all completed requests. */
    public double getAverageBackendResponseTime() {
        if (_numCompletedRequest == 0) {
            return 0.0;
        } else {
            return (double) _sumBackendResponseTime / _numCompletedRequest;
        }
    }

    public void setClusterNodeId(String clusterNodeId) {
        _clusterNodeId = clusterNodeId;
    }

    public void setServiceOid(long serviceOid) {
        _serviceOid = serviceOid;
    }

    public void setResolution(int res) {
        checkResolutionType(res);
        _resolution = res;
    }

    public void setInterval(int interval) {
        _interval = interval;
    }

    public void setPeriodStart(long periodStart) {
        _periodStart = periodStart;
    }

    public void setStartTime(long startTime) {
        _startTime = startTime;
    }

    /** Remember to call this when closing off the bin to new message recording. */
    public void setEndTime(final long endTime) {
        // Ensures duration is greater than zero.
        if (endTime > _startTime) {
            _endTime = endTime;
        } else {
            _endTime = _startTime + 1;
        }
    }

    public void setNumAttemptedRequest(int numAttemptedRequest) {
        _numAttemptedRequest = numAttemptedRequest;
    }

    public void setNumAuthorizedRequest(int numAuthorizedRequest) {
        _numAuthorizedRequest = numAuthorizedRequest;
    }

    public void setNumCompletedRequest(int numCompletedRequest) {
        _numCompletedRequest = numCompletedRequest;
    }

    public void setMinFrontendResponseTime(int minFrontendResponseTime) {
        _minFrontendResponseTime = minFrontendResponseTime;
    }

    public void setMaxFrontendResponseTime(int maxFrontendResponseTime) {
        _maxFrontendResponseTime = maxFrontendResponseTime;
    }

    public void setSumFrontendResponseTime(long sumFrontendResponseTime) {
        _sumFrontendResponseTime = sumFrontendResponseTime;
    }

    public void setMinBackendResponseTime(int minBackendResponseTime) {
        _minBackendResponseTime = minBackendResponseTime;
    }

    public void setMaxBackendResponseTime(int maxBackendResponseTime) {
        _maxBackendResponseTime = maxBackendResponseTime;
    }

    public void setSumBackendResponseTime(long sumBackendResponseTime) {
        _sumBackendResponseTime = sumBackendResponseTime;
    }

    /** Records an attempted request. */
    public void addAttemptedRequest(final int frontendResponseTime) {
        if (_numAttemptedRequest == 0) {
            _minFrontendResponseTime = frontendResponseTime;
            _maxFrontendResponseTime = frontendResponseTime;
        } else {
            if (frontendResponseTime < _minFrontendResponseTime) {
                _minFrontendResponseTime = frontendResponseTime;
            }
            if (frontendResponseTime > _maxFrontendResponseTime) {
                _maxFrontendResponseTime = frontendResponseTime;
            }
        }
        _sumFrontendResponseTime += frontendResponseTime;
        ++ _numAttemptedRequest;
    }

    /** Records an authorized request. */
    public void addAuthorizedRequest() {
        ++ _numAuthorizedRequest;
    }

    /** Records a completed request. */
    public void addCompletedRequest(final int backendResponseTime) {
        if (_numCompletedRequest == 0) {
            _minBackendResponseTime = backendResponseTime;
            _maxBackendResponseTime = backendResponseTime;
        } else {
            if (backendResponseTime < _minBackendResponseTime) {
                _minBackendResponseTime = backendResponseTime;
            }
            if (backendResponseTime > _maxBackendResponseTime) {
                _maxBackendResponseTime = backendResponseTime;
            }
        }
        _sumBackendResponseTime += backendResponseTime;
        ++ _numCompletedRequest;
    }

    public String toString() {
        StringBuffer b = new StringBuffer("<MetricsBin resolution=\"");
        b.append(getResolutionName());
        b.append("\" periodStart=\"").append(DATE_FORMAT.format(new Date(_periodStart)));
        b.append("\" startTime=\"").append(DATE_FORMAT.format(new Date(_startTime)));
        b.append("\" endTime=\"").append(DATE_FORMAT.format(new Date(_endTime)));
        if (_resolution == RES_FINE) b.append("\" _interval=\"").append(_interval);
        b.append("\" attempted=\"").append(_numAttemptedRequest);
        b.append("\" authorized=\"").append(_numAuthorizedRequest);
        b.append("\" completed=\"").append(_numCompletedRequest);
        b.append("\"/>");
        return b.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MetricsBin that = (MetricsBin)o;

        if (_periodStart != that._periodStart) return false;
        if (_resolution != that._resolution) return false;
        if (_serviceOid != that._serviceOid) return false;
        if (_startTime != that._startTime) return false;
        if (_interval != that._interval) return false;
        return !(_clusterNodeId != null ? !_clusterNodeId.equals(that._clusterNodeId) : that._clusterNodeId != null);

    }

    public int hashCode() {
        int result;
        result = _resolution;
        result = 29 * result + (int)(_startTime ^ (_startTime >>> 32));
        result = 29 * result + (int)(_periodStart ^ (_periodStart >>> 32));
        result = 29 * result + (int)(_serviceOid ^ (_serviceOid >>> 32));
        result = 29 * result + (_clusterNodeId != null ? _clusterNodeId.hashCode() : 0);
        result = 29 * result + _interval;
        return result;
    }

    /**
     * Used for ordering the extraction of bins from a {@link EDU.oswego.cs.dl.util.concurrent.BoundedPriorityQueue}, which
     * prioritizes "lesser" objects.  Bins with finer resolution are prioritized over those of coarser
     * resolution.  When the bins are of the same resolution, bins belonging to an earlier period are
     * prioritized.
     */
    public int compareTo(Object o) {
        MetricsBin other = (MetricsBin)o;
        if (_resolution == other._resolution) {
            return (int)(_periodStart - other._periodStart);
        }
        return _resolution - other._resolution;
    }

}