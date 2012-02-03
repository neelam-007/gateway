package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQManagedObject;
import com.ibm.mq.MQMessage;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.AnonymousSslClientSocketFactory;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsSslCustomizerSupport;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoidThrows;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import com.l7tech.util.Pair;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.ValidationUtils.getMinMaxPredicate;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

/**
 * MQ Native connector helper class.
 */
class MqNativeUtils {
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

    static Pair<byte[], byte[]> parseHeader(MQMessage msg) throws IOException {
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

    static Option<String> getQueuePassword( final SsgActiveConnector connector,
                                            final SecurePasswordManager securePasswordManager ) {
        Option<String> password = none();

        if ( connector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED) ) {
            final long passwordOid = connector.getLongProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID, -1L);
            password = passwordOid == -1L ?
                    password :
                    some( getDecryptedPassword( securePasswordManager, passwordOid ) );
        }

        return password;
    }

    static Hashtable buildQueueManagerConnectProperties( final SsgActiveConnector connector,
                                                         final SecurePasswordManager securePasswordManager ) throws MqNativeConfigException {
        return buildQueueManagerConnectProperties( connector, getQueuePassword( connector, securePasswordManager ) );
    }

    private static <T> T validated( final T value,
                                    final String errorMessage,
                                    final Unary<Boolean,T> validator ) throws MqNativeConfigException {
        if ( !validator.call( value ) ) {
            throw new MqNativeConfigException( errorMessage );
        }
        return value;
    }

    static Hashtable buildQueueManagerConnectProperties( final SsgActiveConnector connector,
                                                         final Option<String> password ) throws MqNativeConfigException {
        Hashtable<String, Object> connProps = new Hashtable<String, Object>();
        connProps.put(MQC.HOST_NAME_PROPERTY,
                validated( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_HOST_NAME ), "Host is required", isNotEmpty() ));
        connProps.put(MQC.PORT_PROPERTY,
                validated( connector.getIntegerProperty( PROPERTIES_KEY_MQ_NATIVE_PORT, -1 ), "Port is invalid", getMinMaxPredicate( 1, 65535 ) ));
        connProps.put(MQC.CHANNEL_PROPERTY,
                validated( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_CHANNEL ), "Channel is required", isNotEmpty() ));

        // apply userId and password
        if ( connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED )) {
            final String userId = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_USERID );
            if (!StringUtils.isEmpty( userId )) {
                connProps.put(MQC.USER_ID_PROPERTY, userId);
            }
            if (!StringUtils.isEmpty(password.toNull())) {
                connProps.put(MQC.PASSWORD_PROPERTY, password.some());
            }
        }

        // apply SSL configuration
        if ( connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED )) {
            try {
                final String cipherSuite = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_CIPHER_SUITE );
                if (StringUtils.isEmpty(cipherSuite)) {
                    logger.log( Level.WARNING, "The cipher suite was not set for the connection!");
                }

                final boolean clientAuth = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED );
                final String alias = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS );
                final String skid = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID );
                final SSLSocketFactory socketFactory;
                if (alias != null && skid != null) {
                    socketFactory = JmsSslCustomizerSupport.getSocketFactory( skid, alias );
                }
                else if (clientAuth) {
                    socketFactory = SslClientSocketFactory.getDefault();
                }
                else {
                    socketFactory = AnonymousSslClientSocketFactory.getDefault();
                }

                // set the socket factory on the MQEnvironment with the cipher suite.
                if (socketFactory != null) {
                    connProps.put(MQC.SSL_CIPHER_SUITE_PROPERTY, cipherSuite);
                    connProps.put(MQC.SSL_SOCKET_FACTORY_PROPERTY, socketFactory);
                }
            } catch( JmsConfigException e ) {
                throw new MqNativeConfigException( ExceptionUtils.getMessage( e ), e.getCause() );
            }
        }

        return connProps;
    }

    /*
     * Create an MqNativeKnob.
     */
    static MqNativeKnob buildMqNativeKnob( final byte[] mqHeader ) {
        return buildMqNativeKnob( null, mqHeader );
    }

    /*
     * Create an MqNativeKnob.
     */
    static MqNativeKnob buildMqNativeKnob( @Nullable final String soapAction,
                                           @Nullable final byte[] mqHeader) {
        return new MqNativeKnob() {
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

    static MQMessage buildMqMessage(SsgActiveConnector connector) throws MqNativeException {
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

    static void applyPropertiesToMessage( @NotNull final MQMessage mqMessage,
                                          @NotNull final Map<String,String> properties ) throws MqNativeConfigException {
        for( final Entry<String,String> propertyEntry : properties.entrySet() ) {
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

    static boolean isTransactional(SsgActiveConnector connector) {
        return MqNativeAcknowledgementType.ON_COMPLETION ==
                    connector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE,null,MqNativeAcknowledgementType.class);
    }

    static void closeQuietly( final MQManagedObject object ) {
        closeQuietly( object, Option.<UnaryVoidThrows<MQManagedObject,MQException>>none() );
    }

    static <T extends MQManagedObject> void closeQuietly( final T object,
                                                          final Option<UnaryVoidThrows<T,MQException>> callback ) {
        if ( object != null ) {
            if ( callback.isSome() ) {
                try {
                    callback.some().call( object );
                } catch (MQException e) {
                    if ( logger.isLoggable( Level.FINE ) ) {
                        logger.log( Level.FINE,
                                "Error pre-closing MQ object: " + ExceptionUtils.getMessage( e ),
                                ExceptionUtils.getDebugException( e ) );
                    }
                }
            }


            try {
                object.close();
            } catch (MQException e) {
                if ( logger.isLoggable( Level.FINE ) ) {
                    logger.log( Level.FINE,
                            "Error closing MQ object: " + ExceptionUtils.getMessage( e ),
                            ExceptionUtils.getDebugException( e ) );
                }
            }
        }
    }



    private static SecurePassword getSecurePassword( final SecurePasswordManager securePasswordManager,
                                                     final long passwordOid ) {
        SecurePassword securePassword = null;
        try {
            securePassword = securePasswordManager.findByPrimaryKey(passwordOid);
        } catch (FindException fe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( fe ), ExceptionUtils.getDebugException( fe ) );
        }
        return securePassword;
    }

    private static String getDecryptedPassword( final SecurePasswordManager securePasswordManager,
                                                final long passwordOid ) {
        String decrypted = null;
        try {
            final SecurePassword securePassword = getSecurePassword( securePasswordManager, passwordOid );
            if ( securePassword != null ) {
                final String encrypted = securePassword.getEncodedPassword();
                final char[] pwd = securePasswordManager.decryptPassword(encrypted);
                decrypted = new String(pwd);
            }
        } catch (ParseException pe) {
            logger.log( Level.WARNING, "The password could not be parsed, the stored password is corrupted. "
                    + ExceptionUtils.getMessage( pe ), ExceptionUtils.getDebugException( pe ) );
        } catch (FindException fe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( fe ), ExceptionUtils.getDebugException( fe ) );
        } catch (NullPointerException npe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( npe ), ExceptionUtils.getDebugException( npe ) );
        }
        return decrypted;
    }

    private static int asInt( final String name, final String value ) throws MqNativeConfigException {
        try {
            return Integer.parseInt( value );
        } catch ( NumberFormatException nfe ) {
            throw new MqNativeConfigException( "Invalid value '"+value+"' for property '"+name+"'" );
        }
    }

    private static byte[] asBytes( final String value ) throws MqNativeConfigException {
        return HexUtils.decodeBase64( value );
    }
}
