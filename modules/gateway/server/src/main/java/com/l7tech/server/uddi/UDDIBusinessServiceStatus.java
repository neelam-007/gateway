package com.l7tech.server.uddi;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;

import org.hibernate.annotations.Proxy;

/**
 * Tracks the current status for a UDDI business service.
 *
 * <p>Currently used for metrics and ws-policy status.</p>
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_business_service_status")
public class UDDIBusinessServiceStatus extends PersistentEntityImp {

    //- PUBLIC
    
    @Column(name = "published_service_oid", updatable=false)
    public long getPublishedServiceOid() {
        return publishedServiceOid;
    }

    public void setPublishedServiceOid( final long publishedServiceOid ) {
        this.publishedServiceOid = publishedServiceOid;
    }

    @Column(name = "uddi_registry_oid", updatable=false)
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    public void setUddiRegistryOid( final long uddiRegistryOid ) {
        this.uddiRegistryOid = uddiRegistryOid;
    }

    @Column(name = "uddi_service_key")
    public String getUddiServiceKey() {
        return uddiServiceKey;
    }

    public void setUddiServiceKey( final String uddiServiceKey ) {
        this.uddiServiceKey = uddiServiceKey;
    }

    @Column(name = "uddi_service_name")
    public String getUddiServiceName() {
        return uddiServiceName;
    }

    public void setUddiServiceName( final String uddiServiceName ) {
        this.uddiServiceName = uddiServiceName;
    }

    @Column(name = "uddi_policy_tmodel_key")
    public String getUddiPolicyTModelKey() {
        return uddiPolicyTModelKey;
    }

    public void setUddiPolicyTModelKey( final String uddiPolicyTModelKey ) {
        this.uddiPolicyTModelKey = uddiPolicyTModelKey;
    }

    @Column(name = "policy_status")
    @Enumerated(EnumType.STRING)
    public Status getUddiPolicyStatus() {
        return uddiPolicyStatus;
    }

    public void setUddiPolicyStatus( final Status uddiPolicyStatus ) {
        this.uddiPolicyStatus = uddiPolicyStatus;
    }

    @Column(name = "uddi_metrics_tmodel_key")
    public String getUddiMetricsTModelKey() {
        return uddiMetricsTModelKey;
    }

    public void setUddiMetricsTModelKey( final String uddiMetricsTModelKey ) {
        this.uddiMetricsTModelKey = uddiMetricsTModelKey;
    }

    @Column(name = "metrics_reference_status")
    @Enumerated(EnumType.STRING)
    public Status getUddiMetricsReferenceStatus() {
        return uddiMetricsReferenceStatus;
    }

    public void setUddiMetricsReferenceStatus( final Status uddiMetricsReferenceStatus ) {
        this.uddiMetricsReferenceStatus = uddiMetricsReferenceStatus;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    public enum Status {
        /**
         * Not published.
         */
        NONE,

        /**
         * Publishing (or update) is required.
         */
        PUBLISH,

        /**
         * Published
         */
        PUBLISHED,

        /**
         * Deletion is required
         */
        DELETE
    }

    //- PRIVATE

    private long publishedServiceOid;
    private long uddiRegistryOid;
    private String uddiServiceKey;
    private String uddiServiceName;
    private String uddiPolicyTModelKey;
    private Status uddiPolicyStatus;
    private String uddiMetricsTModelKey;
    private Status uddiMetricsReferenceStatus;

}
