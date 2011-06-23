package com.l7tech.server.transport.ftp;

import org.junit.Ignore;
import org.junit.Test;

import java.io.*;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.jscape.inet.ftp.FtpException;

/**
 * @author jbufu
 */
@Ignore("Developer tests")
public class FtpClientUtilsTest {

    @Test
    public void testUpload() throws IOException, FtpException {
        InputStream is = new FileInputStream("/tmp/rfc3261.txt");
        FtpClientConfig config = FtpClientUtils.newConfig("localhost").setDirectory("incoming");
        OutputStream os = FtpClientUtils.getUploadOutputStream(config, "rfc.txt");
        byte[] b = new byte[8192];
        int r;
        while ((r = is.read(b)) > 0) {
            os.write(b, 0, r);
        }
    }

}
