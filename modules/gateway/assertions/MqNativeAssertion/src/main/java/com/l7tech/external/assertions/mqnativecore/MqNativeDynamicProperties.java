package com.l7tech.external.assertions.mqnativecore;

import java.io.Serializable;

/**
 * User: ashah
 */
public class MqNativeDynamicProperties implements Serializable, Cloneable{
    private String destQName;
    private String destChannelName;

    //constructor
    public MqNativeDynamicProperties() {
    }

    public String getDestQName() {
        return destQName;
    }

    public void setDestQName(String destQName) {
        this.destQName = destQName;
    }

    public String getDestChannelName() {
        return destChannelName;
    }

    public void setDestChannelName(String destChannelName) {
        this.destChannelName = destChannelName;
    }

    public String getFieldsAsVariables() {
        String SEP = " ";
        StringBuffer sb = new StringBuffer();
        if (getDestQName() != null) sb.append(getDestQName()).append(SEP);
        if (getDestChannelName() != null) sb.append(getDestChannelName()).append(SEP);
        return sb.toString();
    }
}
