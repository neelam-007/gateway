package com.l7tech.external.assertions.mqnative;

import static com.l7tech.util.CollectionUtils.list;

import java.io.Serializable;
import java.util.List;

/**
 * Dynamic properties for MQ routing.
 */
public class MqNativeDynamicProperties implements Serializable, Cloneable {

    //- PUBLIC

    public MqNativeDynamicProperties() {
    }

    public MqNativeDynamicProperties( final MqNativeDynamicProperties other ) {
        this.queueName = other.queueName;
        this.replyToQueue = other.replyToQueue;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName( final String queueName ) {
        this.queueName = queueName;
    }

    public String getReplyToQueue() {
        return replyToQueue;
    }

    public void setReplyToQueue( final String replyToQueue ) {
        this.replyToQueue = replyToQueue;
    }

    //- PACKAGE

    List<String> getVariableExpressions() {
        return list( queueName, replyToQueue );
    }

    //- PRIVATE

    private String queueName;
    private String replyToQueue;

}