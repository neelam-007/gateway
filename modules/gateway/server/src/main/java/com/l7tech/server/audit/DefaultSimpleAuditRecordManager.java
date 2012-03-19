package com.l7tech.server.audit;

import com.l7tech.server.EntityManagementContextProvider;
import com.l7tech.util.Config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default implementation of simple audit record manager.
 *
 * <p>This handles switching of the audit record manager instance based on the
 * current configuration.</p>
 */
public class DefaultSimpleAuditRecordManager extends DelegatingSimpleAuditRecordManager implements PropertyChangeListener {

    //- PUBLIC

    public DefaultSimpleAuditRecordManager( final Config config,
                                            final SimpleAuditRecordManager internalManager,
                                            final EntityManagementContextProvider entityManagementContextProvider ) {
        this.config = config;
        this.internalManager = internalManager;
        this.entityManagementContextProvider = entityManagementContextProvider;
        resetAuditDelegate();
    }

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        resetAuditDelegate();
    }

    //- PROTECTED

    @Override
    protected SimpleAuditRecordManager delegate() {
        return delegate.get();
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( DefaultSimpleAuditRecordManager.class.getName() );

    private AtomicReference<SimpleAuditRecordManager> delegate = new AtomicReference<SimpleAuditRecordManager>();
    private final Config config;
    private final SimpleAuditRecordManager internalManager;
    private final EntityManagementContextProvider entityManagementContextProvider;

    private void resetAuditDelegate() {
        boolean reset = false;
        final String name = config.getProperty( "audit.external.name" );
        if ( name != null && !name.isEmpty() ) {
            try {
                delegate.set( entityManagementContextProvider.getEntityManagementContext( name ).getEntityManager( SimpleAuditRecordManager.class ) );
                reset = true;
            } catch ( Exception e ) {
                logger.log( Level.WARNING, "Error setting audit delegate", e );
            }
        }

        if ( !reset ) {
            delegate.set( internalManager );
        }
    }
}
