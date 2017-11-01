package com.l7tech.server.cluster;

import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Elects a single node as the "master" node in the cluster.
 */
public class ClusterMasterImpl implements ClusterMaster {

    //- PUBLIC

    public ClusterMasterImpl(final PlatformTransactionManager transactionManager,
                             final DataSource dataSource,
                             final Config config,
                             final Timer timer,
                             final String nodeId ) {
        timer.schedule( new  MasterCheckTask( transactionManager, dataSource, config, isMaster, nodeId ), 0L, MASTER_INTERVAL );
    }

    @Override
    public boolean isMaster() {
        return isMaster.get();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ClusterMasterImpl.class.getName());

    private static final long MASTER_INTERVAL = ConfigFactory.getLongProperty( "com.l7tech.server.cluster.masterInterval", 10000L );
    private static final long MASTER_TIMEOUT = ConfigFactory.getLongProperty( "com.l7tech.server.cluster.masterTimeout", 40000L );

    private final AtomicBoolean isMaster = new AtomicBoolean(false);

    private static final class MasterCheckTask extends TimerTask {
        private final Config config;
        private final PlatformTransactionManager transactionManager;
        private final SimpleJdbcTemplate jdbcTemplate;
        private final AtomicBoolean isMaster;
        private final String nodeId;

        MasterCheckTask( final PlatformTransactionManager transactionManager,
                         final DataSource dataSource,
                         final Config config,
                         final AtomicBoolean isMaster,
                         final String nodeId ) {
            this.config = config;
            this.transactionManager = transactionManager;
            this.jdbcTemplate = new SimpleJdbcTemplate( dataSource );
            this.isMaster = isMaster;
            this.nodeId = nodeId;
        }

        @Override
        public void run() {
            final boolean wasMaster = isMaster.get();
            final TransactionTemplate template = new TransactionTemplate( transactionManager );
            try {
                Boolean statusOnCommit = template.execute( new TransactionCallback<Boolean>(){
                    @Override
                    public Boolean doInTransaction( final TransactionStatus transactionStatus ) {
                        Boolean statusOnCommit = null;

                        final String configuredMasterNodeId = config.getProperty( "clusterMasterNode" );
                        if ( configuredMasterNodeId != null && !configuredMasterNodeId.isEmpty() ) {
                            isMaster.set( nodeId.equals(configuredMasterNodeId) );
                        } else {
                            final MasterInfo masterInfo = getMasterInfo();
                            final boolean master = nodeId.equals(masterInfo.getMasterNodeId());
                            isMaster.set( master );
                            if ( master ) {
                                if (!updateMasterTimestamp( masterInfo.getVersion() )) {
                                    isMaster.set( nodeId.equals(getMasterInfo().getMasterNodeId()) );
                                }
                            } else if ( isMasterStale( masterInfo.getTimestamp() ) ) {
                                statusOnCommit = becomeMaster( masterInfo.getVersion() );
                            }
                        }

                        return statusOnCommit;
                    }
                } );

                if ( statusOnCommit != null ) {
                    isMaster.set( statusOnCommit );
                }
            } catch ( TransactionException te ) {
                logger.log( Level.FINE, "Transaction commit failed '"+ExceptionUtils.getMessage(te)+"'.", ExceptionUtils.getDebugException(te));
            } catch ( DataAccessException use ) {
                SQLException se = ExceptionUtils.getCauseIfCausedBy( use, SQLException.class );
                if ( se != null && se.getErrorCode() == 1317 ) { // Error: 1317 SQLSTATE: 70100  (ER_QUERY_INTERRUPTED), can occur on shutdown
                    logger.log( Level.FINE, "Interrupted '"+ExceptionUtils.getMessage(se)+"'.", ExceptionUtils.getDebugException(se) );
                } else {
                    logger.log( Level.WARNING, "Error processing master status.", use );
                }
            }

            if ( wasMaster != isMaster.get() ) {
                if ( wasMaster ) {
                    logger.info( "Node master status lost." );
                } else {
                    logger.info( "Node is now master." );                   
                }
            }
        }

        public boolean updateMasterTimestamp( final int currentVersion ) {
            return 1==jdbcTemplate.update("UPDATE cluster_master SET touched_time=?, version=? WHERE version = ?", 
                    System.currentTimeMillis(),
                    currentVersion+1,
                    currentVersion );
        }

        public boolean isMasterStale( long timestamp ) {
            return (System.currentTimeMillis() - timestamp) > MASTER_TIMEOUT;
        }

        public MasterInfo getMasterInfo() {
            return jdbcTemplate.queryForObject( "SELECT nodeid, touched_time, version from cluster_master", new RowMapper<MasterInfo>(){
                @Override
                public MasterInfo mapRow( final ResultSet resultSet, final int i ) throws SQLException {
                    String nodeId = resultSet.getString( 1 );
                    Long timestamp = resultSet.getLong( 2 );
                    Integer version = resultSet.getInt( 3 );
                    return new MasterInfo( nodeId, timestamp, version );
                }
            } );
        }

        public boolean becomeMaster( final int currentVersion ) {
            return 1==jdbcTemplate.update("UPDATE cluster_master SET nodeid=?, touched_time=?, version=? WHERE version = ?",
                    nodeId,
                    System.currentTimeMillis(),
                    currentVersion+1,
                    currentVersion );
        }

        private static class MasterInfo extends Triple<String,Long,Integer> {
            MasterInfo( final String nodeId,
                        final Long timestamp,
                        final Integer version ){
                super( nodeId, timestamp, version );
            }

            public String getMasterNodeId() {
                return left;
            }

            public long getTimestamp() {
                return middle;
            }

            public int getVersion() {
                return right;
            }
        }
    }
}
