/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 27, 2008
 * Time: 11:00:46 AM
 */
package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.server.management.api.node.ReportApi;

public class MockReportService implements ReportService {
    public void enqueueReport(String clusterId, User user, ReportApi.ReportSubmission reportSubmission) throws ReportException {
    }
}
