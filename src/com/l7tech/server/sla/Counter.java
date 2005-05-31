/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 4, 2005<br/>
 */
package com.l7tech.server.sla;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

/**
 * A counter to enforce sla limit against.
 *
 * @author flascelles@layer7-tech.com
 */
public class Counter {
    private long lastUpdate;
    private long currentSecondCounter;
    private long currentHourCounter;
    private long currentDayCounter;
    private long currentMonthCounter;
    final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();

    public Counter(){}

    public Counter(Counter other) {
        copyFrom(other);
    }

    public void copyFrom(Counter other) {
        this.lastUpdate = other.lastUpdate;
        this.currentSecondCounter = other.currentSecondCounter;
        this.currentHourCounter = other.currentHourCounter;
        this.currentDayCounter = other.currentDayCounter;
        this.currentMonthCounter = other.currentMonthCounter;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public long getCurrentSecondCounter() {
        return currentSecondCounter;
    }

    public void setCurrentSecondCounter(long currentSecondCounter) {
        this.currentSecondCounter = currentSecondCounter;
    }

    public long getCurrentHourCounter() {
        return currentHourCounter;
    }

    public void setCurrentHourCounter(long currentHourCounter) {
        this.currentHourCounter = currentHourCounter;
    }

    public long getCurrentDayCounter() {
        return currentDayCounter;
    }

    public void setCurrentDayCounter(long currentDayCounter) {
        this.currentDayCounter = currentDayCounter;
    }

    public long getCurrentMonthCounter() {
        return currentMonthCounter;
    }

    public void setCurrentMonthCounter(long currentMonthCounter) {
        this.currentMonthCounter = currentMonthCounter;
    }

    public String toString() {
        return "com.l7tech.server.sla.Counter" +
                    " this sec:" + currentSecondCounter +
                    " this hr:" + currentHourCounter +
                    " this day:" + currentDayCounter +
                    " this month:" + currentMonthCounter;
    }
}
