/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.common.io.ProcResult;
import static com.l7tech.common.io.ProcUtils.args;
import static com.l7tech.common.io.ProcUtils.exec;
import com.l7tech.server.management.api.monitoring.NtpStatus;

import java.io.File;
import java.io.IOException;

public class NtpStatusSampler extends HostPropertySampler<NtpStatus> {
    private static final String PATH_NTPSTAT = "/usr/bin/ntpstat";

    public NtpStatusSampler(String componentId) {
        super(componentId, "ntpStatus");
    }

    public NtpStatus sample() throws PropertySamplingException {
        try {
            ProcResult result = exec(new File(PATH_NTPSTAT), args(), null, true);
            switch (result.getExitStatus()) {
                case 0:
                    return NtpStatus.OK;
                case 1:
                    return NtpStatus.UNSYNCHRONIZED;
                default:
                    return NtpStatus.UNKNOWN;
            }
        } catch (IOException e) {
            throw new PropertySamplingException(e);
        }
    }
}
