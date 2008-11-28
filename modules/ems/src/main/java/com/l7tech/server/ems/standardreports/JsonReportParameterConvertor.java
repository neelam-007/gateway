/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 26, 2008
 * Time: 4:32:21 PM
 */
package com.l7tech.server.ems.standardreports;

import com.l7tech.server.ems.standardreports.ReportSubmissionClusterBean;
import com.l7tech.server.management.api.node.ReportApi;

import java.util.Collection;
import java.util.Map;

public interface JsonReportParameterConvertor {

    //end gateway runtime params

    //usage interval ssg runtime params

    /**
     * From the Map params, which is the client sent JSON data, convert it into n distinct ReportSubmissionClusterBean
     * objects, which are returned in a cluster. The number of beans returned, is determined by how many clusters the
     * user selected when generating the report.
     * @param params
     * @param reportRanBy
     * @return
     * @throws ReportApi.ReportException
     */
    public Collection<ReportSubmissionClusterBean> getReportSubmissions(Map params, String reportRanBy)
            throws ReportException;
}
