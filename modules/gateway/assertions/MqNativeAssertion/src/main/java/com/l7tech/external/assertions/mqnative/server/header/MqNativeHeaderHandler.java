package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH;
import com.ibm.mq.headers.MQRFH2;
import com.ibm.mq.headers.internal.Header;
import com.l7tech.external.assertions.mqnative.MqNativeMessageHeaderType;
import com.l7tech.external.assertions.mqnative.MqNativeMessagePropertyRuleSet;
import com.l7tech.external.assertions.mqnative.server.*;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

import static com.ibm.mq.constants.MQPropertyIdentifiers.*;
import static com.ibm.mq.constants.MQPropertyIdentifiers.RFH2_JMSX_GROUP_SEQ;
import static com.l7tech.external.assertions.mqnative.MqNativeMessageHeaderType.*;

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

    public static MqNativeMessageHeaderType parsePrimaryHeaderType(@NotNull final MQMessage mqMessage) throws IOException, MQDataException {
        final Header mqHeader = MqNativeUtils.parsePrimaryAdditionalHeader(mqMessage);

        MqNativeMessageHeaderType mqNativeMessageHeaderType;
        if (mqHeader == null) {
            mqNativeMessageHeaderType = NO_HEADER;
        } else if (mqHeader instanceof MQRFH2) {
            mqNativeMessageHeaderType = MQRFH2;
        } else if (mqHeader instanceof MQRFH) {
            mqNativeMessageHeaderType = MQRFH1;
        } else {
            mqNativeMessageHeaderType = UNSUPPORTED;
        }

        return mqNativeMessageHeaderType;
    }

    private final MQMessage mqMessage;
    private final MqNativeHeaderAdaptor mqNativeHeaderAdaptor;
    private final MqNativeMessageHeaderType mqNativeMessageHeaderType;

    public MqNativeHeaderHandler(@NotNull final MQMessage mqMessage) throws IOException, MQDataException {
        this.mqMessage = mqMessage;
        final Header mqHeader = MqNativeUtils.parsePrimaryAdditionalHeader(mqMessage);
        if (mqHeader == null) {
            mqNativeHeaderAdaptor = new MqNoHeader(mqMessage);
            mqNativeMessageHeaderType = NO_HEADER;
        } else if (mqHeader instanceof MQRFH2) {
            mqNativeHeaderAdaptor = new MqRfh2((MQRFH2) mqHeader, mqMessage);
            mqNativeMessageHeaderType = MQRFH2;
        } else if (mqHeader instanceof MQRFH) {
            mqNativeHeaderAdaptor = new MqRfh1((MQRFH) mqHeader, mqMessage);
            mqNativeMessageHeaderType = MQRFH1;
        } else {
            mqNativeHeaderAdaptor = new MqUnsupportedHeader(mqMessage);
            mqNativeMessageHeaderType = UNSUPPORTED;
        }
    }

    public MqNativeHeaderHandler(@Nullable final MqNativeMessageHeaderType mqHeaderType,
                                 @NotNull final MQMessage mqMessage) {
        this.mqMessage = mqMessage;
        if (mqHeaderType == null || mqHeaderType == NO_HEADER) {
            mqNativeHeaderAdaptor = new MqNoHeader(mqMessage);
            mqNativeMessageHeaderType = NO_HEADER;
        } else if (mqHeaderType == MQRFH2 ) {
            mqNativeHeaderAdaptor = new MqRfh2(new MQRFH2(), mqMessage);
            mqNativeMessageHeaderType = MQRFH2;
        } else if (mqHeaderType == MQRFH1) {
            mqNativeHeaderAdaptor = new MqRfh1(new MQRFH(), mqMessage);
            mqNativeMessageHeaderType = MQRFH1;
        } else {
            mqNativeHeaderAdaptor = new MqUnsupportedHeader(mqMessage);
            mqNativeMessageHeaderType = UNSUPPORTED;
        }
    }

    public String getMessageHeaderFormat() {
        return mqNativeHeaderAdaptor.getMessageFormat();
    }

    public void applyMqNativeKnobToMessage(@NotNull final MqNativeMessagePropertyRuleSet mqMessageRules,
                                           @Nullable final MqNativeKnob mqNativeKnob) throws IOException, MQDataException, MQException, MqNativeConfigException {
        if (mqNativeKnob != null) {
            if (mqMessageRules.isPassThroughMqMessageDescriptors()) {
                // apply message descriptor
                MqNativeMessageDescriptor mqmd = mqNativeKnob.getMessageDescriptor();
                if (mqmd != null) {
                    mqmd.copyTo(mqMessage);
                }
            }

            try {
                if (mqMessageRules.isPassThroughMqMessageHeaders()) {
                    // apply header bytes
                    mqNativeHeaderAdaptor.applyHeaderBytesToMessage(mqNativeKnob.getAllMessageHeaderBytes());
                } else {
                    // overwrite all header(s) with one primary header of another type (or the same type)
                    mqMessage.format = mqNativeHeaderAdaptor.getMessageFormat();
                    mqNativeHeaderAdaptor.applyHeaderValuesToMessage(mqNativeKnob.getPrimaryMessageHeaderValueMap());
                }

                if (mqMessageRules.isPassThroughMqMessageProperties()) {
                    // apply properties
                    mqNativeHeaderAdaptor.applyPropertiesToMessage(mqNativeKnob.getMessagePropertyMap());
                }
            } finally {
                if (!mqMessageRules.isPassThroughMqMessageHeaders()) {
                    mqNativeHeaderAdaptor.writeHeaderToMessage();
                }
            }

        }
    }

    public void applyHeaderValuesToMessage(@NotNull Map<String, Object> messageHeaderValueMap) throws IOException, MQException {
        mqNativeHeaderAdaptor.applyHeaderValuesToMessage(messageHeaderValueMap);
    }

    public void applyPropertiesToMessage(@Nullable final Map<String, String> properties,
                                         @Nullable final Map<String, Object> contextVariableMap,
                                         @Nullable final Audit audit) throws IOException, MQException {
        if (properties != null) {
            Map <String, Object> convertedProperties = new HashMap<String, Object>(properties.size());
            for (Map.Entry<String, String> propertiesEntry : properties.entrySet()) {
                String name;
                String value;
                if (contextVariableMap != null && audit != null) {
                    name = ExpandVariables.process(propertiesEntry.getKey(), contextVariableMap, audit);
                    value = ExpandVariables.process(propertiesEntry.getValue(), contextVariableMap, audit);
                } else {
                    name = propertiesEntry.getKey();
                    value = propertiesEntry.getValue();
                }
                convertedProperties.put(name, value);
            }
            mqNativeHeaderAdaptor.applyPropertiesToMessage(convertedProperties);
        }
    }

    public Map<String, Object> parsePrimaryHeaderValues() throws IOException {
        return mqNativeHeaderAdaptor.parseHeaderValues();
    }

    public byte[] parsePrimaryHeaderAsBytes() throws IOException {
        return mqNativeHeaderAdaptor.parseHeaderAsBytes();
    }

    public Map<String, Object> parseProperties() throws IOException, MQDataException, MQException {
        return mqNativeHeaderAdaptor.parseProperties();
    }

    public void exposeContextVariables(@NotNull final PolicyEnforcementContext context,
                                       @Nullable final MqNativeKnob mqNativeKnob) throws IOException {
        if (mqNativeKnob != null) {
            // expose MQMD
            MqNativeMessageDescriptor md = mqNativeKnob.getMessageDescriptor();
            context.setVariable("mq.md.report", md.report);
            context.setVariable("mq.md.messageType", md.messageType);
            context.setVariable("mq.md.expiry", md.expiry);
            context.setVariable("mq.md.feedback", md.feedback);
            context.setVariable("mq.md.encoding", md.encoding);
            context.setVariable("mq.md.characterSet", md.characterSet);
            context.setVariable("mq.md.format", md.format);
            context.setVariable("mq.md.priority", md.priority);
            context.setVariable("mq.md.persistence", md.persistence);
            context.setVariable("mq.md.messageId", new String(md.messageId)); // convert from byte[] to String
            context.setVariable("mq.md.correlationId", new String(md.correlationId)); // convert from byte[] to String
            context.setVariable("mq.md.backoutCount", md.backoutCount);
            context.setVariable("mq.md.replyToQueueName", md.replyToQueueName);
            context.setVariable("mq.md.replyToQueueManagerName", md.replyToQueueManagerName);
            context.setVariable("mq.md.userId", md.userId);
            context.setVariable("mq.md.accountingToken", new String(md.accountingToken)); // convert from byte[] to String
            context.setVariable("mq.md.applicationIdData", md.applicationIdData);
            context.setVariable("mq.md.putApplicationType", md.putApplicationType);
            context.setVariable("mq.md.putApplicationName", md.putApplicationName);
            if (md.putDateTime != null) {
                context.setVariable("mq.md.putDateTime", md.putDateTime.getTime().toString()); // convert from GregorianCalendar to String
            }
            context.setVariable("mq.md.applicationOriginData", md.applicationOriginData);
            context.setVariable("mq.md.groupId", new String(md.groupId)); // convert from byte[] to String
            context.setVariable("mq.md.messageSequenceNumber", md.messageSequenceNumber);
            context.setVariable("mq.md.offset", md.offset);
            context.setVariable("mq.md.messageFlags", md.messageFlags);
            context.setVariable("mq.md.originalLength", md.originalLength);
            context.setVariable("mq.md.version", md.getVersion());

            // expose any header(s) as byte array
            // context.setVariable("mq.headerasbytearray", mqNativeKnob.getAllMessageHeaderBytes());

            // expose header values
            Pair<List<String>, List<String>> mqHeaderNamesAndAllValues =  mqNativeHeaderAdaptor.exposeHeaderValuesToContextVariables(mqNativeKnob.getPrimaryMessageHeaderValueMap(), context);
            context.setVariable("mq.headernames", mqHeaderNamesAndAllValues.left.toArray());
            context.setVariable("mq.allheadervalues", mqHeaderNamesAndAllValues.right.toArray());

            // expose properties
            Pair<List<String>, List<String>> mqPropertyNamesAndAllValues =  mqNativeHeaderAdaptor.exposePropertiesToContextVariables(mqNativeKnob.getMessagePropertyMap(), context);
            context.setVariable("mq.propertynames", mqPropertyNamesAndAllValues.left.toArray());
            context.setVariable("mq.allpropertyvalues", mqPropertyNamesAndAllValues.right.toArray());
        }
    }

    public MqNativeMessageHeaderType getMqNativeMessageHeaderType() {
        return mqNativeMessageHeaderType;
    }
}
