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
    //common
    String IS_RELATIVE = "IS_RELATIVE";
    String RELATIVE_NUM_OF_TIME_UNITS = "RELATIVE_NUM_OF_TIME_UNITS";
    String RELATIVE_TIME_UNIT = "RELATIVE_TIME_UNIT";
    String IS_ABSOLUTE = "IS_ABSOLUTE";
    String ABSOLUTE_START_TIME = "ABSOLUTE_START_TIME";
    String ABSOLUTE_END_TIME = "ABSOLUTE_END_TIME";
    String REPORT_RAN_BY = "REPORT_RAN_BY";
    String SERVICE_NAMES_LIST = "SERVICE_NAMES_LIST";
    String SERVICE_ID_TO_OPERATIONS_MAP = "SERVICE_ID_TO_OPERATIONS_MAP";
    String MAPPING_KEYS = "MAPPING_KEYS";
    String MAPPING_VALUES = "MAPPING_VALUES";
    String VALUE_EQUAL_OR_LIKE = "VALUE_EQUAL_OR_LIKE";
    String USE_USER = "USE_USER";
    String AUTHENTICATED_USERS = "AUTHENTICATED_USERS";
    String IS_CONTEXT_MAPPING = "IS_CONTEXT_MAPPING";
    String IS_DETAIL = "IS_DETAIL";
    String PRINT_CHART = "PRINT_CHART";

    //interval reports only only
    String INTERVAL_TIME_UNIT = "INTERVAL_TIME_UNIT";
    String INTERVAL_NUM_OF_TIME_UNITS = "INTERVAL_NUM_OF_TIME_UNITS";

    //need to be filled in on the SSG - just recording here for now
    String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    String DISPLAY_STRING_TO_MAPPING_GROUP = "DISPLAY_STRING_TO_MAPPING_GROUP";
    String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    String TEMPLATE_FILE_ABSOLUTE = "TEMPLATE_FILE_ABSOLUTE";
    //end gateway runtime params

    //usage interval ssg runtime params
    String SUB_INTERVAL_SUB_REPORT = "SUB_INTERVAL_SUB_REPORT";
    String SUB_REPORT = "SUB_REPORT";

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
