package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER;
import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_2;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class MqNativeUtilsTest {
    /**
     * PROPCTL=COMPAT expected message format
     * On a V7 MQ server, a queue's property control (PROPCTL) is defaulted to compatible (COMPAT).
     */
    private static final byte[] MQ_MESSAGE_COMPAT_HEADER = new byte[0];

    /**
     * PROPCTL=FORCE expected message format
     * On a V7 MQ server, a queue's property control can be set to force all messages properties into MQRFH2 headers (FORCE).
     * This is also the expected format used when a MQ v6 client creates a message on a v7 server.
     * RFH ... MQSTR ... <mcd><Msd>jms_text</Msd></mcd> ... T<jms><Dst>queue:///COREDEVREQUESTQ01</Dst><Tms>1342462125398</Tms><Dlv>2</Dlv></jms>This is the payload.
     * Note the "..." sub string below are unprintable characters in a byte array.
     */
    private static final byte[] MQ_MESSAGE_FORCE_HEADER_PART1 = {82, 70, 72, 32, 0, 0, 0, 2, 0, 0, 0, -96, 0, 0, 1, 17, 0, 0, 4, -72, 77, 81, 83,
            84, 82, 32, 32, 32, 0, 0, 0, 0, 0, 0, 4, -72, 0, 0, 0, 32, 60, 109, 99, 100, 62, 60, 77, 115, 100, 62, 106,
            109, 115, 95, 116, 101, 120, 116, 60, 47, 77, 115, 100, 62, 60, 47, 109, 99, 100, 62, 32, 32, 0, 0, 0};
    private static final String MQ_MESSAGE_FORCE_HEADER_PART2 = "T<jms><Dst>queue:///COREDEVREQUESTQ01</Dst><Tms>1342462125398</Tms><Dlv>2</Dlv></jms>";
    private static final byte[] MQ_MESSAGE_FORCE_HEADER = CONCATENATE(MQ_MESSAGE_FORCE_HEADER_PART1, (MQ_MESSAGE_FORCE_HEADER_PART2).getBytes());

    private static final String MESSAGE_PAYLOAD = "This is the payload.";

    private static byte[] CONCATENATE(@NotNull final byte[] bytes1, @NotNull final byte[] bytes2) {
        byte[] concatenated = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, concatenated, 0, bytes1.length);
        System.arraycopy(bytes2, 0, concatenated, bytes1.length, bytes2.length);
        return concatenated;
    }

    @Test
    public void parseHeaderPayload() throws IOException, MQException, MQDataException {
        // expected for PROPCTL=COMPAT
        MQMessage mqMessage = new MQMessage();
        // no exposed header
        mqMessage.write(MESSAGE_PAYLOAD.getBytes());
        Pair<byte[], byte[]> msgHeaderPayload = MqNativeUtils.parseHeaderPayload(mqMessage);
        assertArrayEquals(MQ_MESSAGE_COMPAT_HEADER, msgHeaderPayload.left);
        assertArrayEquals(MESSAGE_PAYLOAD.getBytes(), msgHeaderPayload.right);

        // expected for PROPCTL=FORCE
        mqMessage = new MQMessage();
        MQRFH2 rfh2 = new MQRFH2(new DataInputStream(new ByteArrayInputStream(MQ_MESSAGE_FORCE_HEADER)));
        rfh2.write(mqMessage);
        mqMessage.format = MQFMT_RF_HEADER_2;
        mqMessage.write(MESSAGE_PAYLOAD.getBytes());
        msgHeaderPayload = MqNativeUtils.parseHeaderPayload(mqMessage);
        assertArrayEquals(MQ_MESSAGE_FORCE_HEADER, msgHeaderPayload.left);
        assertArrayEquals(MESSAGE_PAYLOAD.getBytes(), msgHeaderPayload.right);
    }

    @Test
    public void buildMqNativeKnob() throws MQException  {
        MqNativeMessageDescriptor mqmd = new MqNativeMessageDescriptor(new MQMessage());
        mqmd.messageType = 8;
        mqmd.format = MQFMT_RF_HEADER;
        mqmd.messageId = "AMQ coreDevQueuerÁÏO14".getBytes();

        final Map<String,String> properties = new HashMap<String, String>(3);
        properties.put(MQ_PROPERTY_PRIORITY, "4");
        properties.put(MQ_PROPERTY_EXPIRY, "300");
        properties.put(MQ_PROPERTY_CHARSET, "819");

        MqNativeKnob mqKnob = MqNativeUtils.buildMqNativeKnob(MQ_MESSAGE_FORCE_HEADER, mqmd, properties);

        assertArrayEquals(MQ_MESSAGE_FORCE_HEADER, mqKnob.getMessageHeaderBytes());
        assertEquals(mqmd, mqKnob.getMessageDescriptor());
        assertEquals(properties, mqKnob.getMessageDescriptorOverride());
    }

    @Test
    public void applyMqNativeKnobToMessage() throws IOException, MQDataException, MQException, MqNativeConfigException {
        final byte[] testHeaderBytes = MQ_MESSAGE_FORCE_HEADER;
        final String testFormat = MQFMT_RF_HEADER_2;
        final int testEncoding = 273;
        final byte[] testMessageId = "AMQ coreDevQueuerÁÏO14".getBytes();
        final int testPriority = 4;
        final int testExpiry = 300;
        final int testCharacterSet = 819;

        // setup message descriptors
        MqNativeMessageDescriptor mqmd = new MqNativeMessageDescriptor(new MQMessage());
        mqmd.format = testFormat; // RFH2 header
        mqmd.encoding = testEncoding;
        mqmd.messageId = testMessageId;
        // setup properties override
        final Map<String,String> properties = new HashMap<String, String>(3);
        properties.put(MQ_PROPERTY_PRIORITY, Integer.toString(testPriority));
        properties.put(MQ_PROPERTY_EXPIRY, Integer.toString(testExpiry));
        properties.put(MQ_PROPERTY_CHARSET, Integer.toString(testCharacterSet));
        MqNativeKnob mqKnob = MqNativeUtils.buildMqNativeKnob(testHeaderBytes, mqmd, properties);

        // pass through headers
        MQMessage mqMessage = new MQMessage();
        MqNativeUtils.applyMqNativeKnobToMessage(true, mqKnob, mqMessage);
        // test header
        Pair<byte[], byte[]> msgHeaderPayload = MqNativeUtils.parseHeaderPayload(mqMessage);
        assertArrayEquals(testHeaderBytes, msgHeaderPayload.left);
        // test message descriptors
        assertEquals(testFormat, mqMessage.format);
        assertEquals(testEncoding, mqMessage.encoding);
        assertArrayEquals(testMessageId, mqMessage.messageId);
        // test properties override
        assertEquals(testPriority, mqMessage.priority);
        assertEquals(testExpiry, mqMessage.expiry);
        assertEquals(testCharacterSet, mqMessage.characterSet);

        // don't pass through headers
        mqMessage = new MQMessage();
        MqNativeUtils.applyMqNativeKnobToMessage(false, mqKnob, mqMessage);
        // test header
        msgHeaderPayload = MqNativeUtils.parseHeaderPayload(mqMessage);
        assertThat(testHeaderBytes, is(not(msgHeaderPayload.left)));
        // test message descriptors
        assertThat(testFormat, is(not(mqMessage.format)));
        assertThat(testEncoding, is(not(mqMessage.messageType)));
        assertThat(testMessageId, is(not(mqMessage.messageId)));
        // test properties override
        assertEquals(testPriority, mqMessage.priority);
        assertEquals(testExpiry, mqMessage.expiry);
        assertEquals(testCharacterSet, mqMessage.characterSet);
    }
}
