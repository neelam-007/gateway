package com.l7tech.server.cluster;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.ServerConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ImmutableClusterProperty;

/**
 * Caching for ClusterProperties
 *
 * @author Steve Jones
 */
@ManagedResource(description="Cluster Property Cache", objectName="l7tech:type=ClusterPropertyCache")
public class ClusterPropertyCache implements ApplicationListener {

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
            Map<String,ClusterProperty> clusterProperties = new HashMap<String,ClusterProperty>(props.size());
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
     * Set the server config.
     *
     * @param serverConfig The ServerConfig to use for default values.
     * @throws IllegalArgumentException if the given ServerConfig is null
     * @throws IllegalStateException if the ServerConfig is already set.
     */
    public void setServerConfig(final ServerConfig serverConfig) {
        synchronized (propLock) {
            if (serverConfig == null)
                throw new IllegalArgumentException("serverConfig must not be null");
            if (this.serverConfig != null)
                throw new IllegalStateException("serverConfig already set!");

            this.serverConfig = serverConfig;
        }
        this.defaultValues = serverConfig.getClusterPropertyDefaults();
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

                long[] updatedOids = eiEvent.getEntityIds();
                if (updatedOids != null && updatedOids.length > 0) {
                    Map<String,ClusterProperty> currentProps = clusterPropertyCacheRef.get();
                    Map<String,ClusterProperty> updatedProps;
                    if (currentProps == null) {
                        updatedProps = new HashMap<String,ClusterProperty>();
                    } else {
                        updatedProps = new HashMap<String,ClusterProperty>(currentProps);
                    }

                    List<ClusterProperty[]> updatedList = new ArrayList<ClusterProperty[]>();
                    List<ClusterProperty> deletedList = new ArrayList<ClusterProperty>();
                    for (long oid : updatedOids) {
                        try {
                            ClusterProperty updated = cpm.findByPrimaryKey(oid);
                            if (updated != null && updated.getName() != null) {
                                logger.log(Level.FINE, "Property ''{0}'', updated.", updated.getName());
                                updated = new ImmutableClusterProperty(updated);
                                ClusterProperty old = updatedProps.put(updated.getName(), updated);
                                updatedList.add(new ClusterProperty[]{old, updated});
                            } else if (updated == null) {
                                for (Iterator<ClusterProperty> propIter = updatedProps.values().iterator(); propIter.hasNext();) {
                                    ClusterProperty property = propIter.next();
                                    if (property.getOid() == oid) {
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

        ClusterProperty clusterProperty = getCachedEntityByName( name, 10000 );
        if ( clusterProperty != null ) {
            value = clusterProperty.getValue();
        }

        return value;
    }

    @ManagedOperation(description="Get Property Value With Default Fallback")
    public String getPropertyValueWithDefaultFallback( final String name ) {
        String value = getPropertyValue(name);
        return (value != null || defaultValues == null) ? value : defaultValues.get(name);
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
    private ServerConfig serverConfig;
    private Map<String, String> defaultValues;

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
