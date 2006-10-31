package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.BoundedChannel;
import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import com.l7tech.service.MetricsBin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service metrics compiles statistics on access to published service.
 * <p/>
 * Data are binned and archived at 3 level of resolutions:
 * <ul>
 * <li>fine : user definable interval, arbitrary start time
 * <li>hourly : starts on every hour (hh:00:00 local time) except for the first bin
 * <li>daily : starts on every day (00:00:00 local time) except for the first bin
 * </ul>
 *
 * TODO In the current implementation, the bins at the 3 resolution levels are
 * independently of each other. Each message has to be recorded into the 3
 * current bins. Such CPU usage may be reduced to 1/3 by recording only into the
 * fine resolution bin and then collating them into the hourly bin on the hour
 * and the daily bin at midnight.
 *
 * TODO what happens when the interval changes?
 *
 * @author rmak
 */
public class ServiceMetrics {
    private static final Logger _logger = Logger.getLogger(ServiceMetrics.class.getName());

    /**
     * Protects {@link #_currentFineBin}. This is reader-preference because the MessageProcessor "reads"
     * the currentFineBin, whereas the archive task "writes."
     * <p/>
     * Note that we can't use {@link #_currentFineBin}'s monitor to protect the field,
     * since the monitor belongs to the object, not the field, which gets reset constantly.
     */
    private final ReadWriteLock _fineLock = new ReaderPreferenceReadWriteLock();

    /**
     * Protects {@link #_currentHourlyBin}.
     */
    private final ReadWriteLock _hourlyLock = new ReaderPreferenceReadWriteLock();

    /**
     * Protects {@link #_currentDailyBin}.
     */
    private final ReadWriteLock _dailyLock = new ReaderPreferenceReadWriteLock();

    /**
     * Archived bins are added to this {@link Channel} when they are finished.
     *
     * The {@link ServiceMetricsManager.Flusher} reads from this queue and writes to the database.
     */
    private final Channel _queue;

    /**
     * The OID of the {@link com.l7tech.service.PublishedService} for which these MetricsBins were
     * collected.
     */
    private final long _serviceOid;

    /**
     * The fine resolution bin that is currently collecting statistics.
     */
    private MetricsBin _currentFineBin;

    /**
     * The hourly resolution bin that is currently collecting statistics.
     */
    private MetricsBin _currentHourlyBin;

    /**
     * The daily resolution bin that is currently collecting statistics.
     */
    private MetricsBin _currentDailyBin;

    /**
     * The interval, in milliseconds, to be used for fine bins.
     * TODO what happens when the interval changes?
     */
    private final int _fineInterval;

    /**
     * The MAC address of the cluster node for which these MetricsBins were collected.
     */
    private final String _clusterNodeId;

    /**
     * @param serviceOid the OID of the {@link com.l7tech.service.PublishedService} for which metrics are being gathered
     * @param nodeId the MAC address of the cluster node for which metrics are being gathered
     * @param fineInterval the interval currently being used for buckets of {@link MetricsBin#RES_FINE} resolution
     * @param queue the ServiceMetricsManager queue to which archived bins should be added
     */
    public ServiceMetrics(long serviceOid, String nodeId, int fineInterval, BoundedChannel queue) {
        if (serviceOid < 0) throw new IllegalArgumentException("serviceOid must be positive");
        if (nodeId == null || nodeId.length() == 0) throw new IllegalArgumentException("nodeId must not be null or empty");
        if (fineInterval < 0) throw new IllegalArgumentException("fineInterval must be positive");
        if (queue == null) throw new NullPointerException();

        this._queue = queue;
        _serviceOid = serviceOid;
        _clusterNodeId = nodeId;
        _fineInterval = fineInterval;

        final long now = System.currentTimeMillis();
        _currentFineBin = new MetricsBin(now, fineInterval, MetricsBin.RES_FINE, nodeId, serviceOid);
        _currentHourlyBin = new MetricsBin(now, 0, MetricsBin.RES_HOURLY, nodeId, serviceOid);
        _currentDailyBin = new MetricsBin(now, 0, MetricsBin.RES_DAILY, nodeId, serviceOid);
    }

    public long getServiceOid() {
        return _serviceOid;
    }


    public String getClusterNodeId() {
        return _clusterNodeId;
    }

    /**
     * Record a request.
     *
     * @param authorized True if the policy execution was successful (routing attempted).
     * @param completed  True if the routing was successful
     * @param frontTime  Complete time for request processing
     * @param backTime   Time taken by the protected service
     */
    public void addRequest(boolean authorized, boolean completed, int frontTime, int backTime) {
        lockCurrentBins();
        try {
            addAttemptedRequest(frontTime);
            if (authorized) {
                addAuthorizedRequest();
                if (completed) {
                    addCompletedRequest(backTime);
                }
            }
        } finally {
            unlockCurrentBins();
        }
    }

    /**
     * Archives the current fine resolution bin and starts a new one.
     */
    public void archiveFineBin() {
        // Use locking to stop further modification to the current bin before it is archived.
        try {
            _fineLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentFineBin.setEndTime(now);
            try {
                _queue.put(_currentFineBin);
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }

            _currentFineBin = new MetricsBin(now, _fineInterval, MetricsBin.RES_FINE, _clusterNodeId, _serviceOid);
        } catch (InterruptedException e) {
            _logger.warning("Interrupted waiting for fine bin write lock");
            Thread.currentThread().interrupt();
        } finally {
            _fineLock.writeLock().release();
        }
    }

    /**
     * Archives the current hourly resolution bin and starts a new one.
     */
    public void archiveHourlyBin() {
        // Use locking to stop further modification to the current bin before it is archived.
        try {
            _hourlyLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentHourlyBin.setEndTime(now);
            try {
                _queue.put(_currentHourlyBin);
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            _currentHourlyBin = new MetricsBin(now, 0, MetricsBin.RES_HOURLY, _clusterNodeId, _serviceOid);
        } catch (InterruptedException e) {
            _logger.warning("Interrupted waiting for hourly bin write lock");
            Thread.currentThread().interrupt();
        } finally {
            _hourlyLock.writeLock().release();
        }
    }

    /**
     * Archives the current daily resolution bin and starts a new one.
     */
    public void archiveDailyBin() {
        // Use locking to stop further modification to the current bin before it is archived.
        try {
            _dailyLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentDailyBin.setEndTime(now);
            try {
                _queue.put(_currentDailyBin);
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            _currentDailyBin = new MetricsBin(now, 0, MetricsBin.RES_DAILY, _clusterNodeId, _serviceOid);
        } catch (InterruptedException e) {
            _logger.warning("Interrupted waiting for daily bin write lock");
            Thread.currentThread().interrupt();
        } finally {
            _dailyLock.writeLock().release();
        }
    }

    /**
     * Pins down the current bins, i.e., stops current bins from being archived
     * and new current bins being created until {@link #unlockCurrentBins()} is
     * called.
     * <p/>
     * This is used to ensure group of calls to {@link #addAttemptedRequest},
     * {@link #addAuthorizedRequest}, {@link #addCompletedRequest} are applied
     * to the same current bins.
     * <p/>
     * TODO This is neccessary because we are using counters for the old categories
     * (Attempted, Authorized, Completed) which are successively inclusive and
     * needs to be incremented as a group. If we instead use counters for
     * the new categories (Routing Failure, Policy Violation, Success), which
     * are mutually exclusive, callers will not have to worry about locking, and
     * synchronization with the archive*Bins methods can be internalized.
     */
    private void lockCurrentBins() {
        try {
            _fineLock.readLock().acquire();
            _hourlyLock.readLock().acquire();
            _dailyLock.readLock().acquire();
        } catch (InterruptedException e) {
            _logger.warning("Interrupted waiting for current bins read lock.");
            unlockCurrentBins();
            Thread.currentThread().interrupt();
        }
    }

    private void unlockCurrentBins() {
        _fineLock.readLock().release();
        _hourlyLock.readLock().release();
        _dailyLock.readLock().release();
    }    /**
     * Records an attempted request.
     *
     * Calls to {@link #addAttemptedRequest}, {@link #addAuthorizedRequest}, {@link #addCompletedRequest}
     * should be surrounded by {@link #lockCurrentBins()} and {@link #unlockCurrentBins()}.
     */
    private void addAttemptedRequest(final int frontendResponseTime) {
        _currentFineBin.addAttemptedRequest(frontendResponseTime);
        _currentHourlyBin.addAttemptedRequest(frontendResponseTime);
        _currentDailyBin.addAttemptedRequest(frontendResponseTime);
    }

    /**
     * Records an authorized request.
     *
     * Calls to {@link #addAttemptedRequest}, {@link #addAuthorizedRequest}, {@link #addCompletedRequest}
     * should be surrounded by {@link #lockCurrentBins()} and {@link #unlockCurrentBins()}.
     */
    private void addAuthorizedRequest() {
        _currentFineBin.addAuthorizedRequest();
        _currentHourlyBin.addAuthorizedRequest();
        _currentDailyBin.addAuthorizedRequest();
    }

    /**
     * Records a completed request.
     *
     * Calls to {@link #addAttemptedRequest}, {@link #addAuthorizedRequest}, {@link #addCompletedRequest}
     * should be surrounded by {@link #lockCurrentBins()} and {@link #unlockCurrentBins()}.
     */
    private void addCompletedRequest(final int backendResponseTime) {
        _currentFineBin.addCompletedRequest(backendResponseTime);
        _currentHourlyBin.addCompletedRequest(backendResponseTime);
        _currentDailyBin.addCompletedRequest(backendResponseTime);
    }
}
