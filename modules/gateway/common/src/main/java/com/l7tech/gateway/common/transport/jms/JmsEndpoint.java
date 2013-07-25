/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.transport.jms;

import com.l7tech.gateway.common.security.password.SecurePasswordReferenceExpander;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;
import com.l7tech.policy.wsp.WspSensitive;
import com.l7tech.search.Dependency;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.net.PasswordAuthentication;

/**
 * A reference to a preconfigured JMS Destination (i.e. a Queue or Topic).
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="jms_endpoint")
public class JmsEndpoint extends ZoneableNamedGoidEntityImp implements Serializable {
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1;

    private Goid _connectionGoid;
    private boolean queue = true;
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
    private boolean template;
    private long requestMaxSize = -1;
    private Long oldOid = null;

    public JmsEndpoint(){
    }

    public JmsEndpoint( final JmsEndpoint jmsEndpoint, final boolean readOnly ){
        copyFrom( jmsEndpoint );
        if (readOnly) lock();
    }

    public void copyFrom( JmsEndpoint other ) {
        setGoid(other.getGoid());
        setVersion( other.getVersion() );
        setName( other.getName() );
        setQueue( other.isQueue() );
        setConnectionGoid(other.getConnectionGoid());
        setDestinationName( other.getDestinationName() );
        setFailureDestinationName( other.getFailureDestinationName() );
        setAcknowledgementType( other.getAcknowledgementType() );
        setReplyType( other.getReplyType() );
        setUsername( other.getUsername() );
        setTemplate( other.isTemplate() );
        setPassword( other.getPassword() );
        setMaxConcurrentRequests( other.getMaxConcurrentRequests() );
        setMessageSource( other.isMessageSource() );
        setReplyToQueueName( other.getReplyToQueueName() );
        setOutboundMessageType( other.getOutboundMessageType() );
        setDisabled(other.isDisabled());
        setUseMessageIdForCorrelation(other.isUseMessageIdForCorrelation());
        setRequestMaxSize(other.getRequestMaxSize());
        setSecurityZone(other.getSecurityZone());
        setOldOid(other.getOldOid());
    }

    /**
     * May be null.
     *
     * @param securePasswordReferenceExpander reference expander for looking up secure password references.  Required.
     * @return a PasswordAuthentication for the JMS endpoint, or null.
     * @throws FindException if there is an error looking up a secure password instance.
     */
    @Transient
    public PasswordAuthentication getPasswordAuthentication(SecurePasswordReferenceExpander securePasswordReferenceExpander) throws FindException {
        return _username != null && _password != null
               ? new PasswordAuthentication( _username, securePasswordReferenceExpander.expandPasswordReference(_password) )
               : null;
    }

    @Size(min=1,max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    @Size(max=255)
    @Column(name="username",length=255)
    public String getUsername() {
        return _username;
    }

    public void setUsername( String username ) {
        checkLocked();
        _username = username;
    }

    @Size(max=255)
    @Column(name="password",length=255)
    @WspSensitive
    @Dependency(type = Dependency.DependencyType.SECURE_PASSWORD, methodReturnType = Dependency.MethodReturnType.VARIABLE)
    public String getPassword() {
        return _password;
    }

    public void setPassword( String password ) {
        checkLocked();
        _password = password;
    }

    @Column(name="max_concurrent_requests")
    public int getMaxConcurrentRequests() {
        return _maxConcurrentRequests;
    }

    /**
     * @return true if the endpoint is inbound (that is the ssg gets messages from the queue);
     *         false if the endpoint is outbound (that is the gateway routes messages to the queue).
     */
    @Column(name="is_message_source")
    public boolean isMessageSource() {
        return _messageSource;
    }

    /**
     * @return true if the endpoint is inbound (that is the ssg gets messages from the queue);
     *         false if the endpoint is outbound (that is the gateway routes messages to the queue).
     */
    public void setMessageSource( boolean messageSource ) {
        checkLocked();
        _messageSource = messageSource;
    }

    /**
     * Sets the maximum number of concurrent requests
     * @param maxConcurrentRequests
     */
    public void setMaxConcurrentRequests( int maxConcurrentRequests ) {
        checkLocked();
        _maxConcurrentRequests = maxConcurrentRequests;
    }

    @Column(name="connection_goid", nullable=false)
    @Dependency(methodReturnType = Dependency.MethodReturnType.OID, type = Dependency.DependencyType.JMS_CONNECTION)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getConnectionGoid() {
        return _connectionGoid;
    }

    public void setConnectionGoid(Goid conn) {
        checkLocked();
        _connectionGoid = conn;
    }


    @Column(name="old_objectid")
    public Long getOldOid() {
        return oldOid;
    }

    // only for hibernate
    @Deprecated
    public void setOldOid(Long oldOid) {
        this.oldOid = oldOid;
    }


    /**
     * Is the destination a Queue or Topic.
     *
     * @return True if the destination is a queue.
     */
    @Column(name="destination_type")
    public boolean isQueue() {
        return queue;
    }

    public void setQueue( final boolean queue ) {
        checkLocked();
        this.queue = queue;
    }


    @Pattern(regexp=".*?[^\\p{Space}].*") // at least one non-space character
    @NotNull(groups=StandardValidationGroup.class)
    @Size(min=1,max=128)
    @Column(name="destination_name", length=128)
    public String getDestinationName() {
        return _destinationName;
    }

    public void setDestinationName(String name) {
        checkLocked();
        _destinationName = name;
    }

    @Pattern(regexp=".*?[^\\p{Space}].*") // at least one non-space character
    @Size(max=128)
    @Column(name="failure_destination_name", length=128)
    public String getFailureDestinationName() {
        return _failureDestinationName;
    }

    public void setFailureDestinationName(String name) {
        checkLocked();
        _failureDestinationName = name;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="acknowledgement_type")
    public JmsAcknowledgementType getAcknowledgementType() {
        return _acknowledgementType;
    }

    public void setAcknowledgementType(JmsAcknowledgementType acknowledgementType) {
        checkLocked();
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
        checkLocked();
        _replyType = replyType;
    }

    @Pattern(regexp=".*?[^\\p{Space}].*") // at least one non-space character
    @Size(max=128)
    @Column(name="reply_to_queue_name", length=128)
    public String getReplyToQueueName() {
        return replyToQueueName;
    }

    /** @param replyTo Optional, set only if {@link #getReplyType} is {@link com.l7tech.gateway.common.transport.jms.JmsReplyType#REPLY_TO_OTHER} */
    public void setReplyToQueueName(String replyTo) {
        checkLocked();
        replyToQueueName = replyTo;
    }

    @Enumerated(EnumType.STRING)
    @Column(name="outbound_message_type")
    public JmsOutboundMessageType getOutboundMessageType() {
        return outboundMessageType;
    }
                               
    public void setOutboundMessageType(JmsOutboundMessageType outboundMessageType) {
        checkLocked();
        if (outboundMessageType == null) outboundMessageType = JmsOutboundMessageType.AUTOMATIC;
        this.outboundMessageType = outboundMessageType;
    }

    @Column(name="disabled")
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        checkLocked();
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
        checkLocked();
        this.useMessageIdForCorrelation = useMessageIdForCorrelation;
    }

    @Column(name="is_template")
    public boolean isTemplate() {
        return template;
    }

    public void setTemplate(final boolean template) {
        checkLocked();
        this.template = template;
    }

    @Column(name="request_max_size", nullable=false)
    public long getRequestMaxSize() {
        return requestMaxSize;
    }

    public void setRequestMaxSize(long requestMaxSize) {
        checkLocked();
        this.requestMaxSize = requestMaxSize;
    }

    @Override
    public String toString() {
        return "<JmsEndpoint connectionGoid=\"" + _connectionGoid + "\" name=\"" + _name + "\"/>";
    }

    /**
     * Standard validation group with additional constraints for non-templates.
     */
    public interface StandardValidationGroup {}
}
