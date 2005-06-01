/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 4, 2005<br/>
 */
package com.l7tech.server.sla;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.beans.factory.DisposableBean;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Calendar;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.policy.assertion.sla.ThroughputQuota;

/**
 * Caches sla counters locally in a HashMap with internal locking mechanism.
 * Not scalable across cluster.
 * Reconstitutes counters at boot time.
 *
 * @author flascelles@layer7-tech.com
 * @deprecated (remove as soon as DBCounterManager is complete)
 */
public class CounterCache extends ApplicationObjectSupport implements CounterManager, DisposableBean {
    private final Logger logger =  Logger.getLogger(getClass().getName());
    private final HashMap counters = new HashMap();

    public CounterCache() {
    }

    /**
     * Increment the counter identified by counterId only if the resulting value of the counter for
     * the passed fieldOfInterest will not exceed the passed limit.
     *
     * @param counterId the id of the counter as provided by CounterIDManager
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @return the counter value of interest if incremented. if the limit is already reached, an exceptio is thrown
     * @throws LimitAlreadyReachedException if the limit was already reached
     */
    public long incrementOnlyWithinLimitAndReturnValue(long counterId,
                                                       long timestamp,
                                                       int fieldOfInterest,
                                                       long limit) throws LimitAlreadyReachedException {
        Counter counter = null;
        synchronized (counters) {
            Object key = new Long(counterId);
            counter = (Counter)counters.get(key);
            if (counter == null) {
                counter = new Counter();
                counters.put(key, counter);
            }
        }
        Sync write = counter.rwlock.writeLock();
        try {
            write.acquire();
            Counter tmpCounter = new Counter(counter);
            incrementCounterNoLock(tmpCounter, timestamp);

            // check if the increment violates the limit
            boolean limitViolated = false;
            switch (fieldOfInterest) {
                case ThroughputQuota.PER_SECOND:
                    if (tmpCounter.getCurrentSecondCounter() > limit) {
                        limitViolated = true;
                    }
                    break;
                case ThroughputQuota.PER_HOUR:
                    if (tmpCounter.getCurrentHourCounter() > limit) {
                        limitViolated = true;
                    }
                    break;
                case ThroughputQuota.PER_DAY:
                    if (tmpCounter.getCurrentDayCounter() > limit) {
                        limitViolated = true;
                    }
                    break;
                case ThroughputQuota.PER_MONTH:
                    if (tmpCounter.getCurrentMonthCounter() > limit) {
                        limitViolated = true;
                    }
                    break;
            }

            if (limitViolated) {
                write.release();
                throw new LimitAlreadyReachedException("Limit already met");
            } else {
                counter.copyFrom(tmpCounter);
                // record hit in database
                //CountedHitsManager hitsCounter = (CountedHitsManager)getApplicationContext().getBean("countedHitsManager");
                //hitsCounter.recordHit(counterId, timestamp);

                try {
                    switch (fieldOfInterest) {
                        case ThroughputQuota.PER_SECOND:
                            return counter.getCurrentSecondCounter();
                        case ThroughputQuota.PER_HOUR:
                            return counter.getCurrentHourCounter();
                        case ThroughputQuota.PER_DAY:
                            return counter.getCurrentDayCounter();
                        case ThroughputQuota.PER_MONTH:
                            return counter.getCurrentMonthCounter();
                    }
                    // should not happen
                    throw new RuntimeException("Asked for unsupported fieldOfInterest: " + fieldOfInterest);
                } finally {
                    write.release();
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "unexpected InterruptedException in increment", e);
            throw new RuntimeException(e);
        }
    }

    public void decrement(long counterId) {
        Counter counter = null;

        synchronized (counters) {
            Object key = new Long(counterId);
            counter = (Counter)counters.get(key);
            if (counter == null) {
                counter = new Counter();
                counters.put(key, counter);
            }
        }
        Sync write = counter.rwlock.writeLock();
        try {
            write.acquire();
            counter.setCurrentSecondCounter(counter.getCurrentSecondCounter()-1);
            counter.setCurrentHourCounter(counter.getCurrentHourCounter()-1);
            counter.setCurrentDayCounter(counter.getCurrentDayCounter()-1);
            counter.setCurrentMonthCounter(counter.getCurrentMonthCounter()-1);
            write.release();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "unexpected InterruptedException in decrement", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * increment this counter, record the hit in the database and return the specific value of interest
     * @param counterId the id of the counter as provided by CounterIDManager
     * @param timestamp the time for which this increment should be recorded at
     * @param fieldOfInterest ThroughputQuota.PER_SECOND, ThroughputQuota.PER_HOUR, ThroughputQuota.PER_DAY or
     * ThroughputQuota.PER_MONTH
     * @return the counter value of interest
     */
    public long incrementAndReturnValue(long counterId, long timestamp, int fieldOfInterest) {
        Counter counter = null;
        synchronized (counters) {
            Object key = new Long(counterId);
            counter = (Counter)counters.get(key);
            if (counter == null) {
                counter = new Counter();
                counters.put(key, counter);
            }
        }
        Sync write = counter.rwlock.writeLock();
        try {
            write.acquire();
            incrementCounterNoLock(counter, timestamp);
            // record hit in database
            //CountedHitsManager hitsCounter = (CountedHitsManager)getApplicationContext().getBean("countedHitsManager");
            //hitsCounter.recordHit(counterId, timestamp);

            try {
                switch (fieldOfInterest) {
                    case ThroughputQuota.PER_SECOND:
                        return counter.getCurrentSecondCounter();
                    case ThroughputQuota.PER_HOUR:
                        return counter.getCurrentHourCounter();
                    case ThroughputQuota.PER_DAY:
                        return counter.getCurrentDayCounter();
                    case ThroughputQuota.PER_MONTH:
                        return counter.getCurrentMonthCounter();
                }
                // should not happen
                throw new RuntimeException("Asked for unsupported fieldOfInterest: " + fieldOfInterest);
            } finally {
                write.release();
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "unexpected InterruptedException in increment", e);
            throw new RuntimeException(e);
        }
    }

    public long getCounterValue(long counterId, int fieldOfInterest) {
        Counter counter = null;
        synchronized (counters) {
            Object key = new Long(counterId);
            counter = (Counter)counters.get(key);
            if (counter == null) {
                counter = new Counter();
                counters.put(key, counter);
            }
        }
        Sync read = counter.rwlock.readLock();
        try {
            read.acquire();
            try {
                switch (fieldOfInterest) {
                    case ThroughputQuota.PER_SECOND:
                        return counter.getCurrentSecondCounter();
                    case ThroughputQuota.PER_HOUR:
                        return counter.getCurrentHourCounter();
                    case ThroughputQuota.PER_DAY:
                        return counter.getCurrentDayCounter();
                    case ThroughputQuota.PER_MONTH:
                        return counter.getCurrentMonthCounter();
                }
                // should not happen
                throw new RuntimeException("Asked for unsupported fieldOfInterest: " + fieldOfInterest);
            } finally {
                read.release();
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "unexpected InterruptedException in getCounterValue", e);
            throw new RuntimeException(e);
        }
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

    /**
     * this will rebuild the cache from the persisted hits

    public void regenFromDatabase() {
        CountedHitsManager hitsCounter = (CountedHitsManager)getApplicationContext().getBean("countedHitsManager");
        Long res = hitsCounter.latestHit();
        if (res == null) {
            // no hits, we're done
            return;
        } else {
            if (!counters.isEmpty()) {
                logger.info("regenFromDatabase called but cache is not empty.");
                counters.clear();
            }

            long latestUpdate = res.longValue();
            Calendar basis = Calendar.getInstance();
            basis.setTimeInMillis(latestUpdate);
            long startofsecond = startOfCurrentSec(basis);
            long startofhr = startOfCurrentHr(basis);
            long startofday = startOfCurrentDay(basis);
            long startofmonth = startOfCurrentMonth(basis);

            List distinctcounters = hitsCounter.listOfCountersRecorded(latestUpdate);

            for (Iterator iterator = distinctcounters.iterator(); iterator.hasNext();) {
                Object i = iterator.next();
                if (i != null && i instanceof Long) {
                    Counter counter = new Counter();
                    counter.setLastUpdate(latestUpdate);

                    counters.put(i, counter);
                    long counterid = ((Long)i).longValue();

                    logger.finest("rebuilding counter for " + counterid);

                    counter.setCurrentSecondCounter(hitsCounter.countForInterval(counterid,
                                                                                 startofsecond,
                                                                                 latestUpdate));
                    counter.setCurrentHourCounter(hitsCounter.countForInterval(counterid,
                                                                               startofhr,
                                                                               latestUpdate));
                    counter.setCurrentDayCounter(hitsCounter.countForInterval(counterid,
                                                                              startofday,
                                                                              latestUpdate));
                    counter.setCurrentMonthCounter(hitsCounter.countForInterval(counterid,
                                                                                startofmonth,
                                                                                latestUpdate));

                    logger.finest("sla counter rebuilt for " + counterid + " " + counter);
                }

            }
        }
    }*/

    /*private long startOfCurrentSec(Calendar current) {
        current.set(Calendar.MILLISECOND, 0);
        return current.getTimeInMillis();
    }

    private long startOfCurrentHr(Calendar current) {
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MINUTE, 0);
        return current.getTimeInMillis();
    }

    private long startOfCurrentDay(Calendar current) {
        current.set(Calendar.HOUR_OF_DAY, 0);
        return current.getTimeInMillis();
    }

    private long startOfCurrentMonth(Calendar current) {
        current.set(Calendar.DAY_OF_MONTH, 1);
        return current.getTimeInMillis();
    }*/

    private void incrementCounterNoLock(Counter cntr, long timestamp) {
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

    public void destroy() throws Exception {
        // todo, cleanup
    }

    public void initialize() {
        // todo, if we were in a cluster here, we would sync with another node instead of synching with db
        //regenFromDatabase();
        // todo initialize a process which cleans up the table from old records
    }
}
