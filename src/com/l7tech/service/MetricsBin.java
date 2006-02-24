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
 * This class is not thread safe for performance reason. Since the typical usage
 * is such that mutable methods are called only during the open state and
 * immutable methods are called only during the closed state, synchronization
 * will be more efficiently handled by the owner of this object, who controls
 * when to change from open to close state.
 *
 * @author rmak
 */
public class MetricsBin implements Serializable, Comparable {
    /**
     * Resolution of this bin.
     *
     * Must be one of {@link #RES_FINE}, {@link #RES_HOURLY} or {@link #RES_DAILY}.
     */
    private int _resolution = -1;

    /**
     * Start time as UTC milliseconds from the epoch.
     */
    private long _startTime;

    /**
     * End time as UTC milliseconds from the epoch.
     */
    private long _endTime;

    private int _numAttemptedRequest;
    private int _numAuthorizedRequest;
    private int _numCompletedRequest;

    /**
     * Minimum frontend response time (in milliseconds) of all attempted requests.
     */
    private int _minFrontendResponseTime;

    /**
     * Maximum frontend response time (in milliseconds) of all attempted requests.
     */
    private int _maxFrontendResponseTime;

    /**
     * Sum over frontend response times (in milliseconds) of all attempted requests.
     */
    private long _sumFrontendResponseTime;

    /**
     * Minimum backend response time (in milliseconds) of all completed requests.
     */
    private int _minBackendResponseTime;

    /**
     * Maximum backend response time (in milliseconds) of all completed requests.
     */
    private int _maxBackendResponseTime;

    /**
     * Sum over backend response times (in milliseconds) of all completed requests.
     */
    private long _sumBackendResponseTime;

    /**
     * Time at which the period represented by this bin started, as distinct from
     * {@link #_startTime}, which is the time when data collection for this bin began.
     */
    private long _periodStart;

    /**
     * Object ID of the {@link PublishedService} for which this bin collects data
     */
    private long _serviceOid;

    /**
     * MAC address of the cluster node from which this bin is collected
     */
    private String _clusterNodeId;

    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is fine resolution */
    public static final int RES_FINE = 0;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is hourly resolution */
    public static final int RES_HOURLY = 1;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is daily resolution */
    public static final int RES_DAILY = 2;
    private static final SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private int interval;

    /**
     * Constructs a statistical bin that is open for accumulating data. Remember
     * to call {@link #setEndTime} when closing it off.
     */
    public MetricsBin(final long startTime, int interval, int resolution,
                      String nodeId, long serviceOid)
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

        _startTime = startTime;
        _resolution = resolution;
        _periodStart = periodStart;
        _clusterNodeId = nodeId;
        _serviceOid = serviceOid;
        this.interval = interval;
        _endTime = _startTime;  // Signifies end time has not been set.
    }


    /**
     * Records an attempted request.
     */
    public void addAttemptedRequest(final int frontendResponseTime) {
        checkNotClosed();
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

    /**
     * Records an authorized request.
     */
    public void addAuthorizedRequest() {
        checkNotClosed();
        ++ _numAuthorizedRequest;
    }

    /**
     * Records a completed request.
     */
    public void addCompletedRequest(final int backendResponseTime) {
        checkNotClosed();
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

    /**
     * Returns the start time of the bin interval.
     *
     * @return start time as UTC milliseconds from the epoch
     */
    public long getStartTime() {
        return _startTime;
    }

    /**
     * Returns the end time of the bin interval. The same value as start time
     * will be returned if the end time has not been set explicitly
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

    /**
     * Returns the rate of attempted requests (per second).
     */
    public double getAttemptedRate() {
        checkClosed();
        if (_startTime == _endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numAttemptedRequest / (_endTime - _startTime);
        }
    }

    /**
     * @deprecated only for serialization and persistence
     */
    public void setNumAttemptedRequest(int numAttemptedRequest) {
        this._numAttemptedRequest = numAttemptedRequest;
    }

    /**
     * @deprecated only for serialization and persistence
     */
    public void setNumAuthorizedRequest(int numAuthorizedRequest) {
        this._numAuthorizedRequest = numAuthorizedRequest;
    }

    /**
     * @deprecated only for serialization and persistence
     */
    public void setNumCompletedRequest(int numCompletedRequest) {
        this._numCompletedRequest = numCompletedRequest;
    }

    /**
     * Returns the rate of authorized requests (per second).
     */
    public double getAuthorizedRate() {
        checkClosed();
        if (_startTime == _endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numAuthorizedRequest / (_endTime - _startTime);
        }
    }

    /**
     * Returns the rate of completed requests (per second).
     */
    public double getCompletedRate() {
        checkClosed();
        if (_startTime == _endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numCompletedRequest / (_endTime - _startTime);
        }
    }

    /**
     * Returns the minimum frontend response time (in milliseconds) of all attempted requests.
     */
    public int getMinFrontendResponseTime() {
        checkClosed();
        return _minFrontendResponseTime;
    }

    /**
     * Returns the maximum frontend response time (in milliseconds) of all attempted requests.
     */
    public int getMaxFrontendResponseTime() {
        checkClosed();
        return _maxFrontendResponseTime;
    }

    /**
     * Returns the average frontend response time (in milliseconds) of all attempted requests.
     */
    public double getAverageFrontendResponseTime() {
        checkClosed();
        if (_numAttemptedRequest == 0) {
            return 0.0;
        } else {
            return (double) _sumFrontendResponseTime / _numAttemptedRequest;
        }
    }

    /**
     * Returns the minimum backend response time (in milliseconds) of all completed requests.
     */
    public int getMinBackendResponseTime() {
        checkClosed();
        return _minBackendResponseTime;
    }

    /**
     * Returns the maximum backend response time (in milliseconds) of all completed requests.
     */
    public int getMaxBackendResponseTime() {
        checkClosed();
        return _maxBackendResponseTime;
    }

    /**
     * Returns the average backend response time (in milliseconds) of all completed requests.
     */
    public double getAverageBackendResponseTime() {
        checkClosed();
        if (_numCompletedRequest == 0) {
            return 0.0;
        } else {
            return (double) _sumBackendResponseTime / _numCompletedRequest;
        }
    }

    /**
     * Remember to call this when closing off the bin to new message recording.
     */
    public void setEndTime(final long endTime) {
        // Ensures duration is greater than zero.
        if (endTime > _startTime) {
            _endTime = endTime;
        } else {
            _endTime = _startTime + 1;
        }
    }

    public long getServiceOid() {
        return _serviceOid;
    }

    public int getResolution() {
        return _resolution;
    }

    public long getPeriodStart() {
        return _periodStart;
    }

    public String getClusterNodeId() {
        return _clusterNodeId;
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

    private static void checkResolutionType(int res) {
        if (res != RES_FINE && res != RES_HOURLY && res != RES_DAILY) {
            throw new IllegalArgumentException("Invalid resolution type");
        }
    }

    private void checkClosed() {
        if (_startTime == _endTime) throw new IllegalStateException("Bin is not yet closed");
    }

    private void checkNotClosed() {
        if (_startTime != _endTime) throw new IllegalStateException("Bin is no longer open");
    }

    /**
     * @deprecated should be used only for serialization and persistence
     */
    public void setPeriodStart(long periodStart) {
        this._periodStart = periodStart;
    }

    /**
     * @deprecated should be used only for serialization and persistence
     */
    public void setClusterNodeId(String clusterNodeId) {
        this._clusterNodeId = clusterNodeId;
    }

    /**
     * @deprecated should be used only for serialization and persistence
     */
    public void setResolution(int res) {
        checkResolutionType(res);
        this._resolution = res;
    }

    /**
     * @deprecated should be used only for serialization and persistence
     */
    public void setServiceOid(long serviceOid) {
        this._serviceOid = serviceOid;
    }

    /**
     * @deprecated should be used only for serialization and persistence
     */
    public void setStartTime(long startTime) {
        this._startTime = startTime;
    }

    /**
     * @deprecated to be used only for serialization and persistence
     */
    public MetricsBin() {
    }

    public String getResolutionName() {
        switch (_resolution) {
            case RES_FINE:
                return "fine";
            case RES_HOURLY:
                return "hourly";
            case RES_DAILY:
                return "daily";
            default:
                return "Unknown: " + _resolution;
        }
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String toString() {
        StringBuffer b = new StringBuffer("<MetricsBin resolution=\"");
        b.append(getResolutionName());
        b.append("\" periodStart=\"").append(DATEFORMAT.format(new Date(_periodStart)));
        b.append("\" startTime=\"").append(DATEFORMAT.format(new Date(_startTime)));
        b.append("\" endTime=\"").append(DATEFORMAT.format(new Date(_endTime)));
        if (_resolution == RES_FINE) b.append("\" interval=\"").append(interval);
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
        if (interval != that.interval) return false;
        if (_clusterNodeId != null ? !_clusterNodeId.equals(that._clusterNodeId) : that._clusterNodeId != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = _resolution;
        result = 29 * result + (int)(_startTime ^ (_startTime >>> 32));
        result = 29 * result + (int)(_periodStart ^ (_periodStart >>> 32));
        result = 29 * result + (int)(_serviceOid ^ (_serviceOid >>> 32));
        result = 29 * result + (_clusterNodeId != null ? _clusterNodeId.hashCode() : 0);
        result = 29 * result + interval;
        return result;
    }

}