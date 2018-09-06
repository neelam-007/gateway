package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.test.BugId;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.HexUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER;
import static com.ibm.mq.constants.CMQC.MQFMT_RF_HEADER_2;
import static com.ibm.mq.constants.MQPropertyIdentifiers.*;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_CONVERSION_CCSID;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_CONVERT_MESSAGE_APPLICATION_DATA_FORMAT;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MqNativeUtilsTest {
    /**
     * PROPCTL=COMPAT expected message format
     * On a V7 MQ server, a queue's property control (PROPCTL) is defaulted to compatible (COMPAT).
     */
    public static final byte[] MQ_MESSAGE_COMPAT_HEADER = new byte[0];

    /**
     * PROPCTL=FORCE expected message format
     * On a V7 MQ server, a queue's property control can be set to force all messages properties into MQRFH2 headers (FORCE).
     * This is also the expected format used when a MQ v6 client creates a message on a v7 server.
     * RFH ... MQSTR ... <mcd><Msd>jms_text</Msd></mcd> ... T<jms><Dst>queue:///COREDEVREQUESTQ01</Dst><Tms>1342462125398</Tms><Dlv>2</Dlv></jms>This is the payload.
     * Note the "..." sub string above are unprintable characters in a byte array.
     */
    private static final byte[] MQ_MESSAGE_FORCE_HEADER_PART1 = {82, 70, 72, 32, 0, 0, 0, 2, 0, 0, 0, -96, 0, 0, 1, 17, 0, 0, 4, -72, 77, 81, 83,
            84, 82, 32, 32, 32, 0, 0, 0, 0, 0, 0, 4, -72, 0, 0, 0, 32, 60, 109, 99, 100, 62, 60, 77, 115, 100, 62, 106,
            109, 115, 95, 116, 101, 120, 116, 60, 47, 77, 115, 100, 62, 60, 47, 109, 99, 100, 62, 32, 32, 0, 0, 0};
    public static final String TEST_DESTINATION = "queue:///COREDEVREQUESTQ01";
    public static final long TEST_TIME_STAMP = 1342462125398L;
    public static final int TEST_DELIVERY_MODE = 2;
    private static final String MQ_MESSAGE_FORCE_HEADER_PART2 = "T<jms><Dst>" + TEST_DESTINATION + "</Dst><Tms>" + TEST_TIME_STAMP + "</Tms><Dlv>" + TEST_DELIVERY_MODE + "</Dlv></jms>";
    public static final byte[] MQ_MESSAGE_FORCE_HEADER = CONCATENATE(MQ_MESSAGE_FORCE_HEADER_PART1, (MQ_MESSAGE_FORCE_HEADER_PART2).getBytes());

    public static final String MESSAGE_PAYLOAD = "This is the payload.";

    private static byte[] CONCATENATE(@NotNull final byte[] bytes1, @NotNull final byte[] bytes2) {
        byte[] concatenated = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, concatenated, 0, bytes1.length);
        System.arraycopy(bytes2, 0, concatenated, bytes1.length, bytes2.length);
        return concatenated;
    }

    @Before
    public void setup() {
        ClusterPropertyCache clusterPropertyCache = new ClusterPropertyCache();
        ServerConfig.getInstance().setClusterPropertyCache(clusterPropertyCache);
    }

    @After
    public void tearDown() {
        final String DEFAULT_CCSID = "0";
        final String DEFAULT_CONVERT_FLAG = "true";
        setupContentTypeHeaderConfig(DEFAULT_CONVERT_FLAG, DEFAULT_CCSID);
        ConfigFactory.clearCachedConfig();
    }

    /**
     * Setup cluster wide properties for the content type header unit tests
     *
     * @param mqConvertMessageApplicationDataFormatVal Flag to turn on MQ_CONVERT
     * @param mqConversionCCSIDVal MQ CCSID numerical value
     */
    private void setupContentTypeHeaderConfig(String mqConvertMessageApplicationDataFormatVal, String mqConversionCCSIDVal) {
        final ServerConfig sc = ServerConfig.getInstance();
        List<ServerConfig.PropertyRegistrationInfo> list = new ArrayList<>();
        list.add(ServerConfig.PropertyRegistrationInfo.prInfo(
                PARAM_IO_MQ_CONVERT_MESSAGE_APPLICATION_DATA_FORMAT,
                sc.getClusterPropertyName(PARAM_IO_MQ_CONVERT_MESSAGE_APPLICATION_DATA_FORMAT),
                "Testing",
                mqConvertMessageApplicationDataFormatVal));
        list.add(ServerConfig.PropertyRegistrationInfo.prInfo(
                PARAM_IO_MQ_CONVERSION_CCSID,
                sc.getClusterPropertyName(PARAM_IO_MQ_CONVERSION_CCSID),
                "Testing",
                mqConversionCCSIDVal));
        sc.registerServerConfigProperties(list);
    }

    @Test
    public void testGetContentTypeHeaderConversion() throws IOException {
        final String IBM_CCSID_UTF8 = "1208";
        final String CONVERT_FLAG = "true";

        setupContentTypeHeaderConfig(CONVERT_FLAG, IBM_CCSID_UTF8);

        MQMessage mqMessage = new MQMessage();
        mqMessage.characterSet = MqNativeUtils.getConversionCCSID();
        String msgCCSID = String.valueOf(mqMessage.characterSet);

        assertEquals(IBM_CCSID_UTF8, msgCCSID);

        String msgCharset = MqNativeUtils.getContentTypeHeader().getEncoding().toString();

        assertEquals("UTF-8", msgCharset);
    }

    @Test
    public void testGetContentTypeHeaderNoConversion() throws IOException {
        final String IBM_CCSID_UTF8 = "1208";
        final String CONVERT_FLAG = "false";

        setupContentTypeHeaderConfig(CONVERT_FLAG, IBM_CCSID_UTF8);

        MQMessage mqMessage = new MQMessage();
        mqMessage.characterSet = MqNativeUtils.getConversionCCSID();
        String msgCCSID = String.valueOf(mqMessage.characterSet);

        assertEquals("819", msgCCSID);

        String msgCharset = MqNativeUtils.getContentTypeHeader().getEncoding().toString();

        assertEquals("ISO-8859-1", msgCharset);
    }

    @Test(expected = UnsupportedEncodingException.class)
    public void testGetContentTypeHeaderUnsupportedEncoding() throws IOException {
        final String IBM_CCSID_DUMMY = "101";
        final String CONVERT_FLAG = "true";

        setupContentTypeHeaderConfig(CONVERT_FLAG, IBM_CCSID_DUMMY);

        MQMessage mqMessage = new MQMessage();
        mqMessage.characterSet = MqNativeUtils.getConversionCCSID();
        String msgCCSID = String.valueOf(mqMessage.characterSet);

        assertEquals(IBM_CCSID_DUMMY, msgCCSID);

        MqNativeUtils.getContentTypeHeader();
        fail(); // Expected to throw exception above.
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
    public void parsePrimaryMessageHeader() throws IOException, MQDataException {
        // expect no header for PROPCTL=COMPAT
        MQMessage mqMessage = new MQMessage();
        // no exposed header
        mqMessage.write(MESSAGE_PAYLOAD.getBytes());
        assertNull(MqNativeUtils.parsePrimaryAdditionalHeader(mqMessage));

        // expected for PROPCTL=FORCE
        mqMessage = new MQMessage();
        MQRFH2 rfh2 = new MQRFH2(new DataInputStream(new ByteArrayInputStream(MQ_MESSAGE_FORCE_HEADER)));
        rfh2.write(mqMessage);
        mqMessage.format = MQFMT_RF_HEADER_2;
        mqMessage.write(MESSAGE_PAYLOAD.getBytes());
        MQRFH2 primaryMessageHeader = (MQRFH2) MqNativeUtils.parsePrimaryAdditionalHeader(mqMessage);
        assertArrayEquals(rfh2.getNameValueData(), primaryMessageHeader.getNameValueData());
    }

//    @Test
//    public void parseProperties() throws IOException, MQDataException, MQException {
//        // expected properties for PROPCTL=COMPAT
//        MQMessage mqMessage = new MQMessage();
//        mqMessage.setStringProperty(RFH2_JMS_DESTINATION, TEST_DESTINATION);
//        mqMessage.setLongProperty(RFH2_JMS_TIME_STAMP, TEST_TIME_STAMP);
//        mqMessage.setIntProperty(RFH2_JMS_DELIVERY_MODE, TEST_DELIVERY_MODE);
//        mqMessage.write(MESSAGE_PAYLOAD.getBytes());
//        Map<String, Object> properties = MqNativeUtils.parseProperties(mqMessage);
//        assertEquals(TEST_DESTINATION, properties.get(MQ_JMS_DESTINATION));
//        assertEquals(TEST_TIME_STAMP, properties.get(MQ_JMS_TIME_STAMP));
//        assertEquals(TEST_DELIVERY_MODE, properties.get(MQ_JMS_DELIVERY_MODE));
//
//        // expect no properties for PROPCTL=FORCE
//        mqMessage = new MQMessage();
//        MQRFH2 rfh2 = new MQRFH2(new DataInputStream(new ByteArrayInputStream(MQ_MESSAGE_FORCE_HEADER)));
//        rfh2.write(mqMessage);
//        mqMessage.format = MQFMT_RF_HEADER_2;
//        mqMessage.write(MESSAGE_PAYLOAD.getBytes());
//        properties = MqNativeUtils.parseProperties(mqMessage);
//        assertNull(properties.get(MQ_JMS_DESTINATION));
//        assertNull(properties.get(MQ_JMS_TIME_STAMP));
//        assertNull(properties.get(MQ_JMS_DELIVERY_MODE));
//    }

    @Test
    public void buildMqNativeKnob() throws IOException, MQDataException, MQException  {
        final MqNativeMessageDescriptor mqmd = new MqNativeMessageDescriptor(new MQMessage());
        mqmd.messageType = 8;
        mqmd.format = MQFMT_RF_HEADER;
        mqmd.messageId = HexUtils.decodeBase64("QU1RIGNvcmVEZXZRdWV1ZXLDgcOPTzE0");

        final MQRFH2 rfh2 = new MQRFH2(new DataInputStream(new ByteArrayInputStream(MQ_MESSAGE_FORCE_HEADER)));

        final Map<String,Object> messageProperties = new HashMap<String, Object>(3);
        messageProperties.put(RFH2_JMS_DESTINATION, TEST_DESTINATION);
        messageProperties.put(RFH2_JMS_TIME_STAMP, Long.toString(TEST_TIME_STAMP));
        messageProperties.put(RFH2_JMS_DELIVERY_MODE, Integer.toString(TEST_DELIVERY_MODE));

//        MqNativeKnob mqKnob = MqNativeUtils.buildMqNativeKnob(MQ_MESSAGE_FORCE_HEADER, rfh2, mqmd, messageProperties);

//        assertArrayEquals(MQ_MESSAGE_FORCE_HEADER, mqKnob.getAllMessageHeaderBytes());
//        assertEquals(rfh2, mqKnob.getPrimaryMessageHeader());
//        assertEquals(messageProperties, mqKnob.getMessagePropertyMap());
//        assertEquals(mqmd, mqKnob.getMessageDescriptor());
    }

    @Test
    @BugId("DE292277")
    public void testSuppressExceptionWhenDebugStateOff() {
        JdkLoggerConfigurator.configure("com.l7tech.external.assertions.mqnative", "com/l7tech/external/assertions/mqnative/resources/testSuppressExceptionWhenDebugStateOff.properties");

        // When the debug state is false, then the method will return null no matter which reason code is.
        for (Integer reasonCode: MqNativeUtils.EXPECTED_REASON_CODES) {
            final MQException mqException = new MQException(null, null, reasonCode, -1);
            assertNull(MqNativeUtils.getDebugExceptionForExpectedReasonCode(mqException));
        }
    }

    @Test
    @BugId("DE292277")
    public void testGetDebugExceptionForExpectedReasonCode() {
        JdkLoggerConfigurator.configure("com.l7tech.external.assertions.mqnative", "com/l7tech/external/assertions/mqnative/resources/testGetDebugExceptionForExpectedReasonCode.properties");

        // When the debug state is true, then the method should return a MqException with a reason code.
        for (Integer reasonCode: MqNativeUtils.EXPECTED_REASON_CODES) {
            final MQException mqException = new MQException(null, null, reasonCode, -1);
            final MQException resultException = MqNativeUtils.getDebugExceptionForExpectedReasonCode(mqException);
            assertNotNull(resultException);
            assertTrue(resultException.getReason() == reasonCode);
        }
    }
}
