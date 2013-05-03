package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.external.assertions.mqnative.MqNativeMessageHeaderType;
import com.l7tech.external.assertions.mqnative.server.MqNativeConfigException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.ibm.mq.constants.MQPropertyIdentifiers.*;

/**
 * Delegates to required header adaptor.
 */
public class MqNativeHeaderHandler {

    // map JMS specific fields to MQRFH2 folder and property names
    // reference: http://publib.boulder.ibm.com/infocenter/wmqv7/v7r1/topic/com.ibm.mq.doc/jm25440_.htm
    public static final Map<String, String> JMS_MQ_RFH2_MAP = new HashMap<String, String>();

    static {
        JMS_MQ_RFH2_MAP.put(MQ_JMS_DESTINATION, RFH2_JMS_DESTINATION);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_EXPIRATION, RFH2_JMS_EXPIRATION);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_PRIORITY, RFH2_JMS_PRIORITY);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_DELIVERY_MODE, RFH2_JMS_DELIVERY_MODE);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_CORRELATION_ID, RFH2_JMS_CORREL_ID);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_REPLY_TO, RFH2_JMS_REPLY_TO);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_TIME_STAMP, RFH2_JMS_TIME_STAMP);
        JMS_MQ_RFH2_MAP.put(MQ_JMS_TYPE, RFH2_JMS_MCD_TYPE);
        JMS_MQ_RFH2_MAP.put(MQ_JMSX_GROUP_ID, RFH2_JMSX_GROUP_ID);
        JMS_MQ_RFH2_MAP.put(MQ_JMSX_GROUP_SEQ, RFH2_JMSX_GROUP_SEQ);
        JMS_MQ_RFH2_MAP.put("JMSArmCorrelator", "mqext.Arm");
        JMS_MQ_RFH2_MAP.put("JMSRMCorrelator", "mqext.Wrm");
        JMS_MQ_RFH2_MAP.put("MQTopicString", "mqps.Top");
        JMS_MQ_RFH2_MAP.put("MQSubUserData", "mqps.Sud");
        JMS_MQ_RFH2_MAP.put("MQIsRetained", "mqps.Ret");
        JMS_MQ_RFH2_MAP.put("MQPubOptions", "mqps.Pub");
        JMS_MQ_RFH2_MAP.put("MQPubLevel", "mqps.Pbl");
        JMS_MQ_RFH2_MAP.put("MQPubTime", "mqpse.Pts");
        JMS_MQ_RFH2_MAP.put("MQPubSeqNum", "mqpse.Seq");
        JMS_MQ_RFH2_MAP.put("MQPubStrIntData", "mqpse.Sid");
        JMS_MQ_RFH2_MAP.put("MQPubFormat", "mqpse.Pfmt");
    }

    private MqNativeHeaderAdaptor mqNativeHeaderAdaptor;
    private MqNativeMessageHeaderType type;

    public MqNativeHeaderHandler(final Object mqHeader) throws IOException, MQDataException, MqNativeConfigException {
        if (mqHeader == null) {
            mqNativeHeaderAdaptor = new MqNoHeader();
            return;
        }

        MqNativeMessageHeaderType[] types = MqNativeMessageHeaderType.values();
        for (int i = 0; i < types.length; i++) {
            MqNativeMessageHeaderType type = types[i];
            if (type.getAdaptorClass() == null) {
                continue;
            } else {
                if (mqHeader.getClass().isAssignableFrom(getHeaderClass(type.getHeaderClassName()))) {
                    this.type = type;
                    try {
                        mqNativeHeaderAdaptor = (MqNativeHeaderAdaptor) type.getAdaptorClass().newInstance();
                        mqNativeHeaderAdaptor.setHeader(mqHeader);
                        return;
                    } catch (InstantiationException e) {
                        throw new MqNativeConfigException(e.getMessage());
                    } catch (IllegalAccessException e) {
                        throw new MqNativeConfigException(e.getMessage());
                    }
                }
            }
        }
        mqNativeHeaderAdaptor = new MqUnsupportedHeader();
    }

    public MqNativeHeaderHandler(@Nullable final MqNativeMessageHeaderType mqHeaderType) throws MqNativeConfigException {
        if (mqHeaderType == null) {
            mqNativeHeaderAdaptor = new MqNoHeader();
            return;
        }
        this.type = mqHeaderType;
        try {
            if (mqHeaderType.getAdaptorClass() != null) {
                mqNativeHeaderAdaptor = (MqNativeHeaderAdaptor) mqHeaderType.getAdaptorClass().newInstance();
                if (mqHeaderType.getHeaderClassName() != null) {
                    mqNativeHeaderAdaptor.setHeader(getHeaderClass(mqHeaderType.getHeaderClassName()).newInstance());
                }
            }
        } catch (InstantiationException e) {
            throw new MqNativeConfigException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MqNativeConfigException(e.getMessage());
        }
    }

    public MqNativeMessageHeaderType getType() {
        return type;
    }


    public String getMessageHeaderFormat() {
        return mqNativeHeaderAdaptor.getMessageFormat();
    }

    public Map<String, Object> parsePrimaryHeaderValues() throws IOException {
        return mqNativeHeaderAdaptor.parseHeaderValues();
    }

    public Object getHeader() throws IOException {
        return mqNativeHeaderAdaptor.getHeader();
    }

    public void applyHeaderValuesToMessage(Map<String, Object> convertedProperties) throws MQException, IOException {
        mqNativeHeaderAdaptor.applyHeaderValuesToMessage(convertedProperties);
    }

    private Class getHeaderClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }
}
