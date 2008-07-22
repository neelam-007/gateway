package com.l7tech.server;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;

/**
 * Records transaction status
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 19, 2007<br/>
 */
public interface TrafficMonitor {
    void recordTransactionStatus(PolicyEnforcementContext context, AssertionStatus status, long processingTime);
}
