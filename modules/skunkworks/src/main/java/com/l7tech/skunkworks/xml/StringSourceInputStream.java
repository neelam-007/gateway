package com.l7tech.skunkworks.xml;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Provides an {@link InputStream} view of a {@link StringSource}.
 */
class StringSourceInputStream extends InputStream {
    private final StringSource source;
    private final String charsetName;
    private byte[] cur;
    private int pos;


    public StringSourceInputStream(StringSource source) {
        this.source = source;
        this.charsetName = null;
        try {
            next();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public StringSourceInputStream(StringSource source, String charsetName) throws UnsupportedEncodingException {
        this.source = source;
        this.charsetName = charsetName;
        next();
    }

    private void next() throws UnsupportedEncodingException {
        String s = source.next();
        if (s == null) {
            cur = null;
            return;
        }

        cur = charsetName == null ? s.getBytes() : s.getBytes(charsetName);
        pos = 0;
    }

    public int read() throws IOException {
        if (cur == null)
            return -1;

        while (pos >= cur.length) {
            next();
            if (cur == null)
                return -1;
        }

        return cur[pos++];
    }
}
