package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;

import java.util.logging.Logger;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A ServiceMetrics accumulates request statistics (for one single published
 * service on one gateway node). An external timer can use ServiceMetrics to
 * record metrics periodically.
 *
 * @author rmak
 */
class ServiceMetrics {
    private static final Logger _logger = Logger.getLogger(ServiceMetrics.class.getName());

    /**
     * Protects {@link #_currentCollector}. This is reader-preference because the MessageProcessor "reads"
     * the currentFineBin, whereas the archive task "writes."
     * <p/>
     * Note that we can't use {@link # _currentBin}'s monitor to protect the field,
     * since the monitor belongs to the object, not the field, which gets reset constantly.
     */
    private final ReadWriteLock _metricsLock = new ReentrantReadWriteLock();
    private final ReadWriteLock _metricsDetailLock = new ReentrantReadWriteLock();  // used in conjunction with _metricsLock

    /**
     * The OID of the {@link com.l7tech.gateway.common.service.PublishedService} for which these MetricsBins were
     * collected.
     */
    private final Goid _serviceGoid;

    /**
     * The collector for summary statistics.
     */
    private MetricsCollector _currentCollector;

    /**
     * The fine resolution bin that is currently collecting statistics.
     */
    private Map<MetricsDetailKey, MetricsCollector> _currentDetailCollector;

    private volatile ServiceState lastServiceState;

    /**
     * Creates a ServiceMetrics and starts collection right away.
     *
     * @param serviceGoid the GOID of the {@link com.l7tech.gateway.common.service.PublishedService} for which metrics are being gathered
     */
    public ServiceMetrics(Goid serviceGoid) {
        if (Goid.isDefault(serviceGoid)) throw new IllegalArgumentException("serviceGoid must not be the default");

        _serviceGoid = serviceGoid;
        _currentCollector = new MetricsCollector(System.currentTimeMillis());
        _currentDetailCollector = new HashMap<MetricsDetailKey,MetricsCollector>();
    }

    public Goid getServiceGoid() {
        return _serviceGoid;
    }

    /**
     * Record a request.
     *
     * @param authorized True if the policy execution was successful (routing attempted).
     * @param completed  True if the routing was successful
     * @param frontTime  Complete time for request processing
     * @param backTime   Time taken by the protected service
     */
    public void addRequest(String operation, User user, List<MessageContextMapping> mappings, boolean authorized, boolean completed, int frontTime, int backTime) {
        _metricsLock.readLock().lock();
        try {
            // get details for key
            MetricsDetailKey detailsKey = new MetricsDetailKey( operation, user, mappings );
            MetricsCollector detailCollector = null;
            _metricsDetailLock.readLock().lock();
            try {
                detailCollector = _currentDetailCollector.get(detailsKey);
            } finally {
                _metricsDetailLock.readLock().unlock();
            }

            // no existing details for key, so create and share
            if ( detailCollector == null ) {
                _metricsDetailLock.writeLock().lock();
                try {
                    detailCollector = _currentDetailCollector.get(detailsKey);
                    if (detailCollector==null) {
                        detailCollector = new MetricsCollector(_currentCollector.startTime);
                        _currentDetailCollector.put(detailsKey, detailCollector);
                    }
                } finally {
                    _metricsDetailLock.writeLock().unlock();
                }
            }

            // record metrics for detail and summary
            _currentCollector.addAttemptedRequest(frontTime);
            detailCollector.addAttemptedRequest(frontTime);
            if (authorized) {
                _currentCollector.addAuthorizedRequest();
                detailCollector.addAuthorizedRequest();
                if (completed) {
                    _currentCollector.addCompletedRequest(backTime);
                    detailCollector.addCompletedRequest(backTime);
                }
            }
        } finally {
            _metricsLock.readLock().unlock();
        }
    }

    /**
     * Archives the current fine resolution bin and starts a new one.
     * For performance reason, the current bin will not be archived if there was
     * no request message.
     *
     * @return true if the current bin was archived
     */
    MetricsCollectorSet getMetricsCollectorSet(final ServiceState currentServiceState) {
        MetricsCollectorSet set = null;

        // Use locking to stop further modification to the current bin before it is archived.
        _metricsLock.writeLock().lock();
        try {
            // Bug 3728: Omit no-traffic fine bins to improve performance.
            // Dashboard will use empty uptime bins to keep moving chart advancing.
            if (currentServiceState != lastServiceState || _currentCollector.getNumAttemptedRequest() > 0) {
                set = new MetricsCollectorSet(_serviceGoid, currentServiceState, _currentCollector, _currentDetailCollector);
            }

            _currentCollector = new MetricsCollector(System.currentTimeMillis());
            _currentDetailCollector = new HashMap<MetricsDetailKey,MetricsCollector>();
        } finally {
            _metricsLock.writeLock().unlock();
            lastServiceState = currentServiceState;
        }
        
        return set;
    }

    static MetricsCollectorSet getEmptyMetricsSet( final long startTime, final long endTime ) {
        MetricsCollector collector = new MetricsCollector( -1L );
        return new MetricsCollectorSet( PersistentEntity.DEFAULT_GOID, startTime, endTime, collector, Collections.<MetricsDetailKey,MetricsCollector>emptyMap() );
    }

    static class MetricsDetailKey {
        private final String operation;
        private final Goid userProviderId;
        private final String userId;
        private final Set<MessageContextMapping> mappings;

        MetricsDetailKey(String operation, User user, List<MessageContextMapping> mappings) {
            this.operation = operation;
            this.userProviderId = user==null ? null : user.getProviderId();
            this.userId = user == null ? null : user.getId();
            this.mappings = mappings == null ? null : new TreeSet<MessageContextMapping>(mappings);
        }

        public String getOperation() {
            return operation;
        }

        public Goid getUserProviderId() {
            return userProviderId;
        }

        public String getUserId() {
            return userId;
        }

        public Set<MessageContextMapping> getMappings() {
            return mappings;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MetricsDetailKey that = (MetricsDetailKey) o;

            if (hasUserMapping() && (userProviderId != null ? !userProviderId.equals(that.userProviderId) : that.userProviderId != null)) return false;
            if (mappings != null ? !mappings.equals(that.mappings) : that.mappings != null) return false;
            if (operation != null ? !operation.equals(that.operation) : that.operation != null) return false;
            if (hasUserMapping() && (userId != null ? !userId.equals(that.userId) : that.userId != null)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (operation != null ? operation.hashCode() : 0);
            result = 31 * result + (hasUserMapping() && userProviderId != null ? userProviderId.hashCode() : 0);
            result = 31 * result + (hasUserMapping() && userId != null ? userId.hashCode() : 0);
            result = 31 * result + (mappings != null ? mappings.hashCode() : 0);
            return result;
        }

        /**
         * NOTE: Since saveMessageContextMapping only uses the MetricsDetailKeys user if there is a user mapping
         * so do the equals/hashcode of this class.
         * @see ServiceMetricsManagerImpl#saveMessageContextMapping
         */
        private boolean hasUserMapping() {
            boolean hasUserMapping = false;

            if ( mappings != null ) {
                for ( MessageContextMapping mapping : mappings ) {
                    if ( mapping.getMappingType() == MessageContextMapping.MappingType.AUTH_USER ) {
                        hasUserMapping = true;
                    }
                }
            }

            return hasUserMapping;
        }
    }

    /**
     * Set of collection and summary metrics with identifying information
     */
    public static class MetricsCollectorSet {
        private final Goid serviceGoid;
        private final long startTime;
        private final long endTime;
        private final ServiceState serviceState;
        private final MetricsCollector summaryMetrics;
        private final Map<MetricsDetailKey, MetricsCollector> detailMetrics;

        MetricsCollectorSet( final Goid serviceGoid,
                             final ServiceState serviceState,
                             final MetricsCollector summaryMetrics,
                             final Map<MetricsDetailKey, MetricsCollector> detailMetrics) {
            this.serviceGoid = serviceGoid;
            this.startTime = summaryMetrics.startTime;
            this.endTime = System.currentTimeMillis();
            this.serviceState = serviceState;
            this.summaryMetrics = summaryMetrics;
            this.detailMetrics = detailMetrics;
        }

        MetricsCollectorSet( final Goid serviceGoid,
                             final long startTime,
                             final long endTime,
                             final MetricsCollector summaryMetrics,
                             final Map<MetricsDetailKey, MetricsCollector> detailMetrics) {
            this.serviceGoid = serviceGoid;
            this.startTime = startTime;
            this.endTime = endTime;
            this.serviceState = null;
            this.summaryMetrics = summaryMetrics;
            this.detailMetrics = detailMetrics;
        }

        public Goid getServiceGoid() {
            return serviceGoid;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public ServiceState getServiceState() {
            return serviceState;
        }

        public MetricsCollector getSummaryMetrics() {
            return summaryMetrics;
        }

        public Map<MetricsDetailKey, MetricsCollector> getDetailMetrics() {
            return detailMetrics;
        }
    }

    static class MetricsCollector {
        private final long startTime;
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
        private final Object _attemptedLock = new Object();

        /** Protects write access to {@link #_numAuthorizedRequest}. */
        private final Object _authorizedLock = new Object();

        /** Protects write access to {@link #_numCompletedRequest}, {@link #_minBackendResponseTime},
         * {@link #_maxBackendResponseTime} and {@link #_sumBackendResponseTime}. */
        private final Object _completedLock = new Object();

        MetricsCollector( long startTime ) {
            this.startTime = startTime;
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

        /** @return number of successful requests in this bin */
        public int getNumSuccess() {
            synchronized(_completedLock) {
                return _numCompletedRequest;
            }
        }

        /** @return number of requests with policy violations in this bin */
        public int getNumPolicyViolation() {
            synchronized(_attemptedLock) {
                synchronized(_authorizedLock) {
                    return _numAttemptedRequest - _numAuthorizedRequest;
                }
            }
        }

        /** @return number of requests with routing failures in this bin */
        public int getNumRoutingFailure() {
            synchronized(_authorizedLock) {
                synchronized(_completedLock) {
                    return _numAuthorizedRequest - _numCompletedRequest;
                }
            }
        }

        /** @return number of all requests in this bin */
        public int getNumTotal() {
            synchronized(_attemptedLock) {
                return _numAttemptedRequest;
            }
        }

        /** @return the minimum frontend response time (in milliseconds) of all attempted requests;
         *          this is meaningful only if {@link #getNumAttemptedRequest()} returns non-zero */
        public int getMinFrontendResponseTime() {
            synchronized(_attemptedLock) {
                return _minFrontendResponseTime;
            }
        }

        /** @return the maximum frontend response time (in milliseconds) of all attempted requests;
         *          this is meaningful only if {@link #getNumAttemptedRequest()} returns non-zero */
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

        /** @return the minimum backend response time (in milliseconds) of all completed requests;
         *          this is meaningful only if {@link #getNumCompletedRequest()} returns non-zero */
        public int getMinBackendResponseTime() {
            synchronized(_completedLock){
                return _minBackendResponseTime;
            }
        }

        /** @return the maximum backend response time (in milliseconds) of all completed requests;
         *          this is meaningful only if {@link #getNumCompletedRequest()} returns non-zero */
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

        /**
         * Records an attempted request.
         * @param frontendResponseTime  front end response time for the request being added
         */
        public void addAttemptedRequest(int frontendResponseTime) {
            if (frontendResponseTime < 0) {
                // Don't really know what causes negative response time sometimes,
                // or if this still happens. Just suppress it and log warning.
                // (Bugzilla # 2328)
                _logger.warning("Negative frontend response time (" + frontendResponseTime +" ms) suppressed (forced to zero).");
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

        /**
         * Records a completed request.
         * @param backendResponseTime   back end response time for the request being added
         */
        public void addCompletedRequest(int backendResponseTime) {
            if (backendResponseTime < 0) {
                // Don't really know what causes negative response time sometimes,
                // or if this still happens. Just suppress it and log warning.
                // (Bugzilla # 2328)
                _logger.warning("Negative backend response time (" + backendResponseTime + " ms) suppressed (forced to zero).");
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
    }
}
