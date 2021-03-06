package com.l7tech.server.service;

import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Implementation that does nothing when metrics are sent to the database.
 */
public class DisabledServiceMetricsServices implements ServiceMetricsServices, PropertyChangeListener {

    @Override
    public void addRequest( final Goid serviceGoid,
                            final String operation,
                            final User authorizedUser,
                            final List<MessageContextMapping> mappings,
                            final boolean authorized,
                            final boolean completed,
                            final int frontTime,
                            final int backTime ) {
        // Do nothing.
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void trackServiceMetrics( final Goid serviceGoid ) {
        // Do nothing.
    }

    @Override
    public int getFineInterval() {
        return DEF_FINE_BIN_INTERVAL;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Do nothing.
    }
}
