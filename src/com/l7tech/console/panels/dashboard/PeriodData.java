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

    private final Map binsByNodeId = new HashMap();
    private final Map binsByServiceOid = new HashMap();

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

        bigBin.setMinBackendResponseTime(Math.min(bigBin.getMinBackendResponseTime(), newBin.getMinBackendResponseTime()));
        bigBin.setMaxBackendResponseTime(Math.max(bigBin.getMaxBackendResponseTime(), newBin.getMaxBackendResponseTime()));
        bigBin.setMinFrontendResponseTime(Math.min(bigBin.getMinFrontendResponseTime(), newBin.getMinFrontendResponseTime()));
        bigBin.setMaxFrontendResponseTime(Math.max(bigBin.getMaxFrontendResponseTime(), newBin.getMaxFrontendResponseTime()));

        bigBin.setStartTime(Math.min(bigBin.getStartTime(), newBin.getStartTime()));
        bigBin.setEndTime(Math.max(bigBin.getEndTime(), newBin.getEndTime()));

        bigBin.setNumAttemptedRequest(bigBin.getNumAttemptedRequest() + newBin.getNumAttemptedRequest());
        bigBin.setNumAuthorizedRequest(bigBin.getNumAuthorizedRequest() + newBin.getNumAuthorizedRequest());
        bigBin.setNumCompletedRequest(bigBin.getNumCompletedRequest() + newBin.getNumCompletedRequest());

        bigBin.setSumFrontendResponseTime(bigBin.getSumFrontendResponseTime() + newBin.getSumFrontendResponseTime());
        bigBin.setSumBackendResponseTime(bigBin.getSumBackendResponseTime() + newBin.getSumBackendResponseTime());

        {
            Set bins = (Set)binsByNodeId.get(newBin.getClusterNodeId());
            if (bins == null) {
                bins = new HashSet();
                binsByNodeId.put(newBin.getClusterNodeId(), bins);
            }
            bins.add(newBin);
        }

        {
            Long serviceOid = new Long(newBin.getServiceOid());
            Set bins = (Set)binsByServiceOid.get(serviceOid);
            if (bins == null) {
                bins = new HashSet();
                binsByServiceOid.put(serviceOid, bins);
            }
            bins.add(newBin);
        }

    }

    synchronized MetricsBin get(String nodeId, Long serviceOid) {
        Set binsToAdd;
        if (nodeId == null && serviceOid == null) {
            return bigBin;
        } else if (nodeId != null && serviceOid != null) {
            Set sbins = (Set)binsByServiceOid.get(serviceOid);
            Set nbins = (Set)binsByNodeId.get(nodeId);
            sbins.retainAll(nbins);
            if (sbins.size() != 1) logger.warning(sbins.size() + " bins for period " + periodStart);
            binsToAdd = sbins;
        } else if (nodeId != null) {
            binsToAdd = (Set)binsByNodeId.get(nodeId);
        } else {
            binsToAdd = (Set)binsByServiceOid.get(serviceOid);
        }

        if (binsToAdd == null) binsToAdd = Collections.EMPTY_SET;
        Iterator bins = binsToAdd.iterator();

        if (binsToAdd.size() == 1) return (MetricsBin)bins.next();

        MetricsBin megabin;
        int numAttempted = 0, numAuthorized = 0, numCompleted = 0;
        int backTime = 0, frontTime = 0, backMin = 0, frontMin = 0;
        int backMax = 0, frontMax = 0;
        long start = 0;
        long end = 0;

        while (bins.hasNext()) {
            MetricsBin bin = (MetricsBin)bins.next();
            if (nodeId == null || bin.getClusterNodeId().equals(nodeId) ||
                serviceOid == null || bin.getServiceOid() == serviceOid.longValue()) {
                numAttempted += bin.getNumAttemptedRequest();
                numAuthorized += bin.getNumAuthorizedRequest();
                numCompleted += bin.getNumCompletedRequest();
                backTime += bin.getSumBackendResponseTime();
                frontTime += bin.getSumFrontendResponseTime();

                backMin = backMin == 0 ? bin.getMinBackendResponseTime() : Math.min(backMin, bin.getMinBackendResponseTime());
                backMax = backMax == 0 ? bin.getMaxBackendResponseTime() : Math.max(backMax, bin.getMaxBackendResponseTime());
                frontMin = frontMin == 0 ? bin.getMinFrontendResponseTime() : Math.min(frontMin, bin.getMinFrontendResponseTime());
                frontMax = frontMax == 0 ? bin.getMaxFrontendResponseTime() : Math.max(frontMax, bin.getMaxFrontendResponseTime());
                start = start == 0 ? bin.getStartTime() : Math.min(start, bin.getStartTime());
                end = end == 0 ? bin.getEndTime() : Math.max(end, bin.getEndTime());
            }
        }

        if (backMax == Integer.MAX_VALUE) backMax = 0;
        if (frontMax == Integer.MAX_VALUE) frontMax = 0;
        if (end == Integer.MAX_VALUE) end = start;

        megabin = new MetricsBin(periodStart, interval, resolution,
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
