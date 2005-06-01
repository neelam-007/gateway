/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 4, 2005<br/>
 */
package com.l7tech.server.sla;

import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.util.logging.Logger;
import java.util.List;

/**
 * Hibernate manager for the counted_hits table. Each row is represented by a CountedHitRecord record.
 *
 * @author flascelles@layer7-tech.com
 * @deprecated this is no longer needed
 * todo erase this class when refactor is complete
 */
public class CountedHitsManager extends HibernateDaoSupport {
    private final Logger logger =  Logger.getLogger(getClass().getName());
    private static final String TABLE_NAME = "counted_hits";

    public void recordHit(long counterId, long now) {
        CountedHitRecord rec = new CountedHitRecord();
        rec.setCounterId(counterId);
        rec.setTs(now);
        getHibernateTemplate().save(rec);
    }

    /**
     * @return the timestamp of the last hit in the table
     */
    public Long latestHit() {
        String query = "select max(" + TABLE_NAME + ".ts) from " + TABLE_NAME + " in class " +
                       CountedHitRecord.class.getName();
        List res = getHibernateTemplate().find(query);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            if (res.get(0) instanceof Long) {
                return (Long)(res.get(0));
            } else {
                logger.warning("unexpected type " + res.get(0).getClass().getName());
                return null;
            }
        }
        logger.finest("returned null, empty table");
        return null;
    }

    public List listOfCountersRecorded(long maxts) {
        String query = "select distinct " + TABLE_NAME + ".counterId from " + TABLE_NAME + " in class " +
                       CountedHitRecord.class.getName() + " where " + TABLE_NAME + ".ts <= " + maxts;
        return getHibernateTemplate().find(query);
    }

    public long countForInterval(long counterid, long start, long end) {
        String query = "select count(" + TABLE_NAME + ".hitId) from " + TABLE_NAME +
                       " in class " + CountedHitRecord.class.getName() +
                       " where " + TABLE_NAME + ".ts >= " + start + " and " + TABLE_NAME + ".ts <= " + end +
                       " and " + TABLE_NAME + ".counterId = " + counterid;

        List res = getHibernateTemplate().find(query);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            if (res.get(0) instanceof Integer) {
                return ((Integer)(res.get(0))).longValue();
            } else {
                logger.warning("unexpected type " + res.get(0).getClass().getName());
                return 0;
            }
        }
        logger.finest("retu rned null, empty table");
        return 0;
    }
}
