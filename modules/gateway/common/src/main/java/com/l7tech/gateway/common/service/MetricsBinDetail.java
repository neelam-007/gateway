package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Goid;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

/**
 * A statistical bin for collected service metrics.
 *
 * This bin is used to record accumulated data that is broken up into
 * "reportable" units (for per-customer reporting).
 *
 * Each bin contains details that break down the higher level information
 * contained in the parent metrics bin.
 */
@Entity
@Proxy(lazy=false)
@Table(name="service_metrics_details")
@IdClass(MetricsBinDetail.MetricsBinDetailPK.class)
public class MetricsBinDetail implements Serializable {

    //- PUBLIC

    @Id
    @Column(name="service_metrics_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getMetricsBinGoid() {
        return metricsBinGoid;
    }

    public void setMetricsBinGoid(Goid metricsBinGoid) {
        this.metricsBinGoid = metricsBinGoid;
    }

    @Id
    @Column(name="mapping_values_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getMappingValuesId() {
        return mappingValuesId;
    }

    public void setMappingValuesId(Goid mappingValuesId) {
        this.mappingValuesId = mappingValuesId;
    }

    @Column(name="attempted")
    public int getNumAttemptedRequest() {
        return numAttemptedRequest;
    }

    public void setNumAttemptedRequest(int numAttemptedRequest) {
        this.numAttemptedRequest = numAttemptedRequest;
    }

    @Column(name="authorized")
    public int getNumAuthorizedRequest() {
        return numAuthorizedRequest;
    }

    public void setNumAuthorizedRequest(int numAuthorizedRequest) {
        this.numAuthorizedRequest = numAuthorizedRequest;
    }

    @Column(name="completed")
    public int getNumCompletedRequest() {
        return numCompletedRequest;
    }

    public void setNumCompletedRequest(int numCompletedRequest) {
        this.numCompletedRequest = numCompletedRequest;
    }

    @Column(name="front_min")
    public Integer getMinFrontendResponseTime() {
        return minFrontendResponseTime;
    }

    public void setMinFrontendResponseTime(Integer minFrontendResponseTime) {
        this.minFrontendResponseTime = minFrontendResponseTime;
    }

    @Column(name="front_max")
    public Integer getMaxFrontendResponseTime() {
        return maxFrontendResponseTime;
    }

    public void setMaxFrontendResponseTime(Integer maxFrontendResponseTime) {
        this.maxFrontendResponseTime = maxFrontendResponseTime;
    }

    @Column(name="front_sum")
    public long getSumFrontendResponseTime() {
        return sumFrontendResponseTime;
    }

    public void setSumFrontendResponseTime(long sumFrontendResponseTime) {
        this.sumFrontendResponseTime = sumFrontendResponseTime;
    }

    @Column(name="back_min")
    public Integer getMinBackendResponseTime() {
        return minBackendResponseTime;
    }

    public void setMinBackendResponseTime(Integer minBackendResponseTime) {
        this.minBackendResponseTime = minBackendResponseTime;
    }

    @Column(name="back_max")
    public Integer getMaxBackendResponseTime() {
        return maxBackendResponseTime;
    }

    public void setMaxBackendResponseTime(Integer maxBackendResponseTime) {
        this.maxBackendResponseTime = maxBackendResponseTime;
    }

    @Column(name="back_sum")
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

    public static class MetricsBinDetailPK implements Serializable {
        private Goid metricsBinGoid;
        private Goid mappingValuesId;

        public MetricsBinDetailPK() {}

        public MetricsBinDetailPK(Goid metricsBinGoid, Goid mappingValuesId) {
            this.metricsBinGoid = metricsBinGoid;
            this.mappingValuesId = mappingValuesId;
        }

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MetricsBinDetailPK that = (MetricsBinDetailPK) o;

            if (mappingValuesId != null ? !mappingValuesId.equals(that.mappingValuesId) : that.mappingValuesId != null)
                return false;
            if (metricsBinGoid != null ? !metricsBinGoid.equals(that.metricsBinGoid) : that.metricsBinGoid != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = metricsBinGoid != null ? metricsBinGoid.hashCode() : 0;
            result = 31 * result + (mappingValuesId != null ? mappingValuesId.hashCode() : 0);
            return result;
        }
    }
}