package com.l7tech.gateway.common.transport;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.Functions.Unary;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for an active connector.
 *
 * <p>An active connector retrieves messages from external systems, perhaps by
 * polling or by registering for notifications.</p>
 */
@Entity
@Proxy(lazy=false)
@Table(name="active_connector")
public class SsgActiveConnector extends NamedEntityImp {
    private static final Logger logger = Logger.getLogger(SsgActiveConnector.class.getName());

    /** If specified, incoming messages should be assumed to use the specified content type. */
    public static final String PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE = "overrideContentType";

    /** If specified and true then response messages should be sent. */
    public static final String PROPERTIES_KEY_ENABLE_RESPONSE_MESSAGES = "enableResponseMessages";

    /** If specified, the interval to use when polling for messages. */
    public static final String PROPERTIES_KEY_POLLING_INTERVAL = "pollingInterval";

    /** If specified, the size limit of the request message. */
    public static final String PROPERTIES_KEY_REQUEST_SIZE_LIMIT = "requestSizeLimit";

    /** If specified, the number of ssg active connectors to create using this configuration. */
    public static final String PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE = "numberOfSacToCreate";

    /**
     * The connector is used for inbound messages (transport), if this value is
     * not set then it should be assumed to be true (e.g. "getBooleanProperty(PROPERTIES_KEY_IS_INBOUND,true)")
     *
     * @see #getBooleanProperty(String,boolean)
     */
    public static final String PROPERTIES_KEY_IS_INBOUND = "inbound";

    // SFTP
    public static final String ACTIVE_CONNECTOR_TYPE_SFTP = "SFTP";
    public static final String PROPERTIES_KEY_SFTP_HOST = "SftpHost";
    public static final String PROPERTIES_KEY_SFTP_PORT = "SftpPort";
    public static final String PROPERTIES_KEY_SFTP_DIRECTORY = "SftpDirectory";
    public static final String PROPERTIES_KEY_SFTP_USERNAME = "SftpUsername";
    public static final String PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID = "SftpSecurePasswordOid";
    public static final String PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID = "SftpSecurePasswordKeyOid";
    public static final String PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT = "SftpServerFingerPrint";
    public static final String PROPERTIES_KEY_SFTP_DELETE_ON_RECEIVE = "SftpDeleteOnReceive";

    // MQ Native
    public static final String ACTIVE_CONNECTOR_TYPE_MQ_NATIVE = "MqNative";
    // MQ Native - general connection properties
    public static final String PROPERTIES_KEY_MQ_NATIVE_HOST_NAME = "MqNativeHostName";
    public static final String PROPERTIES_KEY_MQ_NATIVE_PORT = "MqNativePort";
    public static final String PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME = "MqNativeQueueManagerName";
    public static final String PROPERTIES_KEY_MQ_NATIVE_CHANNEL = "MqNativeChannel";
    public static final String PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME = "MqNativeTargetQueueName";
    // MQ Native - security connection properties
    public static final String PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED = "MqNativeIsQueueCredentialRequired";
    public static final String PROPERTIES_KEY_MQ_NATIVE_USERID = "MqNativeUserId";
    public static final String PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID = "MqNativeSecurePasswordOid";
    public static final String PROPERTIES_KEY_MQ_NATIVE_IS_SSL_ENABLED = "MqNativeIsSslEnabled";
    public static final String PROPERTIES_KEY_MQ_NATIVE_CIPHER_SUITE = "MqNativeCipherSuite";
    public static final String PROPERTIES_KEY_MQ_NATIVE_IS_SSL_KEYSTORE_USED = "MqNativeIsSslKeystoreUsed";
    public static final String PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ALIAS = "MqNativeSslKeystoreAlias";
    public static final String PROPERTIES_KEY_MQ_NATIVE_SSL_KEYSTORE_ID = "MqNativeSslKeystoreId";
    // MQ Native - inbound and outbound common properties
    public static final String PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE = "MqNativeReplyType";
    public static final String PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST = "MqNativeIsCopyCorrelationIdFromRequest";
    public static final String PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME = "MqNativeSpecifiedReplyQueueName";
    // MQ Native - inbound properties
    public static final String PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE = "MqNativeInboundAcknowledgementType";
    public static final String PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_SOAP_ACTION_USED = "MqNativeInboundIsSoapActionUsed";
    public static final String PROPERTIES_KEY_MQ_NATIVE_INBOUND_SOAP_ACTION = "MqNativeInboundSoapAction";
    public static final String PROPERTIES_KEY_MQ_NATIVE_INBOUND_CONTENT_TYPE_FROM_PROPERTY = "MqNativeInboundContentTypeFromProperty";
    public static final String PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED = "MqNativeInboundIsFailedQueueUsed";
    public static final String PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME = "MqNativeInboundFailedQueueName";
    // MQ Native - outbound properties
    public static final String PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE = "MqNativeOutboundIsTemplateQueue";
    public static final String PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN = "MqNativeOutboundTemporaryQueueNamePattern";
    public static final String PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_MESSAGE_FORMAT = "MqNativeOutboundMessageFormat";

    private boolean enabled;
    private String type;
    private Long hardwiredServiceOid;
    private Map<String,String> properties = new HashMap<String,String>();

    public SsgActiveConnector() {
    }

    public SsgActiveConnector( final SsgActiveConnector ssgActiveConnector ) {
        super(ssgActiveConnector);
        this.enabled = ssgActiveConnector.isEnabled();
        this.type = ssgActiveConnector.getType();
        this.hardwiredServiceOid = ssgActiveConnector.getHardwiredServiceOid();
        this.setProperties( new HashMap<String, String>( ssgActiveConnector.getProperties() ) );
    }

    public static SsgActiveConnector newWithType(String type) {
        SsgActiveConnector ret = new SsgActiveConnector();
        ret.setType(type);
        return ret;
    }

    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled( boolean enabled ) {
        checkLocked();
        this.enabled = enabled;
    }

    @Column(name = "type", length = 64, nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        checkLocked();
        this.type = type;
    }

    @Column(name="hardwired_service_oid")
    public Long getHardwiredServiceOid() {
        return hardwiredServiceOid;
    }

    public void setHardwiredServiceOid(Long hardwiredServiceOid) {
        checkLocked();
        this.hardwiredServiceOid = hardwiredServiceOid;
    }

    /**
     * Get the extra properties of this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @return a Set containing the extra connector properties.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch=FetchType.EAGER)
    @JoinTable(name="active_connector_property",
               joinColumns=@JoinColumn(name="connector_oid", referencedColumnName="objectid"))
    @MapKeyColumn(name="name",length=128)
    @Column(name="value", nullable=false, length=32672)
    protected Map<String,String> getProperties() {
        //noinspection ReturnOfCollectionOrArrayField
        return properties;
    }

    /**
     * Set the extra properties for this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @param properties the properties set to use
     */
    protected void setProperties( final Map<String,String> properties ) {
        checkLocked();

        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.properties = properties;
    }

    public String getProperty( final String propertyName ) {
        return getProperty( propertyName, null );

    }

    public String getProperty( final String propertyName, @Nullable final String defaultValue ) {
        String propertyValue = null;

        final Map<String,String> properties = this.properties;
        if (properties != null) {
            propertyValue = properties.get(propertyName);
        }

        if ( propertyValue == null ) {
            propertyValue = defaultValue;
        }

        return propertyValue;
    }

    /**
     * Convenience method to get a property as a boolean.
     *
     * @param propertyName the name of the property to get
     * @return boolean represented by the requested property value
     */
    public boolean getBooleanProperty( final String propertyName ) {
        return Boolean.parseBoolean(getProperty(propertyName));
    }

    /**
     * Convenience method to get a property as a boolean.
     *
     * @param propertyName the name of the property to get
     * @param dflt the default value to use if the property is not set
     * @return boolean represented by the requested property value
     */
    public boolean getBooleanProperty( final String propertyName, final boolean dflt ) {
        return Boolean.parseBoolean(getProperty(propertyName,String.valueOf(dflt)));
    }

    /**
     * Convenience method to get a property as a long.
     *
     * @param propertyName the name of the property to get
     * @param dflt the default value to return if the property is not set or if it is not a valid long
     * @return the requested property value, or the default if it is not set or is invalid
     */
    public long getLongProperty( final String propertyName, final long dflt ) {
        String val = getProperty(propertyName);
        if (val == null)
            return dflt;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException nfe) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Invalid long property value for active connector " + getName() + " property " + propertyName + ": " + val);
            return dflt;
        }
    }

    /**
     * Convenience method to get a property as an int.
     *
     * @param propertyName the name of the property to get
     * @param dflt the default value to return if the property is not set or if it is not a valid int
     * @return the requested property value, or the default if it is not set or is invalid
     */
    public int getIntegerProperty( final String propertyName, final int dflt ) {
        String val = getProperty(propertyName);
        if (val == null)
            return dflt;
        try {
            return Integer.parseInt( val );
        } catch (NumberFormatException nfe) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Invalid integer property value for active connector " + getName() + " property " + propertyName + ": " + val);
            return dflt;
        }
    }

    /**
     * Convenience method to get a property as an enum.
     *
     * @param propertyName The name of the property to get
     * @param dflt The default value to return if the property is not set or if it is not a valid enum value
     * @param enumType The enum class
     * @param <E> The enum type
     * @return the requested property value, or the default if it is not set or is invalid
     */
    public <E extends Enum<E>> E getEnumProperty( final String propertyName, final E dflt, final Class<E> enumType ) {
        String val = getProperty(propertyName);
        if (val == null)
            return dflt;
        try {
            return Enum.valueOf( enumType, val );
        } catch (IllegalArgumentException e) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Invalid enum property value for active connector " + getName() + " property " + propertyName + ": " + val);
            return dflt;
        }
    }

    public void setProperty( final String propertyName, final String propertyValue ) {
        checkLocked();

        Map<String,String> properties = this.properties;
        if (properties == null) {
            properties = new HashMap<String, String>();
            this.properties = properties;
        }

        properties.put(propertyName, propertyValue);
    }

    /**
     * Remove some property associated with the name,  ${propertyName} from the property list.
     * @param propertyName the property name whose property will be removed from the list.
     */
    public void removeProperty( final String propertyName ) {
        checkLocked();

        final Map<String,String> properties = this.properties;
        if ( properties != null ) {
            properties.remove(propertyName);
        }
    }

    /**
     * Initialize any lazily-computed fields and mark this instance as read-only.
     */
    private void setReadOnly() {
        this.lock();
    }

    @Transient
    public SsgActiveConnector getCopy() {
        return new SsgActiveConnector( this );
    }

    @Transient
    public SsgActiveConnector getReadOnlyCopy() {
        final SsgActiveConnector copy = getCopy();
        copy.setReadOnly();
        return copy;
    }

    /**
     * Get a list of all extra properties set on this connector.
     *
     * @return a List of Strings.  May be empty, but never null.
     */
    @Transient
    public List<String> getPropertyNames() {
        return new ArrayList<String>(properties.keySet());
    }

    /**
     * First class function for boolean property access.
     *
     * @param name The property name.
     * @return A function to access the named property
     */
    public static Unary<Boolean,SsgActiveConnector> booleanProperty( final String name ) {
        return new Unary<Boolean,SsgActiveConnector>(){
            @Override
            public Boolean call( final SsgActiveConnector connector  ) {
                return connector.getBooleanProperty( name );
            }
        };
    }
}