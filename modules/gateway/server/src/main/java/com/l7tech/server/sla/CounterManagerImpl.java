/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Jun 1, 2005<br/>
 */
package com.l7tech.server.sla;

import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Background;
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
import java.sql.Date;
import java.util.*;
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
    private static final int maxThreads = SyspropUtil.getInteger("com.l7tech.hacounter.maxThreads", 128);
    private static final int keepAliveSec = SyspropUtil.getInteger("com.l7tech.hacounter.keepAliveSec", 10);
    private static final int supervisorQueueSize = SyspropUtil.getInteger("com.l7tech.hacounter.supervisorQueueSize", 4096);
    private static final int counterQueueSize = SyspropUtil.getInteger("com.l7tech.hacounter.counterQueueSize", 4096);

    private static final int flushTime = SyspropUtil.getInteger("com.l7tech.hacounter.flushTimeWriteDatabase", 1000);
    private static final int readPeriod = SyspropUtil.getInteger("com.l7tech.hacounter.timeClearReadCache", 60000);

    private static final ExecutorService updateThreads = new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveSec, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(supervisorQueueSize), new ThreadPoolExecutor.CallerRunsPolicy());
    private final ConcurrentMap<String,WorkQueue> counters = new ConcurrentHashMap<String,WorkQueue>();

    private Map<String, Counter> readCounters = new HashMap<String, Counter>();

    private final PlatformTransactionManager transactionManager;

    public CounterManagerImpl(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                readCounters.clear(); // This is the only time entries are ever removed from the nameCache
            }
        }, readPeriod, readPeriod);

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {

                for(final WorkQueue workQueue : counters.values()) {
                    if(workQueue.queue.size() > 0) {
                        // Ensure someone will service this queue
                        updateThreads.submit(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                if (!workQueue.queue.isEmpty() && workQueue.workMutex.tryLock()) {
                                    try {
                                        serviceCounterUpdateQueue(workQueue);
                                    } finally {
                                        workQueue.workMutex.unlock();
                                    }
                                }
                                return null;
                            }
                        });
                    }
                }
            }
        }, flushTime, flushTime);
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
                                                       final boolean readSynchronous,
                                                       final String counterName,
                                                       final long timestamp,
                                                       final int fieldOfInterest,
                                                       final long limit,
                                                       final int incrementValue) throws LimitAlreadyReachedException
    {
        if (synchronous) {
            return synchronousIncrementOnlyWithinLimitAndReturnValue(counterName, timestamp, fieldOfInterest, limit, incrementValue);
        } else {
            return asyncIncrementOnlyWithinLimitAndReturnValue(readSynchronous, counterName, timestamp, fieldOfInterest, limit, incrementValue);
        }
    }

    private long synchronousIncrementOnlyWithinLimitAndReturnValue(final String counterName,
                                                                   final long timestamp,
                                                                   final int fieldOfInterest,
                                                                   final long limit,
                                                                   final int incrementValue) throws LimitAlreadyReachedException
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

                                incrementCounter(dbcnt, timestamp, incrementValue);

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
    public long incrementAndReturnValue(final boolean synchronous, final boolean readSynchronous, final String counterName, final long timestamp, final int fieldOfInterest, final int incrementValue) {
        if (synchronous) {
            return synchronousIncrementAndReturnValue(counterName, timestamp, fieldOfInterest, incrementValue);
        } else {
            return asyncIncrementAndReturnValue(readSynchronous, counterName, timestamp, fieldOfInterest, incrementValue);
        }
    }

    public long synchronousIncrementAndReturnValue(final String counterName, final long timestamp, final int fieldOfInterest, final int value) {
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

                                incrementCounter(dbcnt, timestamp, value);
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

    public long getCounterValue(final boolean readSynchronous, final String counterName, final int fieldOfInterest) {
        Counter counter = getCurrentCounterValueReadOnly(readSynchronous, counterName);
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
                                    ps = connection.prepareStatement("SELECT countername,cnt_sec,cnt_min,cnt_hr,cnt_day,cnt_mnt,last_update" +
                                        " FROM counters WHERE countername=?");
                                    ps.setString(1, counterName);
                                    rs = ps.executeQuery();
                                    if (rs.next()) {
                                        String name = rs.getString(1);
                                        long sec = rs.getLong(2);
                                        long min = rs.getLong(3);
                                        long hr = rs.getLong(4);
                                        long day = rs.getLong(5);
                                        long mnt = rs.getLong(6);
                                        long lastUpdate = rs.getLong(7);
                                        ret.set(new CounterInfo(name, sec, min, hr, day, mnt, new Date(lastUpdate)));
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
    public void decrement(final boolean synchronous, final String counterName, int decrementValue) {
        if (synchronous) {
            synchronousDecrement(counterName, decrementValue);
        } else {
            asyncDecrement(counterName, decrementValue);
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

    private void synchronousDecrement(final String counterName, final int decrementValue) {
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

                                dbcnt.setCurrentSecondCounter(dbcnt.getCurrentSecondCounter() - decrementValue);
                                dbcnt.setCurrentMinuteCounter(dbcnt.getCurrentMinuteCounter() - decrementValue);
                                dbcnt.setCurrentHourCounter(dbcnt.getCurrentHourCounter() - decrementValue);
                                dbcnt.setCurrentDayCounter(dbcnt.getCurrentDayCounter() - decrementValue);
                                dbcnt.setCurrentMonthCounter(dbcnt.getCurrentMonthCounter() - decrementValue);

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

    private void incrementCounter(Counter cntr, long timestamp, long value) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(timestamp);

        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(cntr.getLastUpdate());

        if (inSameMonth(last, now)) {
            cntr.setCurrentMonthCounter(cntr.getCurrentMonthCounter()+value);
            if (inSameDay(last, now)) {
                cntr.setCurrentDayCounter(cntr.getCurrentDayCounter()+value);
                if (inSameHour(last, now)) {
                    cntr.setCurrentHourCounter(cntr.getCurrentHourCounter()+value);
                    if (inSameMinute(last, now)) {
                        cntr.setCurrentMinuteCounter(cntr.getCurrentMinuteCounter()+value);
                        if (inSameSecond(last, now)) {
                            cntr.setCurrentSecondCounter(cntr.getCurrentSecondCounter()+value);
                        } else {
                            cntr.setCurrentSecondCounter(value);
                        }
                    } else {
                        cntr.setCurrentMinuteCounter(value);
                        cntr.setCurrentSecondCounter(value);
                    }
                } else {
                    cntr.setCurrentHourCounter(value);
                    cntr.setCurrentMinuteCounter(value);
                    cntr.setCurrentSecondCounter(value);
                }
            } else {
                cntr.setCurrentDayCounter(value);
                cntr.setCurrentHourCounter(value);
                cntr.setCurrentMinuteCounter(value);
                cntr.setCurrentSecondCounter(value);
            }
        } else {
            cntr.setCurrentMonthCounter(value);
            cntr.setCurrentDayCounter(value);
            cntr.setCurrentHourCounter(value);
            cntr.setCurrentMinuteCounter(value);
            cntr.setCurrentSecondCounter(value);
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

    public long asyncIncrementAndReturnValue(boolean readSynchronous, final String counterName, final long timestamp, final int fieldOfInterest, final int value) {
        Counter counter = getCurrentCounterValueReadOnly(readSynchronous, counterName);

        incrementCounter(counter, timestamp, value);

        // Schedule an async counter increment and return the current pre-incremented value
        scheduleAsyncIncrement(counterName, timestamp, fieldOfInterest, -1, value);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    public long asyncIncrementOnlyWithinLimitAndReturnValue(boolean readSynchronous, final String counterName,
                                                            final long timestamp,
                                                            final int fieldOfInterest,
                                                            final long limit,
                                                            final int incrementValue)
        throws LimitAlreadyReachedException
    {
        Counter counter = getCurrentCounterValueReadOnly(readSynchronous, counterName);


        if (isLimitViolatedAfterIncrement(counter, timestamp, fieldOfInterest, limit, incrementValue))
            throw new LimitAlreadyReachedException("Limit already met");

        // Schedule an async counter increment and return current pre-incremented value
        scheduleAsyncIncrement(counterName, timestamp, fieldOfInterest, limit, incrementValue);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    private Counter getCurrentCounterValueReadOnly(final boolean readSynchronous, final String counterName) {
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
                                if(!readSynchronous) {
                                    if(!readCounters.containsKey(counterName)) {
                                        counter[0] = loadCounter(connection, counterName, false);
                                        readCounters.put(counterName, counter[0]);
                                        return;
                                    }

                                    counter[0] = readCounters.get(counterName);
                                } else {
                                    counter[0] = loadCounter(connection, counterName, false);
                                }
                            }
                        });
                        return counter[0];
                    }
                });
            }
        });
    }

    private void scheduleAsyncIncrement(final String counterName, long timestamp, int fieldOfInterest, long limit, int value) {
        scheduleAsyncCounterStep(counterName, new CounterStep(timestamp, fieldOfInterest, limit, value));
    }

    private void scheduleAsyncCounterStep(String counterName, CounterStep inc) {
        // Enqueue increment for this counter
        final WorkQueue workQueue = counters.get(counterName);
        if (workQueue == null)
            throw new IllegalStateException("No WorkQueue for counter " + counterName); // can't happen if user called ensureCounterExists() before working with it
        final boolean enqueued = workQueue.queue.offer(inc);

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
                                        counter.setCurrentSecondCounter(counter.getCurrentSecondCounter() - step.value);
                                        counter.setCurrentMinuteCounter(counter.getCurrentMinuteCounter() - step.value);
                                        counter.setCurrentHourCounter(counter.getCurrentHourCounter() - step.value);
                                        counter.setCurrentDayCounter(counter.getCurrentDayCounter() - step.value);
                                        counter.setCurrentMonthCounter(counter.getCurrentMonthCounter() - step.value);
                                        updated = true;
                                    } else if (isLimitViolatedAfterIncrement(counter, step.timestamp, step.fieldOfInterest, step.limit, step.value)) {
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

    private boolean isLimitViolatedAfterIncrement(Counter dbcnt, long timestamp, int fieldOfInterest, long limit, long value) {
        incrementCounter(dbcnt, timestamp, value);

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

    public void asyncDecrement(final String counterName, int decrementValue) {
        scheduleAsyncCounterStep(counterName, new CounterStep(true, decrementValue));
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
        final int value;

        private CounterStep(long timestamp, int fieldOfInterest, long limit, int value) {
            this.timestamp = timestamp;
            this.fieldOfInterest = fieldOfInterest;
            this.limit = limit;
            this.decrement = false;
            this.value = value;
        }

        private CounterStep(boolean decrement, int value) {
            this.decrement = decrement;
            this.timestamp = -1;
            this.fieldOfInterest = -1;
            this.limit = -1;
            this.value = value;
        }
    }
}