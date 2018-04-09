package com.l7tech.external.assertions.pbsmel.server;

import com.l7tech.message.Message;
import com.l7tech.objectmodel.polback.PolicyBacked;
import com.l7tech.objectmodel.polback.PolicyBackedMethod;
import com.l7tech.objectmodel.polback.PolicyParam;

/**
 * A policy-backed service interface that is used to specify a policy that will process service metrics
 * asynchronously.
 */
@PolicyBacked
public interface ServiceMetricsProcessor {

    /**
     * Process service metrics.
     *
     * @param metrics service metrics
     */
    @PolicyBackedMethod
    void process( @PolicyParam("metrics") Message metrics);
}
