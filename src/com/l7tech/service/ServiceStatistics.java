package com.l7tech.service;

import java.io.Serializable;

public class ServiceStatistics implements Serializable {

    public ServiceStatistics( long serviceOid  ) {
        _serviceOid = serviceOid;
    }

    public long getServiceOid() {
        return _serviceOid;
    }

    public String getServiceName() {
        return _serviceName;
    }

    public void setServiceName(String name) {
        _serviceName = name;
    }

    public int getAttemptedRequestCount() {
        return _attemptedRequestCount;
    }

    public synchronized void attemptedRequest() {
        _attemptedRequestCount++;
    }

    public int getAuthorizedRequestCount() {
        return _authorizedRequestCount;
    }

    public synchronized void authorizedRequest() {
        _authorizedRequestCount++;
    }

    public int getCompletedRequestCount() {
        return _completedRequestCount;
    }

    public synchronized void completedRequest() {
        _completedRequestCount++;
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

    private long _serviceOid = PublishedService.DEFAULT_OID;
    private String _serviceName = "";
    private int _attemptedRequestCount = 0;
    private int _authorizedRequestCount = 0;
    private int _completedRequestCount = 0;
}
