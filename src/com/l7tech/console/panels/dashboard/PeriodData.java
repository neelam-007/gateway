/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.service.MetricsBin;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 */
class PeriodData {
    private static final Logger logger = Logger.getLogger(PeriodData.class.getName());

    private final long periodStart;
    private final int resolution;
    private final int interval;

    private final Map<String, Set<MetricsBin>> binsByNodeId = new HashMap<String, Set<MetricsBin>>();
    private final Map<Long, Set<MetricsBin>> binsByServiceOid = new HashMap<Long, Set<MetricsBin>>();

    private final MetricsBin bigBin;

    public PeriodData(int resolution, long periodStart, int interval) {
        this.periodStart = periodStart;
        this.resolution = resolution;
        this.interval = interval;

        bigBin = new MetricsBin(periodStart, interval, resolution, null, -1);
    }

    synchronized void add(MetricsBin newBin) {
        if (newBin.getPeriodStart() != periodStart) throw new IllegalArgumentException();
        if (newBin.getResolution() != resolution) throw new IllegalArgumentException();

        bigBin.setStartTime(Math.min(bigBin.getStartTime(), newBin.getStartTime()));
        bigBin.setEndTime(Math.max(bigBin.getEndTime(), newBin.getEndTime()));

        if (bigBin.getNumAttemptedRequest() == 0) {
            bigBin.setMinFrontendResponseTime(newBin.getMinFrontendResponseTime());
            bigBin.setMaxFrontendResponseTime(newBin.getMaxFrontendResponseTime());
        } else {
            if (newBin.getNumAttemptedRequest() != 0) {
                bigBin.setMinFrontendResponseTime(Math.min(bigBin.getMinFrontendResponseTime(), newBin.getMinFrontendResponseTime()));
                bigBin.setMaxFrontendResponseTime(Math.max(bigBin.getMaxFrontendResponseTime(), newBin.getMaxFrontendResponseTime()));
            }
        }

        if (bigBin.getNumCompletedRequest() == 0) {
            bigBin.setMinBackendResponseTime(newBin.getMinBackendResponseTime());
            bigBin.setMaxBackendResponseTime(newBin.getMaxBackendResponseTime());
        } else {
            if (newBin.getNumCompletedRequest() != 0) {
                bigBin.setMinBackendResponseTime(Math.min(bigBin.getMinBackendResponseTime(), newBin.getMinBackendResponseTime()));
                bigBin.setMaxBackendResponseTime(Math.max(bigBin.getMaxBackendResponseTime(), newBin.getMaxBackendResponseTime()));
            }
        }

        bigBin.setNumAttemptedRequest(bigBin.getNumAttemptedRequest() + newBin.getNumAttemptedRequest());
        bigBin.setNumAuthorizedRequest(bigBin.getNumAuthorizedRequest() + newBin.getNumAuthorizedRequest());
        bigBin.setNumCompletedRequest(bigBin.getNumCompletedRequest() + newBin.getNumCompletedRequest());

        bigBin.setSumFrontendResponseTime(bigBin.getSumFrontendResponseTime() + newBin.getSumFrontendResponseTime());
        bigBin.setSumBackendResponseTime(bigBin.getSumBackendResponseTime() + newBin.getSumBackendResponseTime());

        {
            Set<MetricsBin> bins = binsByNodeId.get(newBin.getClusterNodeId());
            if (bins == null) {
                bins = new HashSet<MetricsBin>();
                binsByNodeId.put(newBin.getClusterNodeId(), bins);
            }
            bins.add(newBin);
        }

        {
            Long serviceOid = new Long(newBin.getServiceOid());
            Set<MetricsBin> bins = binsByServiceOid.get(serviceOid);
            if (bins == null) {
                bins = new HashSet<MetricsBin>();
                binsByServiceOid.put(serviceOid, bins);
            }
            bins.add(newBin);
        }

    }

    synchronized MetricsBin get(String nodeId, Long serviceOid) {
        Set<MetricsBin> binsToAdd = null;
        if (nodeId == null && serviceOid == null) {
            return bigBin;
        } else if (nodeId != null && serviceOid != null) {
            Set<MetricsBin> sbins = binsByServiceOid.get(serviceOid);
            if (sbins != null) {
                Set<MetricsBin> nbins = binsByNodeId.get(nodeId);
                if (nbins == null) {
                    sbins.clear();
                } else {
                    sbins.retainAll(nbins);
                }
                if (sbins.size() != 1)
                    logger.warning("Found " + sbins.size() + " bins for period " +
                                   periodStart + ". Expecting 1 only. (nodeId=" +
                                   nodeId + ", serviceOid=" + serviceOid + ")");
                binsToAdd = sbins;
            }
        } else if (nodeId != null) {
            binsToAdd = binsByNodeId.get(nodeId);
        } else {
            binsToAdd = binsByServiceOid.get(serviceOid);
        }

        if (binsToAdd == null) binsToAdd = Collections.emptySet();

        if (binsToAdd.size() == 1) return binsToAdd.iterator().next();

        int numAttempted = 0, numAuthorized = 0, numCompleted = 0;
        int backTime = 0, frontTime = 0, backMin = 0, frontMin = 0;
        int backMax = 0, frontMax = 0;
        long start = 0;
        long end = 0;

        for (MetricsBin bin : binsToAdd) {
            if (nodeId == null || bin.getClusterNodeId().equals(nodeId) ||
                serviceOid == null || bin.getServiceOid() == serviceOid.longValue()) {

                if (numAttempted == 0) {
                    frontMin = bin.getMinFrontendResponseTime();
                    frontMax = bin.getMaxFrontendResponseTime();
                } else {
                    if (bin.getNumAttemptedRequest() != 0) {
                        frontMin = Math.min(frontMin, bin.getMinFrontendResponseTime());
                        frontMax = Math.max(frontMax, bin.getMaxFrontendResponseTime());
                    }
                }

                if (numCompleted == 0) {
                    backMin = bin.getMinBackendResponseTime();
                    backMax = bin.getMaxBackendResponseTime();
                } else {
                    if (bin.getNumCompletedRequest() != 0) {
                        backMin = Math.min(backMin, bin.getMinBackendResponseTime());
                        backMax = Math.max(backMax, bin.getMaxBackendResponseTime());
                    }
                }

                numAttempted += bin.getNumAttemptedRequest();
                numAuthorized += bin.getNumAuthorizedRequest();
                numCompleted += bin.getNumCompletedRequest();
                backTime += bin.getSumBackendResponseTime();
                frontTime += bin.getSumFrontendResponseTime();

                start = start == 0 ? bin.getStartTime() : Math.min(start, bin.getStartTime());
                end = end == 0 ? bin.getEndTime() : Math.max(end, bin.getEndTime());
            }
        }

        if (backMax == Integer.MAX_VALUE) backMax = 0;
        if (frontMax == Integer.MAX_VALUE) frontMax = 0;
        if (end == Integer.MAX_VALUE) end = start;

        MetricsBin megabin = new MetricsBin(periodStart,
                                            interval,
                                            resolution,
                                            nodeId == null ? null : nodeId,
                                            serviceOid == null ? -1 : serviceOid.longValue());
        megabin.setStartTime(start);
        megabin.setSumBackendResponseTime(backTime);
        megabin.setSumFrontendResponseTime(frontTime);
        megabin.setNumAttemptedRequest(numAttempted);
        megabin.setNumAuthorizedRequest(numAuthorized);
        megabin.setNumCompletedRequest(numCompleted);
        megabin.setMinBackendResponseTime(backMin);
        megabin.setMaxBackendResponseTime(backMax);
        megabin.setMinFrontendResponseTime(frontMin);
        megabin.setMaxFrontendResponseTime(frontMax);
        megabin.setEndTime(end);
        return megabin;
    }

}
