package com.l7tech.server.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.persistence.Column;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Runtime information for a Services UDDI related data.
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_runtime")
public class UDDIRuntimeServiceInfo extends PersistentEntityImp {

    //- PUBLIC

    @Column(name = "published_service_oid")
    public long getPublishedServiceOid() {
        return publishedServiceOid;
    }

    public void setPublishedServiceOid( final long publishedServiceOid ) {
        this.publishedServiceOid = publishedServiceOid;
    }

    @Column(name = "uddi_registry_oid")
    public long getUddiRegistryOid() {
        return uddiRegistryOid;
    }

    public void setUddiRegistryOid( final long uddiRegistryOid ) {
        this.uddiRegistryOid = uddiRegistryOid;
    }

    @Column(name = "uddi_subscription_key")
    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public void setSubscriptionKey( final String subscriptionKey ) {
        this.subscriptionKey = subscriptionKey;
    }

    @Column(name = "uddi_subscription_check_time")
    public long getSubscriptionCheckTime() {
        return subscriptionCheckTime;
    }

    public void setSubscriptionCheckTime( final long subscriptionCheckTime ) {
        this.subscriptionCheckTime = subscriptionCheckTime;
    }

    @Override
    @Version
    @Column(name = "version")
    public int getVersion() {
        return super.getVersion();
    }

    //- PRIVATE

    private long publishedServiceOid;
    private long uddiRegistryOid;
    private String subscriptionKey;
    private long subscriptionCheckTime;
}
