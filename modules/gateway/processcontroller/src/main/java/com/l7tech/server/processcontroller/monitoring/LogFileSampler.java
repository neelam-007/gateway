package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 */
public class LogFileSampler extends HostPropertySampler<Long> {
    private static final String SH_PATH = "/bin/sh";
    private static final String DU_COMMAND = "du -k -c /opt/SecureSpan/Gateway/node/*/var/logs | tail -n1 | cut -f1";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*$");

    public LogFileSampler(String componentId, String propertyName) {
        super(componentId, propertyName);
    }

    Long sample() throws PropertySamplingException {
        try {
            ProcResult result = ProcUtils.exec(new File(SH_PATH), ProcUtils.args("-c", DU_COMMAND));
            return matchNumber(new String(result.getOutput()), "du output", NUMBER_PATTERN) * 1024L;
        } catch (IOException e) {
            throw new PropertySamplingException(e);
        }
    }
}
