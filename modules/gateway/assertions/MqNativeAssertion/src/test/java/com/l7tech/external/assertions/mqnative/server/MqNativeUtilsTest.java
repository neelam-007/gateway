package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.util.HexUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class MqNativeUtilsTest {

    // RFH ... MQSTR ... <mcd><Msd>jms_text</Msd></mcd> ...
    private static final byte[] MQ_MESSAGE_HEADER = {82, 70, 72, 32, 0, 0, 0, 2, 0, 0, 0, -96, 0, 0, 1, 17, 0, 0, 4, -72, 77, 81, 83,
            84, 82, 32, 32, 32, 0, 0, 0, 0, 0, 0, 4, -72, 0, 0, 0, 32, 60, 109, 99, 100, 62, 60, 77, 115, 100, 62, 106,
            109, 115, 95, 116, 101, 120, 116, 60, 47, 77, 115, 100, 62, 60, 47, 109, 99, 100, 62, 32, 32, 0, 0, 0};

    final String testValueStr = "333";
    final int testValueInt = Integer.parseInt(testValueStr);
    final byte[] testValueByteArray = HexUtils.decodeBase64(testValueStr);

    @Test
    public void buildMqNativeKnob() throws MQException  {
        MqNativeMessageDescriptor mqmd = new MqNativeMessageDescriptor(new MQMessage());
        mqmd.report = testValueInt;
        mqmd.format = testValueStr;
        mqmd.messageId = testValueByteArray;

        final Map<String,String> properties = new HashMap<String, String>(18);
        properties.put(MQ_PROPERTY_APPDATA, testValueStr);
        properties.put(MQ_PROPERTY_APPORIGIN, testValueStr);
        properties.put(MQ_PROPERTY_CHARSET, testValueStr);

        MqNativeKnob mqKnob = MqNativeUtils.buildMqNativeKnob(MQ_MESSAGE_HEADER, mqmd, properties);

        assertArrayEquals(MQ_MESSAGE_HEADER, mqKnob.getMessageHeaderBytes());
        assertEquals(mqmd, mqKnob.getMessageDescriptor());
        assertEquals(properties, mqKnob.getMessageDescriptorOverride());
    }

    @Test
    public void applyMqNativeKnobToMessage() throws IOException, MQDataException, MQException, MqNativeConfigException {
        MqNativeMessageDescriptor mqmd = new MqNativeMessageDescriptor(new MQMessage());
        mqmd.report = testValueInt;
        mqmd.format = testValueStr;
        mqmd.messageId = testValueByteArray;

        final Map<String,String> properties = new HashMap<String, String>(18);
        properties.put(MQ_PROPERTY_APPDATA, testValueStr);
        properties.put(MQ_PROPERTY_APPORIGIN, testValueStr);
        properties.put(MQ_PROPERTY_CHARSET, testValueStr);

        MqNativeKnob mqKnob = MqNativeUtils.buildMqNativeKnob(MQ_MESSAGE_HEADER, mqmd, properties);

        // pass through headers
        MQMessage mqMessage = new MQMessage();
        MqNativeUtils.applyMqNativeKnobToMessage(true, mqKnob, mqMessage);
        assertEquals(testValueInt, mqMessage.report);
        assertEquals(testValueStr, mqMessage.format);
        assertArrayEquals(testValueByteArray, mqMessage.messageId);
        // test overrides
        assertEquals(testValueStr, mqMessage.applicationIdData);
        assertEquals(testValueStr, mqMessage.applicationOriginData);
        assertEquals(testValueInt, mqMessage.characterSet);

        // don't pass through headers
        mqMessage = new MQMessage();
        MqNativeUtils.applyMqNativeKnobToMessage(false, mqKnob, mqMessage);
        assertThat(testValueInt, is(not(mqMessage.report)));
        assertThat(testValueStr, is(not(mqMessage.format)));
        assertThat(testValueByteArray, is(not(mqMessage.messageId)));
        // test overrides
        assertEquals(testValueStr, mqMessage.applicationIdData);
        assertEquals(testValueStr, mqMessage.applicationOriginData);
        assertEquals(testValueInt, mqMessage.characterSet);
    }
}
