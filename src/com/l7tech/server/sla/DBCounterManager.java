/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jun 1, 2005<br/>
 */
package com.l7tech.server.sla;

import com.l7tech.policy.assertion.sla.ThroughputQuota;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CounterManager implementation that uses a database table instead of a cache.
 *
 * @author flascelles@layer7-tech.com
 */
public class DBCounterManager extends HibernateDaoSupport implements CounterManager {

    private final Logger logger = Logger.getLogger(DBCounterManager.class.getName());

    private Counter getLockedCounter(Connection conn, long counterId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT cnt_sec, cnt_hr, cnt_day, cnt_mnt, last_update" +
                                                                 " FROM counters WHERE counterid=" + counterId +
                                                                 " FOR UPDATE");
        ResultSet rs = ps.executeQuery();
        Counter output = null;
        while (rs.next()) {
            output = new Counter();
            output.setCurrentSecondCounter(rs.getLong(1));
            output.setCurrentHourCounter(rs.getLong(2));
            output.setCurrentDayCounter(rs.getLong(3));
            output.setCurrentMonthCounter(rs.getLong(4));
            output.setLastUpdate(rs.getLong(5));
            break;
        }
        rs.close();
        ps.close();
        return output;
    }

    private void recordNewCounterValue(Connection conn, long counterId, Counter newValues) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE counters " +
                                                     "SET cnt_sec=?, cnt_hr=?, cnt_day=?, cnt_mnt=?, last_update=? " +
                                                     "WHERE counterid=?");
        ps.clearParameters();
        ps.setLong(1, newValues.getCurrentSecondCounter());
        ps.setLong(2, newValues.getCurrentHourCounter());
        ps.setLong(3, newValues.getCurrentDayCounter());
        ps.setLong(4, newValues.getCurrentMonthCounter());
        ps.setLong(5, newValues.getLastUpdate());
        ps.setLong(6, counterId);
        ps.executeUpdate();
        ps.close();
    }

    public long incrementOnlyWithinLimitAndReturnValue(final long counterId,
                                                       final long timestamp,
                                                       final int fieldOfInterest,
                                                       final long limit) throws LimitAlreadyReachedException {
        Object result = getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                try {
                    Object output = null;
                    Connection conn = session.connection();
                    Counter dbcnt = getLockedCounter(conn, counterId);
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
                        recordNewCounterValue(conn, counterId, dbcnt);
                        conn.commit();
                        conn = null;

                        // return the fieldOfInterest
                        switch (fieldOfInterest) {
                            case ThroughputQuota.PER_SECOND:
                                output = new Long(dbcnt.getCurrentSecondCounter());
                                break;
                            case ThroughputQuota.PER_HOUR:
                                output = new Long(dbcnt.getCurrentHourCounter());
                                break;
                            case ThroughputQuota.PER_DAY:
                                output = new Long(dbcnt.getCurrentDayCounter());
                                break;
                            case ThroughputQuota.PER_MONTH:
                                output = new Long(dbcnt.getCurrentMonthCounter());
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
            return ((Long)result).longValue();
        } else if (result instanceof LimitAlreadyReachedException) {
            throw (LimitAlreadyReachedException)result;
        }

        throw new RuntimeException("unexpected result type " + result);
    }

    public long incrementAndReturnValue(final long counterId, final long timestamp, final int fieldOfInterest) {
        Object result = getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                try {
                    Object output = null;
                    Connection conn = session.connection();
                    Counter dbcnt = getLockedCounter(conn, counterId);
                    if (dbcnt == null) {
                        throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                    }

                    incrementCounter(dbcnt, timestamp);
                    // put new value in database
                    recordNewCounterValue(conn, counterId, dbcnt);
                    conn.commit();
                    conn = null;

                    // return the fieldOfInterest
                    switch (fieldOfInterest) {
                        case ThroughputQuota.PER_SECOND:
                            output = new Long(dbcnt.getCurrentSecondCounter());
                            break;
                        case ThroughputQuota.PER_HOUR:
                            output = new Long(dbcnt.getCurrentHourCounter());
                            break;
                        case ThroughputQuota.PER_DAY:
                            output = new Long(dbcnt.getCurrentDayCounter());
                            break;
                        case ThroughputQuota.PER_MONTH:
                            output = new Long(dbcnt.getCurrentMonthCounter());
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
            return ((Long)result).longValue();
        }

        throw new RuntimeException("unexpected result type " + result);
    }

    public long getCounterValue(final long counterId, final int fieldOfInterest) {
        Object result = getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                try {
                    String fieldstr = null;
                    switch (fieldOfInterest) {
                        case ThroughputQuota.PER_SECOND:
                            fieldstr = "cnt_sec";
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
                                                                 " FROM counters WHERE counterid=" + counterId);
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
                    return new Long(desiredval);
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
            return ((Long)result).longValue();
        }
        throw new RuntimeException("unexpected result type " + result);
    }

    public void decrement(final long counterId) {
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) {
                try {
                    Connection conn = session.connection();
                    Counter dbcnt = getLockedCounter(conn, counterId);
                    if (dbcnt == null) {
                        throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                    }

                    dbcnt.setCurrentSecondCounter(dbcnt.getCurrentSecondCounter()-1);
                    dbcnt.setCurrentHourCounter(dbcnt.getCurrentHourCounter()-1);
                    dbcnt.setCurrentDayCounter(dbcnt.getCurrentDayCounter()-1);
                    dbcnt.setCurrentMonthCounter(dbcnt.getCurrentMonthCounter()-1);

                    // put new value in database
                    recordNewCounterValue(conn, counterId, dbcnt);
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
                    if (inSameSecond(last, now)) {
                        cntr.setCurrentSecondCounter(cntr.getCurrentSecondCounter()+1);
                    } else {
                        cntr.setCurrentSecondCounter(1);
                    }
                } else {
                    cntr.setCurrentHourCounter(1);
                    cntr.setCurrentSecondCounter(1);
                }
            } else {
                cntr.setCurrentDayCounter(1);
                cntr.setCurrentHourCounter(1);
                cntr.setCurrentSecondCounter(1);
            }
        } else {
            cntr.setCurrentMonthCounter(1);
            cntr.setCurrentDayCounter(1);
            cntr.setCurrentHourCounter(1);
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
