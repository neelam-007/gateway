/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.cluster;

import com.l7tech.common.util.Background;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import org.jboss.cache.PropertyConfigurator;
import org.jboss.cache.TreeCache;
import org.jboss.cache.lock.LockingException;
import org.jboss.cache.lock.TimeoutException;
import org.jboss.cache.transaction.DummyTransactionManager;
import org.jboss.cache.transaction.DummyUserTransaction;
import org.springframework.orm.hibernate.HibernateCallback;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import javax.naming.Context;
import javax.transaction.NotSupportedException;
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
 * Uses JBossCache to maintain a distributed cache of message IDs, in order to detect
 * attempts at replaying the same messages to different cluster nodes.
 * <p>
 * This service is used by the {@link com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection} assertion.
 *
 * @author alex
 * @author mike
 * @version $Revision$
 */
public class DistributedMessageIdManager extends HibernateDaoSupport implements MessageIdManager {
    /**
     * Initialize the service using the specified multicast IP address and port
     * @param address the IP address to use for multicast UDP communications
     * @param port the UDP port to use for multicast communications
     * @throws Exception
     */
    public void initialize(String address, int port) throws Exception {
        if (initialized) {
            throw new IllegalStateException("Already Initialized");
        }
        Properties prop = new Properties();
        prop.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.cache.transaction.DummyContextFactory");
        tree = new TreeCache();
        PropertyConfigurator config = new PropertyConfigurator();
        config.configure(tree, "treecache-service.xml");
        String props = tree.getClusterProperties();
        props = props.replaceFirst("mcast_addr=[0-9\\.]+", "mcast_addr=" + address);
        props = props.replaceFirst("mcast_port=[0-9]+", "mcast_port=" + port);
        tree.setClusterProperties(props);
        // if we're the first, load old message ids from database
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                try {
                    start(session);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        initialized = true;
    }

    /**
     * A {@link TimerTask} that periodically purges expired message IDs from the distributed cache and database.
     */
    private class GarbageCollectionTask extends TimerTask {
        public void run() {
          getHibernateTemplate().execute(new HibernateCallback() {
              public Object doInHibernate(Session session) throws HibernateException, SQLException {
                  doRun(session);
                  return null;
              }
          });
        }
        private void doRun(Session session) {
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
                flush(session);

                Connection conn = null;
                PreparedStatement ps = null;
                try {
                    // load old message ids from database
                    conn = session.connection();
                    ps = conn.prepareStatement("DELETE FROM message_id WHERE expires < ?");
                    ps.setLong(1, now);
                    int num = ps.executeUpdate();
                    if (num > 0) logger.fine("Deleted " + num + " stale message ID entries from database");
                    conn = null;
                } finally {
                    try {
                        if (conn != null) conn.rollback();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Caught exception rolling back JDBC transaction", e);
                    }

                    try {
                        if (ps != null) ps.close();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Caught exception closing PreparedStatement", e);
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

    /**
     * Starts the service, loads any unexpired message IDs from the database into the distributed cache,
     * and starts the expiration garbage collection timer.
     *
     * @throws Exception
     */
    private void start(Session session) throws Exception {
        tree.startService(); // kick start tree cache

        // Perturb delay to avoid synchronization with other cluster nodes
        long when = GC_PERIOD * 2 + new Random().nextInt(1 + (int)GC_PERIOD/4);
        Background.schedule(new GarbageCollectionTask(), when, GC_PERIOD);
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            // load old message ids from database
            conn = session.connection();
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
            conn = null;
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
                logger.log( Level.WARNING, "Caught exception while trying to close ResultSet", e );
            }

            try {
                if (ps != null) ps.close();
            } catch (Exception e) {
                logger.log( Level.WARNING, "Caught exception while trying to close PreparedStatement", e );
            }
        }
    }

    /**
     * Closes the service (just calls {@link #flush(Session)} at the moment
     * @throws Exception
     */
    public void close() throws Exception {
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                try {
                    flush(session);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e); // can't happen
                }
            }
        });
    }

    /**
     * Flushes any unexpired message IDs that are in the distributed cache to the database.
     * <p>
     * Once each message ID has been flushed, the sign of its expiry time is flipped to negative,
     * so that the service can avoid writing the same record more than once.
     * @throws Exception
     */
    private void flush(Session session) throws Exception, SQLException, TimeoutException, LockingException, SystemException, NotSupportedException {
        // if we're the last one out the door, turn out the lights
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        UserTransaction tx = null;
        try {
            // save message ids to database
            conn = session.connection();
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
            try {
                if (tx != null) tx.rollback();
            } catch (Exception e) {
                logger.log( Level.WARNING, "Caught exception while trying to roll back JGroups transaction", e );
            }

            try {
                if (conn != null) conn.rollback();
            } catch (Exception e) {
                logger.log( Level.WARNING, "Caught exception while trying to roll back JDBC transaction", e );
            }

            try {
                if (rs != null) rs.close();
            } catch (Exception e) {
                logger.log( Level.WARNING, "Caught exception while trying to roll back ResultSet", e );
            }

            try {
                if (ps != null) ps.close();
            } catch (Exception e) {
                logger.log( Level.WARNING, "Caught exception while trying to close PreparedStatement", e );
            }
        }
    }

    /**
     * Verifies that the specified {@link MessageId} has not been seen by this cluster before
     * @param prospect the {@link MessageId} to check for uniqueness
     * @throws DuplicateMessageIdException if the given {@link MessageId} was seen previously
     */
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

    private TreeCache tree;
    boolean initialized = false;

    private static final int GC_PERIOD = 5 * 60 * 1000;
    private static final String MESSAGEID_PARENT_NODE = DistributedMessageIdManager.class.getName() + "/messageId";
    private static final String EXPIRES_ATTR = "expires";
}
