/*
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.logging;

/**
 * Message statistics for a single published service.
 */

public class StatisticsRecord {

    private final String serviceName;
    private final long attemptedCount;
    private final long authorizedCount;
    private final long completedCount;
//    private final long completedCountPerMinute;
    private final long completedCountLastMinute;

    public StatisticsRecord(String serviceName, long attemptedCount, long authorizedCount, long completedCount, long completedCountLastMinute){
        this.serviceName = serviceName;
        this.attemptedCount = attemptedCount;
        this.authorizedCount = authorizedCount;
        this.completedCount = completedCount;
//        this.completedCountPerMinute = completedCountPerMinute;
        this.completedCountLastMinute = completedCountLastMinute;
    };

    public String getServiceName(){
        return serviceName;
    }

    public long getAttemptedCount(){
        return attemptedCount;
    }

    public long getAuthorizedCount(){
        return authorizedCount;
    }

     public long getCompletedCount(){
        return completedCount;
    }

/*     public long getCompletedCountPerMinute(){
        return completedCountPerMinute;
    }*/

     public long getCompletedCountLastMinute(){
        return completedCountLastMinute;
    }

    public long getNumRoutingFailure() {
        return authorizedCount - completedCount;
    }

    public long getNumPolicyViolation() {
        return attemptedCount - authorizedCount;
    }

    public long getNumSuccess() {
        return completedCount;
    }

    public long getNumSuccessLastMinute() {
        return completedCountLastMinute;
    }
}
