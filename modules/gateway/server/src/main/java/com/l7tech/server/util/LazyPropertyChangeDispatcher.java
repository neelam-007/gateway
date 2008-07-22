package com.l7tech.server.util;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

/**
 * Spring context aware dispatcher for PropertyChangeEvents.
 *
 * <p>This dispatcher lazily accesses spring beans on the first event dispatch.</p>
 *
 * <p>This dispatcher supports filtering of events based on a set of property
 * names that the listener is interested in receiving.</p>
 *
 * @author Steve Jones
 */
public class LazyPropertyChangeDispatcher implements ApplicationContextAware, PropertyChangeListener {

    //- PUBLIC

    /**
     * Create a new dispatcher.
     *
     * @param beansToProps The map of String bean names to List<String> server config property names.
     */
    public LazyPropertyChangeDispatcher(Map beansToProps) {
        Map<String, List<String>> config = new LinkedHashMap<String, List<String>>();

        if (beansToProps != null) {
            for (Map.Entry entry : (Set<Map.Entry>)beansToProps.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                if (key instanceof String && value instanceof List) {
                    String beanName = (String) key;
                    List propNames = (List) value;

                    List<String> propertyNames = new ArrayList<String>(propNames.size());
                    for (Object prop : propNames) {
                        if (prop instanceof String) propertyNames.add((String) prop);
                    }

                    config.put(beanName, propertyNames);
                }
            }
        }

        configMap = Collections.unmodifiableMap(config);
    }

    /**
     *
     */
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        if (applicationContext == null) throw new IllegalArgumentException("applicationContext must not be null");
        listenerLock.writeLock().lock();
        try {
            if (this.applicationContext != null) throw new IllegalStateException("applicationContext is already set");
            this.applicationContext = applicationContext;
        }
        finally {
            listenerLock.writeLock().unlock();            
        }
    }

    /**
     *
     */
    public void propertyChange(final PropertyChangeEvent evt) {
        for (ListenerInfo listenerInfo : getListeners()) {
            if (listenerInfo.propertySet.contains(evt.getPropertyName())) {
                try {
                    listenerInfo.listener.propertyChange(evt);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unexpected exception dispatching property change event.", e);
                }
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(LazyPropertyChangeDispatcher.class.getName());

    private final Map<String, List<String>> configMap;
    private final ReadWriteLock listenerLock = new ReentrantReadWriteLock(false);
    private ApplicationContext applicationContext;
    private Collection<ListenerInfo> listeners = null;

    private Collection<ListenerInfo> getListeners() {
        Collection<ListenerInfo> info;
        listenerLock.readLock().lock();
        try {
            info = listeners;
        } finally {
            listenerLock.readLock().unlock();            
        }

        if (info == null) {
            listenerLock.writeLock().lock();
            try {
                info = listeners;
                if (info == null) {
                    Map<String, List<String>> configMap = this.configMap;
                    ApplicationContext ac = applicationContext;
                    if (configMap == null) {
                        info = Collections.emptyList();
                        listeners = info;
                    }
                    else {
                        List listenerList = new ArrayList<ListenerInfo>();
                        for (Map.Entry<String, List<String>> entry : configMap.entrySet()) {
                            String beanName = entry.getKey();
                            try {
                                PropertyChangeListener listener = (PropertyChangeListener) ac.getBean(beanName, PropertyChangeListener.class);
                                listenerList.add(new ListenerInfo(listener, Collections.unmodifiableSet(new HashSet(entry.getValue()))));
                            } catch (BeansException be) {
                                logger.log(Level.WARNING, "Could not get property change listener '"+beanName+"'.", be);
                            }
                        }

                        info = Collections.unmodifiableList(listenerList);
                        listeners = info;
                    }
                }
            } finally {
                listenerLock.writeLock().unlock();
            }
        }

        return info;
    }

    private static final class ListenerInfo {
        private final PropertyChangeListener listener;
        private final Set<String> propertySet;

        private ListenerInfo(PropertyChangeListener listener, Set<String> propertySet) {
            this.listener = listener;
            this.propertySet = propertySet;
        }
    }
}
