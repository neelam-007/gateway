package com.l7tech.common.io;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A transparent stream that counts the bytes passing through.
 *
 * @author jbufu
 */
public class ByteCountOutputStream extends FilterOutputStream {

    private long byteCount = 0;

    public ByteCountOutputStream(OutputStream stream) {
	    super(stream);
    }

    public long getByteCount() {
	    return byteCount;
    }

    public void write(int b) throws IOException {
        byteCount++;
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        byteCount += len;
        out.write(b, off, len);
    }

    public void reset() {
        byteCount = 0;
    }
}





