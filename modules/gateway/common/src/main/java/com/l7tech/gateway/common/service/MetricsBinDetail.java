package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * A statistical bin for collected service metrics.
 *
 * This bin is used to record accumulated data that is broken up into
 * "reportable" units (for per-customer reporting).
 *
 * Each bin contains details that break down the higher level information
 * contained in the parent metrics bin.
 */
public class MetricsBinDetail extends PersistentEntityImp {

    //- PUBLIC

    public Goid getMetricsBinGoid() {
        return metricsBinGoid;
    }

    public void setMetricsBinGoid(Goid metricsBinGoid) {
        this.metricsBinGoid = metricsBinGoid;
    }

    public Goid getMappingValuesId() {
        return mappingValuesId;
    }

    public void setMappingValuesId(Goid mappingValuesId) {
        this.mappingValuesId = mappingValuesId;
    }

    public int getNumAttemptedRequest() {
        return numAttemptedRequest;
    }

    public void setNumAttemptedRequest(int numAttemptedRequest) {
        this.numAttemptedRequest = numAttemptedRequest;
    }

    public int getNumAuthorizedRequest() {
        return numAuthorizedRequest;
    }

    public void setNumAuthorizedRequest(int numAuthorizedRequest) {
        this.numAuthorizedRequest = numAuthorizedRequest;
    }

    public int getNumCompletedRequest() {
        return numCompletedRequest;
    }

    public void setNumCompletedRequest(int numCompletedRequest) {
        this.numCompletedRequest = numCompletedRequest;
    }

    public Integer getMinFrontendResponseTime() {
        return minFrontendResponseTime;
    }

    public void setMinFrontendResponseTime(Integer minFrontendResponseTime) {
        this.minFrontendResponseTime = minFrontendResponseTime;
    }

    public Integer getMaxFrontendResponseTime() {
        return maxFrontendResponseTime;
    }

    public void setMaxFrontendResponseTime(Integer maxFrontendResponseTime) {
        this.maxFrontendResponseTime = maxFrontendResponseTime;
    }

    public long getSumFrontendResponseTime() {
        return sumFrontendResponseTime;
    }

    public void setSumFrontendResponseTime(long sumFrontendResponseTime) {
        this.sumFrontendResponseTime = sumFrontendResponseTime;
    }

    public Integer getMinBackendResponseTime() {
        return minBackendResponseTime;
    }

    public void setMinBackendResponseTime(Integer minBackendResponseTime) {
        this.minBackendResponseTime = minBackendResponseTime;
    }

    public Integer getMaxBackendResponseTime() {
        return maxBackendResponseTime;
    }

    public void setMaxBackendResponseTime(Integer maxBackendResponseTime) {
        this.maxBackendResponseTime = maxBackendResponseTime;
    }

    public long getSumBackendResponseTime() {
        return sumBackendResponseTime;
    }

    public void setSumBackendResponseTime(long sumBackendResponseTime) {
        this.sumBackendResponseTime = sumBackendResponseTime;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MetricsBinDetail that = (MetricsBinDetail) o;

        if (!Goid.equals(mappingValuesId, that.mappingValuesId)) return false;
        if (metricsBinGoid != that.metricsBinGoid) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + metricsBinGoid.hashCode();
        result = 31 * result + (mappingValuesId != null ? mappingValuesId.hashCode() : 0);
        return result;
    }

    //- PRIVATE

    private Goid metricsBinGoid;
    private Goid mappingValuesId;

    private int numAttemptedRequest;
    private int numAuthorizedRequest;
    private int numCompletedRequest;

    private Integer minFrontendResponseTime;
    private Integer maxFrontendResponseTime;
    private long sumFrontendResponseTime;

    private Integer minBackendResponseTime;
    private Integer maxBackendResponseTime;
    private long sumBackendResponseTime;
    
}