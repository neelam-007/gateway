package com.l7tech.external.assertions.amqpassertion;

import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.objectmodel.Goid;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/8/12
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPDestination implements Serializable {

    public static final String PROCOCOL_TLS1 = "TLSv1";
    public static final String PROTOCOL_TLS11 = "TLSv1.1";
    public static final String PROTOCOL_TLS12 = "TLSv1.2";
    public static final List<String> DEFAULT_TLS_PROTOCOL_LIST = Collections.unmodifiableList(
            Arrays.asList(PROCOCOL_TLS1, PROTOCOL_TLS11, PROTOCOL_TLS12));

    public enum InboundReplyBehaviour {
        AUTOMATIC,
        ONE_WAY,
        SPECIFIED_QUEUE
    }

    public enum InboundCorrelationBehaviour {
        CORRELATION_ID,
        MESSAGE_ID
    }

    public enum OutboundReplyBehaviour {
        TEMPORARY_QUEUE,
        ONE_WAY,
        SPECIFIED_QUEUE
    }

    public enum OutboundCorrelationBehaviour {
        GENERATE_CORRELATION_ID,
        USE_MESSAGE_ID
    }

    private Goid goid = new Goid(0, -1);
    private String name;
    private boolean isInbound = false;
    private String virtualHost;
    private String[] addresses = new String[0];
    private boolean credentialsRequired = false;
    private String username;
    private Goid passwordGoid;
    private boolean useSsl = false;
    private String cipherSpec = "SSL_RSA_WITH_NULL_MD5";
    private String sslClientKeyId = null;
    private String queueName;
    private int threadPoolSize = 10;
    private JmsAcknowledgementType acknowledgementType = JmsAcknowledgementType.AUTOMATIC;
    private int prefetchSize = 0;
    private InboundReplyBehaviour inboundReplyBehaviour = InboundReplyBehaviour.AUTOMATIC;
    private String inboundReplyQueue = null;
    private InboundCorrelationBehaviour inboundCorrelationBehaviour = InboundCorrelationBehaviour.CORRELATION_ID;
    private Goid serviceGoid = null;
    private String contentTypeValue = null;
    private String contentTypePropertyName = null;
    private String failureQueueName = null;
    private boolean enabled = false;
    private String exchangeName;
    private OutboundReplyBehaviour outboundReplyBehaviour = OutboundReplyBehaviour.TEMPORARY_QUEUE;
    private String responseQueue = null;
    private OutboundCorrelationBehaviour outboundCorrelationBehaviour = OutboundCorrelationBehaviour.GENERATE_CORRELATION_ID;
    private String[] tlsProtocols = DEFAULT_TLS_PROTOCOL_LIST.toArray(new String[]{});
    private int version = 1;

    public AMQPDestination() {
    }

    public Goid getGoid() {
        return goid;
    }

    public void setGoid(Goid goid) {
        this.goid = goid;
    }

    public boolean isInbound() {
        return isInbound;
    }

    public void setInbound(boolean inbound) {
        isInbound = inbound;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String[] getAddresses() {
        return addresses;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public boolean isCredentialsRequired() {
        return credentialsRequired;
    }

    public void setCredentialsRequired(boolean credentialsRequired) {
        this.credentialsRequired = credentialsRequired;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Goid getPasswordGoid() {
        return passwordGoid;
    }

    public void setPasswordGoid(Goid passwordGoid) {
        this.passwordGoid = passwordGoid;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getCipherSpec() {
        return cipherSpec;
    }

    public void setCipherSpec(String cipherSpec) {
        this.cipherSpec = cipherSpec;
    }

    public String getSslClientKeyId() {
        return sslClientKeyId;
    }

    public void setSslClientKeyId(String sslClientKeyId) {
        this.sslClientKeyId = sslClientKeyId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public JmsAcknowledgementType getAcknowledgementType() {
        return acknowledgementType;
    }

    public void setAcknowledgementType(JmsAcknowledgementType acknowledgementType) {
        this.acknowledgementType = acknowledgementType;
    }

    public int getPrefetchSize() {
        return prefetchSize;
    }

    public void setPrefetchSize(int prefetchSize) {
        this.prefetchSize = prefetchSize;
    }

    public InboundReplyBehaviour getInboundReplyBehaviour() {
        return inboundReplyBehaviour;
    }

    public void setInboundReplyBehaviour(InboundReplyBehaviour inboundReplyBehaviour) {
        this.inboundReplyBehaviour = inboundReplyBehaviour;
    }

    public String getInboundReplyQueue() {
        return inboundReplyQueue;
    }

    public void setInboundReplyQueue(String inboundReplyQueue) {
        this.inboundReplyQueue = inboundReplyQueue;
    }

    public InboundCorrelationBehaviour getInboundCorrelationBehaviour() {
        return inboundCorrelationBehaviour;
    }

    public void setInboundCorrelationBehaviour(InboundCorrelationBehaviour inboundCorrelationBehaviour) {
        this.inboundCorrelationBehaviour = inboundCorrelationBehaviour;
    }

    public Goid getServiceGoid() {
        return serviceGoid;
    }

    public void setServiceGoid(Goid serviceGoid) {
        this.serviceGoid = serviceGoid;
    }

    public String getContentTypeValue() {
        return contentTypeValue;
    }

    public void setContentTypeValue(String contentTypeValue) {
        this.contentTypeValue = contentTypeValue;
    }

    public String getContentTypePropertyName() {
        return contentTypePropertyName;
    }

    public void setContentTypePropertyName(String contentTypePropertyName) {
        this.contentTypePropertyName = contentTypePropertyName;
    }

    public String getFailureQueueName() {
        return failureQueueName;
    }

    public void setFailureQueueName(String failureQueueName) {
        this.failureQueueName = failureQueueName;
    }

    public boolean isFailureQueueNameSet() {
        return (null != failureQueueName && !failureQueueName.isEmpty());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public OutboundReplyBehaviour getOutboundReplyBehaviour() {
        return outboundReplyBehaviour;
    }

    public void setOutboundReplyBehaviour(OutboundReplyBehaviour outboundReplyBehaviour) {
        this.outboundReplyBehaviour = outboundReplyBehaviour;
    }

    public String getResponseQueue() {
        return responseQueue;
    }

    public void setResponseQueue(String responseQueue) {
        this.responseQueue = responseQueue;
    }

    public OutboundCorrelationBehaviour getOutboundCorrelationBehaviour() {
        return outboundCorrelationBehaviour;
    }

    public void setOutboundCorrelationBehaviour(OutboundCorrelationBehaviour outboundCorrelationBehaviour) {
        this.outboundCorrelationBehaviour = outboundCorrelationBehaviour;
    }

    public String[] getTlsProtocols() {
        return tlsProtocols;
    }

    public void setTlsProtocols(String[] tlsProtocols) {
        if (tlsProtocols == null || tlsProtocols.length < 1) {
            this.tlsProtocols = DEFAULT_TLS_PROTOCOL_LIST.toArray(new String[]{});
        } else {
            this.tlsProtocols = tlsProtocols;
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
