package com.l7tech.common.util;

/** Holds the result of invoking a subprocess using the {@link ProcUtils} utility class. */
public class ProcResult {
    private final int exitStatus;
    private final byte[] output;

    public ProcResult(int exitStatus, byte[] output) {
        this.exitStatus = exitStatus;
        this.output = output;
    }

    /** @return the exit status of the process; {@see java.lang.Process}. */
    public int getExitStatus() {
        return exitStatus;
    }

    /** @return all bytes that the process wrote to stdout while it was running.  May be empty but nevber null. */
    public byte[] getOutput() {
        return output;
    }
}
