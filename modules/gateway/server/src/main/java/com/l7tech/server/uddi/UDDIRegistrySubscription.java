package com.l7tech.server.uddi;

import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Version;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

/**
 * Subscription information for a UDDI registry
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_registry_subscription")
public class UDDIRegistrySubscription extends PersistentEntityImp {

   //- PUBLIC

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

    @Column(name = "uddi_subscription_expiry_time")
    public long getSubscriptionExpiryTime() {
        return subscriptionExpiryTime;
    }

    public void setSubscriptionExpiryTime( final long subscriptionExpiryTime ) {
        this.subscriptionExpiryTime = subscriptionExpiryTime;
    }

    @Column(name = "uddi_subscription_notified_time")
    public long getSubscriptionNotifiedTime() {
        return subscriptionNotifiedTime;
    }

    public void setSubscriptionNotifiedTime( final long subscriptionNotifiedTime ) {
        this.subscriptionNotifiedTime = subscriptionNotifiedTime;
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

    private long uddiRegistryOid;
    private String subscriptionKey;
    private long subscriptionExpiryTime;
    private long subscriptionNotifiedTime;
    private long subscriptionCheckTime;
}
