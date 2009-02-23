/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import static com.l7tech.common.io.ProcUtils.args;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 */
class DiskFreeSampler extends HostPropertySampler<Long> {
    static final Pattern DF_MATCHER = Pattern.compile("(?m)^\\S+\\s+\\d+\\s+\\d+\\s+(\\d+)\\s+\\d+\\s*%\\s+/\\s*$");
    static final String DF_PATH = "/bin/df";

    public DiskFreeSampler(String componentId) {
        super(componentId, BuiltinMonitorables.DISK_FREE_KIB.getName());
    }

    public Long sample() throws PropertySamplingException {
        try {
            ProcResult result = ProcUtils.exec(new File(DF_PATH), args("/"));
            return matchNumber(new String(result.getOutput()) + "\n", "output from " + DF_PATH, DF_MATCHER);
        } catch (IOException e) {
            throw new PropertySamplingException(e, false);
        }
    }
}
