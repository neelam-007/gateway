package com.l7tech.server.ssh.integration;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.l7tech.server.ssh.SshSession;
import com.l7tech.server.ssh.SshSessionFactory;
import com.l7tech.server.ssh.SshSessionKey;
import com.l7tech.server.ssh.client.FileTransferException;
import com.l7tech.server.ssh.client.ScpClient;
import com.l7tech.util.Either;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

/**
 * This was created: 4/16/13 as 9:37 AM
 *
 * @author Victor Kazakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ScpIntegrationTests {
    protected static final Logger logger = Logger.getLogger(ScpIntegrationTests.class.getName());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final int connectionTimeout = 30 * 1000;

    private final String host = "10.7.49.204";
    private final int port = 22;
    private final int socketTimeout = 31 * 1000;
    private final String userName = "layer7";
    private final String password = "7layer";
    private final String fingerprint = "c9:3c:31:f0:55:cc:46:11:9e:4a:cc:a9:d0:56:8e:da";
    private final String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEowIBAAKCAQEAoGHSVk1aMAWJb1dMd9+Ea5v9Nmh5RoFlhoJMwyIkGfkBDCnQ\n" +
            "FuJ+dDDcZq4cgcJgbdCg3gyFokn3m/bd4Ci4ShAt/wQyCDJdZXI2DWaXgXGOT0l+\n" +
            "Duw3i3Ielx9gfFQrwIfxChZvzyVQbQfaRVeq2mEU/5qR5+hU8O1dQWxiMwHTmWLw\n" +
            "WnhK3JOLVHJjDuYqcropEUJ2TEetAN5a4+kKmNgVOIsPDMW9iSM7TR0Wb+U3oPSp\n" +
            "0FuKK8nn8cSU1mbv5xJ9t/hWcHSaEb471BQZrX4wbCgPLN3AfFXdg0BTWqodgAio\n" +
            "GonGkX0Iqf4H2C1ZlAz7JkC4KtFXoEai7xU2CQIDAQABAoIBABsCfmbUXhTqScv7\n" +
            "esBRXz60JX+tNodeMVfmx8CxFj1dhwSiYg4IlN1CgUu1lM1xi8XStdTPbQ34wP9+\n" +
            "Ir2mferTMLva3AYvXTVBd8Ln/SnqktndeaJPu8b7MBWpThmCH2AsRLa7MpdDJHlI\n" +
            "MkUtT3O5l8y4Hkxc/zkX8HWdKCiIqN3mD/rKUjP/9WSQgLyPLGNC3cgc/1iBgLu6\n" +
            "L/brFWNsLU3xtgsj2Hdtc3ki5jibB/1Rv4J8D4vCIu1PnF0uypJ6Y6bEeIEjqx1t\n" +
            "P7amUz0aJeIjMZPWVwqC+A3aDvOUIN5gfmhGUnrD02jy3zFkLcnjY/Azmd4I9eyk\n" +
            "LNY+RHUCgYEA1dV02/do/ABvZidkFQuz6x3M+A+TKvAAsIswpZKaPAOw4sooaeXB\n" +
            "8R5SU4VL5/QMSExz3u+jjVXTRFfMJ3Wwzb96L6mntJa3Vi79H1b/XMB8puEiTqto\n" +
            "xaOJ7keBIs0XI9uQU/mz+GEyjgvE4AoZzvE90PbkxA7Vd4YvyU6XAuMCgYEAwAIR\n" +
            "/OTllLcGoeeu1A2i0AKgNQr9ro10HhYwEda+8j6N3PS0nZI5LbxkzBKKtS/bDhQB\n" +
            "M84mUvwyGCrsxJJMUyiprLurXRZX1C2GkIi5TpoTcaVYv88jUGpjuxKYyKYujg2+\n" +
            "DpKcixuGS0hrZFZKTNINfjSohIcrrj4Vy5WcuyMCgYEAimyRJqhFzQSXGMEdN8aI\n" +
            "pQZ4noiuk1gkiiTAzB5YO8uD4XxOpyb14pnnX2vl1HUW0mKzi7kD3kWV6yMO5Oh6\n" +
            "GOcAOQSRrQT4EBGBCJ2v16I/z2Pkaprpyy9v5dFiFl4/wZWsG8CEsaByMqQJwaKo\n" +
            "TF+8zZ5WP1j+9U3nd2Cpv1MCgYA6DCYuA3FY3WBW8DkNiLXRFwoiHxmEfOU+90w/\n" +
            "CGIf0EOJKj20aXF53zvF6lEe58Hc5rVaWReC/RWT8t4zq1sBxQhO3Um4Ubnpvn2x\n" +
            "EnSbS79XGzrSeifd9twBwWSJPJxUe9kahL5o28Bk4Bb/dYru4I4NupO7xTWybrie\n" +
            "HLQOawKBgAYGNxKRWnxC5VjTXi0kUQth2KeLYnsBpXUxlLiorN6cLoL9dAxl83T1\n" +
            "0Jo4LHwGolLMvWue6/8UCui/0f55Du9wbRRQg32mQJnts8mnXyjGwaZkfG82NrZg\n" +
            "VmD+NlifAqIsLRK9Tl2prH77Bw7Mwv3Lfr/93ufBWbLugCPRu7LG\n" +
            "-----END RSA PRIVATE KEY-----\n";
    private static final String defaultCipherOrder = "aes128-ctr, aes128-cbc, 3des-cbc, blowfish-cbc, aes192-ctr, aes192-cbc, aes256-ctr, aes256-cbc";
    private static final String defaultMacOrder = "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96";
    private static final String defaultCompressionOrder = "none";

    private static final List<String> ciphers = grep(map(list(defaultCipherOrder.split("\\s*,\\s*")), trim()), isNotEmpty());
    private static final List<String> macs = grep(map(list(defaultMacOrder.split("\\s*,\\s*")), trim()), isNotEmpty());
    private static final List<String> compressions = grep(map(list(defaultCompressionOrder.split("\\s*,\\s*")), trim()), isNotEmpty());

    //@Inject
    SshSessionFactory sessionFactory = new SshSessionFactory();
//    @Inject
//    private ServerConfigStub config;

    @BeforeClass
    public static void beforeClass() {
        JSch.setLogger(new TestJSCHLogger());
    }

    @Test
    public void putSimpleTest() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>left(password), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "", "myFile.txt", fileContents.length(), 0, false, null, null);
            }
        }
    }

    /**
     * Note that the ssh server need to have aes128-ctr turned of for this test to work properly.
     */
    @Test
    public void cipherFailTest() throws JSchException, IOException, SftpException, FileTransferException {
        List<String> ciphers = grep(map(list("aes128-ctr".split("\\s*,\\s*")), trim()), isNotEmpty());

        exception.expect(JSchException.class);
        exception.expectMessage("Algorithm negotiation fail");

        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>left(password), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
        }
    }

    @Test
    public void getSimpleTest() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>left(password), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents:getSimpleTest\n";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "", "myFile.txt", fileContents.length(), 0, false, null, null);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "", "myFile.txt", -1, 0, null);

                String returned = bout.toString();
                Assert.assertEquals(fileContents, returned);
            }
        }
    }

    @Test
    public void putGetEmptyFile() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>left(password), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "", "myEmptyFile.txt", fileContents.length(), 0, false, null, null);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "", "myEmptyFile.txt", -1, 0, null);

                String returned = bout.toString();
                Assert.assertEquals(fileContents, returned);
            }
        }
    }

    @Test
    public void putGetNoFingerPrintCheck() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>left(password), connectionTimeout, socketTimeout, null, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents no fingerprint check";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "", "myFile.txt", fileContents.length(), 0, false, null, null);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "", "myFile.txt", -1, 0, null);

                String returned = bout.toString();
                Assert.assertEquals(fileContents, returned);
            }
        }
    }

    @Test
    public void badFingerPrintCheck() throws JSchException, IOException, SftpException, FileTransferException {
        exception.expect(JSchException.class);
        exception.expectMessage("reject HostKey: " + host);
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>left(password), connectionTimeout, socketTimeout, "bad fingerPrint", ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            session.getScpClient();
        }
    }

    @Test
    public void putGetPrivateKey() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents private key check\n";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "", "myFile.txt", fileContents.length(), 0, false, null, null);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "", "myFile.txt", -1, 0, null);

                String returned = bout.toString();
                Assert.assertEquals(fileContents, returned);
            }
        }
    }

    @Test
    public void putGetToDirectory() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents in a directory\n";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "mydir/", "myFile.txt", fileContents.length(), 0, false, null, null);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "mydir/", "myFile.txt", -1, 0, null);

                String returned = bout.toString();
                Assert.assertEquals(fileContents, returned);
            }
        }
    }

    @Test
    public void putToNoExistingDirectory() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents in a directory\n";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                exception.expect(FileTransferException.class);
                exception.expectMessage("myBadDir/myFile.txt: No such file or directory");
                scpClient.upload(is, "myBadDir/", "myFile.txt", fileContents.length(), 0, false, null, null);
            }
        }
    }

    @Test
    public void getFromNoExistingDirectory() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                exception.expect(FileTransferException.class);
                exception.expectMessage("myBadDir/myFile.txt: No such file or directory");
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "myBadDir/", "myFile.txt", -1, 0, null);
            }
        }
    }

    /**
     * This will actually succeed. It will upload a file called mydir to the mydir directory.
     * There will likely not be any workaround for this and it should be documented.
     *
     * @throws JSchException
     * @throws IOException
     * @throws SftpException
     * @throws FileTransferException
     */
    @Test
    public void putFileIsDirectory() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                final String fileContents = "My File Contents in a directory\n";
                InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                scpClient.upload(is, "", "mydir", fileContents.length(), 0, false, null, null);
            }
        }
    }

    @Test
    public void getFileIsDirectory() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), connectionTimeout, socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            try (ScpClient scpClient = session.getScpClient()) {
                scpClient.connect();

                exception.expect(FileTransferException.class);
                exception.expectMessage("mydir: not a regular file");
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                scpClient.download(bout, "", "mydir", -1, 0, null);
            }
        }
    }

    private static class TestJSCHLogger implements com.jcraft.jsch.Logger {
        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            logger.log(Level.INFO, level + ": " + message);
        }
    }
}
