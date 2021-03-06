package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Hashtable;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.closeQuietly;
import static junit.framework.Assert.*;

/**
 * Developer tests to perform various MQ Native tasks against a live MQ server.
 * That is, these tests depend on a running MQ server.  Recommend running these tests manually as a troubleshooting tool.
 */
@Ignore("Developer tests")
public class MqNativeDeveloperTest {

    @Ignore("Developer test")
    @Test
    public void msgPeekPop() {
        MQEnvironment.disableTracing();
        // turn off log (e.g. MQJE001: Completion Code 2, Reason 2033)
        MQException.logExclude(MQRC_NO_MSG_AVAILABLE);

        Hashtable<String, Object> connProps = new Hashtable<String, Object>(20, 0.7f);
        connProps.put(HOST_NAME_PROPERTY, "ibmq6l.l7tech.com");
        connProps.put(PORT_PROPERTY, 7777);
        connProps.put(CHANNEL_PROPERTY, "SYSTEM.DEF.SVRCONN");
        connProps.put(USER_ID_PROPERTY, "mqm");
        connProps.put(PASSWORD_PROPERTY, "7layer");

        MQQueueManager queueManager = null;
        MQQueue queue = null;
        try {
            queueManager = new MQQueueManager("deepQueueManager", connProps);

            final int openOps = MQOO_INPUT_AS_Q_DEF | MQOO_BROWSE | MQOO_INQUIRE;

            // make sure this queue exists
            queue = queueManager.accessQueue("COREDEVREQUESTQ01", openOps);

            MQGetMessageOptions browseFirstOptions = new MQGetMessageOptions();
            browseFirstOptions.options = MQGMO_WAIT | MQGMO_BROWSE_FIRST; // | MQGMO_LOCK;
            browseFirstOptions.waitInterval = 5000;
            MQGetMessageOptions browseNextOpts = new MQGetMessageOptions();
            browseNextOpts.options = MQGMO_WAIT | MQGMO_BROWSE_NEXT; // | MQGMO_LOCK;
            browseNextOpts.waitInterval = 5000;

            // peek at the message
            MQMessage tempReadMsg = new MQMessage();
            queue.get(tempReadMsg, browseFirstOptions);
            byte[] payloadBytes = new byte[tempReadMsg.getTotalMessageLength()];
            tempReadMsg.setDataOffset(0);
            tempReadMsg.readFully(payloadBytes);
            System.out.println(new String(payloadBytes));

            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = MQGMO_WAIT | MQGMO_NO_SYNCPOINT;

            // pop the message
            MQMessage readMsg = new MQMessage();
            queue.get(readMsg, gmo);
            payloadBytes = new byte[readMsg.getTotalMessageLength()];
            readMsg.setDataOffset(0);
            readMsg.readFully(payloadBytes);
            System.out.println(new String(payloadBytes));
        } catch (Exception readEx) {
            if (readEx instanceof MQException && ((MQException) readEx).reasonCode == MQRC_NO_MSG_AVAILABLE) {
                System.out.println("queue is empty");
            } else {
                readEx.printStackTrace();
                fail("Unexpected error testing: " + readEx);
            }
        } finally {
            closeQuietly( queue );
            closeQuietly( queueManager, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void dynamicQueue() {
        MQEnvironment.disableTracing();
        MQException.logExclude(MQRC_NO_MSG_AVAILABLE);

        Hashtable<String, Object> connProps = new Hashtable<String, Object>(20, 0.7f);
        connProps.put(HOST_NAME_PROPERTY, "ibmq6l.l7tech.com");
        connProps.put(PORT_PROPERTY, 7777);
        connProps.put(CHANNEL_PROPERTY, "SYSTEM.DEF.SVRCONN");
        connProps.put(USER_ID_PROPERTY, "mqm");
        connProps.put(PASSWORD_PROPERTY, "7layer");

        MQQueueManager queueManager = null;
        MQQueue dynamicQueue = null;
        try {
            queueManager = new MQQueueManager("deepQueueManager", connProps);

            // temporary dynamic queue - make sure this queue exists
            String modelQName = "COREDEVRESPONSEQ01";
            String dynamicQName = "COREDEVRESPONSEQ01.*";

            dynamicQueue = queueManager.accessQueue(modelQName, MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING, null, dynamicQName, null);
            final MQPutMessageOptions pmo = new MQPutMessageOptions();
            pmo.options = MQPMO_NO_SYNCPOINT;
            MQMessage dynamicWriteMsg = new MQMessage();
            String output = "Simple temporary dynamic queue (" + new Date().toString() + ")";
            dynamicWriteMsg.write(output.getBytes());
            dynamicQueue.put(dynamicWriteMsg, pmo);
            dynamicQueue.close();

            System.out.println("Reading from temporary dynamic queue ...");
            dynamicQueue = queueManager.accessQueue(modelQName, MQOO_INPUT_AS_Q_DEF | MQOO_INQUIRE, null, dynamicQName, null);
            MQMessage dynamicReadMsg = new MQMessage();
            MQGetMessageOptions dynamicGmo = new MQGetMessageOptions();
            dynamicGmo.options = MQGMO_WAIT | MQGMO_NO_SYNCPOINT;
            dynamicQueue.get(dynamicReadMsg, dynamicGmo);
            byte[] payloadBytes = new byte[dynamicReadMsg.getTotalMessageLength()];
            dynamicReadMsg.setDataOffset(0);
            dynamicReadMsg.readFully(payloadBytes);
            System.out.println(new String(payloadBytes));
            dynamicQueue.close();
        } catch (Exception readEx) {
            if (readEx instanceof MQException && ((MQException) readEx).reasonCode == MQRC_NO_MSG_AVAILABLE) {
                System.out.println("queue is empty");
            } else {
                readEx.printStackTrace();
                fail("Unexpected error testing: " + readEx);
            }
        } finally {
            closeQuietly(dynamicQueue);
            closeQuietly( queueManager, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void msgWrite() {
        MQEnvironment.disableTracing();
        MQException.logExclude(MQRC_NO_MSG_AVAILABLE);

        Hashtable<String, Object> connProps = new Hashtable<String, Object>(20, 0.7f);
        connProps.put(HOST_NAME_PROPERTY, "ibmq6l.l7tech.com");
        connProps.put(PORT_PROPERTY, 7777);
        connProps.put(CHANNEL_PROPERTY, "SYSTEM.DEF.SVRCONN");
        connProps.put(USER_ID_PROPERTY, "mqm");
        connProps.put(PASSWORD_PROPERTY, "7layer");

        MQQueueManager queueManager = null;
        MQQueue queue = null;
        try {
            queueManager = new MQQueueManager("deepQueueManager", connProps);

            // make sure this queue exists
            final String queueName = "SYSTEM.DEFAULT.MODEL.QUEUE.4F14B2E020012506";

            queue = queueManager.accessQueue(queueName, MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING);
            final MQPutMessageOptions pmo = new MQPutMessageOptions();
            pmo.options = MQPMO_NO_SYNCPOINT;
            MQMessage dynamicWriteMsg = new MQMessage();
            String output = "Test write to queue (" + new Date().toString() + ")";
            dynamicWriteMsg.write(output.getBytes());
            queue.put(dynamicWriteMsg, pmo);
        } catch (Exception readEx) {
            if (readEx instanceof MQException && ((MQException) readEx).reasonCode == MQRC_NO_MSG_AVAILABLE) {
                System.out.println("queue is empty");
            } else {
                readEx.printStackTrace();
                fail("Unexpected error testing: " + readEx);
            }
        } finally {
            closeQuietly(queue);
            closeQuietly( queueManager, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void correlationIds() {
        MQEnvironment.disableTracing();
        MQException.logExclude(MQRC_NO_MSG_AVAILABLE);

        Hashtable<String, Object> connProps = new Hashtable<String, Object>(20, 0.7f);
        connProps.put(HOST_NAME_PROPERTY, "ibmq6l.l7tech.com");
        connProps.put(PORT_PROPERTY, 7777);
        connProps.put(CHANNEL_PROPERTY, "SYSTEM.DEF.SVRCONN");
        connProps.put(USER_ID_PROPERTY, "mqm");
        connProps.put(PASSWORD_PROPERTY, "7layer");

        MQQueueManager queueManager = null;
        MQQueue queue = null;
        try {
            queueManager = new MQQueueManager("deepQueueManager", connProps);

            // read request message
            queue = queueManager.accessQueue("COREDEVREQUESTQ01", MQOO_INPUT_AS_Q_DEF | MQOO_BROWSE | MQOO_INQUIRE);
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = MQGMO_WAIT | MQGMO_NO_SYNCPOINT;
            MQMessage readMsg = new MQMessage();
            queue.get(readMsg, gmo);
            System.out.println(new String(readMsg.correlationId));
            queue.close();

            // write response message
            queue = queueManager.accessQueue("COREDEVRESPONSEQ01", MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING);
            final MQPutMessageOptions pmo = new MQPutMessageOptions();
            MQMessage writeMsg = new MQMessage();

            //pmo.options = MQPMO_NO_SYNCPOINT | MQRO_PASS_CORREL_ID;
            //byte[] correlationId = readMsg.correlationId;

            pmo.options = MQPMO_NO_SYNCPOINT | MQRO_COPY_MSG_ID_TO_CORREL_ID;

            writeMsg.correlationId = readMsg.messageId;
            String output = "Test write to queue (" + new Date().toString() + ")";
            writeMsg.write(output.getBytes());
            queue.put(writeMsg, pmo);
            System.out.println(new String(writeMsg.correlationId));
            queue.close();
        } catch (Exception readEx) {
            if (readEx instanceof MQException && ((MQException) readEx).reasonCode == MQRC_NO_MSG_AVAILABLE) {
                System.out.println("queue is empty");
            } else {
                readEx.printStackTrace();
                fail("Unexpected error testing: " + readEx);
            }
        } finally {
            closeQuietly( queue );
            closeQuietly( queueManager, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void read() {
        MQQueueManager qmgr = null;
        MQQueue q = null;
        try {
            final String hostname = "vcmqserver.l7tech.com";
            final int port = 1415;
            final String channel = "VCTEST.CHNL";
            final String qmName = "qmgr";
            // final String qName = "VCTEST.Q.IN";
            final String qName = "VCTEST.Q.REPLY";

            Pair<MQQueueManager, MQQueue> connected = connect(hostname, port, channel, qmName, qName);
            qmgr = connected.left;
            q = connected.right;

            // browse some messages
            MQMessage[] msgs = new MQMessage[3];
            try {
                if (q.getCurrentDepth() <= 0)
                    fail("testMqConnection - No messages on queue " + qName);

                for (int i=0; i<msgs.length; i++) {
                    msgs[i] = new MQMessage();

                    MQGetMessageOptions gmo = new MQGetMessageOptions();
                    gmo.matchOptions = MQMO_NONE;
                    gmo.waitInterval = 5000; // 5 sec
                    if (i == 0)
                        gmo.options = MQGMO_WAIT | MQGMO_BROWSE_FIRST;
                    else
                        gmo.options = MQGMO_WAIT | MQGMO_BROWSE_NEXT;

                    q.get(msgs[i], gmo);
                    assertNotNull(msgs[i]);
                    assertTrue(msgs[0].messageId != null && msgs[0].messageId.length > 0);
                    byte[] payload = new byte[msgs[i].getDataLength()];
                    msgs[i].readFully(payload);
                    System.out.println(new String(payload));
                }
            } catch (MQException mqEx) {
                if (mqEx.reasonCode == MQRC_NO_MSG_AVAILABLE)
                    System.out.println("No messages on queue: " + qName);
                else
                    throw mqEx;
            }
        } catch (Throwable th) {
            th.printStackTrace();
            fail("Unexpected error testing MQ connection: " + th);
        } finally {
            closeQuietly(q);
            closeQuietly( qmgr, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void readWithRFH2() {
        MQQueueManager qmgr = null;
        MQQueue q = null;
        try {
            final String hostname = "vcmqserver.l7tech.com";
            final int port = 1415;
            final String channel = "VCTEST.CHNL";
            final String qmName = "qmgr";
            final String qName = "VCTEST.Q.IN";

            Pair<MQQueueManager, MQQueue> connected = connect(hostname, port, channel, qmName, qName);
            qmgr = connected.left;
            q = connected.right;

            // browse some messages
            MQMessage[] msgs = new MQMessage[1];
            try {
                if (q.getCurrentDepth() <= 0)
                    fail("testMqConnection - No messages on queue " + qName);

                for (int i=0; i<msgs.length; i++) {
                    msgs[i] = new MQMessage();

                    MQGetMessageOptions gmo = new MQGetMessageOptions();
                    gmo.matchOptions=MQMO_NONE;
                    gmo.waitInterval = 5000; // 5 sec
                    if (i == 0)
                        gmo.options = MQGMO_WAIT | MQGMO_BROWSE_FIRST;
                    else
                        gmo.options = MQGMO_WAIT | MQGMO_BROWSE_NEXT;

                    q.get(msgs[i], gmo);
                    assertNotNull(msgs[i]);
                    assertTrue(msgs[0].messageId != null && msgs[0].messageId.length > 0);
                    byte[] payload = new byte[msgs[i].getDataLength()];
                    msgs[i].readFully(payload);
                    System.out.println(new String(payload));
                }
            } catch (MQException mqEx) {
                if (mqEx.reasonCode == MQRC_NO_MSG_AVAILABLE)
                    System.out.println("No messages on queue: " + qName);
                else
                    throw mqEx;
            }

        } catch (Throwable th) {
            th.printStackTrace();
            fail("Unexpected error testing MQ connection: " + th);
        } finally {
            closeQuietly(q);
            closeQuietly( qmgr, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void concurrentRead256() {
        MQQueueManager qmgr = null;
        MQQueue q = null;
        try {
            final String hostname = "vcmqserver.l7tech.com";
            final int port = 1415;
            final String channel = "VCTEST.CHNL";
            final String qmName = "qmgr";
            final String qName = "VCTEST.Q.IN";

            Pair<MQQueueManager, MQQueue> connected = connect(hostname, port, channel, qmName, qName);
            qmgr = connected.left;
            q = connected.right;

            MQMessage[] msgs = new MQMessage[600];

            // read first
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.matchOptions = MQMO_NONE;
            gmo.waitInterval = 5000; // 5 sec
            gmo.options = MQGMO_WAIT | MQGMO_BROWSE_FIRST | MQGMO_LOCK;

            msgs[0] = new MQMessage();
            q.get(msgs[0], gmo);
            System.out.println("Msg 0 read: " + HexUtils.encodeBase64(msgs[0].messageId));

            gmo.options = MQGMO_WAIT | MQGMO_BROWSE_NEXT | MQGMO_LOCK;
            for (int i=1; i<msgs.length; i++) {
                // read subsequent
                msgs[i] = new MQMessage();
                q.get(msgs[i], gmo);
                System.out.println("Msg " + i + " read: " + HexUtils.encodeBase64(msgs[i].messageId));
            }

            System.out.println("\n***** removing messages *****\n");

            // remove msgs in order
            MQGetMessageOptions popgmo = new MQGetMessageOptions();
            popgmo.options = MQGMO_WAIT;
            popgmo.waitInterval = 5000; // 5 sec
            popgmo.matchOptions = MQMO_MATCH_MSG_ID;
            MQMessage pop;
            for (int j=0; j<msgs.length; j++) {
                pop = new MQMessage();
                pop.messageId = msgs[j].messageId;
                q.get(pop, popgmo);
                System.out.println("Msg " + j + " popped: " + HexUtils.encodeBase64(pop.messageId));
            }

        } catch (Throwable th) {
            th.printStackTrace();
            fail("Unexpected error testing MQ connection: " + th);
        } finally {
            closeQuietly(q);
            closeQuietly( qmgr, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void concurrentReading() {
        MQQueueManager qmgr = null;
        MQQueue q = null;
        try {
            final String hostname = "vcmqserver.l7tech.com";
            final int port = 1415;
            final String channel = "VCTEST.CHNL";
            final String qmName = "qmgr";
            final String qName = "VCTEST.Q.IN";

            Pair<MQQueueManager, MQQueue> connected = connect(hostname, port, channel, qmName, qName);
            qmgr = connected.left;
            q = connected.right;

            // read first
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.matchOptions=MQMO_NONE;
            gmo.waitInterval = 5000; // 5 sec
            gmo.options = MQGMO_WAIT | MQGMO_BROWSE_FIRST | MQGMO_LOCK;

            MQMessage msg1 = new MQMessage();
            q.get(msg1, gmo);

            System.out.println("Msg 1 read: " + HexUtils.encodeBase64(msg1.messageId));
            // System.out.println(messageToString(msg1, null));

            // read second
            gmo.options = MQGMO_WAIT | MQGMO_BROWSE_NEXT | MQGMO_LOCK;
            MQMessage msg2 = new MQMessage();
            q.get(msg2, gmo);

            System.out.println("Msg 2 read: " + HexUtils.encodeBase64(msg2.messageId));
            // System.out.println(messageToString(msg2, null));

            // read second
            gmo.options = MQGMO_WAIT | MQGMO_BROWSE_NEXT | MQGMO_LOCK;
            MQMessage msg3 = new MQMessage();
            q.get(msg3, gmo);

            System.out.println("Msg 3 read: " + HexUtils.encodeBase64(msg3.messageId));
            // System.out.println(messageToString(msg2, null));

            System.out.println("\n***** removing messages *****\n");

            // remove the 2 messages in order
            MQMessage pop = new MQMessage();
            gmo.options = MQGMO_WAIT;
            gmo.matchOptions = MQMO_MATCH_MSG_ID;
            pop.messageId = msg1.messageId;
            q.get(pop, gmo);
            System.out.println("Msg popped: " + HexUtils.encodeBase64(pop.messageId));
            // System.out.println(messageToString(pop, null));

            MQMessage pop2 = new MQMessage();
            pop2.messageId = msg2.messageId;
            q.get(pop2, gmo);
            System.out.println("Msg popped: " + HexUtils.encodeBase64(pop2.messageId));

            MQMessage pop3 = new MQMessage();
            pop3.messageId = msg3.messageId;
            q.get(pop3, gmo);
            System.out.println("Msg popped: " + HexUtils.encodeBase64(pop3.messageId));

        } catch (Throwable th) {
            th.printStackTrace();
            fail("Unexpected error testing MQ connection: " + th);
        } finally {
            closeQuietly(q);
            closeQuietly( qmgr, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void write() {
        final String testMessage = 
                "<l7:test xmlns:l7=\"com.l7tech.test\">\n" +
                "  <l7:seq>{0}</l7:seq>\n" +
                "  <l7:payload>{1}</l7:payload>\n" +
                "</l7:test>";

        MQQueueManager qmgr = null;
        MQQueue q = null;
        try {
            final String hostname = "vcmqserver.l7tech.com";
            final int port = 1415;
            final String channel = "VCTEST.CHNL";
            final String qmName = "qmgr";
            final String qName = "VCTEST.Q.IN";

            Pair<MQQueueManager, MQQueue> connected = connect(hostname, port, channel, qmName, qName, true);
            qmgr = connected.left;
            q = connected.right;

            // browse some messages
            // MQMessage[] msgs = new MQMessage[ TEST_MSG_COUNT ];
            MQMessage[] msgs = new MQMessage[ 45 ];
            for (int i=0; i<msgs.length; i++) {
                msgs[i] = new MQMessage();
                MQPutMessageOptions pmo = new MQPutMessageOptions();
                pmo.options = MQPMO_NEW_MSG_ID;
                // what else?
                String dasMsg = MessageFormat.format(testMessage, i, "JUnit Test Payload yo!");
                msgs[i].writeString(dasMsg);

                msgs[i].replyToQueueManagerName = "qmgr";
                msgs[i].replyToQueueName = "VCTEST.Q.REPLY";
                // msgs[i].format = MQFMT_NONE;
                msgs[i].format = MQFMT_STRING;
                // msgs[i].format = MQFMT_RF_HEADER_2;
                // msgs[i].format = MQFMT_PCF;
                msgs[i].persistence = 2;
                msgs[i].priority = 5;

                q.put(msgs[i], pmo);
            }
        } catch (Throwable th) {
            th.printStackTrace();
            fail("Unexpected error testing write to MQ queue: " + th);
        } finally {
            closeQuietly(q);
            closeQuietly( qmgr, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                @Override
                public void call(final MQQueueManager mqQueueManager) throws MQException {
                    mqQueueManager.disconnect();
                }
            }) );
        }
    }

    @Ignore("Developer test")
    @Test
    public void buildRFH2Header() throws IOException {

        try {
            String pubCommand = "<psc><Command>Publish</Command><Topic>Stock</Topic>" +
            "<QMgrName>QFLEXT1</QMgrName><QName>QFLEXT1.A</QName></psc>";
            
            int folderLength = pubCommand.length();
            MQMessage msg = new MQMessage();
            msg.format = MQFMT_RF_HEADER_2; // Msg Format
            msg.writeString(MQRFH_STRUC_ID); // StrucId
            msg.writeInt4(MQRFH_VERSION_2); // Version
            msg.writeInt4(MQRFH_STRUC_LENGTH_FIXED_2 + folderLength + 4);
            //4) + rf); // StrucLength
            msg.writeInt4(MQENC_NATIVE); // Encoding
            msg.writeInt4(MQCCSI_DEFAULT); // CodedCharacterSetId
            msg.writeString(MQFMT_NONE); // Format (content)
            msg.writeInt4(MQRFH_NO_FLAGS); // Flags
            msg.writeInt4(1208); // NameValueCCSID = UTF-8
            msg.writeInt4(folderLength);
            msg.writeString(pubCommand);
            msg.writeInt4(folderLength);
            msg.writeString(pubCommand);
            msg.writeInt4(folderLength);
            msg.writeString(pubCommand);
            msg.writeString("begin payload");

            byte[] payload = new byte[msg.getDataLength()];
            msg.readFully(payload);
            System.out.println(new String(payload));
        } catch (Throwable th) {
            th.printStackTrace();
            fail("Unexpected error testing build RHF2 header: " + th);
        }
    }

    private Pair<MQQueueManager, MQQueue> connect(final String hostname,
                                                  final int port,
                                                  final String channel,
                                                  final String qmName,
                                                  final String qName) throws MQException {
        return connect(hostname, port, channel, qmName, qName, false);
    }

    private Pair<MQQueueManager, MQQueue> connect(final String hostname,
                                                  final int port,
                                                  final String channel,
                                                  final String qmName,
                                                  final String qName,
                                                  boolean forWrite) throws MQException {
        Hashtable<String, Object> connProps = new Hashtable<String, Object>(10);
        connProps.put(HOST_NAME_PROPERTY, hostname);
        connProps.put(PORT_PROPERTY, port);
        connProps.put(CHANNEL_PROPERTY, channel);
        // connProps.put(USER_ID_PROPERTY, "");
        // connProps.put(PASSWORD_PROPERTY, "");
        MQQueueManager qmgr = new MQQueueManager(qmName, connProps);

        // int openOps = MQOO_BROWSE;
        int oops;
        if (forWrite)
            oops = MQOO_OUTPUT;
        else
            oops = MQOO_INPUT_SHARED | MQOO_BROWSE | MQOO_INQUIRE;

        MQQueue q = qmgr.accessQueue(qName, oops);
        // alternate method ...
        // MQQueue q = new MQQueue(qmgr, qName, openOps, "qmgr", null, null);

        assertNotNull("Qmgr is null", qmgr);
        assertTrue("Not connect to MQQueueManager", qmgr.isConnected());
        assertNotNull("Queue is null", q);

        return new Pair<MQQueueManager, MQQueue>(qmgr, q);
    }
}
