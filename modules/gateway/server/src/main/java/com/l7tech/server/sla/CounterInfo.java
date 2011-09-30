package com.l7tech.server.sla;

import java.io.Serializable;
import java.util.Date;

/**
 * Can be used to hold a snapshot of the current state of a counter.
 */
public final class CounterInfo implements Serializable {
    final long oid;
    final String name;
    final long sec;
    final long min;
    final long hr;
    final long day;
    final long mnt;
    final Date lastUpdate;

    public CounterInfo(long oid, String name, long sec, long min, long hr, long day, long mnt, Date lastUpdate) {
        this.oid = oid;
        this.name = name;
        this.sec = sec;
        this.min = min;
        this.hr = hr;
        this.day = day;
        this.mnt = mnt;
        this.lastUpdate = lastUpdate;
    }

    public long getOid() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public long getSec() {
        return sec;
    }

    public long getMin() {
        return min;
    }

    public long getHr() {
        return hr;
    }

    public long getDay() {
        return day;
    }

    public long getMnt() {
        return mnt;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }
}
