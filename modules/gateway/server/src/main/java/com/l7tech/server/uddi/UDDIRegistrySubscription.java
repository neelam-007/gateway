package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Subscription information for a UDDI registry
 */
@Entity
@Proxy(lazy=false)
@Table(name="uddi_registry_subscription")
public class UDDIRegistrySubscription extends PersistentEntityImp {

   //- PUBLIC

    @Column(name = "uddi_registry_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getUddiRegistryGoid() {
        return uddiRegistryGoid;
    }

    public void setUddiRegistryGoid( final Goid uddiRegistryGoid ) {
        this.uddiRegistryGoid = uddiRegistryGoid;
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

    private Goid uddiRegistryGoid;
    private String subscriptionKey;
    private long subscriptionExpiryTime;
    private long subscriptionNotifiedTime;
    private long subscriptionCheckTime;
}
