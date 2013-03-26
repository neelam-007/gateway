package com.l7tech.server.cluster;

import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.util.MessageId;
import com.l7tech.server.util.MessageIdManager;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses JBossCache to maintain a distributed cache of message IDs, in order to detect
 * attempts at replaying the same messages to different cluster nodes.
 * <p>
 * This service is used by the {@link com.l7tech.policy.assertion.xmlsec.WssReplayProtection} assertion.
 *
 * @author alex
 * @author mike
 */
@ManagedResource(description="JGroups Distributed Cache", objectName="l7tech:type=JGroups")
public class DistributedMessageIdManager extends HibernateDaoSupport implements MessageIdManager {

    //- PUBLIC

    public DistributedMessageIdManager( final ServerConfig config, final String clusterNodeId ) {
        File configDir = config.getLocalDirectoryProperty( ServerConfigParams.PARAM_CONFIG_DIRECTORY, false );
        String file = null;
        if ( configDir != null ) {
            File jgroupsFile = new File( configDir, "jgroups-config.xml" );
            if ( jgroupsFile.exists() ) {
                file = jgroupsFile.getAbsolutePath();
            }
        }

        this.config = config;
        this.clusterNodeId = clusterNodeId;
        jgroupsConfigFile = file;
    }

    /**
     * Initialize the service using the specified multicast IP address and port
     * @param address the IP address to use for multicast UDP communications
     * @param port the UDP port to use for multicast communications (IN)
     * @param interfaceAddress The IP address of the primary interface
     * @throws Exception
     */
    public void initialize( final String address, final int port, final String interfaceAddress) throws Exception {
        if (initialized) {
            throw new IllegalStateException("Already Initialized");
        }

        boolean useMulticast = config.getBooleanProperty( ServerConfigParams.PARAM_MULTICAST_ENABLED, true);

        cacheLock.readLock().lock();
        try {
            cache = useMulticast
                    ? new JgroupsMessageIdCache(address, port, interfaceAddress, this.jgroupsConfigFile)
                    : new SingleNodeMessageIdCache(clusterNodeId);

            //Clean up the expired data before putting the message to cache.
            (new GarbageCollectionTask()).run();

            populateCacheIfRequired();
        } finally {
            cacheLock.readLock().unlock();            
        }
        initialized = true;
    }

    @ManagedAttribute(description="Group Members", currencyTimeLimit=30)
    public List<String> getMemberIpAddresses() {
        final List<String> addresses = new ArrayList<String>();
        
        cacheLock.readLock().lock();
        try {
            cache.getMemberAddresses(addresses);
        } finally {
            cacheLock.readLock().unlock();
        }

        return Collections.unmodifiableList(addresses);
    }

    /**
     * Closes the service (just calls {@link #flush(Session)} at the moment
     * @throws Exception
     */
    public void close() throws Exception {
        getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) {
                cacheLock.readLock().lock();
                try {
                    flush(session);
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e); // can't happen
                } finally {
                    cacheLock.readLock().lock();                    
                }
            }
        });

        cacheLock.readLock().lock(); // get read lock to ensure the cache is not restarted while shutting down
        try {
            cache.destroy();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Verifies that the specified {@link MessageId} has not been seen by this cluster before.
     * 
     * @param prospect the {@link MessageId} to check for uniqueness
     * @throws DuplicateMessageIdException if the given {@link MessageId} was seen previously
     */
    @Override
    public void assertMessageIdIsUnique( final MessageId prospect ) throws MessageIdCheckException {
        boolean retry = true;
        while ( retry ) {
            try {
                cacheLock.readLock().lock();
                try {
                    Long expires = cache.get(prospect.getOpaqueIdentifier());
                    if ( isExpired( expires, System.currentTimeMillis() ) ) {
                        cache.put(prospect.getOpaqueIdentifier(), prospect.getNotValidOnOrAfterDate());
                        return;
                    } else {
                        retry = false;
                    }
                } finally {
                    cacheLock.readLock().unlock();    
                }
            } catch ( Exception e ) {
                if ( !handleError(e) ) {
                    final String msg = "Failed to determine whether a MessageId is a replay : " + ExceptionUtils.getMessage( e );
                    logger.log( Level.SEVERE, msg , ExceptionUtils.getDebugException( e ) );
                    throw new MessageIdCheckException( msg, e );
                }
            }
        }
        
        // We must have either returned or thrown by now unless it is a replay
        throw new DuplicateMessageIdException();
    }

    //- PRIVATE

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Config config;
    private final String clusterNodeId;

    private MessageIdCache cache;
    boolean initialized = false;
    private final String jgroupsConfigFile;


    /**
     * The cache is thread safe, the lock is to ensure we don't restart the underlying
     * JGroups channel when anyone is using it.
     */
    private static final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static final AtomicLong attemptedCacheRestartTime = new AtomicLong(0);
    private static final boolean CACHE_RESTART_ENABLED = ConfigFactory.getBooleanProperty("com.l7tech.server.cluster.cacheRestartEnabled", true);
    private static final long MIN_RESTART_INTERVAL = 60000;

    /**
     * Populate the cache if it is empty.
     *
     * Caller should hold a read or write lock for the cache.
     */
    private void populateCacheIfRequired() {
        if (cache.isPopulateNeeded()) {
            logger.info( "Populating message ID cache." );

            // If we're the first, load old message ids from database
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate( final Session session ) {
                    try {
                        session.doWork( new Work(){
                            @Override
                            public void execute( final Connection connection ) throws SQLException {
                                populateCache( connection );
                            }
                        } );
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    /**
     * Loads any unexpired message IDs from the database into the distributed cache.
     *
     * Caller should hold a read or write lock for the cache.
     */
    private void populateCache( final Connection conn ) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        //Do not do cache.startBatch() and endBatch, with Batch, the data purge may not work properly,
        //the node is not adding to the ExpirationAlgorithm
        try {
            ps = conn.prepareStatement("SELECT messageid, expires FROM message_id");
            rs = ps.executeQuery();
            final long now = System.currentTimeMillis();
            while (rs.next()) {
                String id = cache.fromDatabaseId(rs.getString(1));
                if (id != null) {
                    long expires = rs.getLong(2);
                    if (expires >= now) {
                        if (logger.isLoggable(Level.FINE)) logger.fine("Reloading saved message ID '" + id + "' from database");
                        cache.put(id, expires > 0 ? -expires : expires);
                    }
                }
            }
        } finally {
            ResourceUtils.closeQuietly( rs );
            ResourceUtils.closeQuietly( ps );
        }
    }

    /**
     * Flushes any unexpired message IDs that are in the distributed cache to the database.
     * <p>
     * Once each message ID has been flushed, the sign of its expiry time is flipped to negative,
     * so that the service can avoid writing the same record more than once.
     *
     * Caller should hold read lock for the cache.
     *
     * @throws Exception
     */
    private void flush( final Session session ) throws Exception {
        // if we're the last one out the door, turn out the lights

        // Build message IDs to save
        final Map<String,Long> toSave = new HashMap<String,Long>();
        {
            final Set<String> ids = cache.getAll();
            if (ids == null) return;
            for ( final String id : ids ) {
                if ( id == null ) continue;
                Long expires = cache.get(id);
                toSave.put(id, expires);
            }
        }

        // save message ids to database
        final Map<String,Long> saved = new HashMap<String,Long>();
        session.doWork( new Work(){
            @Override
            public void execute( final Connection conn ) throws SQLException {
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    ps = conn.prepareStatement("INSERT INTO message_id (messageid, expires) VALUES (?,?)");
                    for ( final Map.Entry<String,Long> entry : toSave.entrySet() ) {
                        final String id = entry.getKey();
                        final Long expires = entry.getValue();

                        if ( expires != null && expires > 0 ) {
                            ps.clearParameters();
                            ps.setString( 1, cache.toDatabaseId(id) );
                            ps.setLong( 2, expires );
                            try {
                                ps.executeUpdate();
                                saved.put( id, expires );
                            } catch (SQLException e) {
                                // Don't care
                                logger.log( Level.FINE, "Caught SQLException inserting record", e );
                            }
                        }
                    }
                    conn.commit();
                } finally {
                    ResourceUtils.closeQuietly( rs );
                    ResourceUtils.closeQuietly( ps );
                }
            }
        } );

        // Flip expiry sign to avoid saving the same record again
        cache.startBatch();
        try {
            for ( final Map.Entry<String,Long> entry : saved.entrySet() ) {
                final String id = entry.getKey();
                final Long expires = entry.getValue();
                    if ( expires >= 0 ) {
                        Long expiry = -expires;
                        cache.put(id, expiry);
                    }
            }
        } finally {
            cache.endBatch(true);
        }
    }

    /**
     * Attempt to handle the given exception.
     *
     * @return true if handled
     */
    private boolean handleError( final Exception e ) {
        boolean handled = false;

        if ( CACHE_RESTART_ENABLED && cache.isTimeoutError(e)) {
            // Timeout handling is a work around for bug 7574, which leaves JGroups in a
            // state in which all futher use of the cache will fail.
            long timenow = System.currentTimeMillis();
            try {
                if ( cacheLock.writeLock().tryLock( 5, TimeUnit.SECONDS ) ) {
                    try {
                        long lastRestartTime = attemptedCacheRestartTime.get();
                        if ( lastRestartTime < timenow ) {
                            if ( lastRestartTime + MIN_RESTART_INTERVAL < timenow ) {
                                attemptedCacheRestartTime.set( System.currentTimeMillis() );
                                logger.warning( "Restarting message ID cache due to '" +ExceptionUtils.getMessage( e )+ "'." );
                                cache.restart();

                                // restarting the cache could cause the contents to be
                                // lost if we are the only member
                                populateCacheIfRequired();

                                handled = true;
                            }
                        } else {
                            handled = true; // someone else restarted while we were waiting for the lock
                        }
                    } finally {
                        cacheLock.writeLock().unlock();
                    }
                }
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
        }

        return handled;
    }

    private static boolean isExpired( final Long expires, final long now ) {
        boolean expired = true;

        if ( expires != null ) {
            final long exp = Math.abs( expires.longValue() );
            expired = exp < now;
        }

        return expired;
    }

    /**
     * A {@link TimerTask} that periodically purges expired message IDs from the distributed cache and database.
     */
    private class GarbageCollectionTask extends TimerTask {
        @Override
        public void run() {
            getHibernateTemplate().execute(new HibernateCallback() {
                @Override
                public Object doInHibernate(Session session) {
                    cacheLock.readLock().lock();
                    try {
                        doRun(session);
                    } finally {
                        cacheLock.readLock().unlock();
                    }
                    return null;
                }
            });
        }

        private void doRun( final Session session ) {
            final long now = System.currentTimeMillis();
            try {
                session.doWork( new Work(){
                    @Override
                    public void execute( final Connection conn ) throws SQLException {
                        PreparedStatement ps = null;
                        try {
                            // remove expired message ids from database
                            ps = conn.prepareStatement("DELETE FROM message_id WHERE expires < ?");
                            ps.setLong(1, now);
                            int num = ps.executeUpdate();
                            conn.commit();
                            if (num > 0) if (logger.isLoggable(Level.FINE)) logger.fine("Deleted " + num + " stale message ID entries from database");
                        } finally {
                            ResourceUtils.closeQuietly( ps );
                        }
                    }
                } );

            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Caught exception in Message ID Garbage Collection task", e );
            }
        }
    }
}
