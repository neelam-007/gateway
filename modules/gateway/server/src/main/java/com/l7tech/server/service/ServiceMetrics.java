package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.BoundedChannel;
import EDU.oswego.cs.dl.util.concurrent.Channel;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.mapping.MessageContextMappingManager;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

/**
 * A ServiceMetrics accumulates request statistics (for one single published
 * service on one gateway node) into {@link MetricsBin}s. An external timer
 * can use ServiceMetrics to generate {@link MetricsBin}s periodically.
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
     * The OID of the {@link com.l7tech.gateway.common.service.PublishedService} for which these MetricsBins were
     * collected.
     */
    private final long _serviceOid;

    private Long _mappingValuesOid = null;

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

    private volatile ServiceState lastServiceState;

    /**
     * Creates a ServiceMetrics and starts new {@link MetricsBin}s right away.
     *
     * @param serviceOid the OID of the {@link com.l7tech.gateway.common.service.PublishedService} for which metrics are being gathered
     * @param nodeId the MAC address of the cluster node for which metrics are being gathered
     * @param fineInterval the interval currently being used for buckets of {@link MetricsBin#RES_FINE} resolution
     * @param queue the ServiceMetricsManager queue to which archived bins should be added
     */
    public ServiceMetrics(long serviceOid, String nodeId, int fineInterval, BoundedChannel queue, Long mappingValuesOid) {
        if (serviceOid < 0) throw new IllegalArgumentException("serviceOid must be positive");
        if (nodeId == null || nodeId.length() == 0) throw new IllegalArgumentException("nodeId must not be null or empty");
        if (fineInterval < 0) throw new IllegalArgumentException("fineInterval must be positive");
        if (queue == null) throw new NullPointerException();

        _queue = queue;
        _serviceOid = serviceOid;
        _mappingValuesOid = mappingValuesOid;
        _clusterNodeId = nodeId;
        _fineInterval = fineInterval;

        final long now = System.currentTimeMillis();
        _currentFineBin = new MetricsBin(now, fineInterval, MetricsBin.RES_FINE, nodeId, serviceOid, mappingValuesOid);
        _currentHourlyBin = new MetricsBin(now, 0, MetricsBin.RES_HOURLY, nodeId, serviceOid, mappingValuesOid);
        _currentDailyBin = new MetricsBin(now, 0, MetricsBin.RES_DAILY, nodeId, serviceOid, mappingValuesOid);
    }

    public long getServiceOid() {
        return _serviceOid;
    }

    public long getMappingValuesOid() {
        return _mappingValuesOid;
    }

    public void setMappingValuesOid(long mappingValuesOid) {
        _mappingValuesOid = mappingValuesOid;
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

    public void addMessageContextMappings(PolicyEnforcementContext context) {
        if (ServiceMetricsManager._clusterPropEnabledToAddMappings && !context.haveMappingsSaved()) {
            saveMessageContextMapping(context);
            _mappingValuesOid = context.getMapping_values_oid();

            _currentFineBin.setMappingValuesOid(_mappingValuesOid);
            _currentHourlyBin.setMappingValuesOid(_mappingValuesOid);
            _currentDailyBin.setMappingValuesOid(_mappingValuesOid);
        }
    }

    /**
     * Archives the current fine resolution bin and starts a new one.
     * For performance reason, the current bin will not be archived if there was
     * no request message.
     *
     * @return true if the current bin was archived
     */
    public boolean archiveFineBin(final ServiceState currentServiceState) {
        // Use locking to stop further modification to the current bin before it is archived.
        boolean archived = false;
        try {
            _fineLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentFineBin.setServiceState(currentServiceState);
            _currentFineBin.setEndTime(now);
            try {
                // Bug 3728: Omit no-traffic fine bins to improve performance.
                // Dashboard will use empty uptime bins to keep moving chart advancing.
                if (currentServiceState != lastServiceState || _currentFineBin.getNumAttemptedRequest() > 0) {
                    _queue.put(_currentFineBin);
                    archived = true;
                }
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }

            _currentFineBin = new MetricsBin(now, _fineInterval, MetricsBin.RES_FINE, _clusterNodeId, _serviceOid, _mappingValuesOid);
        } catch (InterruptedException e) {
            _logger.warning("Interrupted waiting for fine bin write lock");
            Thread.currentThread().interrupt();
        } finally {
            _fineLock.writeLock().release();
            lastServiceState = currentServiceState;
        }
        return archived;
    }

    /**
     * Archives the current hourly resolution bin and starts a new one.
     * @param state
     */
    public void archiveHourlyBin(ServiceState state) {
        // Use locking to stop further modification to the current bin before it is archived.
        try {
            _hourlyLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentHourlyBin.setEndTime(now);
            _currentHourlyBin.setServiceState(state);
            try {
                _queue.put(_currentHourlyBin);
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            _currentHourlyBin = new MetricsBin(now, 0, MetricsBin.RES_HOURLY, _clusterNodeId, _serviceOid, _mappingValuesOid);
        } catch (InterruptedException e) {
            _logger.warning("Interrupted waiting for hourly bin write lock");
            Thread.currentThread().interrupt();
        } finally {
            _hourlyLock.writeLock().release();
        }
    }

    /**
     * Archives the current daily resolution bin and starts a new one.
     * @param state
     */
    public void archiveDailyBin(ServiceState state) {
        // Use locking to stop further modification to the current bin before it is archived.
        try {
            _dailyLock.writeLock().acquire();
            final long now = System.currentTimeMillis();
            _currentDailyBin.setEndTime(now);
            _currentDailyBin.setServiceState(state);
            try {
                _queue.put(_currentDailyBin);
            } catch (InterruptedException e) {
                _logger.log(Level.WARNING, "Interrupted waiting for queue", e);
                Thread.currentThread().interrupt();
            }
            _currentDailyBin = new MetricsBin(now, 0, MetricsBin.RES_DAILY, _clusterNodeId, _serviceOid, _mappingValuesOid);
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

    private void saveMessageContextMapping(PolicyEnforcementContext context) {
        MessageContextMappingKeys keysEntity = new MessageContextMappingKeys();
        keysEntity.setCreateTime(System.currentTimeMillis());
        MessageContextMappingValues valuesEntity = new MessageContextMappingValues();
        valuesEntity.setCreateTime(System.currentTimeMillis());

        List<MessageContextMapping> mappings = context.getMappings();
        for (int i = 0; i < mappings.size(); i++) {
            MessageContextMapping mapping = mappings.get(i);
            keysEntity.setTypeAndKey(i, mapping.getMappingType(), mapping.getKey());
            valuesEntity.setValue(i, mapping.getValue());
        }

        MessageContextMappingManager messageContextMappingManager = context.getMessageContextMappingManager();
        try {
            long mapping_keys_oid = messageContextMappingManager.saveMessageContextMappingKeys(keysEntity);
            valuesEntity.setMappingKeysOid(mapping_keys_oid);

            long mapping_values_oid = messageContextMappingManager.saveMessageContextMappingValues(valuesEntity);
            context.setMapping_values_oid(mapping_values_oid);
        } catch (Exception e) {
            _logger.warning("Faied to save the keys or values of the message context mapping.");
        }
    }
}
