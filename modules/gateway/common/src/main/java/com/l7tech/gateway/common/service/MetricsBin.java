package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.imp.GoidEntityImp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A statistical bin for service metrics.
 *
 * <p>A bin occupies a time interval. There is the <b>nominal</b> period
 * when a bin was supposed to start and end, according to regular scheduling.
 * But since timers are not exact, a bin also has the <b>actual</b> time
 * interval when the bin actually starts and ends.</p>
 *
 * @author rmak
 */
public class MetricsBin extends GoidEntityImp implements Comparable {
    public static final String ATTR_SERVICE_OID = "serviceOid";

    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is fine resolution */
    public static final int RES_FINE = 0;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is hourly resolution */
    public static final int RES_HOURLY = 1;
    /** The value to be used for {@link MetricsBin#getResolution()} to indicate that this bin is daily resolution */
    public static final int RES_DAILY = 2;

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
    protected int _resolution = -1;

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
    private Integer _minFrontendResponseTime;

    /** Maximum frontend response time (in milliseconds) of all attempted requests. */
    private Integer _maxFrontendResponseTime;

    /** Sum over frontend response times (in milliseconds) of all attempted requests. */
    private long _sumFrontendResponseTime;

    /** Minimum backend response time (in milliseconds) of all completed requests. */
    private Integer _minBackendResponseTime;

    /** Maximum backend response time (in milliseconds) of all completed requests. */
    private Integer _maxBackendResponseTime;

    /** Sum over backend response times (in milliseconds) of all completed requests. */
    private long _sumBackendResponseTime;

    /**
     * State of the associated {@link PublishedService} at approximately the time this bin was archived.
     */
    private ServiceState serviceState;

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
     * @param bins      collection of {@link MetricsBin} to combine; must contain at least one
     * @param result    bin to put result in (whose cluster node ID, service OID,
     *                  and resolution are left unmodified
     * @throws IllegalArgumentException if <code>bins</code> is empty
     *
     * @deprecated Use {@link MetricsSummaryBin} instead.
     */
    @Deprecated
    public static void combine(final List<MetricsBin> bins, final MetricsBin result) {
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
        for (MetricsBin bin : bins) {
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
                        minFrontendResponseTime = min(minFrontendResponseTime, bin.getMinFrontendResponseTime());
                        maxFrontendResponseTime = max(maxFrontendResponseTime, bin.getMaxFrontendResponseTime());
                    }
                }
                sumFrontendResponseTime += bin.getSumFrontendResponseTime();
                if (numCompletedRequest == 0) {
                    minBackendResponseTime = bin.getMinBackendResponseTime();
                    maxBackendResponseTime = bin.getMaxBackendResponseTime();
                } else {
                    if (bin.getNumCompletedRequest() != 0) {
                        minBackendResponseTime = min(minBackendResponseTime, bin.getMinBackendResponseTime());
                        maxBackendResponseTime = max(maxBackendResponseTime, bin.getMaxBackendResponseTime());
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

    /** To be used only for serialization and persistence */
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

    /** @return the nominal period interval (in milliseconds) */
    public int getInterval() {
        return _interval;
    }

    /** @return the nominal period start time (as UTC milliseconds from epoch) */
    public long getPeriodStart() {
        return _periodStart;
    }

    /** @return the nominal period end time (as UTC milliseconds from epoch) */
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
        return _numAttemptedRequest;
    }

    public int getNumAuthorizedRequest() {
        return _numAuthorizedRequest;
    }

    public int getNumCompletedRequest() {
        return _numCompletedRequest;
    }

    /** @return number of successful requests in this bin */
    public int getNumSuccess() {
        return _numCompletedRequest;
    }

    /** @return number of requests with policy violations in this bin */
    public int getNumPolicyViolation() {
        return _numAttemptedRequest - _numAuthorizedRequest;
    }

    /** @return number of requests with routing failures in this bin */
    public int getNumRoutingFailure() {
        return _numAuthorizedRequest - _numCompletedRequest;
    }

    /** @return number of all requests in this bin */
    public int getNumTotal() {
        return _numAttemptedRequest;
    }

    /** @return the rate of attempted requests (in messages per second) based on actual time interval */
    public double getActualAttemptedRate() {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (startTime == endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numAttemptedRequest / (endTime - startTime);
        }
    }

    /** @return the rate of authorized requests (in messages per second) based on actual time interval */
    public double getActualAuthorizedRate() {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (startTime == endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numAuthorizedRequest / (endTime - startTime);
        }
    }

    /** @return the rate of completed requests (in messages per second) based on actual time interval */
    public double getActualCompletedRate() {
        long startTime = getStartTime();
        long endTime = getEndTime();
        if (startTime == endTime) {
            // End time has not been set. Returns zero instead of ArithmeticException.
            return 0.0;
        } else {
            return 1000.0 * _numCompletedRequest / (endTime - startTime);
        }
    }

    /** @return the rate of attempted requests (in messages per second) based on nominal period */
    public double getNominalAttemptedRate() {
        int interval = _interval;
        return 1000.0 * _numAttemptedRequest / interval;
    }

    /** @return the rate of authorized requests (in messages per second) based on nominal period */
    public double getNominalAuthorizedRate() {
        int interval = _interval;
        return 1000.0 * _numAuthorizedRequest / interval;
    }

    /** @return the rate of completed requests (in messages per second) based on nominal period */
    public double getNominalCompletedRate() {
        int interval = _interval;
        return 1000.0 * _numCompletedRequest / interval;
    }

    /** @return the rate of successful requests (in requests per second) based on nominal period */
    public double getNominalSuccessRate() {
        return 1000.0 * getNumSuccess() / _interval;
    }

    /** @return the rate of requests with policy violation (in requests per second) based on nominal period */
    public double getNominalPolicyViolationRate() {
        return 1000.0 * getNumPolicyViolation() / _interval;
    }

    /** @return the rate of requests with routing failure (in requests per second) based on nominal period */
    public double getNominalRoutingFailureRate() {
        return 1000.0 * getNumRoutingFailure() / _interval;
    }

    /** @return the rate of all requests (in requests per second) based on nominal period */
    public double getNominalTotalRate() {
        return 1000.0 * getNumTotal() / _interval;
    }

    /** @return the minimum frontend response time (in milliseconds) of all attempted requests;
     *          this is meaningful only if {@link #getNumAttemptedRequest()} returns non-zero */
    public Integer getMinFrontendResponseTime() {
        return _minFrontendResponseTime;
    }

    /** @return the maximum frontend response time (in milliseconds) of all attempted requests;
     *          this is meaningful only if {@link #getNumAttemptedRequest()} returns non-zero */
    public Integer getMaxFrontendResponseTime() {
        return _maxFrontendResponseTime;
    }

    public long getSumFrontendResponseTime() {
        return _sumFrontendResponseTime;
    }

   public void setServiceState(ServiceState serviceState) {
        this.serviceState = serviceState;
    }

    public ServiceState getServiceState() {
        return serviceState;
    }

    /** @return the average frontend response time (in milliseconds) of all attempted requests */
    public double getAverageFrontendResponseTime() {
        int numAttemptedRequest = _numAttemptedRequest;
        long sumFrontendResponseTime = _sumFrontendResponseTime;
        if (numAttemptedRequest == 0) {
            return 0.0;
        } else {
            return (double) sumFrontendResponseTime / numAttemptedRequest;
        }
    }

    /** @return the minimum backend response time (in milliseconds) of all completed requests;
     *          this is meaningful only if {@link #getNumCompletedRequest()} returns non-zero */
    public Integer getMinBackendResponseTime() {
        return _minBackendResponseTime;
    }

    /** @return the maximum backend response time (in milliseconds) of all completed requests;
     *          this is meaningful only if {@link #getNumCompletedRequest()} returns non-zero */
    public Integer getMaxBackendResponseTime() {
        return _maxBackendResponseTime;
    }

    public long getSumBackendResponseTime() {
        return _sumBackendResponseTime;
    }

    /** @return the average backend response time (in milliseconds) of all completed requests */
    public double getAverageBackendResponseTime() {
        int numCompletedRequest = _numCompletedRequest;
        long sumBackendResponseTime = _sumBackendResponseTime;
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

    /**
     * Remember to call this when closing off the bin to new message recording.
     * @param endTime   actual end time (as UTC milliseconds from epoch)
     */
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

    public void setMinFrontendResponseTime(Integer minFrontendResponseTime) {
        _minFrontendResponseTime = minFrontendResponseTime;
    }

    public void setMaxFrontendResponseTime(Integer maxFrontendResponseTime) {
        _maxFrontendResponseTime = maxFrontendResponseTime;
    }

    public void setSumFrontendResponseTime(long sumFrontendResponseTime) {
        _sumFrontendResponseTime = sumFrontendResponseTime;
    }

    public void setMinBackendResponseTime(Integer minBackendResponseTime) {
        _minBackendResponseTime = minBackendResponseTime;
    }

    public void setMaxBackendResponseTime(Integer maxBackendResponseTime) {
        _maxBackendResponseTime = maxBackendResponseTime;
    }

    public void setSumBackendResponseTime(long sumBackendResponseTime) {
        _sumBackendResponseTime = sumBackendResponseTime;
    }

    /**
     * Merge the given bin into this bin if they are equal (i.e., same cluster
     * node ID, published service OID, resolution, period start and interval).
     *
     * <p>This assumes that the bins are NOT currently in use (or are locked).</p>
     *
     * @param other The bin to merge.
     * @throws IllegalArgumentException if the given bin is not compatible.
     */
    public void merge(MetricsBin other) {
        if (other != null) {
            if (this.equals(other)) {
                _startTime = Math.min(_startTime, other.getStartTime());
                _endTime = Math.max(_endTime, other.getEndTime());

                if (_numAttemptedRequest == 0) {
                    _minFrontendResponseTime = other.getMinFrontendResponseTime();
                    _maxFrontendResponseTime = other.getMaxFrontendResponseTime();
                } else {
                    if (other.getNumAttemptedRequest() != 0) {
                        _minFrontendResponseTime = min(_minFrontendResponseTime==null?Integer.MAX_VALUE:_minFrontendResponseTime, other.getMinFrontendResponseTime());
                        _maxFrontendResponseTime = max(_maxFrontendResponseTime==null?-1:_minFrontendResponseTime, other.getMaxFrontendResponseTime());
                    }
                }

                if (_numCompletedRequest == 0) {
                    _minBackendResponseTime = other.getMinBackendResponseTime();
                    _maxBackendResponseTime = other.getMaxBackendResponseTime();
                } else {
                    if (other.getNumCompletedRequest() != 0) {
                        _minBackendResponseTime = min(_minBackendResponseTime==null?Integer.MAX_VALUE:_minBackendResponseTime, other.getMinBackendResponseTime());
                        _maxBackendResponseTime = max(_maxBackendResponseTime==null?-1:_minBackendResponseTime, other.getMaxBackendResponseTime());
                    }
                }

                _sumFrontendResponseTime += other.getSumFrontendResponseTime();
                _sumBackendResponseTime += other.getSumBackendResponseTime();
                _numAttemptedRequest += other.getNumAttemptedRequest();
                _numAuthorizedRequest += other.getNumAuthorizedRequest();
                _numCompletedRequest += other.getNumCompletedRequest();
            }
        }
    }

    public String toString() {
        int numAttemptedRequest = _numAttemptedRequest;
        int numAuthorizedRequest = _numAuthorizedRequest;
        int numCompletedRequest = _numCompletedRequest;

        StringBuffer b = new StringBuffer("<MetricsBin resolution=\"");
        b.append(getResolutionName());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        b.append("\" periodStart=\"").append(dateFormat.format(new Date(_periodStart)));
        b.append("\" startTime=\"").append(dateFormat.format(new Date(_startTime)));
        b.append("\" endTime=\"").append(dateFormat.format(new Date(_endTime)));
        if (_resolution == RES_FINE) b.append("\" interval=\"").append(_interval);
        b.append("\" attempted=\"").append(numAttemptedRequest);
        b.append("\" authorized=\"").append(numAuthorizedRequest);
        b.append("\" completed=\"").append(numCompletedRequest);
        b.append("\"/>");
        return b.toString();
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MetricsBin that = (MetricsBin)o;

        if (_periodStart != that._periodStart) return false;
        if (_resolution != that._resolution) return false;
        if (_serviceOid != that._serviceOid) return false;
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

    static int min( final int value1, final Integer value2 ) {
        return value2==null ? value1 : Math.min( value1, value2 );
    }

    static int max( final int value1, final Integer value2 ) {
        return value2==null ? value1 : Math.max( value1, value2 );
    }
}
