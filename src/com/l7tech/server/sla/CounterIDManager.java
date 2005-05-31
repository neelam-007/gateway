/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.server.sla;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.ObjectModelException;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;
import org.springframework.dao.DataAccessException;

/**
 * The CounterIDManager is responsible for assigning a counter id for each combination of counter_name, identity.
 * This is a spring-managed singleton.
 *
 * @author flascelles@layer7-tech.com
 */
public class CounterIDManager extends HibernateDaoSupport {
    /**
     * key: string [countername+providerid+userid]
     * value: long representing the counter id
     */
    private final HashMap idCache = new HashMap();
    private static final String TABLE_NAME = "counters";
    private final Logger logger =  Logger.getLogger(getClass().getName());

    public CounterIDManager() {
    }

    /**
     * Get the counter id for a counter_name, identity tuple. A new id will is create if it does
     * not yet exist.
     *
     * @param counterName the name of the counter for which the id is required.
     * @param identity may be null to represent a global counter.
     * @return a counterid, if this id does not yet exists, a new one is created and returned.
     */
    public long getCounterId(String counterName, User identity) throws ObjectModelException {
        String key = null;
        if (identity == null) {
            key = counterName;
        } else {
            key = counterName + identity.getProviderId() + identity.getUniqueIdentifier();
        }
        synchronized (idCache) {
            Long res = (Long)idCache.get(key);
            if (res == null) {
                res = new Long(getIdFromDbOrCreateEntry(counterName, identity));
                idCache.put(key, res);
            }
            return res.longValue();
        }
    }

    /**
     * record in database the new counterid and return it's id
     *
     * todo, in a cluster, this could be a problem because the database may return a new id but we dont really know
     * if it's valid until the transaction is comitted. let's figure this out after the poc
     */
    private long getIdFromDbOrCreateEntry(String counterName, User identity) throws SaveException {
        CounterIDRecord data = new CounterIDRecord();
        data.setCounterName(counterName);

        String query = null;
        if (identity != null) {
            data.setUserId(identity.getUniqueIdentifier());
            data.setProviderId(identity.getProviderId());
            query = "from " + TABLE_NAME + " in class " + CounterIDRecord.class.getName() +
                    " where " + TABLE_NAME + "." + "counterName" + " = \'" + data.getCounterName() + "\'" +
                    " and " + TABLE_NAME + "." + "userId" + " = \'" + data.getUserId() + "\'" +
                    " and " + TABLE_NAME + "." + "providerId" + " = \'" + data.getProviderId() + "\'";

        } else {
            query = "from " + TABLE_NAME + " in class " + CounterIDRecord.class.getName() +
                    " where " + TABLE_NAME + "." + "counterName" + " = \'" + data.getCounterName() + "\'" +
                    " and " + TABLE_NAME + "." + "userId = null" +
                    " and " + TABLE_NAME + "." + "providerId" + " = \'" + data.getProviderId() + "\'";
        }
        try {
            // check whether this is already in the db
            List res = getHibernateTemplate().find(query);
            if (res != null && !res.isEmpty()) {
                CounterIDRecord existing = (CounterIDRecord)res.get(0);
                return existing.getCounterId();
            } else {
                return ((Long)getHibernateTemplate().save(data)).longValue();
            }
        } catch (DataAccessException e) {
            String msg = "problem getting existing id from db or creating new one";
            logger.log(Level.WARNING, msg, e);
            throw new SaveException(msg, e);
        }
    }
}
