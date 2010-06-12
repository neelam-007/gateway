package com.l7tech.server.upgrade;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;

import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.ObjectModelException;

/**
 * Upgrade task to create / delete cluster properties.
 */
abstract class ClusterPropertyUpgradeTask implements UpgradeTask {

    //- PUBLIC

    public ClusterPropertyUpgradeTask( final Set<String> delete,
                                       final Map<String,String> create ) {
        this.delete = delete;
        this.create = create;
    }

    @Override
    public final void upgrade( final ApplicationContext applicationContext ) throws NonfatalUpgradeException, FatalUpgradeException {
        ClusterPropertyManager manager = getBean( applicationContext, "clusterPropertyManager", ClusterPropertyManager.class );

        try {
            for ( String name : delete ) {
                final ClusterProperty property = manager.findByUniqueName( name );
                if ( property != null ) {
                    logger.config( "Deleting cluster property '"+name+"'." );
                    manager.delete( property );
                }
            }

            for ( Map.Entry<String,String> entry : create.entrySet() ) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                final ClusterProperty property = manager.findByUniqueName( name );
                if ( property == null ) {
                    logger.config( "Creating cluster property '"+name+"'." );
                    manager.save( new ClusterProperty( name, value ) );
                }
            }
        } catch ( ObjectModelException ome ) {
            throw new NonfatalUpgradeException("Error upgrading cluster properties", ome);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ClusterPropertyUpgradeTask.class.getName() );

    private final Set<String> delete;
    private final Map<String,String> create;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({ "unchecked" })
    private <T> T getBean( final ApplicationContext applicationContext,
                           final String name,
                           final Class<T> type ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            return applicationContext.getBean(name, type);
        } catch ( BeansException be ) {
            throw new FatalUpgradeException("No bean " + name + " is available", be);
        }
    }
}
