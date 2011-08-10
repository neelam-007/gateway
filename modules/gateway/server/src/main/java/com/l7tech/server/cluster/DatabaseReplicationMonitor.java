package com.l7tech.server.cluster;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.server.event.system.DatabaseReplicationCheckEvent;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidatedConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Monitors the replication delay between primary and secondary databases.
 */
public class DatabaseReplicationMonitor implements PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(DatabaseReplicationMonitor.class.getName());

    private static final String TYPE_REPLICATED = "replicated";
    private static final String PROP_CHECK_INTERVAL = "db.replicationDelayCheckInterval";
    private static final String PROP_THRESHOLD = "db.replicationDelayThreshold";
    private static final String PROP_ERROR_INTERVAL = "db.replicationErrorAuditInterval";

    private static final String DEFAULT_SQL_UPDATE = "INSERT INTO replication_status (objectid, sequence, updated, nodeid, delay) VALUES (1,?,?,?,?) ON DUPLICATE KEY UPDATE sequence=VALUES(sequence),updated=VALUES(updated),nodeid=VALUES(nodeid),delay=VALUES(delay)";
    private static final String DEFAULT_SQL_QUERY = "SELECT sequence FROM replication_status WHERE objectid=1 AND nodeid=? AND updated > ?";
    private static final String DEFAULT_SQL_DELAY = "SELECT delay, sequence FROM replication_status WHERE objectid=1";


    private static final String SQL_UPDATE = ConfigFactory.getProperty( "com.l7tech.server.cluster.replicationUpdate", DEFAULT_SQL_UPDATE );
    private static final String SQL_QUERY = ConfigFactory.getProperty( "com.l7tech.server.cluster.replicationQuery", DEFAULT_SQL_QUERY );
    private static final String SQL_DELAY = ConfigFactory.getProperty( "com.l7tech.server.cluster.replicationDelayQuery", DEFAULT_SQL_DELAY );

    private static final long INITIAL_DELAY = ConfigFactory.getLongProperty( "com.l7tech.server.cluster.replicationInitialCheckDelay", TimeUnit.MINUTES.toMillis( 1L ) );

    private final Config config;
    private final ClusterMaster clusterMaster;
    private final String primaryDataSourceDescription;
    private final String secondaryDataSourceDescription;
    private final DataSource primaryDataSource;
    private final DataSource secondaryDataSource;
    private final DataSource poolDataSource;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Audit audit;
    private final Timer timer;
    private final String nodeId;
    private final String clusterType;
    private final Object taskLock = new Object();
    private ReplicationDelayCheckTask task;
    private final AtomicLong replicationDelay = new AtomicLong(0L);

    public DatabaseReplicationMonitor( final Config config,
                                       final ClusterMaster clusterMaster,
                                       final String primaryDataSourceDescription,
                                       final DataSource primaryDataSource,
                                       final String secondaryDataSourceDescription,
                                       final DataSource secondaryDataSource,
                                       final DataSource poolDataSource,
                                       final AuditFactory auditFactory,
                                       final ApplicationEventPublisher applicationEventPublisher,
                                       final String nodeId,
                                       final String clusterType,
                                       final Timer timer ) {
        this.config = validated( config );
        this.clusterMaster = clusterMaster;
        this.primaryDataSourceDescription = primaryDataSourceDescription;
        this.secondaryDataSourceDescription = secondaryDataSourceDescription;
        this.primaryDataSource = primaryDataSource;
        this.secondaryDataSource = secondaryDataSource;
        this.poolDataSource = poolDataSource;
        this.applicationEventPublisher = applicationEventPublisher;
        this.audit = auditFactory.newInstance( this, logger );
        this.nodeId = nodeId;
        this.clusterType = clusterType;
        this.timer = timer;
    }

    public void init() {
        scheduleReplicationChecking();
    }

    /**
     * Get the current replication delay.
     *
     * @return The replication delay in seconds
     */
    public long getReplicationDelay() {
        return replicationDelay.get();
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        logger.config( "Reloading database replication monitoring configuration." );
        scheduleReplicationChecking();
    }

    private void scheduleReplicationChecking() {
        synchronized ( taskLock ) {
            long sequence = 0L;
            if ( task != null ) {
                task.cancel();
                sequence = task.sequence;
                task = null;
            }

            final long checkInterval = config.getTimeUnitProperty( PROP_CHECK_INTERVAL, 10000L );
            if ( TYPE_REPLICATED.equals( clusterType ) && checkInterval > 0L ) {
                logger.info( "(Re)Scheduling database replication monitoring." );
                task = new ReplicationDelayCheckTask(
                        checkInterval,
                        config.getTimeUnitProperty( PROP_THRESHOLD, TimeUnit.SECONDS.toMillis( 60L ) ) / TimeUnit.SECONDS.toMillis( 1L ),
                        config.getTimeUnitProperty( PROP_ERROR_INTERVAL, TimeUnit.HOURS.toMillis( 1L ) ),
                        sequence
                );
                timer.schedule( task, INITIAL_DELAY, checkInterval );
            }
        }
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig validatedConfig = new ValidatedConfig( config, logger );
        validatedConfig.setMinimumValue( PROP_CHECK_INTERVAL, 0 );
        validatedConfig.setMinimumValue( PROP_THRESHOLD, 0 );
        validatedConfig.setMinimumValue( PROP_ERROR_INTERVAL, 0 );
        return validatedConfig;
    }

    private final class ReplicationDelayCheckTask extends ManagedTimerTask {
        private static final long DELAY_ERROR = Long.MAX_VALUE;
        private final long updatePeriod;
        private final long errorThreshold;
        private final long errorSuppressPeriod;
        private long sequence = 0L;
        private long created = System.currentTimeMillis() + INITIAL_DELAY;
        private long lastErrorAudit;
        private boolean wasMaster;

        private ReplicationDelayCheckTask( final long updatePeriod,
                                           final long errorThreshold,
                                           final long errorSuppressPeriod,
                                           final long sequence ) {
            this.updatePeriod = updatePeriod;
            this.errorThreshold = errorThreshold;
            this.errorSuppressPeriod = errorSuppressPeriod;
            this.sequence = sequence;
        }

        @Override
        protected void doRun() {
            final boolean isMaster = clusterMaster.isMaster();
            final long currentTime = System.currentTimeMillis();
            final long staleTime = currentTime - TimeUnit.HOURS.toMillis(24L);
            String dataSourceDescription = primaryDataSourceDescription;
            try {
                if ( isMaster ) {
                    final long sequence = ++this.sequence;

                    // Update primary sequence and include current delay for use by other nodes
                    final JdbcTemplate primaryTemplate = new JdbcTemplate( primaryDataSource );
                    primaryTemplate.update( SQL_UPDATE, sequence, currentTime, nodeId, replicationDelay.get() );

                    // Read the secondary sequence
                    dataSourceDescription = secondaryDataSourceDescription;
                    final JdbcTemplate secondaryTemplate = new JdbcTemplate( secondaryDataSource );
                    final long secondarySequence = secondaryTemplate.queryForLong( SQL_QUERY, nodeId, staleTime );

                    // Process delay in seconds
                    final long delay = ((sequence - secondarySequence) * updatePeriod) / TimeUnit.SECONDS.toMillis( 1L );
                    if ( delay < 0L ) {
                        if ( (System.currentTimeMillis() - created) > errorThreshold ) {
                            replicationDelay.set( DELAY_ERROR );
                        }
                    } else {
                        replicationDelay.set( delay );
                    }
                    auditDelay();
                } else {
                    final JdbcTemplate poolTemplate = new JdbcTemplate( poolDataSource );
                    final Pair<Long,Long> delaySequencePair = poolTemplate.queryForObject( SQL_DELAY, new RowMapper<Pair<Long, Long>>() {
                        @Override
                        public Pair<Long, Long> mapRow( final ResultSet resultSet, final int row ) throws SQLException {
                            return new Pair<Long,Long>( resultSet.getLong( 1 ), resultSet.getLong( 2 ) );
                        }
                    } );
                    final long delay = delaySequencePair.left;
                    replicationDelay.set( delay );
                    sequence = delaySequencePair.right;
                }
            } catch ( IncorrectResultSizeDataAccessException e ) {
                // Replicated data not created by us or is too stale
                // This will occur once when the master node switches, else it is an error
                if ( (System.currentTimeMillis() - created) > errorThreshold && wasMaster==isMaster) {
                    replicationDelay.set( DELAY_ERROR );
                }
                if ( isMaster ) {
                    auditDelay();
                }
            } catch ( DataAccessException e ) {
                replicationDelay.set( DELAY_ERROR );
                auditErrorIfNotSuppressed(
                    SystemMessages.MONITOR_DB_REPLICATION_ERROR,
                    new String[]{dataSourceDescription, ExceptionUtils.getMessage( e )},
                    ExceptionUtils.getDebugException( e ) );
            } finally {
                wasMaster = isMaster;
                applicationEventPublisher.publishEvent( new DatabaseReplicationCheckEvent( DatabaseReplicationMonitor.this, Component.GW_SERVER ) );
            }
        }

        private void auditErrorIfNotSuppressed( final AuditDetailMessage message,
                                                final String[] parameters,
                                                final Throwable throwable ) {
            if ( (System.currentTimeMillis() - lastErrorAudit) > errorSuppressPeriod ) {
                lastErrorAudit = System.currentTimeMillis();
                audit.logAndAudit( message, parameters, throwable );
            }
        }

        private void auditDelay() {
            final long delay = replicationDelay.get();
            if ( (System.currentTimeMillis() - created) > errorThreshold &&
                  delay >= errorThreshold  ) {
                if ( delay == DELAY_ERROR ) {
                    auditErrorIfNotSuppressed(
                        SystemMessages.MONITOR_DB_REPLICATION_FAILED,
                        new String[]{ secondaryDataSourceDescription, "error calculating delay" },
                        null );
                } else {
                    auditErrorIfNotSuppressed(
                            SystemMessages.MONITOR_DB_REPLICATION_FAILED,
                            new String[]{ secondaryDataSourceDescription, "delay is " + delay + " seconds" },
                            null);
                }
            } else {
                if ( lastErrorAudit > 0L ) {
                    lastErrorAudit = 0L;
                    audit.logAndAudit( SystemMessages.MONITOR_DB_REPLICATION_RECOVERED, secondaryDataSourceDescription );
                }
            }
        }
    }
}
