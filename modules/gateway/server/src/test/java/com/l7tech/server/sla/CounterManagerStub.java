package com.l7tech.server.sla;

import com.l7tech.objectmodel.ObjectModelException;
import org.jetbrains.annotations.NotNull;

public class CounterManagerStub implements CounterManager{
    /**
     * Returned by increment methods.
     */
    private long counterValue = 0;

    /**
     * Set to true to throw LimitAlreadyReachedException.
     */
    private boolean throwException;

    public long getCounterValue() {
        return counterValue;
    }

    public void setCounterValue(final long counterValue) {
        this.counterValue = counterValue;
    }

    public boolean isThrowException() {
        return throwException;
    }

    public void setThrowException(final boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    public void ensureCounterExists(@NotNull String counterName) throws ObjectModelException {
    }

    @Override
    public long incrementOnlyWithinLimitAndReturnValue(boolean sync, String counterName, long timestamp, int fieldOfInterest, long limit) throws LimitAlreadyReachedException {
        if(throwException){
            throw new LimitAlreadyReachedException("Throwing exception from stub.");
        }
        return counterValue;
    }

    @Override
    public long incrementAndReturnValue(boolean sync, String counterName, long timestamp, int fieldOfInterest) {
        return counterValue;
    }

    @Override
    public long getCounterValue(String counterName, int fieldOfInterest) {
        return counterValue;
    }

    @Override
    public CounterInfo getCounterInfo(@NotNull String counterName) {
        return null;
    }

    @Override
    public void decrement(boolean sync, String counterName) {
    }

    @Override
    public void reset(String counterName) {
        counterValue = 0;
    }
}
