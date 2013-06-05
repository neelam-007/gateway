package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.MQRFH;
import com.ibm.mq.headers.MQRFH2;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import static com.ibm.mq.constants.MQConstants.*;

public class WriteMessageToQueueTool {

    public static void main(String[] arg) {
        final String host = "ibmq71.l7tech.com";
        final String queueManagerName = "awTestQueueManager";

        Hashtable<String, Object> connProps = new Hashtable<String, Object>(20, 0.7f);
        connProps.put(HOST_NAME_PROPERTY, host);
        connProps.put(PORT_PROPERTY, 1491);
        connProps.put(CHANNEL_PROPERTY, "COREDEVCHANNEL");
        connProps.put(USER_ID_PROPERTY, "mqm");
        connProps.put(PASSWORD_PROPERTY, "7layer");

        MQQueueManager queueManager = null;
        MQQueue queue = null;
        //final String queueName = "AWTESTREPLYQUEUE";
        final String queueName = "AWTESTREQUESTQUEUE";

        // number messages to create
        for (int i = 0; i < 1; i++) {
            try {
                queueManager = new MQQueueManager(queueManagerName, connProps);
                queue = queueManager.accessQueue(queueName, MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING | MQOO_SET_ALL_CONTEXT);
                final MQPutMessageOptions pmo = new MQPutMessageOptions();
                pmo.options = MQPMO_NO_SYNCPOINT | MQPMO_SET_ALL_CONTEXT;

                /*
                queue = queueManager.accessQueue(queueName, MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING);
                final MQPutMessageOptions pmo = new MQPutMessageOptions();
                pmo.options = MQPMO_NO_SYNCPOINT;
                */


                MQMessage writeMsg = new MQMessage();

                writeMsg.accountingToken = "test".getBytes();
                writeMsg.applicationIdData = "test";
                writeMsg.applicationOriginData = "test";
                writeMsg.backoutCount = 2;
                writeMsg.messageSequenceNumber = 2;
                writeMsg.originalLength = 2;
                writeMsg.putApplicationName ="test";
                GregorianCalendar gc = new GregorianCalendar();
                gc.set(1900, 5, 1);
                writeMsg.putDateTime = gc;
                writeMsg.userId = "test";



                writeMsg.replyToQueueManagerName = "test";
                writeMsg.replyToQueueName ="test";
                writeMsg.expiry = 3000;
                writeMsg.feedback = MQC.MQFB_EXPIRATION;
                writeMsg.report = MQC.MQRO_EXPIRATION;
                writeMsg.priority = 2;
                writeMsg.persistence = 2;
                writeMsg.characterSet = 1208;
                writeMsg.correlationId = "test".getBytes();
                writeMsg.messageId = "test".getBytes();
                writeMsg.groupId = "test".getBytes();
                writeMsg.messageType = MQC.MQMT_REQUEST;


                writeMsg.messageFlags = MQC.MQMF_SEGMENT;
                writeMsg.offset = 3; //Can be set only when messageFlags = SEGMENT




                // set a RFH1 header
                /*
                writeMsg.format = MQFMT_RF_HEADER_1;
                MQRFH rfh= new MQRFH();
                rfh.addNameValuePair("rfhField1","rhfValue1");
                rfh.addNameValuePair("folder.rfhField2","rhfValue2");
                //rfh.write(writeMsg);
                */

                // set a RFH2 header
                //writeMsg.format = MQFMT_RF_HEADER_2;
                MQRFH2 rfh2= new MQRFH2();
                rfh2.setFieldValue("folder", "rfh2Field1","rhf2Value1");
                rfh2.setFieldValue("folder", "rfh2Field2","rhf2Value2");
                //rfh2.setFieldValue("usr", "rfh2Field3","rhf2Value3");
                //rfh2.write(writeMsg);

                /*
                writeMsg.format = MQFMT_RF_HEADER_2;
                MQRFH2 rfhMulti= new MQRFH2();
                rfhMulti.setFieldValue("folder", "rfh2Field","rhf2Value");
                //rfhMulti.write(writeMsg);
                */

                MQHeaderList headerList = new MQHeaderList();
                headerList.add(rfh2);
                //headerList.add(rfhMulti);
                headerList.write(writeMsg);


                // set no header (message properties)
                writeMsg.setLongProperty("folder.testLongProperty", Long.MAX_VALUE);
                writeMsg.setStringProperty("folder.propertyField1", "propertyValue1");
                //writeMsg.setObjectProperty("folder.testObjectProperty", new Exception("testObjectValueAsException"));

                // set data
                String output = "Written to queue by MQ v7.1 client (" + new Date().toString() + ")";

                writeMsg.write(output.getBytes());
                queue.put(writeMsg, pmo);
                System.out.println("Wrote message with properties to host: " + host + ", manager: " + queueManagerName +", queue: " + queueName + ".");
            } catch (Exception readEx) {
                if (readEx instanceof MQException && ((MQException) readEx).reasonCode == MQRC_NO_MSG_AVAILABLE) {
                    System.out.println("queue is empty");
                } else {
                    readEx.printStackTrace();
                }
            } finally {
                try {
                    queue.close();
                    queueManager.disconnect();
                    queueManager.close();
                } catch (MQException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
