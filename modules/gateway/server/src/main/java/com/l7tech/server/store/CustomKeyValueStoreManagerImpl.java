package com.l7tech.server.store;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreListener;
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
import java.util.concurrent.CopyOnWriteArraySet;

public class CustomKeyValueStoreManagerImpl extends HibernateEntityManager<CustomKeyValueStore, EntityHeader> implements CustomKeyValueStoreManager {

    private final Map<Goid, String> deletedKeys = new ConcurrentHashMap<>(); // keep track of deleted GOIDs to key names.
    private final KeyValueStoreChangeEventListenerImpl listener = new KeyValueStoreChangeEventListenerImpl();

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
            }
        } catch (FindException e) {
            throw new DeleteException("Unable to delete using key: " + key, e);
        }
    }

    @Override
    public <T extends KeyValueStoreListener> T getListener(Class<T> lClass) {
        if (KeyValueStoreChangeEventListener.class.equals(lClass)) {
            //noinspection unchecked
            return (T) listener;
        }

        return null;
    }

    @Override
    public void delete(final CustomKeyValueStore customKeyValue) throws DeleteException {
        super.delete(customKeyValue);
        deletedKeys.put(customKeyValue.getGoid(), customKeyValue.getName());
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
        List<KeyValueStoreChangeEventListener.Event> allEvents = new ArrayList<>(goids.length);
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

        // Call callbacks.
        //
        for (Map.Entry<String, Set<KeyValueStoreChangeEventListener.Callback>> entry : listener.getCallbacks().entrySet()) {
            // Get events for given key prefix.
            //
            List<KeyValueStoreChangeEventListener.Event> keyPrefixEvents = new ArrayList<>(allEvents.size());
            for (KeyValueStoreChangeEventListener.Event currEvent : allEvents) {
                if (currEvent.getKey().startsWith(entry.getKey())) {
                    keyPrefixEvents.add(currEvent);
                }
            }

            // Call callbacks.
            //
            for (KeyValueStoreChangeEventListener.Callback callback : entry.getValue()) {
                callback.onEvent(keyPrefixEvents);
            }
        }
    }

    private class KeyValueStoreChangeEventImpl extends KeyValueStoreChangeEventListener.Event {
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

    private class KeyValueStoreChangeEventListenerImpl extends KeyValueStoreChangeEventListener {
        private final Map<String, Set<Callback>> callbacks = new ConcurrentHashMap<>();

        @Override
        public void add(Callback callback) {
            String keyPrefix = callback.getKeyPrefix();
            Set<Callback> keyPrefixCallbacks = callbacks.get(keyPrefix);
            if (keyPrefixCallbacks == null) {
                keyPrefixCallbacks = new CopyOnWriteArraySet<>();
                callbacks.put(keyPrefix, keyPrefixCallbacks);
            }
            keyPrefixCallbacks.add(callback);
        }

        @Override
        public void remove(Callback callback) {
            String keyPrefix = callback.getKeyPrefix();
            Set<Callback> keyPrefixCallbacks = callbacks.get(keyPrefix);
            if (keyPrefixCallbacks != null) {
                keyPrefixCallbacks.remove(callback);
            }
        }

        /**
         * Gets registered event callbacks.
         *
         * @return the event callbacks
         */
        private Map<String, Set<Callback>> getCallbacks() {
            return callbacks;
        }
    }
}