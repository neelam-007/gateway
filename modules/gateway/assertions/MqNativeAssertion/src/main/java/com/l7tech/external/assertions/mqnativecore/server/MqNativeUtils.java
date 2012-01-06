package com.l7tech.external.assertions.mqnativecore.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQMessage;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.mqnativecore.MqNativeConstants.MQ_PROPERTY_APPDATA;
import static com.l7tech.external.assertions.mqnativecore.MqNativeConstants.MQ_PROPERTY_APPORIGIN;

/**
 * MQ Native connector helper class.
 */
public class MqNativeUtils {
    private static final Logger logger = Logger.getLogger(MqNativeUtils.class.getName());

    /*
       Sample RFH2 header builder
       theMsg.format = MQC.MQFMT_RF_HEADER_2; // Msg Format
       theMsg.writeString(MQC.MQRFH_STRUC_ID); // StrucId
       theMsg.writeInt4(MQC.MQRFH_VERSION_2); // Version
       theMsg.writeInt4(MQC.MQRFH_STRUC_LENGTH_FIXED_2 + folderLength + 4);
       //4) + rf); // StrucLength
       theMsg.writeInt4(MQC.MQENC_NATIVE); // Encoding
       theMsg.writeInt4(MQC.MQCCSI_DEFAULT); // CodedCharacterSetId
       theMsg.writeString(MQC.MQFMT_NONE); // Format (content)
       theMsg.writeInt4(MQC.MQRFH_NO_FLAGS); // Flags
       theMsg.writeInt4(1208); // NameValueCCSID = UTF-8
       theMsg.writeInt4(folderLength);
       theMsg.writeString(pubCommand);
       theMsg.writeString("begin payload");
    */
    static class RFH2Header
    {
        String structId;
        int version;
        int length;
        int encoding;
        int ccsid;
        String format;
        int flags;
        int nameValueCCSID;
    }

    public static Pair<byte[], byte[]> parseHeader(MQMessage msg) throws IOException {
        byte[] headType = new byte[4];
        boolean hasHeader = false;

        // Parse RFH header
        RFH2Header rfh = new RFH2Header();
        if (msg.getTotalMessageLength() > 4) {
            msg.setDataOffset(0);
            msg.readFully(headType, 0, 4);

            if ( MQC.MQRFH_STRUC_ID.equals(new String(headType)) ) {
                hasHeader = true;
                rfh.structId = new String(headType);
                rfh.version = msg.readInt4();
                if (rfh.version == 2) {
                    // RFH 2
                    rfh.length = msg.readInt4();
                    rfh.encoding = msg.readInt4();
                    rfh.ccsid = msg.readInt4();
                    rfh.format = msg.readStringOfByteLength(8);
                    rfh.flags = msg.readInt4();
                    rfh.nameValueCCSID = msg.readInt4();
                } else {
                    // RFH 1
                    rfh.length = msg.readInt4();
                }
            }
        }

        byte[] headerBytes;
        byte[] payloadBytes;
        if (hasHeader) {

            headerBytes = new byte[rfh.length];
            msg.seek(0);
            msg.readFully(headerBytes);

            payloadBytes = new byte[msg.getTotalMessageLength() - rfh.length];
            msg.seek(rfh.length);
            msg.readFully(payloadBytes);

        } else {
            headerBytes = new byte[0];
            payloadBytes = new byte[msg.getTotalMessageLength()];
            msg.setDataOffset(0);
            msg.readFully(payloadBytes);
        }

        return new Pair<byte[], byte[]>(headerBytes, payloadBytes);
    }

    /*
     * Create an MqNativeKnob.
     */
    public static MqNativeKnob buildMqNativeKnob( final Map<String, Object> requestMsgProps,
                                                  final String soapAction,
                                                  final byte[] mqHeader) {
        return new MqNativeKnob() {
            @Override
            public boolean isBytesMessage() {
                return false; // mqRequest instanceof BytesMessage;
            }
            @Override
            public Map<String, Object> getJmsMsgPropMap() {
            /*
               vchan
               - this is not implemented until we need to map MQ user defined props to JMS props
               - here, we would need to have parsed the custom header folders
            */
                return Collections.unmodifiableMap(new HashMap<String, Object>());
            }
            @Override
            public String getSoapAction() {
                return soapAction;
            }
            @Override
            public long getServiceOid() {
                return -1L;
            }
            @Override
            public byte[] getMessageHeaderBytes() {
                if (mqHeader != null)
                    return mqHeader;
                return new byte[0];
            }
            @Override
            public int getMessageHeaderLength() {
                if (mqHeader != null)
                    return mqHeader.length;
                return 0;
            }
        };
    }

    public static MQMessage buildMqMessage(SsgActiveConnector connector) throws MqNativeException {
        MQMessage mqMessage = new MQMessage();
        mqMessage.applicationIdData = connector.getProperty(MQ_PROPERTY_APPDATA);
        mqMessage.applicationOriginData = connector.getProperty(MQ_PROPERTY_APPORIGIN);
        /*mqMessage.characterSet = connector.getProperty(MqNativeConstants.);
        mqMessage.encoding = connector.getProperty(MqNativeConstants.);
        mqMessage.expiry = connector.getProperty(MqNativeConstants.);
        mqMessage.feedback = connector.getProperty(MqNativeConstants.);
        mqMessage.format = connector.getProperty(MqNativeConstants.);
        mqMessage.groupId = connector.getProperty(MqNativeConstants.);
        mqMessage. = connector.getProperty(MqNativeConstants.);
        mqMessage. = connector.getProperty(MqNativeConstants.);


        Properties properties = mqResource.getProperties();
        for(Object key : properties.keySet())
        {
            if(key == null)
                continue;

            String keyString = (String)key;
            Object value = properties.get(key);

            try{
                if(keyString.equals(MqNativeConstants.MQ_PROPERTY_APPDATA))
                    mqMessage.applicationIdData = ToString(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_APPORIGIN))
                    mqMessage.applicationOriginData = ToString(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_CHARSET))
                    mqMessage.characterSet = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_ENCODING))
                    mqMessage.encoding = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_EXPIRY))
                    mqMessage.expiry = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_FEEDBACK))
                    mqMessage.feedback = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_FORMAT))
                    mqMessage.format = ToString(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_GROUPID))
                    mqMessage.groupId = ToByteArr(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_MSG_FLAGS))
                    mqMessage.messageFlags = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_MSG_SEQNUM))
                    mqMessage.messageSequenceNumber = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_MSG_TYPE))
                    mqMessage.messageType = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_OFFSET))
                    mqMessage.offset = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_PERSISTENCE))
                    mqMessage.persistence = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_PRIORITY))
                    mqMessage.priority = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_APPNAME))
                    mqMessage.putApplicationName = ToString(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_APPTYPE))
                    mqMessage.putApplicationType = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_REPORT))
                    mqMessage.report = ToInt(value);
                else if(keyString.equals(MqConstants.MQ_PROPERTY_USERID))
                    mqMessage.userId = ToString(value);
            }catch(IllegalArgumentException ex){
                String message = "Unable to set property:"+keyString+" value:"+value;
                logger.warning(message);
                throw new MqConfigException(message,ex);
            }
        }*/
        return mqMessage;
    }

    public static boolean isTransactional(SsgActiveConnector connector) {
        boolean isTransactional = false;
        try {
            isTransactional = JmsAcknowledgementType.ON_COMPLETION == JmsAcknowledgementType.valueOf(
                    connector.getProperty(SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE));
        } catch (IllegalArgumentException e) {
            // not transactional
        }
        return isTransactional;
    }
}
