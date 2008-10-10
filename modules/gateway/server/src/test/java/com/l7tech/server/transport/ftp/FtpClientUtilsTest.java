package com.l7tech.server.transport.ftp;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.*;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.jscape.inet.ftp.FtpException;

/**
 * @author jbufu
 */
public class FtpClientUtilsTest extends TestCase {
    public FtpClientUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FtpClientUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testUpload() throws IOException, FtpException {
/*  uncomment with custom config
        InputStream is = new FileInputStream("/tmp/rfc3261.txt");
        FtpClientConfig config = FtpClientUtils.newConfig("localhost").setDirectory("incoming");
        OutputStream os = FtpClientUtils.getUploadOutputStream(config, "rfc.txt");
        byte[] b = new byte[8192];
        int r;
        while ((r = is.read(b)) > 0) {
            os.write(b, 0, r);
        }
*/
    }

}
