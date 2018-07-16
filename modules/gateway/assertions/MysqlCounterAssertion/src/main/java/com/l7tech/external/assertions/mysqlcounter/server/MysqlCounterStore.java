/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p>
 * User: flascell<br/>
 * Date: Jun 1, 2005<br/>
 */
package com.l7tech.external.assertions.mysqlcounter.server;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterStore;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.IllegalFieldOfInterestException;
import com.l7tech.server.extension.provider.sharedstate.SharedCounterConfigConstants;
import com.l7tech.server.sla.Counter;
import com.l7tech.server.sla.CounterInfo;
import com.l7tech.server.sla.CounterRecord;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.*;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CounterManager implementation that create a new counter, find counters, increment or decrement counter values, etc.
 * <p>
 * Note: CounterIDManagerImpl has been removed and merged into this implementation.
 * <p>
 * This implementation does not use automatic transaction management; transactions are managed manually.
 *
 * @author flascelles@layer7-tech.com
 */
class MysqlCounterStore extends HibernateDaoSupport implements SharedCounterStore {
    private static final String SQL_FOR_UPDATE = " FOR UPDATE";
    private static final Logger LOGGER = Logger.getLogger(MysqlCounterStore.class.getName());

    private static final int BATCH_LIMIT = SyspropUtil.getInteger("com.l7tech.hacounter.BATCH_LIMIT", 4096);
    private static final int CORE_THREADS = SyspropUtil.getInteger("com.l7tech.hacounter.CORE_THREADS", 16);
    private static final int MAX_THREADS = SyspropUtil.getInteger("com.l7tech.hacounter.MAX_THREADS", 128);
    private static final int KEEP_ALIVE_SEC = SyspropUtil.getInteger("com.l7tech.hacounter.KEEP_ALIVE_SEC", 10);
    private static final int SUPERVISOR_QUEUE_SIZE = SyspropUtil.getInteger("com.l7tech.hacounter.SUPERVISOR_QUEUE_SIZE", 4096);
    private static final int COUNTER_QUEUE_SIZE = SyspropUtil.getInteger("com.l7tech.hacounter.COUNTER_QUEUE_SIZE", 4096);

    private static final int FLUSH_TIME = SyspropUtil.getInteger("com.l7tech.hacounter.flushTimeWriteDatabase", 500);
    private static final int READ_PERIOD = SyspropUtil.getInteger("com.l7tech.hacounter.timeClearReadCache", 60000);

    private static final ExecutorService UPDATE_THREADS = new ThreadPoolExecutor(CORE_THREADS, MAX_THREADS, KEEP_ALIVE_SEC, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(SUPERVISOR_QUEUE_SIZE), new ThreadPoolExecutor.CallerRunsPolicy());
    private static final String DB_CANNOT_FIND_COUNTER_ERROR_MESSAGE = "the counter could not be fetched from db table";
    private final ConcurrentMap<String, WorkQueue> counters = new ConcurrentHashMap<>();

    private Map<String, Counter> readCounters = new HashMap<>();

    private final PlatformTransactionManager transactionManager;

    static TimeSource timeSource = new TimeSource();

    public MysqlCounterStore(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                readCounters.clear(); // This is the only time entries are ever removed from the nameCache
            }
        }, READ_PERIOD, READ_PERIOD);

        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {

                for (final WorkQueue workQueue : counters.values()) {
                    if (!workQueue.queue.isEmpty()) {
                        // Ensure someone will service this queue
                        UPDATE_THREADS.submit(() -> {
                            if (!workQueue.queue.isEmpty() && workQueue.workMutex.tryLock()) {
                                try {
                                    serviceCounterUpdateQueue(workQueue);
                                } finally {
                                    workQueue.workMutex.unlock();
                                }
                            }
                        });
                    }
                }
            }
        }, FLUSH_TIME, FLUSH_TIME);
    }

    private Counter loadCounter(Connection conn, String counterName, boolean readSync) throws SQLException {
        Counter output;
        String query = "SELECT cnt_sec, cnt_min, cnt_hr, cnt_day, cnt_mnt, last_update FROM counters WHERE countername=?";
        if (readSync) {
            query = query + SQL_FOR_UPDATE;
        }

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            int parameterIndex = 1;
            ps.setString(parameterIndex, counterName);
            try (ResultSet rs = ps.executeQuery()) {
                output = null;
                if (rs.next()) {
                    output = new Counter();
                    output.setCurrentSecondCounter(rs.getLong(1));
                    output.setCurrentMinuteCounter(rs.getLong(2));
                    output.setCurrentHourCounter(rs.getLong(3));
                    output.setCurrentDayCounter(rs.getLong(4));
                    output.setCurrentMonthCounter(rs.getLong(5));
                    output.setLastUpdate(rs.getLong(6));
                }
            }
        }
        return output;
    }


    private void recordNewCounterValue(Connection conn, String counterName, Counter newValues) throws SQLException {
        String query = "UPDATE counters SET cnt_sec=?, cnt_min=?, cnt_hr=?, cnt_day=?, cnt_mnt=?, last_update=? WHERE countername=?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.clearParameters();
            ps.setLong(1, newValues.getCurrentSecondCounter());
            ps.setLong(2, newValues.getCurrentMinuteCounter());
            ps.setLong(3, newValues.getCurrentHourCounter());
            ps.setLong(4, newValues.getCurrentDayCounter());
            ps.setLong(5, newValues.getCurrentMonthCounter());
            ps.setLong(6, newValues.getLastUpdate());
            ps.setString(7, counterName);
            ps.executeUpdate();
        }
    }

    public long incrementOnlyWithinLimitAndReturnValue(final boolean synchronous,
                                                       final boolean readSynchronous,
                                                       final String counterName,
                                                       final long timestamp,
                                                       final CounterFieldOfInterest fieldOfInterest,
                                                       final long limit,
                                                       final int incrementValue) throws CounterLimitReachedException {
        if (synchronous) {
            return synchronousIncrementOnlyWithinLimitAndReturnValue(counterName, timestamp, fieldOfInterest, limit, incrementValue);
        } else {
            return asyncIncrementOnlyWithinLimitAndReturnValue(readSynchronous, counterName, timestamp, fieldOfInterest, limit, incrementValue);
        }
    }

    private long synchronousIncrementOnlyWithinLimitAndReturnValue(final String counterName,
                                                                   final long timestamp,
                                                                   final CounterFieldOfInterest fieldOfInterest,
                                                                   final long limit,
                                                                   final int incrementValue) throws CounterLimitReachedException {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        Either<CounterLimitReachedException, Long> result = tt.execute(transactionStatus -> getHibernateTemplate().execute(session -> {
            final AtomicReference<Either<CounterLimitReachedException, Long>> ret = new AtomicReference<>();
            session.doWork(connection -> getCounterAndUpdateWithLimit(counterName, timestamp, fieldOfInterest, limit, incrementValue, ret, connection));
            return ret.get();
        }));

        if (result.isLeft())
            throw result.left();
        return result.right();
    }

    private void getCounterAndUpdateWithLimit(String counterName, long timestamp, CounterFieldOfInterest fieldOfInterest,
                                              long limit, int incrementValue, AtomicReference<Either<CounterLimitReachedException, Long>> ret,
                                              Connection connection) throws SQLException {
        Counter dbcnt = loadCounter(connection, counterName, true);
        if (dbcnt == null) {
            throw new RuntimeException(DB_CANNOT_FIND_COUNTER_ERROR_MESSAGE); // not supposed to happen
        }

        incrementCounter(dbcnt, timestamp, incrementValue);

        // check if the increment violates the limit
        boolean limitViolated = isLimitViolated(dbcnt, fieldOfInterest, limit);
        if (limitViolated) {
            LOGGER.finest("returning limit already reached");
            ret.set(Either.left(new CounterLimitReachedException("Limit already met")));
        } else {
            // put new value in database
            recordNewCounterValue(connection, counterName, dbcnt);
            ret.set(Either.right(getFieldOfInterest(dbcnt, fieldOfInterest)));
        }
    }

    public long incrementAndReturnValue(final boolean synchronous, final boolean readSynchronous, final String counterName,
                                        final long timestamp, final CounterFieldOfInterest fieldOfInterest, final int incrementValue) {
        if (synchronous) {
            return synchronousIncrementAndReturnValue(counterName, timestamp, fieldOfInterest, incrementValue);
        } else {
            return asyncIncrementAndReturnValue(readSynchronous, counterName, timestamp, fieldOfInterest, incrementValue);
        }
    }

    public long synchronousIncrementAndReturnValue(final String counterName, final long timestamp,
                                                   final CounterFieldOfInterest fieldOfInterest, final int value) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        return tt.execute(transactionStatus -> getHibernateTemplate().execute(session -> {
            final AtomicReference<Long> ret = new AtomicReference<>();
            session.doWork(connection -> getCounterAndUpdate(counterName, timestamp, fieldOfInterest, value, ret, connection)
            );
            return ret.get();
        }));
    }

    private void getCounterAndUpdate(String counterName, long timestamp, CounterFieldOfInterest fieldOfInterest,
                                     int value, AtomicReference<Long> ret, Connection connection) throws SQLException {
        Counter dbcnt = loadCounter(connection, counterName, true);
        if (dbcnt == null) {
            throw new RuntimeException(DB_CANNOT_FIND_COUNTER_ERROR_MESSAGE); // not supposed to happen
        }

        incrementCounter(dbcnt, timestamp, value);
        // put new value in database
        recordNewCounterValue(connection, counterName, dbcnt);

        ret.set(getFieldOfInterest(dbcnt, fieldOfInterest));
    }

    public long getCounterValue(final boolean readSynchronous, final String counterName, final CounterFieldOfInterest fieldOfInterest) {
        Counter counter = getCurrentCounterValueReadOnly(readSynchronous, counterName);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    public CounterInfo getCounterInfo(final @NotNull String counterName) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(true);
        return tt.execute(transactionStatus -> getHibernateTemplate().execute(session -> {
            final AtomicReference<CounterInfo> ret = new AtomicReference<>();
            session.doWork(connection -> getCounterFromDatabase(counterName, ret, connection)
            );
            return ret.get();
        }));
    }

    private void getCounterFromDatabase(@NotNull String counterName, AtomicReference<CounterInfo> ret, Connection connection) throws SQLException {
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
                Map<CounterFieldOfInterest, Long> counts = new HashMap<>();
                counts.put(CounterFieldOfInterest.SEC, sec);
                counts.put(CounterFieldOfInterest.MIN, min);
                counts.put(CounterFieldOfInterest.HOUR, hr);
                counts.put(CounterFieldOfInterest.MONTH, mnt);
                counts.put(CounterFieldOfInterest.DAY, day);
                ret.set(new CounterInfo(name, counts, new Date(lastUpdate)));
            }
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(ps);
        }
    }

    @Override
    public void reset(final String counterName) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                getHibernateTemplate().execute(session -> resetCounterInDatabase(session, counterName));
            }
        });
    }

    @Nullable
    private Void resetCounterInDatabase(Session session, String counterName) {
        session.doWork(connection -> {
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
            dbcnt.setLastUpdate(timeSource.currentTimeMillis());

            // put new value in database
            recordNewCounterValue(connection, counterName, dbcnt);
            readCounters.put(counterName, dbcnt);

        });
        return null;
    }

    private void incrementCounter(Counter cntr, long timestamp, long value) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(timestamp);

        Calendar last = Calendar.getInstance();
        last.setTimeInMillis(cntr.getLastUpdate());

        if (inSameMonth(last, now)) {
            cntr.setCurrentMonthCounter(cntr.getCurrentMonthCounter() + value);
            if (inSameDay(last, now)) {
                cntr.setCurrentDayCounter(cntr.getCurrentDayCounter() + value);
                if (inSameHour(last, now)) {
                    cntr.setCurrentHourCounter(cntr.getCurrentHourCounter() + value);
                    if (inSameMinute(last, now)) {
                        cntr.setCurrentMinuteCounter(cntr.getCurrentMinuteCounter() + value);
                        if (inSameSecond(last, now)) {
                            cntr.setCurrentSecondCounter(cntr.getCurrentSecondCounter() + value);
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
        return tt.execute(status -> {
            final List res;

            res = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                protected Object doInHibernateReadOnly(Session session) throws HibernateException {
                    final Criteria criteria = session.createCriteria(CounterRecord.class);
                    criteria.add(Restrictions.eq("counterName", counterRecord.getCounterName()));
                    return criteria.list();
                }
            });

            return res != null && !res.isEmpty();
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

    private long getFieldOfInterest(Counter counter, CounterFieldOfInterest fieldOfInterest) {
        final long output;

        // return the fieldOfInterest
        switch (fieldOfInterest) {
            case SEC:
                output = counter.getCurrentSecondCounter();
                break;
            case MIN:
                output = counter.getCurrentMinuteCounter();
                break;
            case HOUR:
                output = counter.getCurrentHourCounter();
                break;
            case DAY:
                output = counter.getCurrentDayCounter();
                break;
            case MONTH:
                output = counter.getCurrentMonthCounter();
                break;
            default:
                throw new IllegalFieldOfInterestException(fieldOfInterest.getName());
        }

        return output;
    }

    public long asyncIncrementAndReturnValue(boolean readSynchronous, final String counterName, final long timestamp, final CounterFieldOfInterest fieldOfInterest, final int value) {
        Counter counter = getCurrentCounterValueReadOnly(readSynchronous, counterName);

        incrementCounter(counter, timestamp, value);

        // Schedule an async counter increment and return the current pre-incremented value
        scheduleAsyncIncrement(counterName, timestamp, fieldOfInterest, -1, value);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    public long asyncIncrementOnlyWithinLimitAndReturnValue(boolean readSynchronous, final String counterName,
                                                            final long timestamp,
                                                            final CounterFieldOfInterest fieldOfInterest,
                                                            final long limit,
                                                            final int incrementValue) throws CounterLimitReachedException {
        Counter counter = getCurrentCounterValueReadOnly(readSynchronous, counterName);

        if (isLimitViolatedAfterIncrement(counter, timestamp, fieldOfInterest, limit, incrementValue))
            throw new CounterLimitReachedException("Limit already met");

        incrementCounter(counter, timestamp, incrementValue);

        // Schedule an async counter increment and return current pre-incremented value
        scheduleAsyncIncrement(counterName, timestamp, fieldOfInterest, limit, incrementValue);
        return getFieldOfInterest(counter, fieldOfInterest);
    }

    private Counter getCurrentCounterValueReadOnly(final boolean readSynchronous, final String counterName) {
        // Use read-only transaction for checking current value, no row lock
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(true);
        return tt.execute(status -> getHibernateTemplate().execute(session -> {
            final Counter[] counter = {null};
            session.doWork(connection -> {
                if (!readSynchronous) {
                    if (!readCounters.containsKey(counterName)) {
                        counter[0] = loadCounter(connection, counterName, false);
                        readCounters.put(counterName, counter[0]);
                        return;
                    }

                    counter[0] = readCounters.get(counterName);
                } else {
                    counter[0] = loadCounter(connection, counterName, false);
                }
            });
            return counter[0];
        }));
    }

    private void scheduleAsyncIncrement(final String counterName, long timestamp, CounterFieldOfInterest fieldOfInterest, long limit, int value) {
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

        if (inc.decrement && readCounters.containsKey(counterName)) {
            incrementCounter(readCounters.get(counterName), inc.timestamp, -inc.value);
        }

    }

    // Batch all outstanding updates for the specified counter into a single atomic update
    private void serviceCounterUpdateQueue(@NotNull final WorkQueue workQueue) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                getHibernateTemplate().execute((HibernateCallback<Void>) session -> {
                    session.doWork(connection -> {
                        final String counterName = workQueue.counterName;
                        Counter counter = loadCounter(connection, counterName, true);

                        boolean updated = false;
                        for (int i = 0; i < BATCH_LIMIT; ++i) {
                            CounterStep step = workQueue.queue.poll();
                            if (step == null)
                                break;

                            if (step.decrement) {
                                counter.setCurrentSecondCounter(counter.getCurrentSecondCounter() - step.value);
                                counter.setCurrentMinuteCounter(counter.getCurrentMinuteCounter() - step.value);
                                counter.setCurrentHourCounter(counter.getCurrentHourCounter() - step.value);
                                counter.setCurrentDayCounter(counter.getCurrentDayCounter() - step.value);
                                counter.setCurrentMonthCounter(counter.getCurrentMonthCounter() - step.value);
                                updated = true;
                            } else {
                                if(!isLimitViolatedAfterIncrement(counter, step.timestamp, step.fieldOfInterest, step.limit, step.value)) {
                                    incrementCounter(counter, step.timestamp, step.value);
                                    updated = true;
                                }
                                else{
                                    LOGGER.log(Level.FINE, "Async processing permitted request over quota for counter: {0}", counterName);
                                }
                            }
                        }

                        if (updated) {
                            recordNewCounterValue(connection, counterName, counter);
                        }
                    });
                    return null;
                });
            }
        });
    }

    /**
     * this method checks whether an increment would violate the counter limit by incrementing a copy of the counter
     * and checking if the new value is greater than the limit
     * @param dbcnt counter to check for violation
     * @param timestamp the current time
     * @param fieldOfInterest the field to be incremented
     * @param limit the quota limit
     * @param value the value to increment by
     * @return whether the limit is violated by the increment
     */
    private boolean isLimitViolatedAfterIncrement(final Counter dbcnt, final long timestamp, final CounterFieldOfInterest fieldOfInterest, final long limit, final long value) {
        Counter copyCounter = new Counter(dbcnt);
        incrementCounter(copyCounter, timestamp, value);

        return isLimitViolated(copyCounter, fieldOfInterest, limit);
    }

    private boolean isLimitViolated(Counter dbcnt, CounterFieldOfInterest fieldOfInterest, long limit) {
        if (limit == -1)
            return false;

        // check if the increment violates the limit
        boolean limitViolated = false;
        switch (fieldOfInterest) {
            case SEC:
                if (dbcnt.getCurrentSecondCounter() > limit) {
                    limitViolated = true;
                }
                break;
            case MIN:
                if (dbcnt.getCurrentMinuteCounter() > limit) {
                    limitViolated = true;
                }
                break;
            case HOUR:
                if (dbcnt.getCurrentHourCounter() > limit) {
                    limitViolated = true;
                }
                break;
            case DAY:
                if (dbcnt.getCurrentDayCounter() > limit) {
                    limitViolated = true;
                }
                break;
            case MONTH:
                if (dbcnt.getCurrentMonthCounter() > limit) {
                    limitViolated = true;
                }
                break;
            default:
                throw new IllegalFieldOfInterestException(fieldOfInterest.getName());
        }
        return limitViolated;
    }

    private boolean inSameSecond(Calendar last, Calendar now) {
        return last.get(Calendar.MINUTE) == now.get(Calendar.MINUTE) && last.get(Calendar.SECOND) == now.get(Calendar.SECOND);
    }

    @Override
    public void init() {
        //do nothing
    }

    @Override
    public CounterInfo query(String name) {
        if (!doesCounterExistInDb(name)) {
            return null;
        }

        return this.getCounterInfo(name);
    }

    @Override
    public CounterInfo get(String name) {
        this.ensureCounterExists(name);
        return this.getCounterInfo(name);
    }

    @Override
    public long get(String name, Properties config, CounterFieldOfInterest fieldOfInterest) {
        boolean readSync = isReadSync(config);
        return this.getCounterValue(readSync, name, fieldOfInterest);
    }

    @Override
    public long getAndUpdate(String name, Properties config, CounterFieldOfInterest fieldOfInterest, long timestamp, int value) {
        this.ensureCounterExists(name);
        long count = this.get(name, config, fieldOfInterest);
        this.update(name, config, fieldOfInterest, timestamp, value);
        return count;
    }

    @Override
    public long getAndUpdate(String name, Properties config, CounterFieldOfInterest fieldOfInterest, long timestamp, int value, long limit) throws CounterLimitReachedException {
        this.ensureCounterExists(name);
        long previousCount = this.get(name, config, fieldOfInterest);
        boolean readSync = isReadSync(config);
        boolean writeSync = isWriteSync(config);
        this.incrementOnlyWithinLimitAndReturnValue(writeSync, readSync, name, timestamp, fieldOfInterest, limit, value);
        return previousCount;
    }

    @Override
    public void update(String name, Properties config, CounterFieldOfInterest fieldOfInterest, long timestamp, int value) {
        this.ensureCounterExists(name);
        boolean readSync = isReadSync(config);
        boolean writeSync = isWriteSync(config);
        this.incrementAndReturnValue(writeSync, readSync, name, timestamp, fieldOfInterest, value);
    }

    @Override
    public void update(String name, Properties config, CounterFieldOfInterest fieldOfInterest, long timestamp, int value, long limit) throws CounterLimitReachedException {
        this.ensureCounterExists(name);
        boolean readSync = isReadSync(config);
        boolean writeSync = isWriteSync(config);
        this.incrementOnlyWithinLimitAndReturnValue(writeSync, readSync, name, timestamp, fieldOfInterest, limit, value);
    }

    @Override
    public long updateAndGet(String name, Properties config, CounterFieldOfInterest fieldOfInterest, long timestamp, int value) {
        this.ensureCounterExists(name);
        boolean readSync = isReadSync(config);
        boolean writeSync = isWriteSync(config);
        return this.incrementAndReturnValue(writeSync, readSync, name, timestamp, fieldOfInterest, value);
    }

    @Override
    public long updateAndGet(String name, Properties config, CounterFieldOfInterest fieldOfInterest, long timestamp, int value, long limit) throws CounterLimitReachedException {
        this.ensureCounterExists(name);
        boolean readSync = isReadSync(config);
        boolean writeSync = isWriteSync(config);
        return this.incrementOnlyWithinLimitAndReturnValue(writeSync, readSync, name, timestamp, fieldOfInterest, limit, value);
    }

    private boolean isReadSync(Properties config) {
        return Boolean.parseBoolean(config.getProperty(SharedCounterConfigConstants.CounterOperations.KEY_READ_SYNC, String.valueOf(false)));
    }

    private boolean isWriteSync(Properties config) {
        return Boolean.parseBoolean(config.getProperty(SharedCounterConfigConstants.CounterOperations.KEY_WRITE_SYNC, String.valueOf(false)));
    }

    private static class WorkQueue {
        private final String counterName;
        private final BlockingQueue<CounterStep> queue = new LinkedBlockingQueue<>(COUNTER_QUEUE_SIZE);
        private final Lock workMutex = new ReentrantLock(); // Ensure only one worker thread at a time does update runs for this queue

        private WorkQueue(String counterName) {
            this.counterName = counterName;
        }
    }

    private static class CounterStep {
        final long timestamp;
        final CounterFieldOfInterest fieldOfInterest;
        final long limit;
        final boolean decrement;
        final int value;

        private CounterStep(long timestamp, CounterFieldOfInterest fieldOfInterest, long limit, int value) {
            this.timestamp = timestamp;
            this.fieldOfInterest = fieldOfInterest;
            this.limit = limit;
            this.decrement = false;
            this.value = value;
        }
    }
}