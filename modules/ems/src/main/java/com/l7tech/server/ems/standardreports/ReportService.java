package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.server.management.api.node.ReportApi;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@Transactional(rollbackFor=Throwable.class)
public interface ReportService {

    /**
     * Add a ReportSubmission to the processing queue for the given cluster.
     *
     * @param clusterId The target cluster for the report.
     * @param user The user submitting the report.
     * @param reportSubmission The report to be generated.
     */
    void enqueueReport( String clusterId,
                               User user,
                               ReportApi.ReportSubmission reportSubmission ) throws ReportException;
}
