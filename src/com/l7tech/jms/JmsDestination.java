/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.jms;

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
public class JmsDestination extends NamedEntityImp implements Serializable {
    private JmsProvider _provider;
    private String _destinationName;
    private JmsReplyType _replyType;

    /** Optional, set only if {@link #_replyType} is {@link com.l7tech.jms.JmsReplyType#REPLY_TO_OTHER} */
    private JmsDestination _replyDestination;

    /** Optional */
    private JmsDestination _failureDestination;

    public JmsProvider getProvider() {
        return _provider;
    }

    public void setProvider(JmsProvider provider) {
        _provider = provider;
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

    public JmsDestination getReplyDestination() {
        return _replyDestination;
    }

    public void setReplyDestination(JmsDestination replyTo) {
        _replyDestination = replyTo;
    }

    public JmsDestination getFailureDestination() {
        return _failureDestination;
    }

    public void setFailureDestination(JmsDestination failureDestination) {
        _failureDestination = failureDestination;
    }
}
