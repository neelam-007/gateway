package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.external.assertions.mqnative.MqNativeMessageHeaderType;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasSoapAction;
import com.l7tech.message.MessageKnob;

import java.util.Map;

/**
 * An implementation of MessageKnob for MQ
 */
public interface MqNativeKnob extends MessageKnob, HasSoapAction, HasServiceOid {

    MqNativeMessageDescriptor getMessageDescriptor();

    byte[] getAllMessageHeaderBytes();

    MqNativeMessageHeaderType getPrimaryMessageHeaderType();

    Map<String, Object> getPrimaryMessageHeaderValueMap();

    Map<String, Object> getMessagePropertyMap();

}