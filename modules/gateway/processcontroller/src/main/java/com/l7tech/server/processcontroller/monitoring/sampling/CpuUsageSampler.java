/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.common.io.ProcResult;
import static com.l7tech.common.io.ProcUtils.args;
import static com.l7tech.common.io.ProcUtils.exec;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/**
 *
 */
class CpuUsageSampler extends HostPropertySampler<Integer> {
    public CpuUsageSampler(String componentId) {
        super(componentId, BuiltinMonitorables.CPU_USAGE.getName());
    }

    public Integer sample() throws PropertySamplingException {
        try {
            ProcResult result = exec(new File("/usr/bin/vmstat"), args("1", "2"));
            String out = new String(result.getOutput());
            return parseVmstatOutput(out);
        } catch (NumberFormatException nfe) {
            throw new PropertySamplingException("Unable to parse vmstat output: 'id' value was not an integer", nfe, false);
        } catch (IOException e) {
            throw new PropertySamplingException(e.getMessage(), e, false);
        }
    }

    // If I could get multiline matches to work properly, this class would be a whole lot shorter 
    static Integer parseVmstatOutput(String out) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(out));
        String line;
        while (null != (line = reader.readLine())) {
            if (line.matches("^\\s+r\\s+b\\s+.*\\s+id\\s+.*$")) {
                // Throw away first line -- it is percentages since boot
                String valueLine = reader.readLine();
                if (valueLine == null)
                    throw new IOException("Unable to parse vmstat output: couldn't find data line");

                // Second line is percentages during the 1 second sampling interval
                valueLine = reader.readLine();
                if (valueLine == null)
                    throw new IOException("Unable to parse vmstat output: couldn't find data line");

                String[] headers = line.split("\\s+");
                String[] values = valueLine.split("\\s+");
                String idleStr = findValueForHeader(headers, values, "id");
                if (idleStr == null)
                    throw new IOException("Unable to parse vmstat output: couldn't find value for 'id'", null);
                return 100 - Integer.parseInt(idleStr);
            }
        }

        throw new IOException("Unable to parse vmstat output: couldn't find headers", null);
    }

    static <T> T findValueForHeader(T[] headers, T[] values, T desiredHeader) {
        for (int i = 0; i < headers.length; i++) {
            T header = headers[i];
            if (desiredHeader.equals(header)) {
                if (i >= values.length)
                    return null;
                return values[i];
            }
        }
        return null;
    }
}
