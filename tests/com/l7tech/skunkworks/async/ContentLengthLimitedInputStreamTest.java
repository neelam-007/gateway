package com.l7tech.skunkworks.async;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;

import com.l7tech.skunkworks.async.ContentLengthLimitedInputStream;

public class ContentLengthLimitedInputStreamTest extends TestCase {
    private static Logger log = Logger.getLogger(ContentLengthLimitedInputStreamTest.class.getName());

    public ContentLengthLimitedInputStreamTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ContentLengthLimitedInputStreamTest.class);
    }

    public void testStuff() throws Exception {
        String data = "abcdefghijklmnopqrstuvwxyz";
        byte[] bytes = data.getBytes();

        for (int i = 1; i <= data.length()+1; i++) {
            PushbackInputStream pbis = new PushbackInputStream(new ByteArrayInputStream(bytes));
            ContentLengthLimitedInputStream cllis = new ContentLengthLimitedInputStream(pbis, 'm'-'a'+1);

            byte[] buf = new byte[i];
            int num;

            StringBuffer got = new StringBuffer();
            while ( (num = cllis.read(buf)) != -1) {
                got.append(new String(buf, 0, num));
            }
            assertEquals(13, got.length());
            assertEquals("abcdefghijklm", got.toString());

            got = new StringBuffer();
            while ( (num = pbis.read(buf)) != -1) {
                got.append(new String(buf, 0, num));
            }
            assertEquals(13, got.length());
            assertEquals("nopqrstuvwxyz", got.toString());
        }

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
