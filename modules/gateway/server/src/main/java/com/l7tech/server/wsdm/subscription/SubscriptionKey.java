package com.l7tech.server.wsdm.subscription;

import com.l7tech.objectmodel.Goid;

/**
 * SubscriptionKey is an identifier for a subscription.
 */
public final class SubscriptionKey {

    //- PUBLIC

    /**
     * Create a SubscriptionKey for the given values.
     *
     * @param serviceGoid The service oid (required)
     * @param topic The type of the subscription (required)
     * @param notificationUrl The notification URL (required)
     */
    public SubscriptionKey( final Goid serviceGoid, final int topic, final String notificationUrl ) {
        if ( notificationUrl == null ) throw new IllegalArgumentException("Null notification URL");

        this.serviceGoid = serviceGoid;
        this.topic = topic;
        this.notificationUrl = notificationUrl;
    }

    /**
     * Create a SubscriptionKey for the given Subscription.
     *
     * @param subscription The Subscription (required)
     */
    public SubscriptionKey( final Subscription subscription ) {
        this( subscription.getPublishedServiceGoid(), subscription.getTopic(), subscription.getReferenceCallback() );
    }

    public Goid getServiceGoid() {
        return serviceGoid;
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

        if (!Goid.equals(serviceGoid, that.serviceGoid)) return false;
        if (topic != that.topic) return false;
        if (!notificationUrl.equals(that.notificationUrl)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = serviceGoid != null ? serviceGoid.hashCode() : 0;
        result = 31 * result + topic;
        result = 31 * result + notificationUrl.hashCode();
        return result;
    }

    //- PRIVATE

    private final Goid serviceGoid;
    private final int topic;
    private final String notificationUrl;    
}
