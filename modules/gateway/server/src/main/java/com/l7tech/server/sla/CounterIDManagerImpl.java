/**
 * Copyright (C) 2005-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.sla;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The CounterIDManager is responsible for finding counters and create a new counter.
 *
 * @author flascelles@layer7-tech.com
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class CounterIDManagerImpl extends HibernateDaoSupport implements CounterIDManager {
    private final Collection<String> counterCache = new ArrayList<String>();
    private static final String TABLE_NAME = "counters";
    private final Logger logger =  Logger.getLogger(getClass().getName());

    private static final String HQL_SELECT_ALL_COUNTER_NAMES = MessageFormat.format("SELECT {0}.counterName FROM {0} IN CLASS {1}", TABLE_NAME, CounterIDRecord.class.getName());

    @Override
    public void checkOrCreateCounter(String counterName) throws ObjectModelException {
        synchronized (counterCache) {
            if (counterCache.contains(counterName)) return;

            final CounterIDRecord data = new CounterIDRecord();
            data.setCounterName(counterName);

            try {
                final List res;

                res = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                    protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                        final Criteria criteria = session.createCriteria(CounterIDRecord.class);
                        criteria.add(Restrictions.eq("counterName", data.getCounterName()));
                        return criteria.list();
                    }
                });

                // check whether this is already in the db
                if (res == null || res.isEmpty()) {
                    getHibernateTemplate().save(data);
                }

                counterCache.add(counterName);
            } catch (DataAccessException e) {
                String msg = "problem getting existing counter name from db or creating new one. possible race condition";
                logger.log(Level.WARNING, msg, e);
                throw new FindException(msg, e);
            }
        }
    }

    @Transactional(readOnly=true)
    public String[] getAllCounterNames() throws FindException {
        try {
            List<String> res = getHibernateTemplate().find(HQL_SELECT_ALL_COUNTER_NAMES);
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
}