/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.cluster;

import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import org.jboss.cache.PropertyConfigurator;
import org.jboss.cache.TreeCache;
import org.jboss.cache.transaction.DummyTransactionManager;
import org.jboss.cache.transaction.DummyUserTransaction;

import javax.naming.Context;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                if (names != null) {
                    List toBeRemoved = new ArrayList(names.size()/2);
                    for ( Iterator i = names.iterator(); i.hasNext(); ) {
                        String id = (String)i.next();
                        Long expires = (Long)tree.get(MESSAGEID_PARENT_NODE + "/" + id, EXPIRES_ATTR);
                        if (expires == null) continue; // Maybe someone else removed it
                        final long exp = Math.abs(expires.longValue());
                        if (exp < now) {
                            // Expired
                            toBeRemoved.add(id);
                        }
                    }

                    for (Iterator i = toBeRemoved.iterator(); i.hasNext();) {
                        String id = (String)i.next();
                        tree.remove(MESSAGEID_PARENT_NODE + "/" + id);
                        logger.fine("Removing expired message ID " + id + " from replay cache");
                    }

                    tx.commit();
                } else {
                    tx.rollback();
                }
                tx = null;

                flush();

                HibernatePersistenceContext context = null;
                Connection conn = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    // load old message ids from database
                    context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
                    conn = context.getSession().connection();
                    ps = conn.prepareStatement("DELETE FROM message_id WHERE expires < ?");
                    ps.setLong(1, now);
                    int num = ps.executeUpdate();
                    if (num > 0) logger.fine("Deleted " + num + " stale message ID entries from database");
                    conn.commit();
                    conn = null;
                } finally {
                    if (conn != null) try {
                        conn.rollback();
                    } finally {
                        if (rs != null) try {
                            rs.close();
                        } finally {
                            if (ps != null) ps.close();
                        }
                    }
                }
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Caught exception in Message ID Garbage Collection task", e );
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
        HibernatePersistenceContext context = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            // load old message ids from database
            context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
            conn = context.getSession().connection();
            ps = conn.prepareStatement("SELECT messageid, expires FROM message_id");
            rs = ps.executeQuery();
            final long now = System.currentTimeMillis();
            while (rs.next()) {
                String id = rs.getString(1);
                long expires = rs.getLong(2);
                if (expires >= now) {
                    logger.fine("Reloading saved message ID '" + id + "' from database");
                    tree.put(MESSAGEID_PARENT_NODE + "/" + id, EXPIRES_ATTR, new Long(expires > 0 ? -expires : expires));
                }
            }
            conn.commit();
            conn = null;
        } finally {
            if (conn != null) try {
                conn.rollback();
            } finally {
                if (rs != null) try {
                    rs.close();
                } finally {
                    if (ps != null) ps.close();
                }
            }
        }
    }

    void close() throws Exception {
        flush();
    }

    private void flush() throws Exception {
        // if we're the last one out the door, turn out the lights
        HibernatePersistenceContext context = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        UserTransaction tx = null;
        try {
            // save message ids to database
            context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
            conn = context.getSession().connection();
            ps = conn.prepareStatement("INSERT INTO message_id (messageid, expires) VALUES (?,?)");
            final Set ids = tree.getChildrenNames(MESSAGEID_PARENT_NODE);
            if (ids == null) return;
            Map saved = new HashMap();
            for (Iterator i = ids.iterator(); i.hasNext();) {
                String id = (String)i.next();
                if (id == null) continue;

                Long expires = (Long)tree.get(MESSAGEID_PARENT_NODE + "/" + id, EXPIRES_ATTR);
                if (expires != null && expires.longValue() > 0) {
                    ps.clearParameters();
                    ps.setString(1, id);
                    ps.setLong(2, expires.longValue());
                    try {
                        ps.executeUpdate();
                        saved.put(id, expires);
                    } catch (SQLException e) {
                        // Don't care
                        logger.log(Level.FINE, "Caught SQLException inserting record", e);
                    }
                }
            }
            conn.commit();
            conn = null;

            // Flip expiry sign to avoid saving the same record again
            tx = new DummyUserTransaction(DummyTransactionManager.getInstance());
            tx.begin();

            for (Iterator i = saved.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String id = (String)entry.getKey();
                Long expires = (Long)entry.getValue();
                tree.put(MESSAGEID_PARENT_NODE + "/" + id, EXPIRES_ATTR, new Long(-expires.longValue()));
            }

            tx.commit();
            tx = null;
        } finally {
            if (tx != null) try {
                tx.rollback();
            } finally {
                if (conn != null) try {
                    conn.rollback();
                } finally {
                    if (rs != null) try {
                        rs.close();
                    } finally {
                        if (ps != null) ps.close();
                    }
                }
            }
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
            Long expires = (Long)tree.get(MESSAGEID_PARENT_NODE + "/" + prospect.getOpaqueIdentifier(), EXPIRES_ATTR);
            if (expires == null) {
                tree.put(MESSAGEID_PARENT_NODE + "/" + prospect.getOpaqueIdentifier(),
                         EXPIRES_ATTR, new Long(prospect.getNotValidOnOrAfterDate()));
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
    private TreeCache tree;

    private static final int GC_PERIOD = 1 * 30 * 1000;
    private static final String MESSAGEID_PARENT_NODE = DistributedMessageIdManager.class.getName() + "/messageId";
    private static final String EXPIRES_ATTR = "expires";
}
