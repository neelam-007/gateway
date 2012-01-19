package com.l7tech.external.assertions.mqnative.server;


import com.l7tech.external.assertions.mqnative.MqNativeDynamicProperties;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_NONE;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import com.l7tech.server.security.password.SecurePasswordManager;
import static com.l7tech.util.Functions.grep;
import com.l7tech.util.Option;
import static com.l7tech.util.TextUtils.startsWith;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;
import java.util.Properties;

/**
 * Immutable endpoint configuration for runtime use.
 */
class MqNativeEndpointConfig {

    private final String name;
    private final boolean dynamic;
    private final String channelName;
    private final String queueName;
    private final String replyToQueueName;
    private final MqEndpointKey key;
    private final boolean correlateWithMessageId;
    private final String queueManagerName;
    private final MqNativeReplyType replyType;
    private final String replyToModelQueueName;
    private final Hashtable queueManagerProperties;
    private final Properties messageProperties;

    //TODO [steve] pass in password, not passwordManager
    //TODO [steve] make some initialization lazy since the values may not be used
    MqNativeEndpointConfig( final SsgActiveConnector originalConnector,
                            final SecurePasswordManager securePasswordManager,
                            final Option<MqNativeDynamicProperties> dynamicProperties ) {
        final SsgActiveConnector connector = originalConnector.getCopy();
        if ( dynamicProperties.isSome() ) applyOverrides( connector, dynamicProperties.some() );

        this.name = connector.getName();
        this.dynamic = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE );
        this.channelName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_CHANNEL);
        this.queueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME );
        this.replyToQueueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME );
        this.key = dynamic ?
                new MqEndpointKey( connector.getOid(), connector.getVersion(), channelName, queueName, replyToQueueName ):
                new MqEndpointKey( connector.getOid(), connector.getVersion() );
        this.correlateWithMessageId = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST );
        this.queueManagerName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME );
        this.replyType = connector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_NONE, MqNativeReplyType.class );
        this.replyToModelQueueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN );
        this.queueManagerProperties = buildQueueManagerProperties(connector,securePasswordManager);
        this.messageProperties = buildProperties( connector, PROPERTIES_KEY_MQ_NATIVE_ADVANCED_PROPERTY_PREFIX );
    }

    boolean isDynamic() {
        return dynamic;
    }

    MqEndpointKey getMqEndpointKey() {
        return key;
    }

    String getQueueManagerName() {
        return queueManagerName;
    }

    boolean isCorrelateWithMessageId() {
        return correlateWithMessageId;
    }

    String getReplyToQueueName() {
        return replyToQueueName;
    }

    Hashtable getQueueManagerProperties() {
        return (Hashtable)queueManagerProperties.clone();
    }

    MqNativeReplyType getReplyType() {
        return replyType;
    }

    String getQueueName() {
        return queueName;
    }

    String getReplyToModelQueueName() {
        return replyToModelQueueName;
    }

    String getName() {
        return name;
    }

    Properties getMessageProperties() {
        return (Properties)messageProperties.clone();
    }

    static final class MqEndpointKey {
        private final long id;
        private final int version;
        private final String channelName;
        private final String queueName;
        private final String replyToQueue;

        MqEndpointKey( final long id,
                       final int version ) {
            this( id, version, null, null, null );
        }

        MqEndpointKey( final long id,
                       final int version,
                       @Nullable final String channelName,
                       @Nullable final String queueName,
                       @Nullable final String replyToQueue ) {
            this.id = id;
            this.version = version;
            this.channelName = channelName;
            this.queueName = queueName;
            this.replyToQueue = replyToQueue;
        }

        long getId() {
            return id;
        }

        int getVersion() {
            return version;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final MqEndpointKey that = (MqEndpointKey) o;

            if ( id != that.id ) return false;
            if ( version != that.version ) return false;
            if ( channelName != null ? !channelName.equals( that.channelName ) : that.channelName != null )
                return false;
            if ( queueName != null ? !queueName.equals( that.queueName ) : that.queueName != null ) return false;
            if ( replyToQueue != null ? !replyToQueue.equals( that.replyToQueue ) : that.replyToQueue != null )
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + version;
            result = 31 * result + (channelName != null ? channelName.hashCode() : 0);
            result = 31 * result + (queueName != null ? queueName.hashCode() : 0);
            result = 31 * result + (replyToQueue != null ? replyToQueue.hashCode() : 0);
            return result;
        }

        /**
         * Get a String representation of this key.
         *
         * <p>The representation should include all properties of the key.</p>
         *
         * @return The string representation.
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("MqEndpointKey[");
            sb.append(getId());
            sb.append(',');
            sb.append(getVersion());

            /*
             * For dynamic endpoints, append the full info
             */
            if ( channelName != null ) {
                sb.append("-");
                sb.append(channelName);
                sb.append(',');
                sb.append(queueName);
                sb.append(',');
                sb.append(replyToQueue);
            }

            sb.append("]");

            return sb.toString();
        }
    }

    /**
     * Override empty template settings with values from the given dynamic properties.
     */
    private void applyOverrides( final SsgActiveConnector connector,
                                 @Nullable final MqNativeDynamicProperties overrides ) {
        if ( overrides != null ) {
            setIfEmpty( connector, PROPERTIES_KEY_MQ_NATIVE_CHANNEL, overrides.getChannelName() );
            setIfEmpty( connector, PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, overrides.getQueueName() );
            setIfEmpty( connector, PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, overrides.getReplyToQueue() );
        }
    }

    private void setIfEmpty( final SsgActiveConnector connector,
                             final String propertyName,
                             final String propertyValue ) {
        if (connector.getProperty( propertyName, "" ).isEmpty() )
            connector.setProperty( propertyName, propertyValue );
    }

    private Hashtable buildQueueManagerProperties( final SsgActiveConnector connector,
                                                   final SecurePasswordManager securePasswordManager ) {
        return MqNativeUtils.buildQueueManagerConnectProperties( connector, securePasswordManager );
    }

    private Properties buildProperties( final SsgActiveConnector connector,
                                        final String prefix ) {
        final Properties properties = new Properties();

        for ( final String property : grep( connector.getPropertyNames(), startsWith( prefix ) ) ) {
            properties.setProperty(
                property.substring(prefix.length()),
                connector.getProperty(property)
            );
        }

        return properties;
    }
}
