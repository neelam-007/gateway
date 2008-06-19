package com.l7tech.server.wsdm.subscription;

/**
 * SubscriptionKey is an identifier for a subscription.
 */
public final class SubscriptionKey {

    //- PUBLIC

    /**
     * Create a SubscriptionKey for the given values.
     *
     * @param serviceOid The service oid (required)
     * @param topic The type of the subscription (required)
     * @param notificationUrl The notification URL (required)
     */
    public SubscriptionKey( final long serviceOid, final int topic, final String notificationUrl ) {
        if ( notificationUrl == null ) throw new IllegalArgumentException("Null notification URL");

        this.serviceOid = serviceOid;
        this.topic = topic;
        this.notificationUrl = notificationUrl;
    }

    /**
     * Create a SubscriptionKey for the given Subscription.
     *
     * @param subscription The Subscription (required)
     */
    public SubscriptionKey( final Subscription subscription ) {
        this( subscription.getPublishedServiceOid(), subscription.getTopic(), subscription.getReferenceCallback() );
    }

    public long getServiceOid() {
        return serviceOid;
    }

    public int getTopic() {
        return topic;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionKey that = (SubscriptionKey) o;

        if (serviceOid != that.serviceOid) return false;
        if (topic != that.topic) return false;
        if (!notificationUrl.equals(that.notificationUrl)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (serviceOid ^ (serviceOid >>> 32));
        result = 31 * result + topic;
        result = 31 * result + notificationUrl.hashCode();
        return result;
    }

    //- PRIVATE

    private final long serviceOid;
    private final int topic;
    private final String notificationUrl;    
}
