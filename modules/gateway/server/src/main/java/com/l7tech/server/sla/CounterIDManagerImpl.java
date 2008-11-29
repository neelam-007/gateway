/**
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.sla;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The CounterIDManager is responsible for assigning a counter id for each combination of counter_name, identity.
 * This is a spring-managed singleton.
 *
 * @author flascelles@layer7-tech.com
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class CounterIDManagerImpl extends HibernateDaoSupport implements CounterIDManager {
    /**
     * key: string [countername+providerid+userid]
     * value: long representing the counter id
     */
    private final Map<String, Long> idCache = new HashMap<String, Long>();
    private static final String TABLE_NAME = "counters";
    private final Logger logger =  Logger.getLogger(getClass().getName());

    private static final String HQL_SELECT_DISTINCT_COUNTER_NAMES = MessageFormat.format("SELECT DISTINCT {0}.counterName FROM {0} IN CLASS {1}", TABLE_NAME, CounterIDRecord.class.getName());

    /**
     * Get the counter id for a counter_name, identity tuple. A new id will is create if it does
     * not yet exist.
     *
     * @param counterName the name of the counter for which the id is required.
     * @param identity may be null to represent a global counter.
     * @return a counterid, if this id does not yet exists, a new one is created and returned.
     */
    public long getCounterId(String counterName, User identity) throws ObjectModelException {
        final String key;
        if (identity == null) {
            key = counterName;
        } else {
            key = counterName + identity.getProviderId() + identity.getId();
        }
        synchronized (idCache) {
            Long res = idCache.get(key);
            if (res == null) {
                res = new Long(getIdFromDbOrCreateEntry(counterName, identity));
                idCache.put(key, res);
            }
            return res.longValue();
        }
    }

    @Transactional(readOnly=true)
    public String[] getDistinctCounterNames() throws FindException {
        try {
            List<String> res = getHibernateTemplate().find(HQL_SELECT_DISTINCT_COUNTER_NAMES);
            String[] output = new String[res.size()];
            int i = 0;
            for (String re : res) {
                output[i] = re;
                i++;
            }
            return output;
        } catch (DataAccessException e) {
            String msg = "problem getting distinct counter names";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }
    }

    /**
     * Record in database the new counterid and return it's id
     */
    private long getIdFromDbOrCreateEntry(String counterName, User identity) throws SaveException {
        final CounterIDRecord data = new CounterIDRecord();
        data.setCounterName(counterName);

        try {
            final List res;
            if (identity != null) {
                data.setUserId(identity.getId());
                data.setProviderId(identity.getProviderId());
            }

            res = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria crit = session.createCriteria(CounterIDRecord.class);
                    crit.add(Restrictions.eq("counterName", data.getCounterName()));
                    crit.add(Restrictions.eq("userId", data.getUserId()));
                    crit.add(Restrictions.eq("providerId", data.getProviderId()));
                    return crit.list();
                }
            });
            // check whether this is already in the db
            if (res != null && !res.isEmpty()) {
                CounterIDRecord existing = (CounterIDRecord)res.get(0);
                return existing.getCounterId();
            } else {
                return ((Long)getHibernateTemplate().save(data)).longValue();
            }
        } catch (DataAccessException e) {
            String msg = "problem getting existing id from db or creating new one. possible race condition";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }
    }
}
