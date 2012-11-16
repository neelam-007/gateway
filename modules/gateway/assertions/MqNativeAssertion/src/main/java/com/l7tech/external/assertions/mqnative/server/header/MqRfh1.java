package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_1;

public class MqRfh1 implements MqNativeHeaderAdaptor {
    final private MQRFH rfh;
    private final MQMessage mqMessage;

    public MqRfh1(@NotNull MQRFH rfh, @NotNull final MQMessage mqMessage) {
        this.rfh = rfh;
        this.mqMessage = mqMessage;
    }

    public String getMessageFormat() {
        return MQFMT_RF_HEADER_1;
    }

    /**
     * Apply RFH1 format header bytes to message.
     *
     * @param headerBytes header bytes to apply
     * @throws java.io.IOException
     * @throws com.ibm.mq.headers.MQDataException
     */
    public void applyHeaderBytesToMessage(final byte[] headerBytes) throws IOException, MQDataException {
        if (headerBytes != null && headerBytes.length > 0) {
            mqMessage.write(headerBytes);
        }
    }

    public void applyHeaderValuesToMessage(@NotNull Map<String, Object> messageHeaderValueMap) throws IOException, MQException {
        applyPropertiesToMessage(messageHeaderValueMap);
    }

    /**
     * Apply RFH1 format properties to message.
     *
     * @param messagePropertyMap message properties to apply
     * @throws IOException
     * @throws com.ibm.mq.MQException
     */
    public void applyPropertiesToMessage(@NotNull final Map<String, Object> messagePropertyMap) throws IOException, MQException {
        if (messagePropertyMap != null) {
            for (Map.Entry<String, Object> entry : messagePropertyMap.entrySet()) {
                rfh.addNameValuePair(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
            }
        }
    }

    public void writeHeaderToMessage() throws IOException {
        rfh.write(mqMessage);
    }

    public Pair<List<String>, List<String>> exposeHeaderValuesToContextVariables(@Nullable final Map<String, Object> messageHeaderValues,
                                                                                 @NotNull final PolicyEnforcementContext context) throws IOException {
        List<String> mqHeaderNames = new ArrayList<String>();
        List<String> mqAllHeaderValues = new ArrayList<String>();

        if (messageHeaderValues != null) {
            for (Map.Entry<String, Object> entry : messageHeaderValues.entrySet() ) {
                String propertyName = "mq.header." + entry.getKey();
                Object propertyValue = entry.getValue();

                context.setVariable(propertyName, propertyValue);

                mqHeaderNames.add(propertyName);
                mqAllHeaderValues.add(propertyName + ":" + propertyValue);
            }
        }

        return new Pair(mqHeaderNames, mqAllHeaderValues);
    }

    public Pair<List<String>, List<String>> exposePropertiesToContextVariables(@Nullable final Map<String, Object> messageProperties,
                                                                               @NotNull final PolicyEnforcementContext context) throws IOException {
        return new MqNoHeader(mqMessage).exposePropertiesToContextVariables(messageProperties, context);
    }

    public byte[] parseHeaderAsBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        rfh.write(new DataOutputStream(out));
        return out.toByteArray();
    }

    public Map<String, Object> parseHeaderValues() throws IOException {
        Map<String, Object> msgPropertyMap = new HashMap<String, Object>();

        String namedValueData = rfh.getNameValueData();
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

    public Map<String, Object> parseProperties() throws IOException, MQDataException, MQException {
        return new HashMap<String, Object>(0);
    }
}
