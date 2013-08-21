package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ImmutableClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.PostStartupApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caching for ClusterProperties
 *
 * @author Steve Jones
 */
@ManagedResource(description="Cluster Property Cache", objectName="l7tech:type=ClusterPropertyCache")
public class ClusterPropertyCache implements PostStartupApplicationListener {

    //- PUBLIC

    /**
     * Bean
     */
    public ClusterPropertyCache() {
    }

    /**
     * Set the cluster property manager.
     *
     * <p>This will cause all properties to be cached.</p>
     *
     * @param clusterPropertyManager The manager to use
     * @throws IllegalArgumentException if the given manager is null
     * @throws IllegalStateException if the manager is already set.
     */
    public void setClusterPropertyManager(final ClusterPropertyManager clusterPropertyManager) {
        synchronized (propLock) {
            if (clusterPropertyManager == null)
                throw new IllegalArgumentException("clusterPropertyManager must not be null");
            if (this.clusterPropertyManager != null)
                throw new IllegalStateException("clusterPropertyManager already set!");
        }

        try {
            Collection<ClusterProperty> props = clusterPropertyManager.findAll();
            Map<String, ClusterProperty> clusterProperties = new TreeMap<String, ClusterProperty>(String.CASE_INSENSITIVE_ORDER);
            for(ClusterProperty prop : props) {
                if (prop.getName() != null)
                    clusterProperties.put(prop.getName(), new ImmutableClusterProperty(prop));
            }
            clusterPropertyCacheRef.set(Collections.unmodifiableMap(clusterProperties));
        }
        catch(FindException fe) {
            logger.log(Level.WARNING, "Error loading cluster properties", fe);
        }

        synchronized (propLock) {
            this.clusterPropertyManager = clusterPropertyManager;
        }
    }

    /**
     * Set the cluster property listener.
     *
     * @param clusterPropertyListener The listener to use
     * @throws IllegalArgumentException if the given listener is null
     * @throws IllegalStateException if the listener is already set.
     */
    public void setClusterPropertyListener(final ClusterPropertyListener clusterPropertyListener) {
        synchronized (propLock) {
            if (clusterPropertyListener == null)
                throw new IllegalArgumentException("clusterPropertyListener must not be null");
            if (this.clusterPropertyListener != null)
                throw new IllegalStateException("clusterPropertyListener already set!");

            this.clusterPropertyListener = clusterPropertyListener;
        }
    }    

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eiEvent = (EntityInvalidationEvent) event;
            if (ClusterProperty.class.equals(eiEvent.getEntityClass())) {
                ClusterPropertyManager cpm;
                ClusterPropertyListener cpl;
                synchronized (propLock) {
                    cpm = clusterPropertyManager;
                    cpl = clusterPropertyListener;
                }
                if (cpm == null) {
                    logger.warning("Received entity invalidation event but cluster property manager is not set!");
                    return;
                }

                Goid[] updatedIds = eiEvent.getEntityIds();
                if (updatedIds != null && updatedIds.length > 0) {
                    Map<String, ClusterProperty> currentProps = clusterPropertyCacheRef.get();
                    Map<String, ClusterProperty> updatedProps = new TreeMap<String, ClusterProperty>(String.CASE_INSENSITIVE_ORDER);
                    if (currentProps != null) {
                        updatedProps.putAll(currentProps);
                    }

                    List<ClusterProperty[]> updatedList = new ArrayList<ClusterProperty[]>();
                    List<ClusterProperty> deletedList = new ArrayList<ClusterProperty>();
                    for (Goid goid : updatedIds) {
                        try {
                            ClusterProperty updated = cpm.findByPrimaryKey(goid);
                            if (updated != null && updated.getName() != null) {
                                logger.log(Level.FINE, "Property ''{0}'', updated.", updated.getName());
                                updated = new ImmutableClusterProperty(updated);
                                ClusterProperty old = updatedProps.put(updated.getName(), updated);
                                updatedList.add(new ClusterProperty[]{old, updated});
                            } else if (updated == null) {
                                for (Iterator<ClusterProperty> propIter = updatedProps.values().iterator(); propIter.hasNext();) {
                                    ClusterProperty property = propIter.next();
                                    if (property.getGoid().equals(goid)) {
                                        propIter.remove();
                                        deletedList.add(property);
                                        break;
                                    }
                                }
                            }
                        }
                        catch(FindException fe) {
                            logger.log(Level.WARNING, "Error loading cluster property", fe);
                        }
                    }

                    clusterPropertyCacheRef.set(Collections.unmodifiableMap(updatedProps));

                    for (ClusterProperty[] updated : updatedList) {
                        fireChanged(cpl, updated[0], updated[1]);
                    }
                    for (ClusterProperty deleted : deletedList) {
                        fireDeleted(cpl, deleted);
                    }
                }
            }
        }
    }

    /**
     *
     */
    public ClusterProperty getCachedEntityByName(final String name) {
        ClusterProperty clusterProperty = null;

        Map<String,ClusterProperty> cache = clusterPropertyCacheRef.get();
        if (cache != null)
            clusterProperty = cache.get(name);

        return clusterProperty;
    }

    /**
     * @deprecated Use {@link #getCachedEntityByName(String)}
     */
    @Deprecated
    @SuppressWarnings({"UnusedDeclaration"})
    public ClusterProperty getCachedEntityByName(final String name, final int maxAge) {
        ClusterProperty clusterProperty = null;

        Map<String,ClusterProperty> cache = clusterPropertyCacheRef.get();
        if (cache != null)
            clusterProperty = cache.get(name);

        return clusterProperty;
    }

    /**
     *
     */
    @ManagedOperation(description="Get Property Value")
    public String getPropertyValue( final String name ) {
        String value = null;

        ClusterProperty clusterProperty = getCachedEntityByName( name );
        if ( clusterProperty != null ) {
            value = clusterProperty.getValue();
        }

        return value;
    }

    /**
     *
     */
    @ManagedAttribute(description="Cached Property Names", currencyTimeLimit=30)
    public Set<String> getPropertyNames() {
        Set<String> names = new TreeSet<String>();

        Map<String,ClusterProperty> cache = clusterPropertyCacheRef.get();
        if ( cache != null ) {
            names.addAll( cache.keySet() );
        }

        return names;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ClusterPropertyCache.class.getName());

    private final AtomicReference<Map<String,ClusterProperty>> clusterPropertyCacheRef = new AtomicReference<Map<String,ClusterProperty>>();
    private final Object propLock = new Object();
    private ClusterPropertyManager clusterPropertyManager;
    private ClusterPropertyListener clusterPropertyListener;

    private void fireChanged(final ClusterPropertyListener cpl, final ClusterProperty classic, final ClusterProperty updated) {
        if (cpl != null) {
            try {
                cpl.clusterPropertyChanged(classic, updated);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cluster property listener threw exception.", e);
            }
        }
    }

    private void fireDeleted(final ClusterPropertyListener cpl, final ClusterProperty deleted) {
        if (cpl != null) {
            try {
                cpl.clusterPropertyDeleted(deleted);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cluster property listener threw exception.", e);
            }
        }
    }
}
