package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.gateway.common.transport.SsgActiveConnector;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 14/03/12
 * Time: 2:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class AmqpSsgActiveConnector extends SsgActiveConnector {

    public static final String ACTIVE_CONNECTOR_TYPE_AMQP = "AMQP";
    public static final String PROPERTY_KEY_AMQP_NAME = "AMQPName";
    public static final String PROPERTY_KEY_AMQP_VIRTUALHOST = "AMQPVirtualHost";
    public static final String PROPERTY_KEY_AMQP_ADDRESSES = "AMQPAddresses"; //Array of string serialized to a xml
    public static final String PROPERTY_KEY_AMQP_CREDENTIALS_REQUIRED = "AMQPCredentialsRequired";
    public static final String PROPERTY_KEY_AMQP_USERNAME = "AMQPUsername";
    public static final String PROPERTY_KEY_AMQP_PASSWORD_GOID = "AMQPPasswordGoid";
    public static final String PROPERTY_KEY_AMQP_USESSL = "AMQPUseSSL";
    public static final String PROPERTY_KEY_AMQP_TLS_PROTOCOLS = "AMQPSSLProtocols";
    public static final String PROPERTY_KEY_AMQP_CIPHERSPEC = "AMQPCipherSpec";
    public static final String PROPERTY_KEY_AMQP_SSL_CLIENT_KEY_ID = "AMQPSslClientKeyId";
    public static final String PROPERTY_KEY_AMQP_QUEUE_NAME = "AMQPQueueName";
    public static final String PROPERTY_KEY_AMQP_THREADPOOLSIZE = "AMQPThreadPoolSize";
    public static final String PROPERTY_KEY_AMQP_ACKNOWLEDGEMENT_TYPE = "AMQPAcknowledgement"; //Its an enum use just string ..
    public static final String PROPERTY_KEY_AMQP_INBOUND_REPLY_BEHAVIOUR = "AMQPInboundReplyBehaviour"; //Its an enum use just string ..
    public static final String PROPERTY_KEY_AMQP_INBOUND_REPLY_QUEUE = "AMQPInboundReplyQueue";
    public static final String PROPERTY_KEY_AMQP_INBOUND_CORRELATION_BEHAVIOUR = "AMQPInboundCorrelationBehaviour";////Its an enum use just string ..
    public static final String PROPERTY_KEY_AMQP_SERVICEGOID = "AMQPServiceGoid";
    public static final String PROPERTY_KEY_AMQP_CONTENT_TYPE_VALUE = "AMQPContentType";
    public static final String PROPERTY_KEY_AMQP_CONTENT_TYPE_PROPERTY_NAME = "AMQPContentTypePropertyName";
    public static final String PROPERTY_KEY_AMQP_FAILURE_QUEUE_NAME = "AMQPFailureQueueName";
    public static final String PROPERTY_KEY_AMQP_EXCHANGE_NAME = "AMQPExchangeName";
    public static final String PROPERTY_KEY_AMQP_OUTBOUND_REPLY_BEHAVIOUR = "AMQPOutboundReplyBehaviour";//Its an enum use just string ..
    public static final String PROPERTY_KEY_AMQP_RESPONSE_QUEUE = "AMQPResponseQueue";
    public static final String PROPERTY_KEY_AMQP_OUTBOUND_CORRELATION_BEHAVIOUR = "AMQPOutboundCorrelationBehaviour";//Its an enum use just string ..
}
