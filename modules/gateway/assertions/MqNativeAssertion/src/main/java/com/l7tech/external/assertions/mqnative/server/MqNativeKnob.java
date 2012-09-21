package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasSoapAction;
import com.l7tech.message.MessageKnob;

/**
 * An implementation of MessageKnob for MQ
 */
public interface MqNativeKnob extends MessageKnob, HasSoapAction, HasServiceOid {

    byte[] getMessageHeaderBytes();

    int getMessageHeaderLength();

}