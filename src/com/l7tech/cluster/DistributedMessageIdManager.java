/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.cluster;

import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import org.jboss.cache.PropertyConfigurator;
import org.jboss.cache.TreeCache;
import org.jboss.cache.transaction.DummyTransactionManager;
import org.jboss.cache.transaction.DummyUserTransaction;

import javax.naming.Context;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class DistributedMessageIdManager implements MessageIdManager {
    static void initialize(String address, int port) throws Exception {
        if (singleton != null) throw new IllegalStateException("Can only initialize once");
        singleton = new DistributedMessageIdManager(address, port);
        // if we're the first, load old message ids from database
        singleton.start();
    }

    private class GarbageCollectionTask extends TimerTask {
        public void run() {
            final long now = System.currentTimeMillis();
            UserTransaction tx = null;
            try {
                tx = new DummyUserTransaction(DummyTransactionManager.getInstance());
                tx.begin();
                Set names = tree.getChildrenNames(MESSAGEID_PARENT_NODE);
                if (names == null) return;
                for ( Iterator i = names.iterator(); i.hasNext(); ) {
                    String name = (String)i.next();
                    Long expires = (Long)tree.get(name, EXPIRES_ATTR);
                    if (expires == null) continue; // Maybe someone else removed it
                    if (expires.longValue() < now ) {
                        // Expired
                        logger.info("Removing stale message ID " + name + " that expired " +
                                    (now - expires.longValue()) + "ms ago");
                        tree.remove(name);
                    }
                }
                tx.commit();
                tx = null;
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Caught exception while trying to begin garbage collection transaction", e );
            } finally {
                try {
                    if (tx != null) tx.rollback();
                } catch ( SystemException e1 ) {
                    logger.log( Level.WARNING, "Caught exception while trying to rollback garbage collection transaction", e1 );
                }
            }
        }
    }

    private void start() throws Exception {
        tree.startService(); // kick start tree cache

        gcTimer = new Timer(true);
        // Perturb delay to avoid synchronization with other cluster nodes
        long when = GC_PERIOD * 2 + new Random().nextInt(1 + (int)GC_PERIOD/4);
        gcTimer.schedule(new GarbageCollectionTask(), when, GC_PERIOD);
        UserTransaction tx = null;
        try {
            tx = new DummyUserTransaction(DummyTransactionManager.getInstance());
            tx.begin();
            Integer state = (Integer)tree.get(STATE_NODE, STATE_ATTR);
            tx.commit();
            tx = null;
            if ( state == null ) {
                // TODO load old message ids from database
            } else if ( STATE_SHUTTINGDOWN.equals(state) ) {
                throw new IllegalStateException("Initialize was called when the cluster thinks it's shutting down");
            } else if ( !STATE_OPERATIONAL.equals(state) ) {
                throw new IllegalStateException("Cluster state unknown: " + state);
            }
        } finally {
            if (tx != null) tx.rollback();
        }
    }

    void close() throws Exception {
        // if we're the last one out the door, turn out the lights
        UserTransaction tx = null;
        try {
            tx = new DummyUserTransaction(DummyTransactionManager.getInstance());
            tx.begin();
            Integer state = (Integer)tree.get(STATE_NODE, STATE_ATTR);
            if ( state == null ) {
                logger.info("Close was called when the cluster thinks it hasn't started yet");
            } else if ( STATE_SHUTTINGDOWN.equals(state) ) {
                logger.info("Another node is cleaning up");
            } else if ( STATE_OPERATIONAL.equals(state) ) {
                if (tree.getMembers().size() == 1) {
                    logger.info("Last cluster node shutting down, will save replay records");
                    tree.put(STATE_NODE, STATE_ATTR, STATE_SHUTTINGDOWN);
                    tx.commit();
                    // TODO save message ids to database
                    // TODO set state to DOWN
                    tx = null;
                } else {
                    logger.info("Cluster node shutting down. Another node should save replay records");
                }
            } else {
                throw new IllegalStateException("Cluster state unknown: " + state);
            }
        } finally {
            if (tx != null) tx.rollback();
        }
    }

    public static DistributedMessageIdManager getInstance() {
        if ( singleton == null ) throw new IllegalStateException("Must be initialized");
        return singleton;
    }

    private DistributedMessageIdManager( String address, int port ) throws Exception {
        Properties prop = new Properties();
        prop.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.cache.transaction.DummyContextFactory");
        tree = new TreeCache();
        PropertyConfigurator config = new PropertyConfigurator();
        config.configure(tree, "treecache-service.xml");
        String props = tree.getClusterProperties();
        props = props.replaceFirst("mcast_addr=[0-9\\.]+", "mcast_addr=" + address);
        props = props.replaceFirst("mcast_port=[0-9]+", "mcast_port=" + port);
        tree.setClusterProperties(props);
    }

    public void assertMessageIdIsUnique(MessageId prospect) throws DuplicateMessageIdException {
        UserTransaction tx = null;
        try {
            tx = new DummyUserTransaction(DummyTransactionManager.getInstance());
            tx.begin();
            final String messageIdNodeName = MESSAGEID_PARENT_NODE + "/" + prospect.getOpaqueIdentifier();
            Long expires = (Long)tree.get(messageIdNodeName, EXPIRES_ATTR);
            if (expires == null) {
                tree.put(messageIdNodeName, EXPIRES_ATTR, new Long(prospect.getNotValidOnOrAfterDate()) );
                tx.commit();
                tx = null;
                return;
            }
        } catch ( Exception e ) {
            final String msg = "Failed to determine whether a MessageId is a replay";
            logger.log( Level.SEVERE, msg, e );
            throw new RuntimeException(msg,e);
        } finally {
            try {
                if (tx != null) tx.rollback();
            } catch ( SystemException e ) {
                final String msg = "Unable to rollback transaction";
                logger.log( Level.WARNING, msg, e );
                throw new RuntimeException(msg, e);
            }
        }
        // We must have either returned or thrown by now
        throw new DuplicateMessageIdException();
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private static DistributedMessageIdManager singleton;

    private Timer gcTimer;
    private static final int GC_PERIOD = 4000;

    private TreeCache tree;
    private static final String STATE_NODE = DistributedMessageIdManager.class.getName() + "/state";
    private static final String MESSAGEID_PARENT_NODE = DistributedMessageIdManager.class.getName() + "/messageId";

    private static final String EXPIRES_ATTR = "expires";
    private static final String STATE_ATTR = "state";

    private static final Integer STATE_OPERATIONAL = new Integer(1);
    private static final Integer STATE_SHUTTINGDOWN = new Integer(2);

}
