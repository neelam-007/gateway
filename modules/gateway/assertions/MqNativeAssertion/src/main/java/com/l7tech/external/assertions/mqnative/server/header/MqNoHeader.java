package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;

import java.io.IOException;
import java.util.*;

public class MqNoHeader implements MqNativeHeaderAdaptor {

    public MqNoHeader() {
    }

    public String getMessageFormat() {
        return null;
    }

    @Override
    public Object getHeader() {
        return null;
    }

    @Override
    public void setHeader(Object header) {
    }

    public void applyHeaderValuesToMessage(Map<String, Object> messageHeaderValueMap) throws IOException, MQException {
    }

    public Map<String, Object> parseHeaderValues() throws IOException {
        return new HashMap<String, Object>(0);
    }
}
