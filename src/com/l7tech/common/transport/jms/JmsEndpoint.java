/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.io.Serializable;

/**
 * A reference to a preconfigured JMS Destination (i.e. a Queue or Topic).
 *
 * Persistent.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsEndpoint extends NamedEntityImp implements Serializable {
    private JmsConnection _connection;
    private String _destinationName;
    private JmsReplyType _replyType;
    private String _username;
    private String _password;

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

    public String toString() {
        return _connection.getName() + "/" + _name;
    }

    /** Optional, set only if {@link #_replyType} is {@link com.l7tech.common.transport.jms.JmsReplyType#REPLY_TO_OTHER} */
    private JmsEndpoint _replyEndpoint;

    /** Optional */
    private JmsEndpoint _failureEndpoint;

    public JmsConnection getConnection() {
        return _connection;
    }

    public void setConnection(JmsConnection conn) {
        _connection = conn;
    }

    public String getDestinationName() {
        return _destinationName;
    }

    public void setDestinationName(String name) {
        _destinationName = name;
    }

    public JmsReplyType getReplyType() {
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
}
