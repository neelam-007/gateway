/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 */
class LogFileSampler extends HostPropertySampler<Long> {
    private static final String SH_PATH = "/bin/sh";
    private static final String DU_COMMAND = "du -k -c /opt/SecureSpan/Gateway/node/*/var/logs | tail -n1 | cut -f1";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*$");

    public LogFileSampler(String componentId) {
        super(componentId, BuiltinMonitorables.LOG_SIZE.getName());
    }

    public Long sample() throws PropertySamplingException {
        try {
            ProcResult result = ProcUtils.exec(new File(SH_PATH), ProcUtils.args("-c", DU_COMMAND));
            return matchNumber(new String(result.getOutput()), "du output", NUMBER_PATTERN);
        } catch (IOException e) {
            throw new PropertySamplingException(e, false);
        }
    }
}
