package com.l7tech.message;

import java.util.Map;


/**
 * An implementation of MessageKnob for MQ
 */
public interface MqNativeKnob extends MessageKnob, HasSoapAction, HasServiceOid {

    byte[] getMessageHeaderBytes();

    int getMessageHeaderLength();

    Map<String, Object> getMessagePropertyMap();

    String getMessageFormat();
}