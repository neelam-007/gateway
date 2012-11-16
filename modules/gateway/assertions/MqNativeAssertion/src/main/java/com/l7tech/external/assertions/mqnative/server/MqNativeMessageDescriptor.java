package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQMD;
import com.ibm.mq.MQMessage;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Apply given descriptors.
     * @param descriptors descriptors to apply
     * @throws MqNativeConfigException
     */
    public void applyDescriptors(@Nullable final Map<String, String> descriptors,
                                 @Nullable final Map<String, Object> contextVariableMap,
                                 @Nullable final Audit audit) throws MqNativeConfigException {
        if (descriptors != null) {
            for( final Map.Entry<String,String> propertyEntry : descriptors.entrySet() ) {
                String name;
                String value;
                if (contextVariableMap != null && audit != null) {
                    name = ExpandVariables.process(propertyEntry.getKey(), contextVariableMap, audit);
                    value = ExpandVariables.process(propertyEntry.getValue(), contextVariableMap, audit);
                } else {
                    name = propertyEntry.getKey();
                    value = propertyEntry.getValue();
                }

                if( MQ_PROPERTY_APPDATA.equals( name ) ) {
                    applicationIdData = value;
                } else if( MQ_PROPERTY_APPORIGIN.equals( name ) ) {
                    applicationOriginData = value;
                } else if( MQ_PROPERTY_CHARSET.equals( name ) ) {
                    characterSet = asInt( name, value );
                } else if( MQ_PROPERTY_ENCODING.equals( name ) ){
                    encoding = asInt( name, value );
                } else if( MQ_PROPERTY_EXPIRY.equals( name ) ) {
                    expiry = asInt( name, value );
                } else if( MQ_PROPERTY_FEEDBACK.equals( name ) ) {
                    feedback = asInt( name, value );
                } else if( MQ_PROPERTY_FORMAT.equals( name ) ) {
                    format = value;
                } else if( MQ_PROPERTY_GROUPID.equals( name ) ) {
                    groupId = asBytes( value );
                } else if( MQ_PROPERTY_MSG_FLAGS.equals( name ) ) {
                    messageFlags = asInt( name, value );
                } else if( MQ_PROPERTY_MSG_SEQNUM.equals( name ) ){
                    messageSequenceNumber = asInt( name, value );
                } else if( MQ_PROPERTY_MSG_TYPE.equals( name ) ){
                    messageType = asInt( name, value );
                } else if( MQ_PROPERTY_OFFSET.equals( name ) ) {
                    offset = asInt( name, value );
                } else if( MQ_PROPERTY_PERSISTENCE.equals( name ) ) {
                    persistence = asInt( name, value );
                } else if( MQ_PROPERTY_PRIORITY.equals( name ) ){
                    priority = asInt( name, value );
                } else if( MQ_PROPERTY_APPNAME.equals( name ) ){
                    putApplicationName = value;
                } else if( MQ_PROPERTY_APPTYPE.equals( name ) ) {
                    putApplicationType = asInt( name, value );
                } else if( MQ_PROPERTY_REPORT.equals( name ) ){
                    report = asInt( name, value );
                } else if( MQ_PROPERTY_USERID.equals( name ) ) {
                    userId = value;
                } else if( MQ_PROPERTY_BACKOUT_COUNT.equals( name ) ) {
                    backoutCount = asInt( name, value );
                } else if( MQ_PROPERTY_REPLY_TO_QUEUE_NAME.equals( name ) ) {
                    replyToQueueName = value;
                } else if( MQ_PROPERTY_REPLY_TO_QUEUE_MGR_NAME.equals( name ) ) {
                    replyToQueueManagerName = value;
                } else if( MQ_PROPERTY_ORIGINAL_LENGTH.equals( name ) ) {
                    originalLength = asInt(name, value);
                } else if( MQ_PROPERTY_VERSION.equals( name ) ) {
                    try {
                        setVersion( asInt( name, value ) );
                    } catch (MQException e) {
                        throw new MqNativeConfigException("Error setting version: ", e);
                    }
                } else if( MQ_PROPERTY_MSG_ID.equals( name ) ) {
                    messageId = asBytes( value );
                } else if( MQ_PROPERTY_CORRELATION_ID.equals( name ) ) {
                    correlationId = asBytes( value );
                } else if( MQ_PROPERTY_ACCOUNTING_TOKEN.equals( name ) ) {
                    accountingToken = asBytes( value );
                } // else not a message property
            }
        }
    }

    /**
     * Apply given descriptors to the MQMessage.
     * @param descriptors descriptors to apply
     * @param mqMessage The MQ message (destination)
     * @throws MqNativeConfigException
     */
    public static void applyDescriptorsToMessage(@Nullable final Map<String, String> descriptors,
                                                 @NotNull final MQMessage mqMessage,
                                                 @Nullable final Map<String, Object> contextVariableMap,
                                                 @Nullable final Audit audit) throws MqNativeConfigException {
        if (descriptors != null) {
            for( final Map.Entry<String,String> propertyEntry : descriptors.entrySet() ) {
                String name;
                String value;
                if (contextVariableMap != null && audit != null) {
                    name = ExpandVariables.process(propertyEntry.getKey(), contextVariableMap, audit);
                    value = ExpandVariables.process(propertyEntry.getValue(), contextVariableMap, audit);
                } else {
                    name = propertyEntry.getKey();
                    value = propertyEntry.getValue();
                }

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
                } else if( MQ_PROPERTY_BACKOUT_COUNT.equals( name ) ) {
                    mqMessage.backoutCount = asInt( name, value );
                } else if( MQ_PROPERTY_REPLY_TO_QUEUE_NAME.equals( name ) ) {
                    mqMessage.replyToQueueName = value;
                } else if( MQ_PROPERTY_REPLY_TO_QUEUE_MGR_NAME.equals( name ) ) {
                    mqMessage.replyToQueueManagerName = value;
                } else if( MQ_PROPERTY_ORIGINAL_LENGTH.equals( name ) ) {
                    mqMessage.originalLength = asInt( name, value );
                } else if( MQ_PROPERTY_VERSION.equals( name ) ) {
                    try {
                        mqMessage.setVersion(asInt(name, value));
                    } catch (MQException e) {
                        throw new MqNativeConfigException("Error setting version: ", e);
                    }
                } else if( MQ_PROPERTY_MSG_ID.equals( name ) ) {
                    mqMessage.messageId = asBytes( value );
                } else if( MQ_PROPERTY_CORRELATION_ID.equals( name ) ) {
                    mqMessage.correlationId = asBytes( value );
                } else if( MQ_PROPERTY_ACCOUNTING_TOKEN.equals( name ) ) {
                    mqMessage.accountingToken = asBytes( value );
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
