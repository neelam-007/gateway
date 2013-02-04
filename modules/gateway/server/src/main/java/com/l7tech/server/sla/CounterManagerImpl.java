/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jun 1, 2005<br/>
 */
package com.l7tech.server.sla;

import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Either;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CounterManager implementation that create a new counter, find counters, increment or decrement counter values, etc.
 *
 * Note: CounterIDManagerImpl has been removed and merged into this implementation.
 *
 * This implementation does not use automatic transaction management; transactions are managed manually.
 *
 * @author flascelles@layer7-tech.com
 */
public class CounterManagerImpl extends HibernateDaoSupport implements CounterManager {
    private final Logger logger = Logger.getLogger(CounterManagerImpl.class.getName());

    private static final int batchLimit = SyspropUtil.getInteger("com.l7tech.hacounter.batchLimit", 4096);
    private static final int coreThreads = SyspropUtil.getInteger("com.l7tech.hacounter.coreThreads", 16);
    private static final int maxThreads = SyspropUtil.getInteger("com.l7tech.hacounter.maxThreads", 64);
    private static final int keepAliveSec = SyspropUtil.getInteger("com.l7tech.hacounter.keepAliveSec", 10);
    private static final int supervisorQueueSize = SyspropUtil.getInteger("com.l7tech.hacounter.supervisorQueueSize", 1024);
    private static final int counterQueueSize = SyspropUtil.getInteger("com.l7tech.hacounter.counterQueueSize", 10240);
    private static final ExecutorService updateThreads = new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveSec, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(supervisorQueueSize));
    private final ConcurrentMap<String,WorkQueue> counters = new ConcurrentHashMap<String,WorkQueue>();

    private final PlatformTransactionManager transactionManager;

    public CounterManagerImpl(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    private Counter loadCounter(Connection conn, String counterName, boolean lockForUpdate) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT cnt_sec, cnt_min, cnt_hr, cnt_day, cnt_mnt, last_update" +
            " FROM counters WHERE countername=?" +
            (lockForUpdate ? " FOR UPDATE" : ""));
        ps.setString(1, counterName);
        ResultSet rs = ps.executeQuery();
        Counter output = null;
        if (rs.next()) {
            output = new Counter();
            output.setCurrentSecondCounter(rs.getLong(1));
            output.setCurrentMinuteCounter(rs.getLong(2));
            output.setCurrentHourCounter(rs.getLong(3));
            output.setCurrentDayCounter(rs.getLong(4));
            output.setCurrentMonthCounter(rs.getLong(5));
            output.setLastUpdate(rs.getLong(6));
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
    public long incrementOnlyWithinLimitAndReturnValue(final boolean synchronous,
                                                       final String counterName,
                                                       final long timestamp,
                                                       final int fieldOfInterest,
                                                       final long limit) throws LimitAlreadyReachedException
    {
        if (synchronous) {
            return synchronousIncrementOnlyWithinLimitAndReturnValue(counterName, timestamp, fieldOfInterest, limit);
        } else {
            return asyncIncrementOnlyWithinLimitAndReturnValue(counterName, timestamp, fieldOfInterest, limit);
        }
    }

    private long synchronousIncrementOnlyWithinLimitAndReturnValue(final String counterName,
                                                                   final long timestamp,
                                                                   final int fieldOfInterest,
                                                                   final long limit) throws LimitAlreadyReachedException
    {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        Either<LimitAlreadyReachedException,Long> result = tt.execute(new TransactionCallback<Either<LimitAlreadyReachedException,Long>>() {
            @Override
            public Either<LimitAlreadyReachedException,Long> doInTransaction(TransactionStatus transactionStatus) {
                return getHibernateTemplate().execute(new HibernateCallback<Either<LimitAlreadyReachedException, Long>>() {
                    @Override
                    public Either<LimitAlreadyReachedException, Long> doInHibernate(Session session) throws HibernateException, SQLException {
                        final AtomicReference<Either<LimitAlreadyReachedException,Long>> ret = new AtomicReference<Either<LimitAlreadyReachedException, Long>>();
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                Counter dbcnt = loadCounter(connection, counterName, true);
                                if (dbcnt == null) {
                                    throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                                }

                                incrementCounter(dbcnt, timestamp);

                                // check if the increment violates the limit
                                boolean limitViolated = isLimitViolated(dbcnt, fieldOfInterest, limit);
                                if (limitViolated) {
                                    logger.finest("returning limit already reached");
                                    ret.set(Either.<LimitAlreadyReachedException,Long>left(new LimitAlreadyReachedException("Limit already met")));
                                } else {
                                    // put new value in database
                                    recordNewCounterValue(connection, counterName, dbcnt);
                                    ret.set(Either.<LimitAlreadyReachedException,Long>right(getFieldOfInterest(dbcnt, fieldOfInterest)));
                                }
                            }
                        });
                        return ret.get();
                    }
                });
            }
        });

        if (result.isLeft())
            throw result.left();
        return result.right();
    }

    @Override
    public long incrementAndReturnValue(final boolean synchronous, final String counterName, final long timestamp, final int fieldOfInterest) {
        if (synchronous) {
            return synchronousIncrementAndReturnValue(counterName, timestamp, fieldOfInterest);
        } else {
            return asyncIncrementAndReturnValue(counterName, timestamp, fieldOfInterest);
        }
    }

    public long synchronousIncrementAndReturnValue(final String counterName, final long timestamp, final int fieldOfInterest) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        return tt.execute(new TransactionCallback<Long>() {
            @Override
            public Long doInTransaction(TransactionStatus transactionStatus) {
                return getHibernateTemplate().execute(new HibernateCallback<Long>() {
                    @Override
                    public Long doInHibernate(Session session) throws HibernateException, SQLException {
                        final AtomicReference<Long> ret = new AtomicReference<Long>();
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                Counter dbcnt = loadCounter(connection, counterName, true);
                                if (dbcnt == null) {
                                    throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                                }

                                incrementCounter(dbcnt, timestamp);
                                // put new value in database
                                recordNewCounterValue(connection, counterName, dbcnt);

                                ret.set(getFieldOfInterest(dbcnt, fieldOfInterest));
                            }
                        });
                        return ret.get();
                    }
                });
            }
        });
    }

    public long getCounterValue(final String counterName, final int fieldOfInterest) {
        Counter counter = getCurrentCounterValueReadOnly(counterName);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    @Override
    public CounterInfo getCounterInfo(final @NotNull String counterName) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(true);
        return tt.execute(new TransactionCallback<CounterInfo>() {
            @Override
            public CounterInfo doInTransaction(TransactionStatus transactionStatus) {
                return getHibernateTemplate().execute(new HibernateCallback<CounterInfo>() {
                    @Override
                    public CounterInfo doInHibernate(Session session) throws HibernateException, SQLException {
                        final AtomicReference<CounterInfo> ret = new AtomicReference<CounterInfo>();
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                PreparedStatement ps = null;
                                ResultSet rs = null;
                                try {
                                    ps = connection.prepareStatement("SELECT counterid,countername,cnt_sec,cnt_min,cnt_hr,cnt_day,cnt_mnt,last_update" +
                                        " FROM counters WHERE countername=?");
                                    ps.setString(1, counterName);
                                    rs = ps.executeQuery();
                                    if (rs.next()) {
                                        long oid = rs.getLong(1);
                                        String name = rs.getString(2);
                                        long sec = rs.getLong(3);
                                        long min = rs.getLong(4);
                                        long hr = rs.getLong(5);
                                        long day = rs.getLong(6);
                                        long mnt = rs.getLong(7);
                                        long lastUpdate = rs.getLong(8);
                                        ret.set(new CounterInfo(oid, name, sec, min, hr, day, mnt, new Date(lastUpdate)));
                                    }
                                } finally {
                                    ResourceUtils.closeQuietly(rs);
                                    ResourceUtils.closeQuietly(ps);
                                }
                            }
                        });
                        return ret.get();
                    }
                });
            }
        });
    }

    @Override
    public void decrement(final boolean synchronous, final String counterName) {
        if (synchronous) {
            synchronousDecrement(counterName);
        } else {
            asyncDecrement(counterName);
        }
    }

    @Override
    public void reset(final String counterName) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                getHibernateTemplate().execute(new HibernateCallback<Void>() {
                    @Override
                    public Void doInHibernate(Session session) throws HibernateException, SQLException {
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                Counter dbcnt = loadCounter(connection, counterName, true);
                                if (dbcnt == null) {
                                    //Do nothing
                                    return;
                                }
                                dbcnt.setCurrentSecondCounter(0);
                                dbcnt.setCurrentMinuteCounter(0);
                                dbcnt.setCurrentHourCounter(0);
                                dbcnt.setCurrentDayCounter(0);
                                dbcnt.setCurrentMonthCounter(0);
                                dbcnt.setLastUpdate(Calendar.getInstance().getTime().getTime());

                                // put new value in database
                                recordNewCounterValue(connection, counterName, dbcnt);
                            }
                        });
                        return null;
                    }
                });
            }
        });
    }

    private void synchronousDecrement(final String counterName) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                getHibernateTemplate().execute(new HibernateCallback<Void>() {
                    @Override
                    public Void doInHibernate(Session session) throws HibernateException, SQLException {
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                Counter dbcnt = loadCounter(connection, counterName, true);
                                if (dbcnt == null) {
                                    throw new RuntimeException("the counter could not be fetched from db table"); // not supposed to happen
                                }

                                dbcnt.setCurrentSecondCounter(dbcnt.getCurrentSecondCounter() - 1);
                                dbcnt.setCurrentMinuteCounter(dbcnt.getCurrentMinuteCounter() - 1);
                                dbcnt.setCurrentHourCounter(dbcnt.getCurrentHourCounter() - 1);
                                dbcnt.setCurrentDayCounter(dbcnt.getCurrentDayCounter() - 1);
                                dbcnt.setCurrentMonthCounter(dbcnt.getCurrentMonthCounter() - 1);

                                // put new value in database
                                recordNewCounterValue(connection, counterName, dbcnt);
                            }
                        });
                        return null;
                    }
                });
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
        return last.get(Calendar.YEAR) == now.get(Calendar.YEAR) && last.get(Calendar.MONTH) == now.get(Calendar.MONTH);
    }

    private boolean inSameDay(Calendar last, Calendar now) {
        return last.get(Calendar.DATE) == now.get(Calendar.DATE);
    }

    private boolean inSameHour(Calendar last, Calendar now) {
        return last.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY);
    }

    private boolean inSameMinute(Calendar last, Calendar now) {
        return last.get(Calendar.MINUTE) == now.get(Calendar.MINUTE);
    }

    public void ensureCounterExists(@NotNull final String counterName) {
        if (counters.containsKey(counterName))
            return;

        final CounterRecord data = new CounterRecord();
        data.setCounterName(counterName);

        if (!doesCounterExistInDb(counterName)) {
            try {
                tryCreateCounter(counterName);
            } catch (DataAccessException e) {
                if (!doesCounterExistInDb(counterName))
                    throw e;
            }
        }

        counters.putIfAbsent(counterName, new WorkQueue(counterName));
    }

    private boolean doesCounterExistInDb(@NotNull final String counterName) {
        final CounterRecord counterRecord = new CounterRecord();
        counterRecord.setCounterName(counterName);

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(true);
        return tt.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                final List res;

                res = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                    protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                        final Criteria criteria = session.createCriteria(CounterRecord.class);
                        criteria.add(Restrictions.eq("counterName", counterRecord.getCounterName()));
                        return criteria.list();
                    }
                });

                return res != null && !res.isEmpty();
            }
        });
    }

    private void tryCreateCounter(@NotNull final String counterName) {
        final CounterRecord counterRecord = new CounterRecord();
        counterRecord.setCounterName(counterName);

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                getHibernateTemplate().save(counterRecord);
            }
        });
    }

    private long getFieldOfInterest(Counter counter, int fieldOfInterest) {
        final long output;

        // return the fieldOfInterest
        switch (fieldOfInterest) {
            case ThroughputQuota.PER_SECOND:
                output = counter.getCurrentSecondCounter();
                break;
            case ThroughputQuota.PER_MINUTE:
                output = counter.getCurrentMinuteCounter();
                break;
            case ThroughputQuota.PER_HOUR:
                output = counter.getCurrentHourCounter();
                break;
            case ThroughputQuota.PER_DAY:
                output = counter.getCurrentDayCounter();
                break;
            case ThroughputQuota.PER_MONTH:
                output = counter.getCurrentMonthCounter();
                break;
            default:
                throw new IllegalArgumentException("Unknown ThroughputQuota field of interest: " + fieldOfInterest);
        }

        return output;
    }

    public long asyncIncrementAndReturnValue(final String counterName, final long timestamp, final int fieldOfInterest) {
        Counter counter = getCurrentCounterValueReadOnly(counterName);

        incrementCounter(counter, timestamp);

        // Schedule an async counter increment and return the current pre-incremented value
        scheduleAsyncIncrement(counterName, timestamp, fieldOfInterest, -1);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    public long asyncIncrementOnlyWithinLimitAndReturnValue(final String counterName,
                                                            final long timestamp,
                                                            final int fieldOfInterest,
                                                            final long limit)
        throws LimitAlreadyReachedException
    {
        Counter counter = getCurrentCounterValueReadOnly(counterName);

        if (isLimitViolatedAfterIncrement(counter, timestamp, fieldOfInterest, limit))
            throw new LimitAlreadyReachedException("Limit already met");

        // Schedule an async counter increment and return current pre-incremented value
        scheduleAsyncIncrement(counterName, timestamp, fieldOfInterest, limit);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    private Counter getCurrentCounterValueReadOnly(final String counterName) {
        // Use read-only transaction for checking current value, no row lock
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(true);
        return tt.execute(new TransactionCallback<Counter>() {
            @Override
            public Counter doInTransaction(TransactionStatus status) {
                return getHibernateTemplate().execute(new HibernateCallback<Counter>() {
                    @Override
                    public Counter doInHibernate(Session session) throws HibernateException, SQLException {
                        final Counter[] counter = {null};
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                counter[0] = loadCounter(connection, counterName, false);
                            }
                        });
                        return counter[0];
                    }
                });
            }
        });
    }

    private void scheduleAsyncIncrement(final String counterName, long timestamp, int fieldOfInterest, long limit) {
        scheduleAsyncCounterStep(counterName, new CounterStep(timestamp, fieldOfInterest, limit));
    }

    private void scheduleAsyncCounterStep(String counterName, CounterStep inc) {
        // Enqueue increment for this counter
        final WorkQueue workQueue = counters.get(counterName);
        if (workQueue == null)
            throw new IllegalStateException("No WorkQueue for counter " + counterName); // can't happen if user called ensureCounterExists() before working with it
        final boolean enqueued = workQueue.queue.offer(inc);

        // Ensure someone will service this queue
        updateThreads.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                while (!workQueue.queue.isEmpty() && workQueue.workMutex.tryLock()) {
                    try {
                        serviceCounterUpdateQueue(workQueue);
                    } finally {
                        workQueue.workMutex.unlock();
                    }
                }
                return null;
            }
        });

        // If queue was full, resubmit with block
        if (!enqueued) {
            try {
                workQueue.queue.put(inc); // might block here if updates have fallen way behind
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Batch all outstanding updates for the specified counter into a single atomic update
    private void serviceCounterUpdateQueue(@NotNull final WorkQueue workQueue) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                getHibernateTemplate().execute(new HibernateCallback<Void>() {
                    @Override
                    public Void doInHibernate(Session session) throws HibernateException, SQLException {
                        session.doWork(new Work() {
                            @Override
                            public void execute(Connection connection) throws SQLException {
                                final String counterName = workQueue.counterName;
                                Counter counter = loadCounter(connection, counterName, true);

                                boolean updated = false;
                                for (int i = 0; i < batchLimit; ++i) {
                                    CounterStep step = workQueue.queue.poll();
                                    if (step == null)
                                        break;

                                    Counter beforeStep = new Counter(counter);

                                    if (step.decrement) {
                                        counter.setCurrentSecondCounter(counter.getCurrentSecondCounter() - 1);
                                        counter.setCurrentMinuteCounter(counter.getCurrentMinuteCounter() - 1);
                                        counter.setCurrentHourCounter(counter.getCurrentHourCounter() - 1);
                                        counter.setCurrentDayCounter(counter.getCurrentDayCounter() - 1);
                                        counter.setCurrentMonthCounter(counter.getCurrentMonthCounter() - 1);
                                        updated = true;
                                    } else if (isLimitViolatedAfterIncrement(counter, step.timestamp, step.fieldOfInterest, step.limit)) {
                                        // Oops.  We already let them through.  Log it at low level, in case any one cares
                                        logger.log(Level.FINE, "Async processing permitted request over quota for counter: {0}", counterName);
                                        // Roll back counter change
                                        counter = beforeStep;
                                    } else {
                                        updated = true;
                                    }
                                }

                                if (updated) {
                                    recordNewCounterValue(connection, counterName, counter);
                                }
                            }
                        });
                        return null;
                    }
                });
            }
        });
    }

    private boolean isLimitViolatedAfterIncrement(Counter dbcnt, long timestamp, int fieldOfInterest, long limit) {
        incrementCounter(dbcnt, timestamp);

        return isLimitViolated(dbcnt, fieldOfInterest, limit);
    }

    private boolean isLimitViolated(Counter dbcnt, int fieldOfInterest, long limit) {
        if (limit == -1)
            return false;

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
        return limitViolated;
    }

    public void asyncDecrement(final String counterName) {
        scheduleAsyncCounterStep(counterName, new CounterStep(true));
    }

    private boolean inSameSecond(Calendar last, Calendar now) {
        return last.get(Calendar.MINUTE) == now.get(Calendar.MINUTE) && last.get(Calendar.SECOND) == now.get(Calendar.SECOND);
    }

    private static class WorkQueue {
        private final String counterName;
        private final BlockingQueue<CounterStep> queue = new LinkedBlockingQueue<CounterStep>(counterQueueSize);
        private final Lock workMutex = new ReentrantLock(); // Ensure only one worker thread at a time does update runs for this queue

        private WorkQueue(String counterName) {
            this.counterName = counterName;
        }
    }

    private static class CounterStep {
        final long timestamp;
        final int fieldOfInterest;
        final long limit;
        final boolean decrement;

        private CounterStep(long timestamp, int fieldOfInterest, long limit) {
            this.timestamp = timestamp;
            this.fieldOfInterest = fieldOfInterest;
            this.limit = limit;
            this.decrement = false;
        }

        private CounterStep(boolean decrement) {
            this.decrement = decrement;
            this.timestamp = -1;
            this.fieldOfInterest = -1;
            this.limit = -1;
        }
    }
}