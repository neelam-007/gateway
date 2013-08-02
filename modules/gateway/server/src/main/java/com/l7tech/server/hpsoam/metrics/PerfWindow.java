package com.l7tech.server.hpsoam.metrics;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.ArrayList;

/**
 * Maps http://openview.hp.com/xmlns/soa/1/perf:PerformanceWindow
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public class PerfWindow {
    private long windowStart;
    private long windowEnd;
    private long windowIndex;
    private ArrayList<ServicePerformance> svcPerfs = new ArrayList<ServicePerformance>();

    public PerfWindow(long start) {
        windowStart = start;
        windowEnd = windowStart + 30000;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(long windowStart) {
        this.windowStart = windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public long getWindowIndex() {
        return windowIndex;
    }

    public void setWindowIndex(long windowIndex) {
        this.windowIndex = windowIndex;
    }

    public ArrayList<ServicePerformance> getSvcPerfs() {
        return svcPerfs;
    }

    public ServicePerformance getOrMakeServicePerformance(PolicyEnforcementContext context) {
        Goid svcid = context.getService().getGoid();
        for (ServicePerformance sp : svcPerfs) {
            if (Goid.equals(sp.getServiceGOID(), svcid)) return sp;
        }
        ServicePerformance output = new ServicePerformance(context.getService());
        svcPerfs.add(output);
        return output;
    }

    public String toString() {
        StringBuffer output = new StringBuffer();
        output.append("start " + windowStart);
        output.append(" end " + windowEnd);
        output.append(" index " + windowIndex);
        for (ServicePerformance sp : svcPerfs) {
            output.append(sp.toString());
        }
        return output.toString();
    }
}
