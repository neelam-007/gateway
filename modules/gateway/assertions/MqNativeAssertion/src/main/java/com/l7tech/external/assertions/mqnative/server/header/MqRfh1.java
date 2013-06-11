package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.headers.MQRFH;

import java.io.IOException;
import java.util.*;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_1;

public class MqRfh1 implements MqNativeHeaderAdaptor {
    private MQRFH rfh;

    public String getMessageFormat() {
        return MQFMT_RF_HEADER_1;
    }

    @Override
    public Object getHeader() {
        return rfh;
    }

    @Override
    public void setHeader(Object header) {
        this.rfh = (MQRFH) header;
    }

    public void applyHeaderValuesToMessage(Map<String, Object> messageHeaderValueMap) throws IOException, MQException {
        applyPropertiesToMessage(messageHeaderValueMap);
    }

    /**
     * Apply RFH1 format properties to message.
     *
     * @param messagePropertyMap message properties to apply
     * @throws IOException
     * @throws com.ibm.mq.MQException
     */
    public void applyPropertiesToMessage(final Map<String, Object> messagePropertyMap) throws IOException, MQException {
        if (messagePropertyMap != null) {
            Map<String, Object> origHeader = parseHeaderValues(rfh);
            for (Map.Entry<String, Object> entry : messagePropertyMap.entrySet()) {
                if (entry.getValue() == null) {
                    origHeader.remove(entry.getKey());
                } else {
                    origHeader.put(entry.getKey(), entry.getValue());
                }
            }
            reallyApplyPropertiesToMessage(origHeader);
        }
    }

    public void reallyApplyPropertiesToMessage(final Map<String, Object> messagePropertyMap) throws IOException, MQException {
        if (messagePropertyMap != null) {
            StringBuilder sb = new StringBuilder();
            Iterator iterator = messagePropertyMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
                sb.append(entry.getKey());
                sb.append(" ");
                sb.append(entry.getValue().toString());
                if (iterator.hasNext()) {
                    sb.append("\u0000");
                }
            }
            rfh = new MQRFH(sb.toString());
        }
    }

    public Map<String, Object> parseHeaderValues() throws IOException {
        return parseHeaderValues(rfh);
    }

    private Map<String, Object> parseHeaderValues(MQRFH header) throws IOException {
        Map<String, Object> msgPropertyMap = new LinkedHashMap<String, Object>();

        String namedValueData = header.getNameValueData();
        StringTokenizer st = new StringTokenizer(namedValueData, "\u0000");
        while (st.hasMoreTokens()) {
            String currentNamedValueData = st.nextToken();
            String[] split = currentNamedValueData.split(" ");

            if (split.length == 2) {
                String propertyName = split[0];
                String propertyValue = split[1];
                msgPropertyMap.put(propertyName, propertyValue);
            }
        }

        return msgPropertyMap;
    }
}
