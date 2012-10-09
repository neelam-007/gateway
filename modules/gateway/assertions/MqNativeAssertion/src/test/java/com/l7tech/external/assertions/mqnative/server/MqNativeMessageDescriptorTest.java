package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.l7tech.util.HexUtils;
import org.junit.Test;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static org.junit.Assert.assertEquals;

public class MqNativeMessageDescriptorTest {
    final String testValueStr = "333";
    final int testValueInt = Integer.parseInt(testValueStr);
    final byte[] testValueByteArray = HexUtils.decodeBase64(testValueStr);
    final GregorianCalendar testValueCalendar = new GregorianCalendar();
    final int testVersionInt = 2;

    @Test
    public void copy() throws MQException, MqNativeConfigException {
        MQMessage mqMessage = new MQMessage();
        mqMessage.report = testValueInt;
        mqMessage.messageType = testValueInt;
        mqMessage.expiry = testValueInt;
        mqMessage.feedback = testValueInt;
        mqMessage.encoding = testValueInt;
        mqMessage.characterSet = testValueInt;
        mqMessage.format = testValueStr;
        mqMessage.priority = testValueInt;
        mqMessage.persistence = testValueInt;
        mqMessage.messageId = testValueByteArray;
        mqMessage.correlationId = testValueByteArray;
        mqMessage.backoutCount = testValueInt;
        mqMessage.replyToQueueName = testValueStr;
        mqMessage.replyToQueueManagerName = testValueStr;
        mqMessage.userId = testValueStr;
        mqMessage.accountingToken = testValueByteArray;
        mqMessage.applicationIdData = testValueStr;
        mqMessage.putApplicationType = testValueInt;
        mqMessage.putApplicationName = testValueStr;
        mqMessage.putDateTime = testValueCalendar;
        mqMessage.applicationOriginData = testValueStr;
        mqMessage.groupId = testValueByteArray;
        mqMessage.messageSequenceNumber = testValueInt;
        mqMessage.offset = testValueInt;
        mqMessage.messageFlags = testValueInt;
        mqMessage.originalLength = testValueInt;
        mqMessage.setVersion(testVersionInt);

        MqNativeMessageDescriptor mqmd = new MqNativeMessageDescriptor(mqMessage);
        mqmd.copyTo(mqMessage);

        assertEquals(testValueInt, mqMessage.report);
        assertEquals(testValueInt, mqMessage.messageType);
        assertEquals(testValueInt, mqMessage.expiry);
        assertEquals(testValueInt, mqMessage.feedback);
        assertEquals(testValueInt, mqMessage.encoding);
        assertEquals(testValueInt, mqMessage.characterSet);
        assertEquals(testValueStr, mqMessage.format);
        assertEquals(testValueInt, mqMessage.priority);
        assertEquals(testValueInt, mqMessage.persistence);
        assertEquals(testValueByteArray.length, mqMessage.messageId.length);
        assertEquals(testValueByteArray.length, mqMessage.correlationId.length);
        assertEquals(testValueInt, mqMessage.backoutCount);
        assertEquals(testValueStr, mqMessage.replyToQueueName);
        assertEquals(testValueStr, mqMessage.replyToQueueManagerName);
        assertEquals(testValueStr, mqMessage.userId);
        assertEquals(testValueByteArray.length, mqMessage.accountingToken.length);
        assertEquals(testValueStr, mqMessage.applicationIdData);
        assertEquals(testValueInt, mqMessage.putApplicationType);
        assertEquals(testValueStr, mqMessage.putApplicationName);
        assertEquals(testValueCalendar, mqMessage.putDateTime);
        assertEquals(testValueStr, mqMessage.applicationOriginData);
        assertEquals(testValueByteArray.length, mqMessage.groupId.length);
        assertEquals(testValueInt, mqMessage.messageSequenceNumber);
        assertEquals(testValueInt, mqMessage.offset);
        assertEquals(testValueInt, mqMessage.messageFlags);
        assertEquals(testValueInt, mqMessage.originalLength);
        assertEquals(testVersionInt, mqMessage.getVersion());
    }

    @Test
    public void applyPropertiesToMessage() throws MQException, MqNativeConfigException {
        final Map<String,String> properties = new HashMap<String, String>(18);
        properties.put(MQ_PROPERTY_APPDATA, testValueStr);
        properties.put(MQ_PROPERTY_APPORIGIN, testValueStr);
        properties.put(MQ_PROPERTY_CHARSET, testValueStr);
        properties.put(MQ_PROPERTY_ENCODING, testValueStr);
        properties.put(MQ_PROPERTY_EXPIRY, testValueStr);
        properties.put(MQ_PROPERTY_FEEDBACK, testValueStr);
        properties.put(MQ_PROPERTY_FORMAT, testValueStr);
        properties.put(MQ_PROPERTY_GROUPID, testValueStr);
        properties.put(MQ_PROPERTY_MSG_FLAGS, testValueStr);
        properties.put(MQ_PROPERTY_MSG_SEQNUM, testValueStr);
        properties.put(MQ_PROPERTY_MSG_TYPE, testValueStr);
        properties.put(MQ_PROPERTY_OFFSET, testValueStr);
        properties.put(MQ_PROPERTY_PERSISTENCE, testValueStr);
        properties.put(MQ_PROPERTY_PRIORITY, testValueStr);
        properties.put(MQ_PROPERTY_APPNAME, testValueStr);
        properties.put(MQ_PROPERTY_APPTYPE, testValueStr);
        properties.put(MQ_PROPERTY_REPORT, testValueStr);
        properties.put(MQ_PROPERTY_USERID, testValueStr);

        final MQMessage mqMessage = new MQMessage();
        MqNativeMessageDescriptor.applyPropertiesToMessage(properties, mqMessage);

        assertEquals(testValueStr, mqMessage.applicationIdData);
        assertEquals(testValueStr, mqMessage.applicationOriginData);
        assertEquals(testValueInt, mqMessage.characterSet);
        assertEquals(testValueInt, mqMessage.encoding);
        assertEquals(testValueInt, mqMessage.expiry);
        assertEquals(testValueInt, mqMessage.feedback);
        assertEquals(testValueStr, mqMessage.format);
        assertEquals(HexUtils.decodeBase64(testValueStr).length, mqMessage.groupId.length);
        assertEquals(testValueInt, mqMessage.messageFlags);
        assertEquals(testValueInt, mqMessage.messageType);
        assertEquals(testValueInt, mqMessage.offset);
        assertEquals(testValueInt, mqMessage.persistence);
        assertEquals(testValueInt, mqMessage.priority);
        assertEquals(testValueStr, mqMessage.putApplicationName);
        assertEquals(testValueInt, mqMessage.putApplicationType);
        assertEquals(testValueInt, mqMessage.report);
        assertEquals(testValueStr, mqMessage.userId);
    }
}
