package com.l7tech.service;

import java.io.Serializable;

/**
 * Immutable.
 */
public class ServiceStatistics implements Serializable {
    public ServiceStatistics( long serviceOid, int attemptedRequestCount, int authorizedRequestCount, int completedRequestCount ) {
        _serviceOid = serviceOid;
        _attemptedRequestCount = attemptedRequestCount;
        _authorizedRequestCount = authorizedRequestCount;
        _completedRequestCount = completedRequestCount;
    }

    public long getServiceOid() {
        return _serviceOid;
    }

    public int getAttemptedRequestCount() {
        return _attemptedRequestCount;
    }

    public int getAuthorizedRequestCount() {
        return _authorizedRequestCount;
    }

    public int getCompletedRequestCount() {
        return _completedRequestCount;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceStatistics)) return false;

        final ServiceStatistics serviceStatistics = (ServiceStatistics) o;

        if (_attemptedRequestCount != serviceStatistics._attemptedRequestCount) return false;
        if (_authorizedRequestCount != serviceStatistics._authorizedRequestCount) return false;
        if (_completedRequestCount != serviceStatistics._completedRequestCount) return false;
        if (_serviceOid != serviceStatistics._serviceOid) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (_serviceOid ^ (_serviceOid >>> 32));
        result = 29 * result + _attemptedRequestCount;
        result = 29 * result + _authorizedRequestCount;
        result = 29 * result + _completedRequestCount;
        return result;
    }

    private long _serviceOid;
    private int _attemptedRequestCount;
    private int _authorizedRequestCount;
    private int _completedRequestCount;
}
