/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jun 1, 2005<br/>
 */
package com.l7tech.server.sla;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CounterManager implementation that create a new counter, find counters, increment or decrement counter values, etc.
 *
 * Note: CounterIDManagerImpl has been removed and merged into this implementation.
 *
 * @author flascelles@layer7-tech.com
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class CounterManagerImpl extends HibernateDaoSupport implements CounterManager {
    private final Logger logger = Logger.getLogger(CounterManagerImpl.class.getName());
    private final Collection<String> counterCache = new ArrayList<String>();

    private Counter getLockedCounter(Connection conn, String counterName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT cnt_sec, cnt_min, cnt_hr, cnt_day, cnt_mnt, last_update" +
                                                                 " FROM counters WHERE countername='" + counterName + "'" +
                                                                 " FOR UPDATE");
        ResultSet rs = ps.executeQuery();
        Counter output = null;
        while (rs.next()) {
            output = new Counter();
            output.setCurrentSecondCounter(rs.getLong(1));
            output.setCurrentMinuteCounter(rs.getLong(2));
            output.setCurrentHourCounter(rs.getLong(3));
            output.setCurrentDayCounter(rs.getLong(4));
            output.setCurrentMonthCounter(rs.getLong(5));
            output.setLastUpdate(rs.getLong(6));
            break;
        }
        rs.close();
        ps.close();
        return output;
    }

    private void recordNewCounterValue(Connection conn, String counterName, Counter newValues) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE counters " +
                                                     "SET cnt_sec=?, cnt_min=?, cnt_hr=?, cnt_day=?, cnt_mnt=?, last_update=? " +
                                                     "WHERE countername=?");
        ps.clearParameters();
        ps.setLong(1, newValues.getCurrentSecondCounter());
        ps.setLong(2, newValues.getCurrentMinuteCounter());
        ps.setLong(3, newValues.getCurrentHourCounter());
        ps.setLong(4, newValues.getCurrentDayCounter());
        ps.setLong(5, newValues.getCurrentMonthCounter());
        ps.setLong(6, newValues.getLastUpdate());
        ps.setString(7, counterName);
        ps.executeUpdate();
        ps.close();
    }

    @Override
    public long incrementOnlyWithinLimitAndReturnValue(final String counterName,
                                                       final long timestamp,
                                                       final int fieldOfInterest,
                                                       final long limit) throws LimitAlreadyReachedException {
         Object result = getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                try {
                    Object output = null;
                    Connection conn = session.connection();
                    Counter dbcnt = getLockedCounter(conn, counterName);
                    if (dbcnt == null) {
                        throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                    }

                    incrementCounter(dbcnt, timestamp);

                    // check if the increment violates the limit
                    boolean limitViolated = false;
                    switch (fieldOfInterest) {
                        case ThroughputQuota.PER_SECOND:
                            if (dbcnt.getCurrentSecondCounter() > limit) {
                                limitViolated = true;
                            }
                            break;
                        case ThroughputQuota.PER_MINUTE:
                            if (dbcnt.getCurrentMinuteCounter() > limit) {
                                limitViolated = true;
                            }
                            break;
                        case ThroughputQuota.PER_HOUR:
                            if (dbcnt.getCurrentHourCounter() > limit) {
                                limitViolated = true;
                            }
                            break;
                        case ThroughputQuota.PER_DAY:
                            if (dbcnt.getCurrentDayCounter() > limit) {
                                limitViolated = true;
                            }
                            break;
                        case ThroughputQuota.PER_MONTH:
                            if (dbcnt.getCurrentMonthCounter() > limit) {
                                limitViolated = true;
                            }
                            break;
                    }
                    if (limitViolated) {
                        logger.finest("returning limit already reached");
                        output = new LimitAlreadyReachedException("Limit already met");
                        conn.commit();
                        conn = null;
                    } else {
                        // put new value in database
                        recordNewCounterValue(conn, counterName, dbcnt);
                        conn.commit();
                        conn = null;

                        // return the fieldOfInterest
                        switch (fieldOfInterest) {
                            case ThroughputQuota.PER_SECOND:
                                output = dbcnt.getCurrentSecondCounter();
                                break;
                            case ThroughputQuota.PER_MINUTE:
                                output = dbcnt.getCurrentMinuteCounter();
                                break;
                            case ThroughputQuota.PER_HOUR:
                                output = dbcnt.getCurrentHourCounter();
                                break;
                            case ThroughputQuota.PER_DAY:
                                output = dbcnt.getCurrentDayCounter();
                                break;
                            case ThroughputQuota.PER_MONTH:
                                output = dbcnt.getCurrentMonthCounter();
                                break;
                        }
                    }
                    return output;
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "SQLException getting counter from db", e);
                    throw new RuntimeException(e);
                } catch (HibernateException e) {
                    logger.log(Level.SEVERE, "HibernateException getting counter from db", e);
                    throw new RuntimeException(e);
                }
            }
        });

        if (result instanceof Long) {
            return (Long) result;
        } else if (result instanceof LimitAlreadyReachedException) {
            throw (LimitAlreadyReachedException)result;
        }

        throw new RuntimeException("unexpected result type " + result);
    }

    @Override
    public void checkOrCreateCounter(String counterName) throws ObjectModelException {
        synchronized (counterCache) {
            if (counterCache.contains(counterName)) return;

            final CounterRecord data = new CounterRecord();
            data.setCounterName(counterName);

            try {
                final List res;

                res = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                    protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                        final Criteria criteria = session.createCriteria(CounterRecord.class);
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

    @Override
    public long incrementAndReturnValue(final String counterName, final long timestamp, final int fieldOfInterest) {
        Object result = getHibernateTemplate().execute(new HibernateCallback() {
            @Override
            public Object doInHibernate(Session session) {
                try {
                    Object output = null;
                    Connection conn = session.connection();
                    Counter dbcnt = getLockedCounter(conn, counterName);
                    if (dbcnt == null) {
                        throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                    }

                    incrementCounter(dbcnt, timestamp);
                    // put new value in database
                    recordNewCounterValue(conn, counterName, dbcnt);
                    conn.commit();
                    conn = null;

                    // return the fieldOfInterest
                    switch (fieldOfInterest) {
                        case ThroughputQuota.PER_SECOND:
                            output = dbcnt.getCurrentSecondCounter();
                            break;
                        case ThroughputQuota.PER_MINUTE:
                            output = dbcnt.getCurrentMinuteCounter();
                            break;
                        case ThroughputQuota.PER_HOUR:
                            output = dbcnt.getCurrentHourCounter();
                            break;
                        case ThroughputQuota.PER_DAY:
                            output = dbcnt.getCurrentDayCounter();
                            break;
                        case ThroughputQuota.PER_MONTH:
                            output = dbcnt.getCurrentMonthCounter();
                            break;
                    }

                    return output;
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "SQLException getting counter from db", e);
                    throw new RuntimeException(e);
                } catch (HibernateException e) {
                    logger.log(Level.SEVERE, "HibernateException getting counter from db", e);
                    throw new RuntimeException(e);
                }
            }
        });

        if (result instanceof Long) {
            return (Long) result;
        }

        throw new RuntimeException("unexpected result type " + result);
    }

    @Override
    public long getCounterValue(final String counterName, final int fieldOfInterest) {
        Object result = getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
            @Override
            public Object doInHibernateReadOnly(Session session) {
                try {
                    String fieldstr = null;
                    switch (fieldOfInterest) {
                        case ThroughputQuota.PER_SECOND:
                            fieldstr = "cnt_sec";
                            break;
                        case ThroughputQuota.PER_MINUTE:
                            fieldstr = "cnt_min";
                            break;
                        case ThroughputQuota.PER_HOUR:
                            fieldstr = "cnt_hr";
                            break;
                        case ThroughputQuota.PER_DAY:
                            fieldstr = "cnt_day";
                            break;
                        case ThroughputQuota.PER_MONTH:
                            fieldstr = "cnt_mnt";
                            break;
                    }

                    Connection conn = session.connection();
                    PreparedStatement ps = conn.prepareStatement("SELECT " + fieldstr +
                                                                 " FROM counters WHERE countername='" + counterName + "'");
                    ResultSet rs = ps.executeQuery();
                    long desiredval = -1;
                    while (rs.next()) {
                        desiredval = rs.getLong(1);
                        break;
                    }
                    rs.close();
                    ps.close();
                    conn.commit();
                    conn = null;
                    if (desiredval < 0) {
                        return null;
                    }
                    return desiredval;
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "SQLException getting counter from db", e);
                    throw new RuntimeException(e);
                } catch (HibernateException e) {
                    logger.log(Level.SEVERE, "HibernateException getting counter from db", e);
                    throw new RuntimeException(e);
                }
            }
        });
        if (result == null) {
            throw new RuntimeException("could not find value in db (?)"); // should not happen
        }
        if (result instanceof Long) {
            return (Long) result;
        }
        throw new RuntimeException("unexpected result type " + result);
    }

    @Override
    public void decrement(final String counterName) {
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                try {
                    Connection conn = session.connection();
                    Counter dbcnt = getLockedCounter(conn, counterName);
                    if (dbcnt == null) {
                        throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                    }

                    dbcnt.setCurrentSecondCounter(dbcnt.getCurrentSecondCounter()-1);
                    dbcnt.setCurrentMinuteCounter(dbcnt.getCurrentMinuteCounter()-1);
                    dbcnt.setCurrentHourCounter(dbcnt.getCurrentHourCounter()-1);
                    dbcnt.setCurrentDayCounter(dbcnt.getCurrentDayCounter()-1);
                    dbcnt.setCurrentMonthCounter(dbcnt.getCurrentMonthCounter()-1);

                    // put new value in database
                    recordNewCounterValue(conn, counterName, dbcnt);
                    conn.commit();
                    conn = null;

                    return null;
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "SQLException getting counter from db", e);
                    throw new RuntimeException(e);
                } catch (HibernateException e) {
                    logger.log(Level.SEVERE, "HibernateException getting counter from db", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void incrementCounter(Counter cntr, long timestamp) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(timestamp);

        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(cntr.getLastUpdate());

        if (inSameMonth(last, now)) {
            cntr.setCurrentMonthCounter(cntr.getCurrentMonthCounter()+1);
            if (inSameDay(last, now)) {
                cntr.setCurrentDayCounter(cntr.getCurrentDayCounter()+1);
                if (inSameHour(last, now)) {
                    cntr.setCurrentHourCounter(cntr.getCurrentHourCounter()+1);
                    if (inSameMinute(last, now)) {
                        cntr.setCurrentMinuteCounter(cntr.getCurrentMinuteCounter()+1);
                        if (inSameSecond(last, now)) {
                            cntr.setCurrentSecondCounter(cntr.getCurrentSecondCounter()+1);
                        } else {
                            cntr.setCurrentSecondCounter(1);
                        }
                    } else {
                        cntr.setCurrentMinuteCounter(1);
                        cntr.setCurrentSecondCounter(1);
                    }
                } else {
                    cntr.setCurrentHourCounter(1);
                    cntr.setCurrentMinuteCounter(1);
                    cntr.setCurrentSecondCounter(1);
                }
            } else {
                cntr.setCurrentDayCounter(1);
                cntr.setCurrentHourCounter(1);
                cntr.setCurrentMinuteCounter(1);
                cntr.setCurrentSecondCounter(1);
            }
        } else {
            cntr.setCurrentMonthCounter(1);
            cntr.setCurrentDayCounter(1);
            cntr.setCurrentHourCounter(1);
            cntr.setCurrentMinuteCounter(1);
            cntr.setCurrentSecondCounter(1);
        }
        cntr.setLastUpdate(timestamp);
    }

    private boolean inSameMonth(Calendar last, Calendar now) {
        if (last.get(Calendar.YEAR) != now.get(Calendar.YEAR)) {
            return false;
        }
        if (last.get(Calendar.MONTH) != now.get(Calendar.MONTH)) {
            return false;
        }
        return true;
    }

    private boolean inSameDay(Calendar last, Calendar now) {
        if (last.get(Calendar.DATE) != now.get(Calendar.DATE)) {
            return false;
        }
        return true;
    }

    private boolean inSameHour(Calendar last, Calendar now) {
        if (last.get(Calendar.HOUR_OF_DAY) != now.get(Calendar.HOUR_OF_DAY)) {
            return false;
        }
        return true;
    }

    private boolean inSameMinute(Calendar last, Calendar now) {
        if (last.get(Calendar.MINUTE) != now.get(Calendar.MINUTE)) {
            return false;
        }
        return true;
    }

    private boolean inSameSecond(Calendar last, Calendar now) {
        if (last.get(Calendar.MINUTE) != now.get(Calendar.MINUTE)) {
            return false;
        }
        if (last.get(Calendar.SECOND) != now.get(Calendar.SECOND)) {
            return false;
        }
        return true;
    }
}