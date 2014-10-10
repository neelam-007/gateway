package com.l7tech.external.assertions.mqnative.server;


import com.l7tech.external.assertions.mqnative.MqNativeDynamicProperties;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_NONE;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_SPECIFIED_QUEUE;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.extract;
import com.l7tech.util.Functions.Nullary;
import static com.l7tech.util.Functions.memoize;
import com.l7tech.util.Option;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;

/**
 * Immutable endpoint configuration for runtime use.
 */
class MqNativeEndpointConfig {

    private final String name;
    private final boolean dynamic;
    private final String queueName;
    private final String replyToQueueName;
    private final MqNativeEndpointKey key;
    private final boolean copyCorrelationId;
    private final String queueManagerName;
    private final MqNativeReplyType replyType;
    private final String replyToModelQueueName;
    private final Nullary<Either<MqNativeConfigException,Hashtable>> queueManagerProperties;
    private final int connectionPoolMaxActive;
    private final long connectionPoolMaxWait;
    private final int connectionPoolMaxIdle;

    MqNativeEndpointConfig( final SsgActiveConnector originalConnector,
                            final Option<String> password,
                            final Option<MqNativeDynamicProperties> dynamicProperties ) {
        final SsgActiveConnector connector = originalConnector.getCopy();
        if ( dynamicProperties.isSome() ) applyOverrides( connector, dynamicProperties.some() );

        this.name = connector.getName();
        this.dynamic = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE );
        this.queueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME );
        this.replyToQueueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME );
        this.key = new MqNativeEndpointKey( connector.getGoid(), connector.getVersion() );
        this.copyCorrelationId = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST );
        this.queueManagerName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME );
        this.replyType = connector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_NONE, MqNativeReplyType.class );
        this.replyToModelQueueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN );
        this.queueManagerProperties = memoize(buildQueueManagerProperties(connector,password));
        this.connectionPoolMaxActive = connector.getIntegerProperty(PROPERTIES_KEY_MQ_NATIVE_CONNECTION_POOL_MAX_ACTIVE, 8);
        this.connectionPoolMaxWait = connector.getIntegerProperty(PROPERTIES_KEY_MQ_NATIVE_CONNECTION_POOL_MAX_WAIT, -1);
        this.connectionPoolMaxIdle = connector.getIntegerProperty(PROPERTIES_KEY_MQ_NATIVE_CONNECTION_POOL_MAX_IDLE, 8);
    }

    boolean isDynamic() {
        return dynamic;
    }

    MqNativeEndpointKey getMqEndpointKey() {
        return key;
    }

    String getQueueManagerName() {
        return queueManagerName;
    }

    boolean isCopyCorrelationId() {
        return copyCorrelationId;
    }

    String getReplyToQueueName() {
        return replyToQueueName;
    }

    Hashtable getQueueManagerProperties() throws MqNativeConfigException {
        return (Hashtable)extract(queueManagerProperties.call()).clone();
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

    /**
     * Validate this endpoint configuration.
     *
     * @throws MqNativeConfigException If the configuration is invalid.
     */
    void validate() throws MqNativeConfigException {
        throwIfEmpty( queueName, "Queue name is required" );
        if ( replyType==REPLY_SPECIFIED_QUEUE ) throwIfEmpty( replyToQueueName, "Reply queue is required" );
    }

    private void throwIfEmpty( final String value, final String errorMessage ) throws MqNativeConfigException {
        if ( value == null || value.isEmpty() ) {
            throw new MqNativeConfigException( errorMessage);
        }
    }

    /**
     * Represents all endpoint properties necessary to identify an appropriate MQQueueManager.
     *
     * <p>If any dynamic properties effect the lookup of an MQQueueManager then
     * these properties must be added to the key.</p>
     */
    static final class MqNativeEndpointKey {
        private final Goid id;
        private final int version;

        MqNativeEndpointKey( final Goid id,
                             final int version ) {
            this.id = id;
            this.version = version;
        }

        Goid getId() {
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

            final MqNativeEndpointKey that = (MqNativeEndpointKey) o;

            if ( !id.equals(that.id) ) return false;
            if ( version != that.version ) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result += 31 * result + version;
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

            sb.append("MqNativeEndpointKey[");
            sb.append(getId());
            sb.append(',');
            sb.append(getVersion());
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

    private Nullary<Either<MqNativeConfigException,Hashtable>> buildQueueManagerProperties(
            final SsgActiveConnector connector,
            final Option<String> password ) {
        return new Nullary<Either<MqNativeConfigException,Hashtable>>(){
            @Override
            public Either<MqNativeConfigException,Hashtable> call() {
                try {
                    return right( MqNativeUtils.buildQueueManagerConnectProperties( connector, password ) );
                } catch ( MqNativeConfigException e ) {
                    return left( e );
                }
            }
        };
    }

    public int getConnectionPoolMaxActive() {
        return connectionPoolMaxActive;
    }

    public long getConnectionPoolMaxWait() {
        return connectionPoolMaxWait;
    }

    public int getConnectionPoolMaxIdle() {
        return connectionPoolMaxIdle;
    }
}
