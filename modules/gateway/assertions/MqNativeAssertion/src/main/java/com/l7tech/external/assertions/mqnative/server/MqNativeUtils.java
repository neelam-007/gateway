package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQManagedObject;
import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.CCSID;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.internal.Header;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import com.l7tech.external.assertions.mqnative.MqNativeConstants;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.AnonymousSslClientSocketFactory;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsSslCustomizerSupport;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryVoidThrows;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_CONVERT_MESSAGE_APPLICATION_DATA_FORMAT;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_FORCE_RETURN_PROPS_IN_MQRFH2_HEADER;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_CONVERSION_CCSID;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.ValidationUtils.getMinMaxPredicate;

/**
 * MQ Native connector helper class.
 */
public class MqNativeUtils {

    public static final String PREIFX = "mqnative";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    private static final int DEFAULT_CCSID = 819; // IBM CCSID for ISO-8859-1
    private static final String XML_CONTENT_TYPE_HEADER_TEMPLATE = "text/xml; charset=";

    private static final Logger logger = Logger.getLogger(MqNativeUtils.class.getName());

    // Some expected reason codes (see more from http://publib.boulder.ibm.com/infocenter/wmqv7/v7r1/topic/com.ibm.mq.doc/fm12030_.htm)
    static final List<Integer> EXPECTED_REASON_CODES = Arrays.asList(new Integer[] {
            MQRC_CONNECTION_BROKEN,
            MQRC_NOT_AUTHORIZED,
            MQRC_Q_MGR_NAME_ERROR,
            MQRC_Q_MGR_NOT_AVAILABLE,
            MQRC_UNKNOWN_OBJECT_NAME,
            MQRC_JSSE_ERROR,
            MQRC_EXPIRY_ERROR,
            MQRC_FEEDBACK_ERROR,
            MQRC_PERSISTENCE_ERROR,
            MQRC_PRIORITY_EXCEEDS_MAXIMUM,
            MQRC_PRIORITY_ERROR,
            MQRC_PROPERTY_NAME_ERROR,
            MQRC_MD_ERROR,
            MQRC_OFFSET_ERROR,
            MQRC_Q_FULL
    });

    public static Pair<byte[], byte[]> parseHeaderPayload(@NotNull final MQMessage msg) throws IOException, MQDataException {
        byte[] headerBytes;
        byte[] payloadBytes;

        int headerLength = 0;
        int payloadLength;

        msg.seek(0);

        // parse header
        final MQHeaderList headerList = new MQHeaderList(msg);
        if (!headerList.isEmpty()) {
            headerLength = headerList.asMQData().size();
            headerBytes = new byte[headerLength];
            msg.seek(0);
            msg.readFully(headerBytes);
            //For V7 the total message length contains the message properties length also.
            payloadLength = msg.getMessageLength() - headerLength;
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

    public static Header parsePrimaryAdditionalHeader(@NotNull final MQMessage msg) throws IOException, MQDataException {
        msg.seek(0);
        MQHeaderList headerList = new MQHeaderList(msg);
        if (headerList != null && headerList.size() > 0) {
            return (Header) headerList.get(0);
        } else {
            return null;
        }
    }

    static Option<String> getQueuePassword( final SsgActiveConnector connector,
                                            final SecurePasswordManager securePasswordManager ) {
        Option<String> password = none();

        if ( connector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED) ) {
            final Goid passwordGoid = GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID));
            password = (passwordGoid == null || Goid.isDefault(passwordGoid)) ?
                    password :
                    some( getDecryptedPassword( securePasswordManager, passwordGoid ) );
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
    public static MqNativeKnob buildMqNativeKnob(@Nullable final MqMessageProxy mqMessage) {
        return buildMqNativeKnob( null, mqMessage );
    }

    /*
     * Create an MqNativeKnob.
     */
    static MqNativeKnob buildMqNativeKnob( @Nullable final String soapAction,
                                           @Nullable final MqMessageProxy mqMessageProxy) {
        return new MqNativeKnob() {

            private static final String DESCRIPTOR_PREFIX = "md.";
            private static final String PROPERTY_PREFIX = "property.";
            private static final String HEADER_PREFIX = "additionalheader.";
            private static final String HEADER_NAMES = "additionalheadernames";
            private static final String HEADER_VALUES = "alladditionalheadervalues";
            private static final String PROPERTY_NAMES = "propertynames";
            private static final String PROPERTY_VALUES = "allpropertyvalues";
            private MqMessageProxy proxy = mqMessageProxy;

            @Override
            public MqMessageProxy getMessage() {
                return proxy;
            }

            @Override
            public void reset(Object message) {
                proxy = (MqMessageProxy) message;
            }

            @Override
            public String[] getHeaderValues(String name) {
                if (name.startsWith(DESCRIPTOR_PREFIX)) {
                    String attr = name.substring(DESCRIPTOR_PREFIX.length());
                    Object result = proxy.getMessageDescriptor(attr);
                    if (result instanceof byte[]) {
                        result = HexUtils.encodeBase64((byte[])result);
                    } else if (result instanceof GregorianCalendar) {
                        result = asString((GregorianCalendar) result);
                    }
                    return new String[]{String.valueOf(result)};
                }
                if (name.startsWith(PROPERTY_PREFIX)) {
                    String attr = name.substring(PROPERTY_PREFIX.length());
                    return new String[]{String.valueOf(proxy.getMessageProperty(attr))};
                }
                if (name.startsWith(HEADER_PREFIX)) {
                    String attr = name.substring(HEADER_PREFIX.length());
                    return new String[]{String.valueOf(proxy.getPrimaryHeaderValue(attr))};
                }
                if (name.equalsIgnoreCase(HEADER_NAMES)) {
                    return getKeys(proxy.getPrimaryHeaderProperties());
                }

                if (name.equalsIgnoreCase(HEADER_VALUES)) {
                    return getValues(proxy.getPrimaryHeaderProperties());
                }

                if (name.equalsIgnoreCase(PROPERTY_NAMES)) {
                    return getKeys(proxy.getMessageProperties());
                }

                if (name.equalsIgnoreCase(PROPERTY_VALUES)) {
                    return getValues(proxy.getMessageProperties());
                }
                return new String[0];
            }

            @Override
            public String[] getHeaderNames() {
                return getHeaderValues(HEADER_NAMES);
            }

            @Override
            public Goid getServiceGoid() {
                return PersistentEntity.DEFAULT_GOID;
            }

            @Override
            public String getSoapAction() throws IOException {
                return soapAction;
            }

            private String[] getKeys(Map<String, Object> map) {

                String[] mqHeaderNames = new String[0];
                if (map != null && !map.isEmpty()) {
                    mqHeaderNames = new String[map.size()];
                    int i = 0;
                    for (Map.Entry<String, Object> entry : map.entrySet() ) {
                        mqHeaderNames[i] = entry.getKey();
                        i++;
                    }
                }
                return mqHeaderNames;
            }

            private String[] getValues(Map<String, Object> map) {
                String[] mqAllHeaderValues = new String[0];
                if (map != null) {
                    mqAllHeaderValues = new String[map.size()];
                    int i = 0;
                    for (Map.Entry<String, Object> entry : map.entrySet() ) {
                        mqAllHeaderValues[i] =  entry.getValue() + "";
                        i++;
                    }
                }
                return mqAllHeaderValues;
            }

            private String asString(GregorianCalendar calendar) {
                if (calendar != null) {
                    return MqNativeUtils.DATE_FORMAT.format(calendar.getTime());
                }
                return null;
            }
        };
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
            } catch (Throwable t) {
                // attempt to recover from unexpected error when closing MQ objects
                // For example Rally ticket DE237841, NullPointerException at com.ibm.mq.jmqi.remote.api.RemoteFAP.MQCLOSE(RemoteFAP.java:6727)
                StackTraceElement[] stackTraceElements = t.getStackTrace();
                if (stackTraceElements != null && stackTraceElements.length > 0 && stackTraceElements[0] != null && stackTraceElements[0].getClassName().startsWith("com.ibm.mq.")) {
                    if ( logger.isLoggable( Level.FINE ) ) {
                        logger.log( Level.FINE,
                                "Error closing MQ object: " + ExceptionUtils.getMessage( t ),
                                ExceptionUtils.getDebugException( t ) );
                    }
                } else {
                    throw t;
                }
            }
        }
    }

    /**
     * Get debug exception based on a given expected reason code.
     *
     * @param e MQ exception
     * @return original exception or if expected reason code, return exception only if in debug mode
     */
    static MQException getDebugExceptionForExpectedReasonCode(MQException e) {
        final int reasonCode = e.getReason();
        if (EXPECTED_REASON_CODES.contains(reasonCode)) {
            return ExceptionUtils.getDebugException(e);
        }
        return e;
    }

    private static SecurePassword getSecurePassword( final SecurePasswordManager securePasswordManager,
                                                     final Goid passwordGoid ) {
        SecurePassword securePassword = null;
        try {
            securePassword = securePasswordManager.findByPrimaryKey(passwordGoid);
        } catch (FindException fe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( fe ), ExceptionUtils.getDebugException( fe ) );
        }
        return securePassword;
    }

    private static String getDecryptedPassword( final SecurePasswordManager securePasswordManager,
                                                final Goid passwordGoid ) {
        String decrypted = null;
        try {
            final SecurePassword securePassword = getSecurePassword( securePasswordManager, passwordGoid );
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

    public static int getOutboundPutMessageOption() {
        ServerConfig config = ServerConfig.getInstance();
        if (config.getBooleanProperty(ServerConfigParams.PARAM_IO_MQ_SET_ALL_CONTEXT, false)) {
            return MqNativeConstants.QUEUE_OPEN_OPTIONS_OUTBOUND_PUT_SETALLCONTEXT;
        } else {
            return MqNativeConstants.QUEUE_OPEN_OPTIONS_OUTBOUND_PUT;
        }
    }

    public static int getTempOutboundPutMessageOption() {
        ServerConfig config = ServerConfig.getInstance();
        if (config.getBooleanProperty(ServerConfigParams.PARAM_IO_MQ_SET_ALL_CONTEXT, false)) {
            return MqNativeConstants.QUEUE_OPEN_OPTIONS_OUTBOUND_PUT_TEMP_SETALLCONTEXT;
        } else {
            return MqNativeConstants.QUEUE_OPEN_OPTIONS_OUTBOUND_PUT_TEMP;
        }
    }


    public static int getIntboundReplyMessageOption() {
        ServerConfig config = ServerConfig.getInstance();
        if (config.getBooleanProperty(ServerConfigParams.PARAM_IO_MQ_SET_ALL_CONTEXT, false)) {
            return MqNativeConstants.QUEUE_OPEN_OPTIONS_INBOUND_REPLY_SPECIFIED_QUEUE_SETALLCONTEXT;
        } else {
            return MqNativeConstants.QUEUE_OPEN_OPTIONS_INBOUND_REPLY_SPECIFIED_QUEUE;
        }
    }

    public static boolean isOpenForSetAllContext() {
        ServerConfig config = ServerConfig.getInstance();
        return config.getBooleanProperty(ServerConfigParams.PARAM_IO_MQ_SET_ALL_CONTEXT, false);
    }

    public static boolean isMessageDataConversionEnabled() {
        ServerConfig config = ServerConfig.getInstance();
        return config.getBooleanProperty(PARAM_IO_MQ_CONVERT_MESSAGE_APPLICATION_DATA_FORMAT, true);
    }

    public static boolean isForcePropertiesInMQRFH2HeaderEnabled() {
        ServerConfig config = ServerConfig.getInstance();
        return config.getBooleanProperty(PARAM_IO_MQ_FORCE_RETURN_PROPS_IN_MQRFH2_HEADER, false);
    }

    /**
     * Returns the CCSID/charset.
     *
     * @return the CCSID/charset
     */
    public static int getConversionCCSID() {
        int ccsid = DEFAULT_CCSID;

        if (isMessageDataConversionEnabled()) {
            ServerConfig config = ServerConfig.getInstance();
            ccsid = config.getIntProperty(PARAM_IO_MQ_CONVERSION_CCSID, DEFAULT_CCSID);
        }

        return ccsid;
    }

    /**
     * Handles incoming content headers from MQ messages
     *
     * @throws IOException if an error occurs while parsing header
     */
    public static ContentTypeHeader getContentTypeHeader() throws IOException {
        return getContentTypeHeader("");
    }

    /**
     * Handles incoming content headers from MQ messages
     *
     * @param contentTypeValue Content Type Value to be set in this function
     * @throws IOException if an error occurs while parsing header
     */
    public static ContentTypeHeader getContentTypeHeader(final String contentTypeValue) throws IOException {
        String contentTypeHeaderValue;

        // if the content type is not specified and mq convert is on, it will be set as "text/xml; charset='charset value from io.mqConversionCCSID'"
        // default is set to "text/xml; charset=ISO-8859-1"
        if (contentTypeValue == null || contentTypeValue.trim().length() == 0) {
            int ccsid = getConversionCCSID();
            String charset = CCSID.getCodepage(ccsid);
            contentTypeHeaderValue = XML_CONTENT_TYPE_HEADER_TEMPLATE + charset;
        } else {
            contentTypeHeaderValue = contentTypeValue;
        }

        ContentTypeHeader cType = ContentTypeHeader.parseValue(contentTypeHeaderValue);
        cType.getEncoding();

        return cType;
    }
}
