package com.l7tech.skunkworks.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Provides an {@link InputStream} view of a {@link StringSource}.
 */
class StringSourceInputStream extends InputStream {
    private final StringSource source;
    private final Charset charset;
    private byte[] cur;
    private int pos;


    public StringSourceInputStream(StringSource source) {
        this.source = source;
        this.charset = null;
        next();
    }

    public StringSourceInputStream(StringSource source, String charset) throws UnsupportedEncodingException {
        this.source = source;
        try {
            this.charset = Charset.forName(charset);
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(charset);
        }
        next();
    }

    private void next() {
        String s = source.next();
        if (s == null) {
            cur = null;
            return;
        }

        cur = charset == null ? s.getBytes() : s.getBytes(charset);
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
