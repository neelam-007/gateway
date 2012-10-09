package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.ibm.mq.headers.*;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.AnonymousSslClientSocketFactory;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsSslCustomizerSupport;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoidThrows;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeMessageDescriptor.applyPropertiesToMessage;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.ValidationUtils.getMinMaxPredicate;

/**
 * MQ Native connector helper class.
 */
class MqNativeUtils {
    private static final Logger logger = Logger.getLogger(MqNativeUtils.class.getName());

    static Pair<byte[], byte[]> parseHeaderPayload(MQMessage msg) throws IOException, MQDataException {
        byte[] headerBytes;
        byte[] payloadBytes;

        int headerLength = 0;
        int payloadLength;

        // parse header
        final MQHeaderList headerList = new MQHeaderList(msg);
        if (!headerList.isEmpty()) {
            headerLength = headerList.asMQData().size();
            headerBytes = new byte[headerLength];
            msg.seek(0);
            msg.readFully(headerBytes);

            payloadLength = msg.getTotalMessageLength() - headerLength;
        } else {
            headerBytes = new byte[0];
            payloadLength = msg.getMessageLength();
        }

        // parse payload
        payloadBytes = new byte[payloadLength];
        msg.seek(headerLength);
        msg.readFully(payloadBytes);

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
                validated( connector.getIntegerProperty( PROPERTIES_KEY_MQ_NATIVE_PORT, -1 ), "Port is out of the range [1, 65535]", getMinMaxPredicate( 1, 65535 ) ));
        connProps.put(MQC.CHANNEL_PROPERTY,
                validated( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_CHANNEL ), "Channel is required", isNotEmpty() ));

        // apply userId and password
        if ( connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED )) {
            final String userId = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_USERID );
            if (userId != null) {
                connProps.put(MQC.USER_ID_PROPERTY, userId);
            }
            if (password.isSome()) {
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
    static MqNativeKnob buildMqNativeKnob(@Nullable final byte[] mqHeader,
                                          @Nullable final MqNativeMessageDescriptor mqmd,
                                          @Nullable final Map<String, String> mqmdOverride) {
        return buildMqNativeKnob( null, mqHeader, mqmd, mqmdOverride );
    }
    /*
     * Create an MqNativeKnob.
     */
    static MqNativeKnob buildMqNativeKnob( @Nullable final String soapAction,
                                           @Nullable final byte[] mqHeader,
                                           @Nullable final MqNativeMessageDescriptor mqmd) {
        return buildMqNativeKnob( soapAction, mqHeader, mqmd, null );
    }

    /*
     * Create an MqNativeKnob.
     */
    static MqNativeKnob buildMqNativeKnob( @Nullable final String soapAction,
                                           @Nullable final byte[] mqHeader,
                                           @Nullable final MqNativeMessageDescriptor mqmd,
                                           @Nullable final Map<String, String> mqmdOverride) {
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
                return mqHeader != null ? mqHeader : new byte[0];
            }
            @Override
            public int getMessageHeaderLength() {
                return mqHeader != null ? mqHeader.length : 0;
            }
            @Override
            public MqNativeMessageDescriptor getMessageDescriptor() {
                return mqmd;
            }
            @Override
            public Map<String, String> getMessageDescriptorOverride() {
                return mqmdOverride;
            }
        };
    }

    static void applyMqNativeKnobToMessage(final boolean isPassThroughHeaders,
                                           @Nullable final MqNativeKnob mqNativeKnob,
                                           @NotNull final MQMessage mqMessage) throws IOException, MQException, MqNativeConfigException {
        if (mqNativeKnob != null) {
            if (isPassThroughHeaders) {
                // apply message descriptor
                MqNativeMessageDescriptor mqmd = mqNativeKnob.getMessageDescriptor();
                if (mqmd != null) {
                    mqmd.copyTo(mqMessage);
                }

                // apply header bytes
                if (mqNativeKnob.getMessageHeaderLength() > 0) {
                    mqMessage.write(mqNativeKnob.getMessageHeaderBytes());
                }
            }

            // always apply override
            applyPropertiesToMessage(mqNativeKnob.getMessageDescriptorOverride(), mqMessage);
        }
    }

    static boolean isTransactional(SsgActiveConnector connector) {
        return MqNativeAcknowledgementType.ON_COMPLETION ==
                    connector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class);
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

    /**
     *  Expected reason codes:
     *      2009: MQRC_CONNECTION_BROKEN
     *      2035: MQRC_NOT_AUTHORIZED
     *      2058: MQRC_Q_MGR_NAME_ERROR
     *      2059: MQRC_Q_MGR_NOT_AVAILABLE
     *      2085: MQRC_UNKNOWN_OBJECT_NAME
     *      2397: MQRC_JSSE_ERROR
     *
     *      http://publib.boulder.ibm.com/infocenter/wmqv7/v7r1/topic/com.ibm.mq.doc/fm12030_.htm
     *
     * @param e MQ exception
     * @return original exception or if expected reason code, return exception only if in debug mode
     */
    static MQException getDebugExceptionForExpectedReasonCode(MQException e) {
        int reasonCode = e.getReason();
        if ( reasonCode == MQRC_CONNECTION_BROKEN
                || reasonCode == MQRC_NOT_AUTHORIZED
                || reasonCode == MQRC_Q_MGR_NAME_ERROR
                || reasonCode == MQRC_Q_MGR_NOT_AVAILABLE
                || reasonCode == MQRC_UNKNOWN_OBJECT_NAME
                || reasonCode == MQRC_JSSE_ERROR ) {
            return ExceptionUtils.getDebugException(e);
        }
        return e;
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


}
