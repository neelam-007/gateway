package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.*;
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
 * @author rmak
 */
public class ServiceMetrics {
    private static final Logger logger = Logger.getLogger(ServiceMetrics.class.getName());

    /**
     * Protects {@link _currentFineBin}. This is reader-preference because the MessageProcessor "reads"
     * the currentFineBin, whereas the archive task "writes."
     * <p/>
     * Note that we can't use {@link _currentFineBin}'s monitor to protect the field,
     * since the monitor belongs to the object, not the field, which gets reset constantly.
     */
    private final ReadWriteLock fineLock = new ReaderPreferenceReadWriteLock();

    /**
     * Protects {@link _currentHourlyBin}.  Not a ReadWriteLock due to infrequent use.
     */
    private final Object hourlyLock = new Object();

    /**
     * Protects {@link _currentDailyBin}.  Not a ReadWriteLock due to infrequent use.
     */
    private final Object dailyLock = new Object();

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
     * TODO what happens when the interval changes?
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
        _fineInterval = fineInterval; // TODO what happens when the interval changes?

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
     * Records an attempted request.
     */
    public void addAttemptedRequest(final int frontendResponseTime) {
        // Use locking to prevent the current bins from being archived when they are being modified.
        try {
            fineLock.readLock().acquire();
            _currentFineBin.addAttemptedRequest(frontendResponseTime);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for fine read lock");
            Thread.currentThread().interrupt();
        } finally {
            fineLock.readLock().release();
        }

        synchronized (hourlyLock) {
            _currentHourlyBin.addAttemptedRequest(frontendResponseTime);
        }

        synchronized (dailyLock) {
            _currentDailyBin.addAttemptedRequest(frontendResponseTime);
        }
    }

    /**
     * Records an authorized request.
     */
    public void addAuthorizedRequest() {
        // Use locking to prevent the current bins from being archived when they are being modified.
        try {
            fineLock.readLock().acquire();
            _currentFineBin.addAuthorizedRequest();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for fine read lock");
            Thread.currentThread().interrupt();
        } finally {
            fineLock.readLock().release();
        }

        synchronized (hourlyLock) {
            _currentHourlyBin.addAuthorizedRequest();
        }

        synchronized (dailyLock) {
            _currentDailyBin.addAuthorizedRequest();
        }
    }

    /**
     * Records a completed request.
     */
    public void addCompletedRequest(final int backendResponseTime) {
        // Use locking to prevent the current bins from being archived when they are being modified.
        try {
            fineLock.readLock().acquire();
            _currentFineBin.addCompletedRequest(backendResponseTime);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for fine read lock");
            Thread.currentThread().interrupt();
        } finally {
            fineLock.readLock().release();
        }

        synchronized (hourlyLock) {
            _currentHourlyBin.addCompletedRequest(backendResponseTime);
        }

        synchronized (dailyLock) {
            _currentDailyBin.addCompletedRequest(backendResponseTime);
        }
    }

    /**
     * Archives the current fine resolution bin and starts a new one.
     *
     * @param limit maximum number of bins to keep
     */
    public void archiveFineBin(final int limit) {
        // Use locking to stop further modification to the current bin before it is archived.
        try {
            fineLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentFineBin.setEndTime(now);
            try {
                _queue.put(_currentFineBin);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            // TODO what happens when the interval changes?
            _currentFineBin = new MetricsBin(now, _fineInterval, MetricsBin.RES_FINE, _clusterNodeId, _serviceOid);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for fine write lock");
            Thread.currentThread().interrupt();
        } finally {
            fineLock.writeLock().release();
        }
    }

    /**
     * Archives the current hourly resolution bin and starts a new one.
     *
     * @param limit maximum number of bins to keep
     */
    public void archiveHourlyBin(final int limit) {
        // Use locking to stop further modification to the current bin before it is archived.
        synchronized (hourlyLock) {
            final long now = System.currentTimeMillis();
            _currentHourlyBin.setEndTime(now);
            try {
                _queue.put(_currentHourlyBin);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            _currentHourlyBin = new MetricsBin(now, 0, MetricsBin.RES_HOURLY, _clusterNodeId, _serviceOid);
        }
    }

    /**
     * Archives the current daily resolution bin and starts a new one.
     *
     * @param limit maximum number of bins to keep
     */
    public void archiveDailyBin(final int limit) {
        // Use locking to stop further modification to the current bin before it is archived.
        synchronized (dailyLock) {
            final long now = System.currentTimeMillis();
            _currentDailyBin.setEndTime(now);
            try {
                _queue.put(_currentDailyBin);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            _currentDailyBin = new MetricsBin(now, 0, MetricsBin.RES_DAILY, _clusterNodeId, _serviceOid);
        }
    }
}
