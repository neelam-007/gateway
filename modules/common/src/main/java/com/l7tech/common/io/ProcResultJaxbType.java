package com.l7tech.common.io;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author jbufu
 */
public class ProcResultJaxbType {

    private int exitStatus;
    private byte[] output;
    private byte[] errOutput;

    public ProcResultJaxbType() { }

    public ProcResultJaxbType(ProcResult procResult) {
        this.exitStatus = procResult.getExitStatus();
        this.output = procResult.getOutput();
        this.errOutput = procResult.getErrOutput();
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }

    public byte[] getOutput() {
        return output;
    }

    public void setOutput(byte[] output) {
        this.output = output;
    }

    public byte[] getErrOutput() {
        return errOutput;
    }

    public void setErrOutput(byte[] errOutput) {
        this.errOutput = errOutput;
    }

    public static final class Adapter extends XmlAdapter<ProcResultJaxbType,ProcResult> {
        @Override
        public ProcResult unmarshal(ProcResultJaxbType prType) throws Exception {
            return new ProcResult(prType.getExitStatus(), prType.getOutput(), prType.getErrOutput());
        }

        @Override
        public ProcResultJaxbType marshal(ProcResult procResult) throws Exception {
            return new ProcResultJaxbType(procResult);
        }
    }
}
