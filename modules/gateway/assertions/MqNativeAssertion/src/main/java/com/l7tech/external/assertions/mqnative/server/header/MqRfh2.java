package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.headers.MQRFH2;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_2;

public class MqRfh2 implements MqNativeHeaderAdaptor {
    private MQRFH2 rfh2;

    public String getMessageFormat() {
        return MQFMT_RF_HEADER_2;
    }

    @Override
    public Object getHeader() {
        return rfh2;
    }

    @Override
    public void setHeader(Object header) {
        this.rfh2 = (MQRFH2)header;
    }

    public void applyHeaderValuesToMessage(Map<String, Object> messageHeaderValueMap) throws IOException, MQException {
        applyPropertiesToMessage(messageHeaderValueMap);
    }

    /**
     * Apply RFH2 format properties (including JMS properties) to message.
     *
     * @param messagePropertyMap message properties to apply
     * @throws IOException
     * @throws MQException
     */
    public void applyPropertiesToMessage(final Map<String, Object> messagePropertyMap) throws IOException, MQException {
        if (messagePropertyMap != null ) {
            for (Map.Entry<String, Object> entry : messagePropertyMap.entrySet()) {
                String key = entry.getKey();
                String mqRfh2Key = MqNativeHeaderHandler.JMS_MQ_RFH2_MAP.get(key);
                if (mqRfh2Key != null) {
                    key = mqRfh2Key;
                }

                String rfh2Folder;
                String rfh2Field;
                String[] split = key.split("\\.");
                if (split.length == 2) {
                    rfh2Folder = split[0];
                    rfh2Field = split[1];
                } else {
                    rfh2Folder = "usr";
                    rfh2Field = key;
                }

                rfh2.setFieldValue(rfh2Folder, rfh2Field, entry.getValue());
            }
        }
    }

    public Map<String, Object> parseHeaderValues() throws IOException {
        Map<String, Object> msgPropertyMap = new LinkedHashMap<String, Object>();

        for (MQRFH2.Element folder : rfh2.getFolders()) {
            String folderName = folder.getName();
            for (MQRFH2.Element property : folder.getChildren()) {
                String propertyName;
                Object propertyValue = property.getValue();
                if (folderName.compareTo("usr") == 0) {
                    // do not include "usr" folder name
                    propertyName = property.getName();
                } else {
                    propertyName = folderName + "." + property.getName();
                }
                msgPropertyMap.put(propertyName, propertyValue);
            }
        }

        return msgPropertyMap;
    }
}
