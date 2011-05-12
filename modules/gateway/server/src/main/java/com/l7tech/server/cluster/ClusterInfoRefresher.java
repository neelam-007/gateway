package com.l7tech.server.cluster;

import javax.inject.Inject;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Updates cluster info when ESM interface tag changes.
 */
public class ClusterInfoRefresher implements PropertyChangeListener {

    @Inject
    private ClusterInfoManager clusterInfoManager;

    @Override
    public void propertyChange( final PropertyChangeEvent evt ) {
        if ( "admin.esmInterfaceTag".equals( evt.getPropertyName() ) ) {
            clusterInfoManager.getSelfNodeInf( true );
        }
    }
}
