package com.l7tech.external.assertions.mqnative.server;

import com.l7tech.external.assertions.mqnative.MqNativeConstants;
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
    public static final String PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_REPLY_QUEUE_GET_MESSAGE_OPTIONS_USED = "MqNativeOutboundIsReplyQueueGetMessageOptionsUsed";
    public static final String PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_REPLY_QUEUE_GET_MESSAGE_OPTIONS = "MqNativeOutboundReplyQueueGetMessageOptions";

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
    private final boolean isReplyQueueGetMessageOptionsUsed;
    private final int replyQueueGetMessageOptions;

    MqNativeEndpointConfig( final SsgActiveConnector originalConnector,
                            final Option<String> password,
                            final Option<MqNativeDynamicProperties> dynamicProperties,
                            final int connectionPoolMaxActive,
                            final int connectionPoolMaxIdle,
                            final long connectionPoolMaxWait ) {
        final SsgActiveConnector connector = originalConnector.getCopy();
        if ( dynamicProperties.isSome() ) applyOverrides( connector, dynamicProperties.some() );

        this.name = connector.getName();
        this.dynamic = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE );
        this.queueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME );
        this.replyToQueueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME );
        this.key = new MqNativeEndpointKey( connector.getGoid(), connector.getVersion(), connectionPoolMaxActive, connectionPoolMaxIdle, connectionPoolMaxWait );
        this.copyCorrelationId = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST );
        this.queueManagerName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME );
        this.replyType = connector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_NONE, MqNativeReplyType.class );
        this.replyToModelQueueName = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN );
        this.queueManagerProperties = memoize(buildQueueManagerProperties(connector,password));
        this.isReplyQueueGetMessageOptionsUsed = connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_REPLY_QUEUE_GET_MESSAGE_OPTIONS_USED );
        this.replyQueueGetMessageOptions = connector.getIntegerProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_REPLY_QUEUE_GET_MESSAGE_OPTIONS, 0);
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
     * Represents all endpoint properties necessary to identify a MqNativeCachedConnectionPool.
     *
     * <p>If any properties effect the lookup of a MqNativeCachedConnectionPool then
     * these properties must be added to the key.</p>
     */
    static final class MqNativeEndpointKey {
        private final Goid id;
        private final int version;

        private int maxActive = MqNativeConstants.DEFAULT_MQ_NATIVE_CONNECTION_POOL_MAX_ACTIVE;
        private int maxIdle = MqNativeConstants.DEFAULT_MQ_NATIVE_CONNECTION_POOL_MAX_IDLE;
        private long maxWait = MqNativeConstants.DEFAULT_MQ_NATIVE_CONNECTION_POOL_MAX_WAIT;

        MqNativeEndpointKey( final Goid id,
                             final int version,
                             final int maxActive,
                             final int maxIdle,
                             final long maxWait ) {
            this.id = id;
            this.version = version;
            this.maxActive = maxActive;
            this.maxIdle = maxIdle;
            this.maxWait = maxWait;
        }

        Goid getId() {
            return id;
        }

        int getVersion() {
            return version;
        }

        public int getMaxActive() {
            return maxActive;
        }

        public int getMaxIdle() {
            return maxIdle;
        }

        public long getMaxWait() {
            return maxWait;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final MqNativeEndpointKey that = (MqNativeEndpointKey) o;

            if ( !id.equals(that.id) ) return false;
            if ( version != that.version ) return false;
            if (maxActive != that.maxActive) return false;
            if (maxIdle != that.maxIdle) return false;
            if (maxWait != that.maxWait) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + version;
            result = 31 * result + maxActive;
            result = 31 * result + maxIdle;
            result = 31 * result + (int) (maxWait ^ (maxWait >>> 32));
            return result;
        }

        /**
         * Get a String representation of this key.
         *
         * <p>The representation should include all properties of the key.</p>
         *
         * @return The string representation.
         */
        @SuppressWarnings("StringBufferReplaceableByString")
        @Override
        public String toString() {
            return "MqNativeEndpointKey{" +
                    "id=" + id +
                    ", version=" + version +
                    ", maxActive=" + maxActive +
                    ", maxIdle=" + maxIdle +
                    ", maxWait=" + maxWait +
                    '}';
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
        return () -> {
            try {
                return right( MqNativeUtils.buildQueueManagerConnectProperties( connector, password ) );
            } catch ( MqNativeConfigException e ) {
                return left( e );
            }
        };
    }

    /**
     * It controls the maximum number of objects that can be allocated by the
     * pool (checked out to clients, or idle awaiting checkout) at a given time. When non-positive, there is no limit to the number of objects
     * that can be managed by the pool at one time. When maxActive is reached, the pool is said to be exhausted.
     * @return maxActive setting
     */
    int getConnectionPoolMaxActive() {
        return key.getMaxActive();
    }

    /**
     * The maxWait value determines how the Connection Pool will behave when borrowing an object when none are available.
     * The default setting is to BLOCK when no objects/connections are available.  And the default maxWait is -1, which means
     * that borrowObject will block indefinitely until an idel instance becomes available.
     *
     * If a positive maxWait value is supplied, then borrowObject() will block for at most that many milliseconds, after which a NoSuchElementException
     * will be thrown.
     *
     * @return maxWait setting
     */
    long getConnectionPoolMaxWait() {
        return key.getMaxWait();
    }

    /**
     * Controls the maximum number of objects that can sit idle in the pool at any time.
     * When negative, there is no limit to the number of objects that may be idle at one time.
     * @return maxIdle setting
     */
    int getConnectionPoolMaxIdle() {
        return key.getMaxIdle();
    }

    boolean isReplyQueueGetMessageOptionsUsed() {
        return isReplyQueueGetMessageOptionsUsed;
    }

    int getReplyQueueGetMessageOptions() {
        return replyQueueGetMessageOptions;
    }
}
