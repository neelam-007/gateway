package com.l7tech.server.audit;

import java.util.logging.Logger;

/**
 * Extension of auditor for use when there is no audit context available.
 *
 * @author Steve Jones
 */
public class LogOnlyAuditor extends Auditor {

    /**
     * Create an Auditor that will send detail messages to the log file but will never send any audit
     * detail events.
     *
     * @param logger the Logger to use as a sink for detail messages.  Required.
     */
    public LogOnlyAuditor(Logger logger) {
        super(logger);
    }
}
