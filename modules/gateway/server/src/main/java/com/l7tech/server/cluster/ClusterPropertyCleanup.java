package com.l7tech.server.cluster;

import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.ObjectModelException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Bean to delete the cluster properties once the SSG is running.
 *
 * <p>This is intended to perform removal of properties that are no longer
 * required.</p>
 */
public class ClusterPropertyCleanup implements ApplicationListener {

    //- PUBLIC

    public ClusterPropertyCleanup( final Set<String> properties,
                                   final PlatformTransactionManager transactionManager,
                                   final ClusterPropertyManager clusterPropertyManager ) {
        if ( properties == null ) {
            throw new IllegalArgumentException("properties is required");            
        }
        if ( transactionManager == null ) {
            throw new IllegalArgumentException("transactionManager is required");            
        }
        if ( clusterPropertyManager == null ) {
            throw new IllegalArgumentException("clusterPropertyManager is required");
        }

        this.properties = properties;
        this.transactionManager = transactionManager;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( !run && event instanceof ReadyForMessages) {
            run = true;
            deleteProperties();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ClusterPropertyCleanup.class.getName());

    private volatile boolean run = false;
    private final PlatformTransactionManager transactionManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final Set<String> properties;

    private void deleteProperties() {
        final List<String> deletedProperties = new ArrayList<String>();
        try {
            TransactionTemplate template = new TransactionTemplate( transactionManager );
            template.setPropagationBehavior( TransactionTemplate.PROPAGATION_REQUIRES_NEW );
            template.execute(new TransactionCallbackWithoutResult(){
                protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                    try {
                        for ( String propertyName : properties ) {
                            ClusterProperty clusterProperty = clusterPropertyManager.findByUniqueName(propertyName);
                            if ( clusterProperty != null ) {
                                deletedProperties.add( propertyName );
                                clusterPropertyManager.delete(clusterProperty);
                            }
                        }
                    } catch ( ObjectModelException ome ) {
                        transactionStatus.setRollbackOnly();
                        logger.log( Level.WARNING, "Error deleting cluster properties " + deletedProperties + ".", ome );
                    }
                }
            });
            if ( !deletedProperties.isEmpty() ) {
                logger.log( Level.INFO, "Deleted cluster properties " + deletedProperties + "." );
            }
        } catch ( TransactionException e ) {
            logger.log( Level.WARNING, "Error deleting properties " + deletedProperties + ".", e );
        }
    }
}
