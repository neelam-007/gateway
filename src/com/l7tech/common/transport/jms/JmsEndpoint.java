/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import java.io.Serializable;
import java.net.PasswordAuthentication;

/**
 * A reference to a preconfigured JMS Destination (i.e. a Queue or Topic).
 *
 * Persistent.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsEndpoint extends NamedEntityImp implements Serializable, Comparable {
    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 1;

    private long _connectionOid;
    private String _destinationName;
    private JmsReplyType _replyType = JmsReplyType.AUTOMATIC;
    private String _username;
    private String _password;
    private int _maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
    private boolean _messageSource;
    /** Optional, set only if {@link #_replyType} is {@link com.l7tech.common.transport.jms.JmsReplyType#REPLY_TO_OTHER} */
    private JmsEndpoint _replyEndpoint;
    /** Optional */
    private JmsEndpoint _failureEndpoint;

    public void copyFrom( JmsEndpoint other ) {
        setOid( other.getOid() );
        setVersion( other.getVersion() );
        setName( other.getName() );
        setConnectionOid( other.getConnectionOid() );
        setDestinationName( other.getDestinationName() );
        setReplyType( other.getReplyType() );
        setUsername( other.getUsername() );
        setPassword( other.getPassword() );
        setMaxConcurrentRequests( other.getMaxConcurrentRequests() );
        setMessageSource( other.isMessageSource() );
        setReplyEndpoint( other.getReplyEndpoint() );
        setFailureEndpoint( other.getFailureEndpoint() );
    }

    /**
     * May be null.
     * @return
     */
    public PasswordAuthentication getPasswordAuthentication() {
        return _username != null && _password != null 
               ? new PasswordAuthentication( _username, _password.toCharArray() )
               : null;
    }

    public EntityHeader toEntityHeader() {
        return new EntityHeader(Long.toString(getOid()), EntityType.JMS_ENDPOINT, getName(), getDestinationName());
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername( String username ) {
        _username = username;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword( String password ) {
        _password = password;
    }

    public int getMaxConcurrentRequests() {
        return _maxConcurrentRequests;
    }

    /**
     * @return true if the endpoint is outbound (that is the gateway routes messages to the
     * queue). false means inbound (that is the ssg gets messages from the queue)
     */
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

    public String toString() {
        return "<JmsEndpoint connectionOid=\"" + _connectionOid + "\" name=\"" + _name + "\"/>";
    }

    public long getConnectionOid() {
        return _connectionOid;
    }

    public void setConnectionOid(long conn) {
        _connectionOid = conn;
    }

    public String getDestinationName() {
        return _destinationName;
    }

    public void setDestinationName(String name) {
        _destinationName = name;
    }

    public JmsReplyType getReplyType() {
        if (_replyType == null)
            return JmsReplyType.AUTOMATIC;
        return _replyType;
    }

    public void setReplyType(JmsReplyType replyType) {
        _replyType = replyType;
    }

    public JmsEndpoint getReplyEndpoint() {
        return _replyEndpoint;
    }

    public void setReplyEndpoint(JmsEndpoint replyTo) {
        _replyEndpoint = replyTo;
    }

    public JmsEndpoint getFailureEndpoint() {
        return _failureEndpoint;
    }

    public void setFailureEndpoint(JmsEndpoint failureEndpoint) {
        _failureEndpoint = failureEndpoint;
    }

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
