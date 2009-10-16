package com.l7tech.common.io;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/** Holds the result of invoking a subprocess using the {@link ProcUtils} utility class. */
@XmlJavaTypeAdapter(ProcResultJaxbType.Adapter.class)
public class ProcResult {
    private final int exitStatus;
    private final byte[] output;
    private final byte[] errOutput;

    public ProcResult(int exitStatus, byte[] output) {
        this(exitStatus, output, null);
    }

    public ProcResult(int exitStatus, byte[] output, byte[] errOutput) {
        this.exitStatus = exitStatus;
        this.output = output;
        this.errOutput = errOutput;
    }

    /** @return the exit status of the process; {@link java.lang.Process}. */
    public int getExitStatus() {
        return exitStatus;
    }

    /** @return all bytes that the process wrote to stdout while it was running.  May be empty but never null. */
    public byte[] getOutput() {
        return output;
    }

    public byte[] getErrOutput() {
        return errOutput;
    }
}
