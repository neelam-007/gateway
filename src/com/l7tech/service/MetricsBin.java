package com.l7tech.service;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * A statistical bin for collecting service metrics. Conceptually, a bin has
 * 2 distinct states: an open state to accumulate data (i.e., mutable state),
 * a closed state to read accumulated data (i.e., immutable state).
 * <p/>
 * A bin occupies a time interval. There is the <b>nominal</b> period
 * when a bin was supposed to start and end, according to regular scheduling.
 * But since timers are not exact, a bin also has the <b>actual</b> time
 * interval when the bin actually starts and ends.
 * <p/>
 * This class has limited thread safety for mutable methods only. For performance
 * reason, there is no synchronization for immutable methods and hibernate
 * setter/getter methods because, in typical usage, mutable methods are called
 * only during the open state, which never overlaps the closed state.
 *
 * @author rmak
 */
public class MetricsBin extends PersistentEntityImp implements Comparable {
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is fine resolution */
    public static final int RES_FINE = 0;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is hourly resolution */
    public static final int RES_HOURLY = 1;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is daily resolution */
    public static final int RES_DAILY = 2;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Logger _logger = Logger.getLogger(MetricsBin.class.getName());

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

    /** Nominal period start time (as UTC milliseconds from epoch). */
    private long _periodStart;


    /** Acutal start time (as UTC milliseconds from epoch). */
    private long _startTime;

    /** Actual end time (as UTC milliseconds from epoch). */
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

    /** Protects write access to {@link #_numAttemptedRequest}, {@link #_minFrontendResponseTime},
     * {@link #_maxFrontendResponseTime} and {@link #_sumFrontendResponseTime}. */
    private transient Object _attemptedLock = new Object();

    /** Protects write access to {@link #_numAuthorizedRequest}. */
    private transient Object _authorizedLock = new Object();

    /** Protects write access to {@link #_numCompletedRequest}, {@link #_minBackendResponseTime},
     * {@link #_maxBackendResponseTime} and {@link #_sumBackendResponseTime}. */
    private transient Object _completedLock = new Object();

    private static void checkResolutionType(int res) {
        if (res != RES_FINE && res != RES_HOURLY && res != RES_DAILY) {
            throw new IllegalArgumentException("Invalid resolution type");
        }
    }

    /**
     * Calculates the start time of a nominal period that would bracket the
     * given time instance.
     *
     * @param resolution    bin resolution; one of {@link #RES_FINE}, {@link #RES_HOURLY}
     *                      or {@link #RES_DAILY}
     * @param fineInterval  fine bin interval (in milliseconds); requried if
     *                      <code>resolution</code> is {@link #RES_FINE}
     * @param millis        time instance to be bracketed (as UTC milliseconds from epoch)
     * @return start time of the nominal period (as UTC milliseconds from epoch)
     * @throws IllegalArgumentException if <code>resolution</code> is {@link #RES_FINE}
     *                                  but <code>fineInterval</code> <= 0
     */
    public static long periodStartFor(final int resolution,
                                      final int fineInterval,
                                      final long millis) {
        long result = -1;
        switch (resolution) {
            case RES_FINE: {
                if (fineInterval <= 0) {
                    throw new IllegalArgumentException("Fine bin interval is required.");
                }
                result = (millis / fineInterval) * fineInterval;
                break;
            }
            case RES_HOURLY: {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                result = cal.getTimeInMillis();
                break;
            }
            case RES_DAILY: {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                result = cal.getTimeInMillis();
                break;
            }
        }
        return result;
    }

    /**
     * Calculates the end time of a nominal period that would bracket the
     * given time instance.
     *
     * @param resolution    bin resolution; one of {@link #RES_FINE}, {@link #RES_HOURLY}
     *                      or {@link #RES_DAILY}
     * @param fineInterval  fine bin interval (in milliseconds); requried if
     *                      <code>resolution</code> is {@link #RES_FINE}
     * @param millis        time instance to be bracketed (as UTC milliseconds from epoch)
     * @return start time of the nominal period (as UTC milliseconds from epoch)
     * @throws IllegalArgumentException if <code>resolution</code> is {@link #RES_FINE}
     *                                  but <code>fineInterval</code> <= 0
     */
    public static long periodEndFor(final int resolution,
                                    final int fineInterval,
                                    final long millis) {
        long result = -1;
        switch (resolution) {
            case RES_FINE: {
                if (fineInterval <= 0) {
                    throw new IllegalArgumentException("Fine bin interval is required.");
                }
                result = ((millis / fineInterval) + 1) * fineInterval;
                break;
            }
            case RES_HOURLY: {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);
                cal.add(Calendar.HOUR_OF_DAY, 1);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                result = cal.getTimeInMillis();
                break;
            }
            case RES_DAILY: {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(millis);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                result = cal.getTimeInMillis();
                break;
            }
        }
        return result;
    }

    /**
     * Combines data from multiple bins into one.
     *
     * @param bins      list of {@link MetricsBin} to combine; must contain at least one
     * @param result    bin to put result in (whose cluster node ID, service OID,
     *                  and resolution are left unmodified
     * @throws IllegalArgumentException if <code>bins</code> is empty
     */
    public static void combine(final List bins, final MetricsBin result) {
        if (bins.size() == 0) {
            throw new IllegalArgumentException("Must have at least one metrics bin.");
        }

        long periodStart = -1;
        long periodEnd = -1;
        long startTime = -1;
        long endTime = -1;
        int numAttemptedRequest = 0;
        int numAuthorizedRequest = 0;
        int numCompletedRequest = 0;
        int minFrontendResponseTime = 0;
        int maxFrontendResponseTime = 0;
        long sumFrontendResponseTime = 0;
        int minBackendResponseTime = 0;
        int maxBackendResponseTime = 0;
        long sumBackendResponseTime = 0;

        boolean first = true;
        for (Iterator itor = bins.iterator(); itor.hasNext();) {
            final MetricsBin bin = (MetricsBin) itor.next();
            if (first) {
                periodStart = bin.getPeriodStart();
                periodEnd = bin.getPeriodEnd();
                startTime = bin.getStartTime();
                endTime = bin.getEndTime();
                minFrontendResponseTime = bin.getMinFrontendResponseTime();
                maxFrontendResponseTime = bin.getMaxFrontendResponseTime();
                sumFrontendResponseTime = bin.getSumFrontendResponseTime();
                minBackendResponseTime = bin.getMinBackendResponseTime();
                maxBackendResponseTime = bin.getMaxBackendResponseTime();
                sumBackendResponseTime = bin.getSumBackendResponseTime();
                numAttemptedRequest = bin.getNumAttemptedRequest();
                numAuthorizedRequest = bin.getNumAuthorizedRequest();
                numCompletedRequest = bin.getNumCompletedRequest();
                first = false;
            } else {
                periodStart = Math.min(periodStart, bin.getPeriodStart());
                periodEnd = Math.max(periodEnd, bin.getPeriodEnd());
                startTime = Math.min(startTime, bin.getStartTime());
                endTime = Math.max(endTime, bin.getEndTime());
                if (numAttemptedRequest == 0) {
                    minFrontendResponseTime = bin.getMinFrontendResponseTime();
                    maxFrontendResponseTime = bin.getMaxFrontendResponseTime();
                } else {
                    if (bin.getNumAttemptedRequest() != 0) {
                        minFrontendResponseTime = Math.min(minFrontendResponseTime, bin.getMinFrontendResponseTime());
                        maxFrontendResponseTime = Math.max(maxFrontendResponseTime, bin.getMaxFrontendResponseTime());
                    }
                }
                sumFrontendResponseTime += bin.getSumFrontendResponseTime();
                if (numCompletedRequest == 0) {
                    minBackendResponseTime = bin.getMinBackendResponseTime();
                    maxBackendResponseTime = bin.getMaxBackendResponseTime();
                } else {
                    if (bin.getNumCompletedRequest() != 0) {
                        minBackendResponseTime = Math.min(minBackendResponseTime, bin.getMinBackendResponseTime());
                        maxBackendResponseTime = Math.max(maxBackendResponseTime, bin.getMaxBackendResponseTime());
                    }
                }
                sumBackendResponseTime += bin.getSumBackendResponseTime();
                numAttemptedRequest += bin.getNumAttemptedRequest();
                numAuthorizedRequest += bin.getNumAuthorizedRequest();
                numCompletedRequest += bin.getNumCompletedRequest();
            }
        }

        result.setPeriodStart(periodStart);
        result.setInterval((int)(periodEnd - periodStart));
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setNumAttemptedRequest(numAttemptedRequest);
        result.setNumAuthorizedRequest(numAuthorizedRequest);
        result.setNumCompletedRequest(numCompletedRequest);
        result.setMinFrontendResponseTime(minFrontendResponseTime);
        result.setMaxFrontendResponseTime(maxFrontendResponseTime);
        result.setSumFrontendResponseTime(sumFrontendResponseTime);
        result.setMinBackendResponseTime(minBackendResponseTime);
        result.setMaxBackendResponseTime(maxBackendResponseTime);
        result.setSumBackendResponseTime(sumBackendResponseTime);
    }

    /**
     * Constructs a statistical bin that is open for accumulating data. Remember
     * to call {@link #setEndTime} when closing it off.
     *
     * @param fineInterval  fine bin interval (in milliseconds); requried if
     *                      <code>resolution</code> is {@link #RES_FINE}
     * @throws IllegalArgumentException if <code>resolution</code> is {@link #RES_FINE}
     *                                  but <code>fineInterval</code> <= 0
     */
    public MetricsBin(final long startTime, int fineInterval, int resolution,
                      String clusterNodeId, long serviceOid)
    {
        checkResolutionType(resolution);

        _clusterNodeId = clusterNodeId;
        _serviceOid = serviceOid;
        _resolution = resolution;
        _periodStart = periodStartFor(resolution, fineInterval, startTime);
        if (resolution == RES_FINE) {
            _interval = fineInterval;
        } else if (resolution == RES_HOURLY) {
            _interval = 60 * 60 * 1000;
        } else if (resolution == RES_DAILY) {
            // Not neccessarily 24 hours, e.g., when switching Daylight Savings Time.
            final long periodEnd = periodEndFor(resolution, fineInterval, startTime);
            _interval = (int)(periodEnd - _periodStart);
            if (_interval != 24 * 60 * 60 * 1000 && _logger.isLoggable(Level.FINE)) {
                _logger.fine("Created non-24-hour long daily bin: interval = " +
                             (_interval / 60. / 60. / 1000.) + " hours, start time = " +
                             new Date(startTime));
            }
        }
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

    /** Returns the nominal period interval (in milliseconds). */
    public int getInterval() {
        return _interval;
    }

    /** Returns the nominal period start time (as UTC milliseconds from epoch). */
    public long getPeriodStart() {
        return _periodStart;
    }

    /** Returns the nominal period end time (as UTC milliseconds from epoch). */
    public long getPeriodEnd() {
        return _periodStart + _interval;
    }

    /**
     * Returns the actual start time of the bin interval.
     *
     * @return start time as UTC milliseconds from epoch
     */
    public long getStartTime() {
        return _startTime;
    }

    /**
     * Returns the actual end time of the bin interval. The same value as start
     * time will be returned if the end time has not been set explicitly
     * (i.e., the bin is still open).
     *
     * @return end time as UTC milliseconds from epoch
     */
    public long getEndTime() {
        return _endTime;
    }

    public int getNumAttemptedRequest() {
        synchronized(_attemptedLock) {
            return _numAttemptedRequest;
        }
    }

    public int getNumAuthorizedRequest() {
        synchronized(_authorizedLock){
            return _numAuthorizedRequest;
        }
    }

    public int getNumCompletedRequest() {
        synchronized(_completedLock){
            return _numCompletedRequest;
        }
    }

    /** Returns the rate of attempted requests (in messages per second) based on actual time interval. */
    public double getActualAttemptedRate() {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (startTime == endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            synchronized(_attemptedLock){
                return 1000.0 * _numAttemptedRequest / (endTime - startTime);
            }
        }
    }

    /** Returns the rate of authorized requests (in messages per second) based on actual time interval. */
    public double getActualAuthorizedRate() {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (startTime == endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            synchronized(_authorizedLock){
                return 1000.0 * _numAuthorizedRequest / (endTime - startTime);
            }
        }
    }

    /** Returns the rate of completed requests (in messages per second) based on actual time interval. */
    public double getActualCompletedRate() {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (startTime == endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            synchronized(_completedLock){
                return 1000.0 * _numCompletedRequest / (endTime - startTime);
            }
        }
    }

    /** Returns the rate of attempted requests (in messages per second) based on nominal period. */
    public double getNominalAttemptedRate() {
        int interval = _interval;
        synchronized(_attemptedLock){
            return 1000.0 * _numAttemptedRequest / interval;
        }
    }

    /** Returns the rate of authorized requests (in messages per second) based on nominal period. */
    public double getNominalAuthorizedRate() {
        int interval = _interval;
        synchronized(_authorizedLock){
            return 1000.0 * _numAuthorizedRequest / interval;
        }
    }

    /** Returns the rate of completed requests (in messages per second) based on nominal period. */
    public double getNominalCompletedRate() {
        int interval = _interval;
        synchronized(_completedLock){
            return 1000.0 * _numCompletedRequest / interval;
        }
    }

    /** Returns the minimum frontend response time (in milliseconds) of all attempted requests. */
    public int getMinFrontendResponseTime() {
        synchronized(_attemptedLock) {
            return _minFrontendResponseTime;
        }
    }

    /** Returns the maximum frontend response time (in milliseconds) of all attempted requests. */
    public int getMaxFrontendResponseTime() {
        synchronized(_attemptedLock) {
            return _maxFrontendResponseTime;
        }
    }

    public long getSumFrontendResponseTime() {
        synchronized(_attemptedLock){
            return _sumFrontendResponseTime;
        }
    }

    /** Returns the average frontend response time (in milliseconds) of all attempted requests. */
    public double getAverageFrontendResponseTime() {
        int numAttemptedRequest;
        long sumFrontendResponseTime;
        synchronized(_attemptedLock){
            numAttemptedRequest = _numAttemptedRequest;
            sumFrontendResponseTime = _sumFrontendResponseTime;
        }
        if (numAttemptedRequest == 0) {
            return 0.0;
        } else {
            return (double) sumFrontendResponseTime / numAttemptedRequest;
        }
    }

    /** Returns the minimum backend response time (in milliseconds) of all completed requests. */
    public int getMinBackendResponseTime() {
        synchronized(_completedLock){
            return _minBackendResponseTime;
        }
    }

    /** Returns the maximum backend response time (in milliseconds) of all completed requests. */
    public int getMaxBackendResponseTime() {
        synchronized(_completedLock){
            return _maxBackendResponseTime;
        }
    }

    public long getSumBackendResponseTime() {
        synchronized(_completedLock){
            return _sumBackendResponseTime;
        }
    }

    /** Returns the average backend response time (in milliseconds) of all completed requests. */
    public double getAverageBackendResponseTime() {
        int numCompletedRequest;
        long sumBackendResponseTime;
        synchronized(_completedLock){
            numCompletedRequest = _numCompletedRequest;
            sumBackendResponseTime = _sumBackendResponseTime;
        }
        if (numCompletedRequest == 0) {
            return 0.0;
        } else {
            return (double) sumBackendResponseTime / numCompletedRequest;
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
        synchronized(_attemptedLock){
            _numAttemptedRequest = numAttemptedRequest;
        }
    }

    public void setNumAuthorizedRequest(int numAuthorizedRequest) {
        synchronized(_authorizedLock){
            _numAuthorizedRequest = numAuthorizedRequest;
        }
    }

    public void setNumCompletedRequest(int numCompletedRequest) {
        synchronized(_completedLock){
            _numCompletedRequest = numCompletedRequest;
        }
    }

    public void setMinFrontendResponseTime(int minFrontendResponseTime) {
        synchronized(_attemptedLock) {
            _minFrontendResponseTime = minFrontendResponseTime;
        }
    }

    public void setMaxFrontendResponseTime(int maxFrontendResponseTime) {
        synchronized(_attemptedLock) {
            _maxFrontendResponseTime = maxFrontendResponseTime;
        }
    }

    public void setSumFrontendResponseTime(long sumFrontendResponseTime) {
        synchronized(_attemptedLock){
            _sumFrontendResponseTime = sumFrontendResponseTime;
        }
    }

    public void setMinBackendResponseTime(int minBackendResponseTime) {
        synchronized(_completedLock){
            _minBackendResponseTime = minBackendResponseTime;
        }
    }

    public void setMaxBackendResponseTime(int maxBackendResponseTime) {
        synchronized(_completedLock){
            _maxBackendResponseTime = maxBackendResponseTime;
        }
    }

    public void setSumBackendResponseTime(long sumBackendResponseTime) {
        synchronized(_completedLock){
            _sumBackendResponseTime = sumBackendResponseTime;
        }
    }

    /** Records an attempted request. */
    public void addAttemptedRequest(int frontendResponseTime) {
        if (frontendResponseTime < 0) {
            // Don't really know what causes negative response time sometimes,
            // or if this still happens. Just suppress it and log warning.
            // (Bugzilla # 2328)
            _logger.warning("Negative frontend response time (" + frontendResponseTime +
                            " ms) suppressed (forced to zero) in " + describeResolution(_resolution) +
                            " bin (period start = " + new Date(_periodStart) + ").");
            frontendResponseTime = 0;
        }

        synchronized(_attemptedLock) {
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
    }

    /** Records an authorized request. */
    public void addAuthorizedRequest() {
        synchronized(_authorizedLock) {
            ++ _numAuthorizedRequest;
        }
    }

    /** Records a completed request. */
    public void addCompletedRequest(int backendResponseTime) {
        if (backendResponseTime < 0) {
            // Don't really know what causes negative response time sometimes,
            // or if this still happens. Just suppress it and log warning.
            // (Bugzilla # 2328)
            _logger.warning("Negative backend response time (" + backendResponseTime +
                            " ms) suppressed (forced to zero) in " + describeResolution(_resolution) +
                            " bin (period start = " + new Date(_periodStart) + ").");
            backendResponseTime = 0;
        }

        synchronized(_completedLock) {
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
    }

    /**
     * Merge the given bin into this bin.
     *
     * <p>This assumes that the bins are NOT currently in use (or are locked).</p>
     *
     * @param other The bin to merge.
     * @throws IllegalArgumentException if the given bin is not compatible.
     */
    public void merge(MetricsBin other) {
        if (other != null) {
            if (this.equals(other)) {
                List bins = new ArrayList();
                bins.add(this);
                bins.add(other);
                combine(bins, this);
            }
        }
    }

    public String toString() {
        int numAttemptedRequest;
        synchronized(_attemptedLock){
            numAttemptedRequest = _numAttemptedRequest;
        }
        int numAuthorizedRequest;
        synchronized(_authorizedLock){
            numAuthorizedRequest = _numAuthorizedRequest;
        }
        int numCompletedRequest;
        synchronized(_completedLock){
            numCompletedRequest = _numCompletedRequest;
        }
        StringBuffer b = new StringBuffer("<MetricsBin resolution=\"");
        b.append(getResolutionName());
        b.append("\" periodStart=\"").append(DATE_FORMAT.format(new Date(_periodStart)));
        b.append("\" startTime=\"").append(DATE_FORMAT.format(new Date(_startTime)));
        b.append("\" endTime=\"").append(DATE_FORMAT.format(new Date(_endTime)));
        if (_resolution == RES_FINE) b.append("\" _interval=\"").append(_interval);
        b.append("\" attempted=\"").append(numAttemptedRequest);
        b.append("\" authorized=\"").append(numAuthorizedRequest);
        b.append("\" completed=\"").append(numCompletedRequest);
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

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
        in.defaultReadObject();
        _attemptedLock = new Object();
        _authorizedLock = new Object();
        _completedLock = new Object();
    }
}