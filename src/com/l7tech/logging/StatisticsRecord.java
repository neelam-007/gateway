package com.l7tech.logging;

/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Oct 27, 2003
 * Time: 4:28:41 PM
 * To change this template use Options | File Templates.
 */
public class StatisticsRecord {

    private final String serviceName;
    private final long attemptedCount;
    private final long authorizedCount;
    private final long completedCount;
    private final long completedCountPerMinute;
    private final long completedCountLastMinute;

    public StatisticsRecord(String serviceName, long attemptedCount, long authorizedCount, long completedCount, long completedCountPerMinute, long completedCountLastMinute){
        this.serviceName = serviceName;
        this.attemptedCount = attemptedCount;
        this.authorizedCount = authorizedCount;
        this.completedCount = completedCount;
        this.completedCountPerMinute = completedCountPerMinute;
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

     public long getCompletedCountPerMinute(){
        return completedCountPerMinute;
    }

     public long getCompletedCountLastMinute(){
        return completedCountLastMinute;
    }
}
