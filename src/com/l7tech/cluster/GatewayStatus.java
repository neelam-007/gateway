package com.l7tech.cluster;

import java.util.Vector;
import java.util.logging.Logger;

/*
 * This class encapsulates the data to be displayed in the cluster status panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class GatewayStatus {
    // IMPORTANT NOTE:
    // 1. need to make sure that NUMBER_OF_SAMPLE_PER_MINUTE has no fraction when REFRESH_INTERVAL is changed
    // 2. REFRESH_INTERVAL must be <= 60
    public static final int REFRESH_INTERVAL = 5;
    public static final int STATUS_REFRESH_TIMER = 1000 * REFRESH_INTERVAL;
    public static final int NUMBER_OF_SAMPLE_PER_MINUTE = 60 / REFRESH_INTERVAL;
    public static final int MAX_UPDATE_FAILURE_COUNT = 2;

    private final ClusterNodeInfo clusterInfo;
    private int status;
    private int loadSharing;
    private boolean addRequestCounterInCache;
    private boolean addCompletedCounterInCache;
    private Vector requestCounterCache = new Vector();
    private Vector completedCounterCache = new Vector();
    private long secondLastUpdateTimeStamp;
    private int timeStampUpdateFailureCount;
    static Logger logger = Logger.getLogger(GatewayStatus.class.getName());
    /**
     * Constructor
     * @param clusterInfo  The node info of the gateway
     */
    public GatewayStatus(ClusterNodeInfo clusterInfo) {
        this.clusterInfo = clusterInfo;

        addRequestCounterInCache = true;
        addCompletedCounterInCache = true;

        // the second last update time stamp is -1 to indicate this is the first time the node status is retrieved
        secondLastUpdateTimeStamp = -1;
        timeStampUpdateFailureCount = 0;
    }

    /**
     * Get node name
     */
    public String getName() {
        return clusterInfo.getName();
    }

    /**
     * Get node status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get load sharing (%)
     */
    public int getLoadSharing() {
        return loadSharing;
    }

    /**
     * Get request failure (%)
     */
    public int getRequestRouted() {
        long totalRequest = getTotalCountFromCache(requestCounterCache);

        //       System.out.println("Node is: " + getName());
        //       System.out.println("totalRequest : " + totalRequest);
        if (totalRequest > 0) {
            long totalCompleted = getTotalCountFromCache(completedCounterCache);
            //           System.out.println("totalCompleted : " + totalCompleted);

            return (new Long(totalCompleted * 100 / totalRequest)).intValue();
        } else {
            //           System.out.println("totalRequest : 0");
            return 0;
        }
    }

    /**
     * Get load average for the last minute
     */
    public double getAvgLoad() {
        return clusterInfo.getAvgLoad();
    }

    /**
     * Get timestamp of when the node last booted
     */
    public long getUptime() {
        return clusterInfo.getUptime();
    }

    /**
     * Get direct ip address of this node
     */
    public String getAddress() {
        return clusterInfo.getAddress();
    }

    /**
     * the timestamp of when the avg load was last updated
     */
    public long getLastUpdateTimeStamp() {
        return clusterInfo.getLastUpdateTimeStamp();
    }

    /**
     * Set node status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Set load sharing (%)
     */
    public void setLoadSharing(int loadSharing) {
        this.loadSharing = loadSharing;
    }

    /**
     * Update the request counter
     *
     * @param newCount  The new value of the request count.
     */
    public void updateRequestCounterCache(long newCount) {

        //       System.out.println("Add new counter in requestCounterCache - flag: " + addRequestCounterInCache);

        if (addRequestCounterInCache) {
            updateCounterCache(requestCounterCache, newCount, true);

            // add a counter only the first time, reset the flag
            addRequestCounterInCache = false;
        } else {
            updateCounterCache(requestCounterCache, newCount, false);
        }

    }

    /**
     * Update the completed counter
     *
     * @param newCount  The new value of the completed count.
     */
    public void updateCompletedCounterCache(long newCount) {

        //       System.out.println("Add new counter in completedCounterCache - flag: " + addCompletedCounterInCache);

        if (addCompletedCounterInCache) {
            updateCounterCache(completedCounterCache, newCount, true);

            // add a counter only the first time, reset the flag
            addCompletedCounterInCache = false;
        } else {
            updateCounterCache(completedCounterCache, newCount, false);
        }
    }

    /**
     * Update the counter cache.
     *
     * @param cache   The cache to be updated.
     * @param newCount   The new value of the count.
     * @param newCounter   true if a new counter should be created in the cache, false otherwise.
     */
    private void updateCounterCache(Vector cache, long newCount, boolean newCounter) {

        if (newCounter) {
            if (cache.size() <= NUMBER_OF_SAMPLE_PER_MINUTE) {
                cache.add(new Long(newCount));
            } else {
                cache.remove(0);
                cache.add(new Long(newCount));
            }
        } else {
            Long count = new Long(((Long) cache.lastElement()).longValue() + newCount);

            // remove the last element
            cache.remove(cache.size() - 1);

            // add the updated count back to the end of the cache
            cache.add(count);
        }
    }

    /**
     * Get the total count from the counter cache.
     *
     * @param cache  The cache to be used.
     * @return  long  The total count.
     */
    private long getTotalCountFromCache(Vector cache) {

        long totalCount = 0;

        int index = cache.size() - 1;

        for (int i = 0; i < cache.size() - 1; i++, index--) {

            totalCount += ((Long) cache.get(index)).longValue() - ((Long) cache.get(index - 1)).longValue();
        }

        return totalCount;
    }

    /**
     * Return the request count
     *
     * @return long  The request count.
     */
    public long getRequestCount() {
        return getTotalCountFromCache(requestCounterCache);
    }

    /**
     * Set the reference to the request counter cache.
     *
     * @param cache  The reference to the request counter cache.
     */
    public void setRequestCounterCache(Vector cache) {
        requestCounterCache = cache;
    }

    /**
     * Set the reference to the completed counter cache.
     *
     * @param cache  The reference to the completed counter cache.
     */
    public void setCompletedCounterCache(Vector cache) {
        completedCounterCache = cache;
    }

    /**
     * Return the reference to the request counter cache.
     *
     * @return  Vector  The reference to the request counter cache.
     */
    public Vector getRequestCounterCache() {
        return requestCounterCache;
    }

    /**
     * Return the reference to the completed counter cache.
     *
     * @return  Vector  The reference to the completed counter cache.
     */
    public Vector getCompletedCounterCache() {
        return completedCounterCache;
    }

    /**
     * Reset the cache update flag.
     */
    public void resetCacheUpdateFlag() {
        addRequestCounterInCache = true;
        addCompletedCounterInCache = true;
    }

    /**
     * Return the second last update timestamp.
     *
     * @return long  The second last update timestamp.
     */
    public long getSecondLastUpdateTimeStamp() {
        return secondLastUpdateTimeStamp;
    }

    /**
     * Set the last update timestamp
     *
     * @param secondLastUpdateTime  The last update timestamp.
     */
    public void setSecondLastUpdateTimeStamp(long secondLastUpdateTime) {
        this.secondLastUpdateTimeStamp = secondLastUpdateTime;
    }

    /**
     * Return Node Id.
     *
     * @return String  The node Id.
     */
    public String getNodeId() {
        return clusterInfo.getMac();
    }

    /**
     * Increment the failure count of the timestamp update.
     */
    public void incrementTimeStampUpdateFailureCount() {
        timeStampUpdateFailureCount++;

        if(timeStampUpdateFailureCount >= MAX_UPDATE_FAILURE_COUNT) {
            logger.warning("The Node " + getName() + " has not updated the status table for " + timeStampUpdateFailureCount * REFRESH_INTERVAL + " seconds.");
        }
    }

    /**
     * Reset the failure count of the timestamp update.
     */
    public void resetTimeStampUpdateFailureCount() {
        timeStampUpdateFailureCount = 0;
    }

    /**
     * Return the failure count of the timestamp update.
     *
     * @return int The failure count of the timestamp update.
     */
    public int getTimeStampUpdateFailureCount() {
        return timeStampUpdateFailureCount;
    }

    /**
     * Set the failure count of the timestamp update.
     * @param count  The failure count of the timestamp update.
     */
    public void setTimeStampUpdateFailureCount(int count) {
        timeStampUpdateFailureCount = count;
    }
}
