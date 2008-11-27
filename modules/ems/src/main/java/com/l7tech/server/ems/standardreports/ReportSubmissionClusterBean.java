/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 26, 2008
 * Time: 10:48:11 AM
 */
package com.l7tech.server.ems.standardreports;

import com.l7tech.server.management.api.node.ReportApi;

/**
 * A bean which provides access to the ReportApi.ReportSubmission object for a cluster
 */
public class ReportSubmissionClusterBean {

    private String clusterId;
    private ReportApi.ReportSubmission reportSubmission;

    public ReportSubmissionClusterBean(String clusterId, ReportApi.ReportSubmission reportSubmission) {
        this.clusterId = clusterId;
        this.reportSubmission = reportSubmission;
    }

    public String getClusterId() {
        return clusterId;
    }

    public ReportApi.ReportSubmission getReportSubmission() {
        return reportSubmission;
    }
}
