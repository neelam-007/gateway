package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_2;

public class MqRfh2 implements MqNativeHeaderAdaptor {
    private final MQRFH2 rfh2;
    private final MQMessage mqMessage;

    public MqRfh2(@NotNull final MQRFH2 rfh2, @NotNull final MQMessage mqMessage) {
        this.rfh2 = rfh2;
        this.mqMessage = mqMessage;
    }

    public String getMessageFormat() {
        return MQFMT_RF_HEADER_2;
    }

    /**
     * Apply RFH2 format header bytes to message.
     *
     * @param headerBytes header bytes to apply
     * @throws IOException
     * @throws MQDataException
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
     * Apply RFH2 format properties (including JMS properties) to message.
     *
     * @param messagePropertyMap message properties to apply
     * @throws IOException
     * @throws MQException
     */
    public void applyPropertiesToMessage(@NotNull final Map<String, Object> messagePropertyMap) throws IOException, MQException {
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

    public void writeHeaderToMessage() throws IOException {
        rfh2.write(mqMessage);
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
        rfh2.write(new DataOutputStream(out));
        return out.toByteArray();
    }

    public Map<String, Object> parseHeaderValues() throws IOException {
        Map<String, Object> msgPropertyMap = new HashMap<String, Object>();

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

    public Map<String, Object> parseProperties() throws IOException, MQDataException, MQException {
        return new HashMap<String, Object>(0);
    }
}
