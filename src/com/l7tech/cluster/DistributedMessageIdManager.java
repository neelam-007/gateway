/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.cluster;

import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import org.jgroups.Address;
import org.jgroups.MembershipListener;
import org.jgroups.View;
import org.jgroups.blocks.TransactionalHashtable;
import org.jgroups.blocks.Xid;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class DistributedMessageIdManager implements MessageIdManager {
    public static final String STATE_KEY = DistributedMessageIdManager.class.getName() + ".state";

    public static final Integer STATE_OPERATIONAL = new Integer(1);
    public static final Integer STATE_SHUTTINGDOWN = new Integer(2);

    static void initialize(String jgroupsProperties) throws Exception {
        if (singleton != null) throw new IllegalStateException("Can only initialize once");
        singleton = new DistributedMessageIdManager(jgroupsProperties);
        // if we're the first, load old message ids from database
        singleton.start();
    }

    private void start() throws Exception {
        try {
            hash.begin(Xid.REPEATABLE_READ);
            Integer state = (Integer)hash.get(STATE_KEY);
            if ( state == null ) {
                // TODO load old message ids from database
            } else if ( STATE_SHUTTINGDOWN.equals(state) ) {
                throw new IllegalStateException("Initialize was called when the cluster thinks it's shutting down");
            } else if ( !STATE_OPERATIONAL.equals(state) ) {
                throw new IllegalStateException("Cluster state unknown: " + state);
            }
        } finally {
            hash.commit();
        }
    }

    void close() throws Exception {
        // if we're the last one out the door, turn out the lights
        try {
            hash.begin();
            Integer state = (Integer)hash.get(STATE_KEY);
            if ( state == null ) {
                logger.info("Close was called when the cluster thinks it hasn't started yet");
            } else if ( STATE_SHUTTINGDOWN.equals(state) ) {
                logger.info("Another node is cleaning up");
            } else if ( STATE_OPERATIONAL.equals(state) ) {
                if (lastView != null && lastView.getMembers().size() == 1) {
                    logger.info("Last cluster node shutting down, will save replay records");
                    // TODO save message ids to database
                    hash.put(STATE_KEY, STATE_SHUTTINGDOWN);
                } else {
                    logger.info("Cluster node shutting down. Another node should save replay records");
                }
            } else {
                throw new IllegalStateException("Cluster state unknown: " + state);
            }
        } finally {
            hash.commit();
        }
    }

    public static DistributedMessageIdManager getInstance() {
        if ( singleton == null ) throw new IllegalStateException("Must be initialized");
        return singleton;
    }

    private DistributedMessageIdManager(String jgroupsProperties) throws Exception {
        hash = new TransactionalHashtable(getClass().getName(), jgroupsProperties, 5000);
        hash.setMembershipListener(new MembershipListener() {
            public void viewAccepted( View newView ) {
                logger.info("The cluster currently has " + newView.getMembers().size() + " members");
                lastView = newView;
            }

            public void suspect( Address suspect ) {
                logger.info("The cluster node at address " + suspect.toString() + " is suspected to be down");
            }

            public void block() {
                logger.warning("This cluster node is supposed to block but there's not much we can do");
            }
        });
    }

    public void assertMessageIdIsUnique(MessageId prospect) throws DuplicateMessageIdException {
        TransactionalHashtable h = hash;
        try {
            h.begin(Xid.READ_COMMITTED); // TODO is this too conservative?
            Long expires = (Long)hash.get(prospect.getOpaqueIdentifier());
            if (expires == null) {
                h.put(prospect.getOpaqueIdentifier(), new Long(prospect.getNotValidOnOrAfterDate()));
                h.commit();
                h = null;
                return;
            } else {
                throw new DuplicateMessageIdException();
            }
        } catch ( Exception e ) {
            final String msg = "Failed to determine whether a MessageId is a replay";
            logger.log( Level.SEVERE, msg, e );
            throw new RuntimeException(msg,e);
        } finally {
            if (h != null) h.rollback();
        }
    }

    private final TransactionalHashtable hash;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private static DistributedMessageIdManager singleton;

    private View lastView = null;
}
