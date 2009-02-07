/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.transport.jms;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import java.io.Serializable;
import java.net.PasswordAuthentication;

import org.hibernate.annotations.Proxy;

/**
 * A reference to a preconfigured JMS Destination (i.e. a Queue or Topic).
 *
 * Persistent.
  */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="jms_endpoint")
public class JmsEndpoint extends NamedEntityImp implements Serializable, Comparable {
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1;

    private long _connectionOid;
    private String _destinationName;
    private String _failureDestinationName;
    private JmsAcknowledgementType _acknowledgementType;
    private JmsReplyType _replyType = JmsReplyType.AUTOMATIC;
    private String _username;
    private String _password;
    private int _maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
    private boolean _messageSource;
    private String replyToQueueName;
    private JmsOutboundMessageType outboundMessageType = JmsOutboundMessageType.AUTOMATIC;
    private boolean disabled;
    private boolean useMessageIdForCorrelation;

    public void copyFrom( JmsEndpoint other ) {
        setOid( other.getOid() );
        setVersion( other.getVersion() );
        setName( other.getName() );
        setConnectionOid( other.getConnectionOid() );
        setDestinationName( other.getDestinationName() );
        setFailureDestinationName( other.getFailureDestinationName() );
        setAcknowledgementType( other.getAcknowledgementType() );
        setReplyType( other.getReplyType() );
        setUsername( other.getUsername() );
        setPassword( other.getPassword() );
        setMaxConcurrentRequests( other.getMaxConcurrentRequests() );
        setMessageSource( other.isMessageSource() );
        setReplyToQueueName( other.getReplyToQueueName() );
        setOutboundMessageType( other.getOutboundMessageType() );
        setDisabled(other.isDisabled());
        setUseMessageIdForCorrelation(other.isUseMessageIdForCorrelation());
    }

    /**
     * May be null.
     */
    @Transient
    public PasswordAuthentication getPasswordAuthentication() {
        return _username != null && _password != null
               ? new PasswordAuthentication( _username, _password.toCharArray() )
               : null;
    }

    @Column(name="username",length=32)
    public String getUsername() {
        return _username;
    }

    public void setUsername( String username ) {
        _username = username;
    }

    @Column(name="password",length=32)
    public String getPassword() {
        return _password;
    }

    public void setPassword( String password ) {
        _password = password;
    }

    @Column(name="max_concurrent_requests")
    public int getMaxConcurrentRequests() {
        return _maxConcurrentRequests;
    }

    /**
     * @return true if the endpoint is outbound (that is the gateway routes messages to the
     * queue). false means inbound (that is the ssg gets messages from the queue)
     */
    @Column(name="is_message_source")
    public boolean isMessageSource() {
        return _messageSource;
    }

    /**
     * @param messageSource true if the endpoint is outbound (that is the gateway routes messages to the
     * queue). false means inbound (that is the ssg gets messages from the queue)
     */
    public void setMessageSource( boolean messageSource ) {
        _messageSource = messageSource;
    }

    /**
     * Sets the maximum number of concurrent requests
     * @param maxConcurrentRequests
     */
    public void setMaxConcurrentRequests( int maxConcurrentRequests ) {
        _maxConcurrentRequests = maxConcurrentRequests;
    }

    @Column(name="connection_oid", nullable=false)
    public long getConnectionOid() {
        return _connectionOid;
    }

    public void setConnectionOid(long conn) {
        _connectionOid = conn;
    }

    @Column(name="destination_name", nullable=false, length=128)
    public String getDestinationName() {
        return _destinationName;
    }

    public void setDestinationName(String name) {
        _destinationName = name;
    }

    @Column(name="failure_destination_name", length=128)
    public String getFailureDestinationName() {
        return _failureDestinationName;
    }

    public void setFailureDestinationName(String name) {
        _failureDestinationName = name;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="acknowledgement_type")
    public JmsAcknowledgementType getAcknowledgementType() {
        return _acknowledgementType;
    }

    public void setAcknowledgementType(JmsAcknowledgementType acknowledgementType) {
        _acknowledgementType = acknowledgementType;
    }

    @Enumerated
    @Column(name="reply_type")
    public JmsReplyType getReplyType() {
        if (_replyType == null)
            return JmsReplyType.AUTOMATIC;
        return _replyType;
    }

    public void setReplyType(JmsReplyType replyType) {
        _replyType = replyType;
    }

    @Column(name="reply_to_queue_name", length=128)
    public String getReplyToQueueName() {
        return replyToQueueName;
    }

    /** @param replyTo Optional, set only if {@link #getReplyType} is {@link com.l7tech.gateway.common.transport.jms.JmsReplyType#REPLY_TO_OTHER} */
    public void setReplyToQueueName(String replyTo) {
        replyToQueueName = replyTo;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="outbound_message_type")
    public JmsOutboundMessageType getOutboundMessageType() {
        return outboundMessageType;
    }
                               
    public void setOutboundMessageType(JmsOutboundMessageType outboundMessageType) {
        if (outboundMessageType == null) outboundMessageType = JmsOutboundMessageType.AUTOMATIC;
        this.outboundMessageType = outboundMessageType;
    }

    @Column(name="disabled")
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * True if {@link #_replyType} is {@link JmsReplyType#REPLY_TO_OTHER} and requests sent to this endpoint should not
     * have a JMSCorrelationID set (i.e. the receiver is expected to copy the reqest JMSMessageID into the response's
     * JMSCorrelationID field).  If false and {@link #_replyType} is {@link JmsReplyType#REPLY_TO_OTHER, a random
     * JMSCorrelationID value will be generated for the request, and the receiver will be expected to copy it into the
     * response's JMSCorrelationID field.
     */
    @Column(name="use_message_id_for_correlation")
    public boolean isUseMessageIdForCorrelation() {
        return useMessageIdForCorrelation;
    }

    public void setUseMessageIdForCorrelation(boolean useMessageIdForCorrelation) {
        this.useMessageIdForCorrelation = useMessageIdForCorrelation;
    }

    @Override
    public String toString() {
        return "<JmsEndpoint connectionOid=\"" + _connectionOid + "\" name=\"" + _name + "\"/>";
    }

    @Override
    public int compareTo(Object o) {
        if (o.getClass().equals(JmsEndpoint.class)) {
            JmsEndpoint that = (JmsEndpoint) o;
            if (this.getOid() < that.getOid())
                return -1;
            else if (this.getOid() > that.getOid())
                return 1;
            return 0;
        }
        throw new IllegalArgumentException("May only compare JmsEndpoint to other JmsEndpoints");
    }
}
