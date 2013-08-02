package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Goid;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceStatistics implements Serializable {

    public ServiceStatistics( Goid serviceGoid  ) {
        _serviceGoid = serviceGoid;
    }

    public ServiceStatistics(Goid serviceGoid,
                             int attemptedRequestCount,
                             int authorizedRequestCount,
                             int completedRequestCount) {
        _serviceGoid = serviceGoid;
        _attemptedRequestCount.set(attemptedRequestCount);
        _authorizedRequestCount.set(authorizedRequestCount);
        _completedRequestCount.set(completedRequestCount);
    }

    public Goid getServiceGoid() {
        return _serviceGoid;
    }

    public String getServiceName() {
        return _serviceName;
    }

    public void setServiceName(String name) {
        _serviceName = name;
    }

    public int getAttemptedRequestCount() {
        return _attemptedRequestCount.get();
    }

    public void attemptedRequest() {
        _attemptedRequestCount.incrementAndGet();
    }

    public int getAuthorizedRequestCount() {
        return _authorizedRequestCount.get();
    }

    public void authorizedRequest() {
        _authorizedRequestCount.incrementAndGet();
    }

    public int getCompletedRequestCount() {
        return _completedRequestCount.get();
    }

    public void completedRequest() {
        _completedRequestCount.incrementAndGet();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceStatistics)) return false;

        final ServiceStatistics serviceStatistics = (ServiceStatistics) o;

        if (_attemptedRequestCount.get() != serviceStatistics._attemptedRequestCount.get()) return false;
        if (_authorizedRequestCount.get() != serviceStatistics._authorizedRequestCount.get()) return false;
        if (_completedRequestCount.get() != serviceStatistics._completedRequestCount.get()) return false;
        if (!Goid.equals(_serviceGoid, serviceStatistics._serviceGoid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = _serviceGoid != null ? _serviceGoid.hashCode() : 0;
        result = 29 * result + _attemptedRequestCount.get();
        result = 29 * result + _authorizedRequestCount.get();
        result = 29 * result + _completedRequestCount.get();
        return result;
    }

    private Goid _serviceGoid = PublishedService.DEFAULT_GOID;
    private String _serviceName = "";
    private final AtomicInteger _attemptedRequestCount = new AtomicInteger(0);
    private final AtomicInteger _authorizedRequestCount = new AtomicInteger(0);
    private final AtomicInteger _completedRequestCount = new AtomicInteger(0);
}
