package com.l7tech.xml.xslt;

import java.io.IOException;

/**
 * Represents the output of an XSL transformation using {@link CompiledStylesheet}.
 */
public class TransformOutput {
    private byte[] bytes;

    /**
     * Report the bytes that compose the result of a successful transformation.
     *
     * @param bytes the bytes that were emitted by the transformation.  Required.
     * @throws IOException if there is a problem sending these bytes wherever they needed to go.
     */
    public void setBytes(byte[] bytes) throws IOException {
        this.bytes = bytes;
    }

    /**
     * Obtain the bytes that were last set with {@link #setBytes}.
     *
     * @return the output bytes, or null if none have yet been set.
     */
    public byte[] getBytes() {
        return bytes;
    }
}
