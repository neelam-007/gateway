package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.message.*;

/**
 * An implementation of MessageKnob for MQ
 */
public interface MqNativeKnob extends HasSoapAction, HasServiceId, HasHeaders {

    /**
     * Retrieve the Message
     * @return The Message
     */
    Object getMessage();

    void reset(Object message);

}