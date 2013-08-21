package com.l7tech.server.store;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.policy.CustomKeyValueStoreManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class CustomKeyValueStoreManagerImpl extends HibernateEntityManager<CustomKeyValueStore, EntityHeader> implements CustomKeyValueStoreManager {

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
}