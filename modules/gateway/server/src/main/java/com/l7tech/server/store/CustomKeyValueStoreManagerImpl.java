package com.l7tech.server.store;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomKeyValueStoreManagerImpl extends HibernateEntityManager<CustomKeyValueStore, EntityHeader> implements CustomKeyValueStoreManager {

    private final Map<Goid, String> deletedKeys = new ConcurrentHashMap<>(); // keep track of deleted GOIDs to key names.
    private final Map<String, Set<KeyValueStoreChangeEventListener>> listeners = new ConcurrentHashMap<>();

    public CustomKeyValueStoreManagerImpl(ApplicationEventProxy eventProxy) {
        eventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                handleEvent(event);
            }
        });
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return CustomKeyValueStore.class;
    }

    @NotNull
    @Override
    public Collection<CustomKeyValueStore> findByKeyPrefix(@NotNull final String keyPrefix) throws FindException {
        try {
            //noinspection unchecked
            return (Collection<CustomKeyValueStore>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.like("name", keyPrefix+"%"));
                    return crit.list();
                }
            });
        } catch (Exception e) {
            throw new FindException("Unable to find using key prefix: " + keyPrefix, e);
        }
    }

    @Override
    public CustomKeyValueStore findByUniqueName(final String name) throws FindException {
        if (name == null) {
            return null;
        }
        return super.findByUniqueName(name);
    }

    @Override
    public void deleteByKey(@NotNull String key) throws DeleteException {
        try {
            CustomKeyValueStore customKeyValue = this.findByUniqueName(key);
            if (customKeyValue != null) {
                this.delete(customKeyValue);
                deletedKeys.put(customKeyValue.getGoid(), key);
            }
        } catch (FindException e) {
            throw new DeleteException("Unable to delete using key: " + key, e);
        }
    }

    @Override
    public void addListener(String keyPrefix, KeyValueStoreChangeEventListener listener) {
        Set<KeyValueStoreChangeEventListener> keyPrefixListeners = listeners.get(keyPrefix);
        if (keyPrefixListeners == null) {
            keyPrefixListeners = new HashSet<>();
            listeners.put(keyPrefix, keyPrefixListeners);
        }

        keyPrefixListeners.add(listener);
    }

    @Override
    public void removeListener(String keyPrefix, KeyValueStoreChangeEventListener listener) {
        Set<KeyValueStoreChangeEventListener> keyPrefixListeners = listeners.get(keyPrefix);
        if (keyPrefixListeners != null) {
            keyPrefixListeners.remove(listener);
        }
    }

    private void handleEvent(ApplicationEvent event) {
        if (!(event instanceof EntityInvalidationEvent)) {
            return;
        }

        EntityInvalidationEvent entityInvalidationEvent = (EntityInvalidationEvent) event;
        if (!CustomKeyValueStore.class.equals(entityInvalidationEvent.getEntityClass())) {
            return;
        }

        Goid[] goids = entityInvalidationEvent.getEntityIds();
        char[] ops = entityInvalidationEvent.getEntityOperations();

        // Convert from EntityInvalidationEvent to KeyValueStoreChangeEvent
        //
        List<KeyValueStoreChangeEventListener.Event> allEvents = new LinkedList<>();
        for (int ix = 0; ix < goids.length; ix++) {
            String key = null;
            KeyValueStoreChangeEventListener.Operation operation;

            char op = ops[ix];
            Goid goid = goids[ix];

            if (op == EntityInvalidationEvent.CREATE) {
                operation = KeyValueStoreChangeEventListener.Operation.CREATE;
                try {
                    CustomKeyValueStore keyValue = this.findByPrimaryKey(goid);
                    if (keyValue != null) {
                        key = keyValue.getName();
                    }
                } catch (FindException e) {
                    key = null;
                }
            } else if (op == EntityInvalidationEvent.UPDATE) {
                operation = KeyValueStoreChangeEventListener.Operation.UPDATE;
                try {
                    CustomKeyValueStore keyValue = this.findByPrimaryKey(goid);
                    if (keyValue != null) {
                        key = keyValue.getName();
                    }
                } catch (FindException e) {
                    key = null;
                }
            } else if (op == EntityInvalidationEvent.DELETE) {
                operation = KeyValueStoreChangeEventListener.Operation.DELETE;
                key = deletedKeys.remove(goid);
            } else {
                throw new IllegalArgumentException("Unexpected operation: " + op);
            }

            if (key != null) {
                allEvents.add(new KeyValueStoreChangeEventImpl(key, operation));
            }
        }

        // Call event listeners.
        //
        for (Map.Entry<String, Set<KeyValueStoreChangeEventListener>> entry : listeners.entrySet()) {
            // Get events for given key prefix.
            //
            List<KeyValueStoreChangeEventListener.Event> keyPrefixEvents = new LinkedList<>();
            for (KeyValueStoreChangeEventListener.Event currEvent : allEvents) {
                if (currEvent.getKey().startsWith(entry.getKey())) {
                    keyPrefixEvents.add(currEvent);
                }
            }

            // Call event listeners.
            //
            for (KeyValueStoreChangeEventListener listener : entry.getValue()) {
                listener.onEvent(keyPrefixEvents);
            }
        }
    }

    private class KeyValueStoreChangeEventImpl implements KeyValueStoreChangeEventListener.Event {
        private final String key;
        private final KeyValueStoreChangeEventListener.Operation operation;

        public KeyValueStoreChangeEventImpl(String key, KeyValueStoreChangeEventListener.Operation operation) {
            this.key = key;
            this.operation = operation;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public KeyValueStoreChangeEventListener.Operation getOperation() {
            return operation;
        }
    }
}