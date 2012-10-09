package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMD;
import com.ibm.mq.MQMessage;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.GregorianCalendar;
import java.util.Map;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;

/**
 * This class provides convenience methods for accessing the MQMD portion of an MQMessage object.
 */
public class MqNativeMessageDescriptor extends MQMD {

    public MqNativeMessageDescriptor(final MQMessage mqMessage) throws MQException{
        copyFrom(mqMessage);
    }

    /**
     * Copy contents of this instance to the specified MQMessage.
     * @param mqMessage The MQ message (destination)
     * @throws com.ibm.mq.MQException
     */
    public void copyTo(final MQMessage mqMessage) throws MQException {
        mqMessage.report = report;
        mqMessage.messageType = messageType;
        mqMessage.expiry = expiry;
        mqMessage.feedback = feedback;
        mqMessage.encoding = encoding;
        mqMessage.characterSet = characterSet;
        mqMessage.format = format;
        mqMessage.priority = priority;
        mqMessage.persistence = persistence;
        mqMessage.messageId = messageId;
        mqMessage.correlationId = correlationId;
        mqMessage.backoutCount = backoutCount;
        mqMessage.replyToQueueName = replyToQueueName;
        mqMessage.replyToQueueManagerName = replyToQueueManagerName;
        mqMessage.userId = userId;
        mqMessage.accountingToken = accountingToken;
        mqMessage.applicationIdData = applicationIdData;
        mqMessage.putApplicationType = putApplicationType;
        mqMessage.putApplicationName = putApplicationName;
        mqMessage.putDateTime = putDateTime;
        mqMessage.applicationOriginData = applicationOriginData;
        mqMessage.groupId = groupId;
        mqMessage.messageSequenceNumber = messageSequenceNumber;
        mqMessage.offset = offset;
        mqMessage.messageFlags = messageFlags;
        mqMessage.originalLength = originalLength;
        mqMessage.setVersion(getVersion());
    }

    /**
     * Copy the contents from the specified MQ message to this instance.
     * @param mqMessage The MQ message (source)
     */
    public void copyFrom(final MQMessage mqMessage) throws MQException{
        report = mqMessage.report;
        messageType = mqMessage.messageType;
        expiry = mqMessage.expiry;
        feedback = mqMessage.feedback;
        encoding = mqMessage.encoding;
        characterSet = mqMessage.characterSet;
        format = mqMessage.format;
        priority = mqMessage.priority;
        persistence = mqMessage.persistence;
        messageId = mqMessage.messageId;
        correlationId = mqMessage.correlationId;
        backoutCount = mqMessage.backoutCount;
        replyToQueueName = mqMessage.replyToQueueName;
        replyToQueueManagerName = mqMessage.replyToQueueManagerName;
        userId = mqMessage.userId;
        accountingToken = mqMessage.accountingToken;
        applicationIdData = mqMessage.applicationIdData;
        putApplicationType = mqMessage.putApplicationType;
        putApplicationName = mqMessage.putApplicationName;
        putDateTime = mqMessage.putDateTime;
        applicationOriginData = mqMessage.applicationOriginData;
        groupId = mqMessage.groupId;
        messageSequenceNumber = mqMessage.messageSequenceNumber;
        offset = mqMessage.offset;
        messageFlags = mqMessage.messageFlags;
        originalLength = mqMessage.originalLength;
        setVersion(mqMessage.getVersion());
    }

    /**
     * Apply given properties to the MQMessage.
     * @param properties properties to apply
     * @param mqMessage The MQ message (destination)
     * @throws MqNativeConfigException
     */
    public static void applyPropertiesToMessage(@Nullable final Map<String,String> properties,
                                                @NotNull final MQMessage mqMessage ) throws MqNativeConfigException {
        if (properties != null) {
            for( final Map.Entry<String,String> propertyEntry : properties.entrySet() ) {
                final String name = propertyEntry.getKey();
                final String value = propertyEntry.getValue();

                if( MQ_PROPERTY_APPDATA.equals( name ) ) {
                    mqMessage.applicationIdData = value;
                } else if( MQ_PROPERTY_APPORIGIN.equals( name ) ) {
                    mqMessage.applicationOriginData = value;
                } else if( MQ_PROPERTY_CHARSET.equals( name ) ) {
                    mqMessage.characterSet = asInt( name, value );
                } else if( MQ_PROPERTY_ENCODING.equals( name ) ){
                    mqMessage.encoding = asInt( name, value );
                } else if( MQ_PROPERTY_EXPIRY.equals( name ) ) {
                    mqMessage.expiry = asInt( name, value );
                } else if( MQ_PROPERTY_FEEDBACK.equals( name ) ) {
                    mqMessage.feedback = asInt( name, value );
                } else if( MQ_PROPERTY_FORMAT.equals( name ) ) {
                    mqMessage.format = value;
                } else if( MQ_PROPERTY_GROUPID.equals( name ) ) {
                    mqMessage.groupId = asBytes( value );
                } else if( MQ_PROPERTY_MSG_FLAGS.equals( name ) ) {
                    mqMessage.messageFlags = asInt( name, value );
                } else if( MQ_PROPERTY_MSG_SEQNUM.equals( name ) ){
                    mqMessage.messageSequenceNumber = asInt( name, value );
                } else if( MQ_PROPERTY_MSG_TYPE.equals( name ) ){
                    mqMessage.messageType = asInt( name, value );
                } else if( MQ_PROPERTY_OFFSET.equals( name ) ) {
                    mqMessage.offset = asInt( name, value );
                } else if( MQ_PROPERTY_PERSISTENCE.equals( name ) ) {
                    mqMessage.persistence = asInt( name, value );
                } else if( MQ_PROPERTY_PRIORITY.equals( name ) ){
                    mqMessage.priority = asInt( name, value );
                } else if( MQ_PROPERTY_APPNAME.equals( name ) ){
                    mqMessage.putApplicationName = value;
                } else if( MQ_PROPERTY_APPTYPE.equals( name ) ) {
                    mqMessage.putApplicationType = asInt( name, value );
                } else if( MQ_PROPERTY_REPORT.equals( name ) ){
                    mqMessage.report = asInt( name, value );
                } else if( MQ_PROPERTY_USERID.equals( name ) ) {
                    mqMessage.userId = value;
                } // else not a message property
            }
        }
    }

    private static int asInt( final String name, final String value ) throws MqNativeConfigException {
        try {
            return Integer.parseInt( value );
        } catch ( NumberFormatException nfe ) {
            throw new MqNativeConfigException( "Invalid value '" + value + "' for property '" + name + "'" );
        }
    }

    private static byte[] asBytes( final String value ) {
        return HexUtils.decodeBase64(value);
    }
}
