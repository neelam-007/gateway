/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.jms;

import com.l7tech.jms.JmsDestination;
import com.l7tech.jms.JmsProvider;
import com.l7tech.jms.JmsReplyType;

/**
 * Message processing runtime support for JMS messages.
 *
 * Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class JmsReceiver {
    private final JmsProvider _provider;
    private final JmsReplyType _replyType;

    private final JmsDestination _inboundRequestDestination;
    private final JmsDestination _outboundResponseDestination;

    /**
     * Complete constructor
     *
     * @param provider The {@link com.l7tech.jms.JmsProvider} from which to receive messages
     * @param replyType A {@link com.l7tech.jms.JmsReplyType} value indicating this receiver's
     * reply semantics
     * @param inbound The {@link com.l7tech.jms.JmsDestination} from which to receive requests
     * @param outbound The {@link com.l7tech.jms.JmsDestination} into which to submit replies
     */
    public JmsReceiver( JmsProvider provider, JmsReplyType replyType,
                        JmsDestination inbound, JmsDestination outbound ) {
        _provider = provider;
        _replyType = replyType;
        _inboundRequestDestination = inbound;
        _outboundResponseDestination = outbound;
    }

    /**
     * Convenience constructor for one-way or reply-to-same configurations.
     * <p>
     * Use this constructor when the replyType is either
     * {@link com.l7tech.jms.JmsReplyType#NO_REPLY} or {@link com.l7tech.jms.JmsReplyType#REPLY_TO_SAME},
     * since in those cases there is no meaningful outbound destination.
     *
     * @param provider The {@link com.l7tech.jms.JmsProvider} from which to receive messages
     * @param replyType A {@link com.l7tech.jms.JmsReplyType} value indicating this receiver's
     * reply semantics
     * @param inbound The {@link com.l7tech.jms.JmsDestination} from which to receive requests
     */
    public JmsReceiver( JmsProvider provider, JmsReplyType replyType,
                        JmsDestination inbound ) {
        this( provider, replyType, inbound, null );
    }

    /**
     * Starts the receiver.
     */
    public void start() {
    }

    /**
     * Stops the receiver, e.g. temporarily.
     */
    public void stop() {
    }

    /**
     * Closes the receiver, and any resources it may have allocated.  Note that
     * a receiver that has been closed cannot be restarted.
     */
    public void close() {
    }

    public JmsProvider getProvider() {
        return _provider;
    }

    public JmsReplyType getReplyType() {
        return _replyType;
    }

    public JmsDestination getInboundRequestDestination() {
        return _inboundRequestDestination;
    }

    public JmsDestination getOutboundResponseDestination() {
        return _outboundResponseDestination;
    }
}
