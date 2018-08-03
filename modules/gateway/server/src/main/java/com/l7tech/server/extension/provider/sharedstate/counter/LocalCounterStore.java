package com.l7tech.server.extension.provider.sharedstate.counter;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterState;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterStore;
import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LocalCounterStore provides a local in-memory version of the SharedCounterStore.  The counter data here will NOT be
 * shared amongst different gateway in a cluster.
 * <p>
 * This implementation ignores the sync/async read/write properties passed in to counter operations.
 */
public final class LocalCounterStore implements SharedCounterStore {

    private ConcurrentHashMap<String, Counter> counterStore;

    LocalCounterStore() {
        this.counterStore = new ConcurrentHashMap<>();
    }

    @Override
    public void init() {
        // Do nothing
    }

    @Override
    public SharedCounterState query(String name) {
        final AtomicReference<SharedCounterState> counterState = new AtomicReference<>(null);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    if (null == nullableCounter) {
                        return null;
                    }

                    return doGet(counterState, nullableCounter);
        });
        return counterState.get();
    }

    @Override
    public SharedCounterState get(String name) {
        final AtomicReference<SharedCounterState> counterState = new AtomicReference<>(null);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doGet(counterState, counter);
                });
        return counterState.get();
    }

    @NotNull
    private Counter doGet(AtomicReference<SharedCounterState> counterState, @NotNull Counter counter)
    {
        counterState.set(counter.getState());
        return counter;
    }

    @Override
    public long get(String name, Properties counterOperationProperties, CounterFieldOfInterest fieldOfInterest)
    {
        AtomicReference<Long> count = new AtomicReference<>(0L);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                        Counter counter = createCounterIfNull(counterName, nullableCounter);
                        return doGetField(fieldOfInterest, count, counter);
                });
        return count.get();
    }

    @NotNull
    private Counter doGetField(CounterFieldOfInterest fieldOfInterest,
                               AtomicReference<Long> count,
                               @NotNull Counter counter)
    {
        count.set(counter.getCounterField(fieldOfInterest));
        return counter;
    }

    @Override
    public long getAndUpdate(String name,
                             Properties counterOperationsProperties,
                             CounterFieldOfInterest fieldOfInterest,
                             long timestamp,
                             int delta)
    {
        final AtomicReference<Long> oldCounterValue = new AtomicReference<>(0L);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doGetAndUpdate(fieldOfInterest, timestamp, delta, oldCounterValue, counter);
                });
        return oldCounterValue.get();
    }

    @NotNull
    private Counter doGetAndUpdate(CounterFieldOfInterest fieldOfInterest,
                                   long timestamp, int delta,
                                   AtomicReference<Long> oldCounterValue,
                                   @NotNull Counter counter)
    {
        oldCounterValue.set(counter.getCounterField(fieldOfInterest));
        counter.updateCounter(timestamp, delta);
        return counter;
    }

    @Override
    public long getAndUpdate(String name,
                             Properties counterOperationsProperties,
                             CounterFieldOfInterest fieldOfInterest,
                             long timestamp,
                             int delta,
                             long limit) throws CounterLimitReachedException
    {
        final AtomicReference<Long> oldCounterValue = new AtomicReference<>(0L);
        final AtomicReference<CounterLimitReachedException> exception = new AtomicReference<>(null);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doGetAndUpdateWithLimit(fieldOfInterest, timestamp, delta, limit, oldCounterValue, exception, counter);
                });
        if (exception.get() != null) {
            throw exception.get();
        }
        return oldCounterValue.get();
    }

    @NotNull
    private Counter doGetAndUpdateWithLimit(CounterFieldOfInterest fieldOfInterest,
                                            long timestamp,
                                            int delta,
                                            long limit,
                                            AtomicReference<Long> oldCounterValue,
                                            AtomicReference<CounterLimitReachedException> exception,
                                            @NotNull Counter counter)
    {
        oldCounterValue.set(counter.getCounterField(fieldOfInterest));
        try {
            counter.updateCounterWithoutExceedingLimit(fieldOfInterest, timestamp, delta, limit);
        } catch (CounterLimitReachedException e) {
            exception.set(e);
        }
        return counter;
    }

    @Override
    public void reset(final String name) {
        Counter counter = new Counter(name);
        counterStore.put(name, counter);
    }

    @Override
    public void update(String name,
                       Properties counterOperationsProperties,
                       CounterFieldOfInterest fieldOfInterest,
                       long updateTime,
                       int delta)
    {
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doUpdate(updateTime, delta, counter);
                });
    }

    @NotNull
    private Counter doUpdate(long updateTime, int delta, Counter counter) {
        counter.updateCounter(updateTime, delta);
        return counter;
    }

    @Override
    public void update(String name,
                       Properties counterOperationsProperties,
                       CounterFieldOfInterest fieldOfInterest,
                       long timestamp,
                       int delta,
                       long limit) throws CounterLimitReachedException
    {
        final AtomicReference<CounterLimitReachedException> exception = new AtomicReference<>(null);
        counterStore.compute(name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doUpdateWithLimit(fieldOfInterest, timestamp, delta, limit, exception, counter);
                });
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    @NotNull
    private Counter doUpdateWithLimit(CounterFieldOfInterest fieldOfInterest,
                                      long timestamp,
                                      int delta,
                                      long limit,
                                      AtomicReference<CounterLimitReachedException> exception,
                                      @NotNull Counter counter)
    {
        try {
            counter.updateCounterWithoutExceedingLimit(fieldOfInterest, timestamp, delta, limit);
        } catch (CounterLimitReachedException e) {
            exception.set(e);
        }
        return counter;
    }

    @Override
    public long updateAndGet(String name,
                             Properties counterOperationsProperties,
                             CounterFieldOfInterest fieldOfInterest,
                             long timestamp,
                             int delta)
    {
        AtomicReference<Long> newCounterValue = new AtomicReference<>(0L);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doUpdateAndGet(fieldOfInterest, timestamp, delta, newCounterValue, counter);
                });
        return newCounterValue.get();
    }

    @NotNull
    private Counter doUpdateAndGet(CounterFieldOfInterest fieldOfInterest,
                                   long timestamp,
                                   int delta,
                                   AtomicReference<Long> newCounterValue,
                                   @NotNull Counter counter)
    {
        counter.updateCounter(timestamp, delta);
        newCounterValue.set(counter.getCounterField(fieldOfInterest));
        return counter;
    }

    @Override
    public long updateAndGet(String name,
                             Properties counterOperationsProperties,
                             CounterFieldOfInterest fieldOfInterest,
                             long timestamp,
                             int delta,
                             long limit) throws CounterLimitReachedException
    {
        AtomicReference<Long> newCounterValue = new AtomicReference<>(0L);
        final AtomicReference<CounterLimitReachedException> exception = new AtomicReference<>(null);
        counterStore.compute(
                name,
                (counterName, nullableCounter) -> {
                    Counter counter = createCounterIfNull(counterName, nullableCounter);
                    return doUpdateAndGetWithLimit(fieldOfInterest, timestamp, delta, limit, newCounterValue, exception, counter);
                });
        if (exception.get() != null) {
            throw exception.get();
        }
        return newCounterValue.get();
    }

    @NotNull
    private Counter doUpdateAndGetWithLimit(CounterFieldOfInterest fieldOfInterest,
                                            long timestamp,
                                            int delta,
                                            long limit,
                                            AtomicReference<Long> newCounterValue,
                                            AtomicReference<CounterLimitReachedException> exception,
                                            @NotNull Counter counter)
    {
        try {
            counter.updateCounterWithoutExceedingLimit(fieldOfInterest, timestamp, delta, limit);
        } catch (CounterLimitReachedException e) {
            exception.set(e);
        }
        newCounterValue.set(counter.getCounterField(fieldOfInterest));
        return counter;
    }

    private Counter createCounterIfNull(String counterName, @Nullable Counter nullableCounter) {

        return nullableCounter == null ? new Counter(counterName) : nullableCounter;
    }
}