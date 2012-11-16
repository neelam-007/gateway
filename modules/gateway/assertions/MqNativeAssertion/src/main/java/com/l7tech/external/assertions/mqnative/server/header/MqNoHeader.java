package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

import static com.ibm.mq.constants.CMQC.MQFMT_NONE;

public class MqNoHeader implements MqNativeHeaderAdaptor {
    protected final MQMessage mqMessage;

    public MqNoHeader(@NotNull final MQMessage mqMessage) {
        this.mqMessage = mqMessage;
    }

    public String getMessageFormat() {
        return MQFMT_NONE;
    }

    /**
     * No header to apply.
     *
     * @param headerBytes header bytes to apply
     * @throws IOException
     */
    public void applyHeaderBytesToMessage(final byte[] headerBytes) throws IOException, MQDataException {
        // do nothing no header to apply
    }

    public void applyHeaderValuesToMessage(@NotNull Map<String, Object> messageHeaderValueMap) throws IOException, MQException {
        applyPropertiesToMessage(messageHeaderValueMap);
    }

    /**
     * Apply non specific properties to message.  For a specific header type (e.g. RFH2) create subclass to override the implementation.
     *
     * @param messagePropertyMap message properties to apply
     * @throws IOException
     * @throws MQException
     */
    public void applyPropertiesToMessage(@NotNull final Map<String, Object> messagePropertyMap) throws IOException, MQException {
        for (Map.Entry<String, Object> entry : messagePropertyMap.entrySet() ) {
            String key = entry.getKey();
            String mqRfh2Key = MqNativeHeaderHandler.JMS_MQ_RFH2_MAP.get(key);
            if (mqRfh2Key != null) {
                key = mqRfh2Key;
            }

            // delete existing property
            try {
                mqMessage.getObjectProperty(key);
                mqMessage.deleteProperty(key);
            } catch (MQException e) {
                // do nothing: getObjectProperty(key) throws MQException if property does not exist
            }

            mqMessage.setObjectProperty(key, entry.getValue());
        }
    }

    public void writeHeaderToMessage() throws IOException {
        // do nothing no header to write
    }

    public Pair<List<String>, List<String>> exposeHeaderValuesToContextVariables(@Nullable final Map<String, Object> messageProperties,
                                                                                 @NotNull final PolicyEnforcementContext context) throws IOException {
        return new Pair(new ArrayList<String>(0), new ArrayList<String>(0));
    }

    public Pair<List<String>, List<String>> exposePropertiesToContextVariables(@Nullable final Map<String, Object> messageProperties,
                                                                               @NotNull final PolicyEnforcementContext context) throws IOException {
        List<String> mqPropertyNames = new ArrayList<String>();
        List<String> mqAllPropertyValues = new ArrayList<String>();

        if (messageProperties != null) {
            for (Map.Entry<String, Object> entry : messageProperties.entrySet() ) {
                String propertyName = "mq.property." + entry.getKey();
                Object propertyValue = entry.getValue();

                context.setVariable(propertyName, propertyValue);

                mqPropertyNames.add(propertyName);
                mqAllPropertyValues.add(propertyName + ":" + propertyValue);
            }
        }

        return new Pair(mqPropertyNames, mqAllPropertyValues);
    }

    public  byte[] parseHeaderAsBytes() throws IOException {
        return new byte[0];
    }

    public Map<String, Object> parseHeaderValues() throws IOException {
        return new HashMap<String, Object>(0);
    }

    public Map<String, Object> parseProperties() throws IOException, MQException {
        mqMessage.seek(0);

        Map<String, Object> msgPropertyMap = new HashMap<String, Object>();
        for (Enumeration e = mqMessage.getPropertyNames("%"); e.hasMoreElements() ;) {
            String propertyName = (String)e.nextElement();
            msgPropertyMap.put(propertyName, mqMessage.getObjectProperty(propertyName));
        }

        return msgPropertyMap;
    }
}
