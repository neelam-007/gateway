package com.l7tech.external.assertions.mqnativecore.server;

import com.l7tech.message.JmsKnob;

import java.util.Map;

/**
 * An implementation of MessageKnob for MQ, based on the JmsKnob interface.
 */
public interface MqNativeKnob extends JmsKnob {

    @Override
    boolean isBytesMessage();

    @Override
    Map<String, Object> getJmsMsgPropMap();

    
    byte[] getMessageHeaderBytes();


    int getMessageHeaderLength();

}