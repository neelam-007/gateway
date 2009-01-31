package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 */
public class DiskFreeSampler extends HostPropertySampler<Long> {
    private static final Pattern DF_MATCHER = Pattern.compile("(?m)\\d+\\s+\\d+\\s+(\\d+)\\s*%\\s+/\\s*$");
    private static final String DF_PATH = "/bin/df";

    public DiskFreeSampler(String componentId) {
        super(componentId, "diskFree");
    }

    Long sample() throws PropertySamplingException {
        try {
            ProcResult result = ProcUtils.exec(new File(DF_PATH));
            return matchNumber(new String(result.getOutput()), DF_PATH, DF_MATCHER);
        } catch (IOException e) {
            throw new PropertySamplingException(e);
        }
    }
}
