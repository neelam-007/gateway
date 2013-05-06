package com.l7tech.server.ssh.integration;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.l7tech.server.ssh.SshSession;
import com.l7tech.server.ssh.SshSessionFactory;
import com.l7tech.server.ssh.SshSessionKey;
import com.l7tech.server.ssh.client.FileTransferException;
import com.l7tech.server.ssh.client.SftpClient;
import com.l7tech.util.Either;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

/**
 * This was created: 4/25/13 as 4:02 PM
 *
 * @author Victor Kazakov
 */
public class SshConcurrencyPerformanceIntegrationTests {
    protected static final Logger logger = Logger.getLogger(ScpIntegrationTests.class.getName());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final int connectionTimeout = 30 * 1000;

    private final String host = "10.7.48.135";
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

    SshSessionFactory sessionFactory = new SshSessionFactory();

    @BeforeClass
    public static void beforeClass() {
        JSch.setLogger(new TestJSCHLogger());
    }

    //Test asynchronousity
    //Test one big file many small files
    @Test
    public void putGetPrivateKey() throws JSchException, IOException, SftpException, FileTransferException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), socketTimeout, fingerprint, ciphers, macs, compressions);
        try (SshSession session = sessionFactory.makeObject(sessionKey)) {
            session.connect(connectionTimeout);
            final AtomicBoolean continueBigUpload = new AtomicBoolean(true);

            try {
                final SftpClient sftpClient = session.getSftpClient();
                final PipedOutputStream bigOut = new PipedOutputStream();
                final PipedInputStream bigIn = new PipedInputStream(bigOut);
                sftpClient.connect();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sftpClient.upload(bigIn, "", "myBigFile.txt", -1, 0, false, null, null);
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (JSchException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (FileTransferException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (continueBigUpload.get()) {
                            byte[] b = new byte[36 * 1024];
                            new Random().nextBytes(b);
                            try {
                                bigOut.write(b);
                            } catch (IOException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    }
                }).start();

                for (int i = 0; i < 20; i++) {
                    //SshSession session2 = sessionFactory.create(sessionKey, connectionTimeout);
                    SftpClient sftpClient2 = session.getSftpClient();
                    sftpClient2.connect();

                    final String fileContents = "My File Contents " + i + ":getSimpleTest\n";
                    InputStream is = new ByteArrayInputStream(fileContents.getBytes());
                    sftpClient2.upload(is, "", "myFile" + i + ".txt", fileContents.length(), 0, false, null, null);

                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    sftpClient2.download(bout, "", "myFile" + i + ".txt", -1, 0, null);

                    String returned = bout.toString();
                    Assert.assertEquals(fileContents, returned);

                    // sftpClient2.close();
                }

            } finally {
                continueBigUpload.set(false);
            }
        }
    }


    //Test high concurrency on one session
    @Test
    public void highConcurrencyOneSession() throws JSchException, IOException, SftpException, FileTransferException, InterruptedException {
        SshSessionKey sessionKey = new SshSessionKey(userName, host, port, Either.<String, String>right(privateKey), socketTimeout, fingerprint, ciphers, macs, compressions);

        final int amountToUpload = 1024 * 1024 * 100;
        final int chunkSize = 32 * 1024;
        int numClients = 2;

        final ArrayList<Long> results = new ArrayList<>(numClients);
        for (int i = 0; i < numClients; i++) {
            results.add(0L);
        }
        boolean manySessions = false;

        SshSession session = null;
        if (!manySessions) {
            session = sessionFactory.makeObject(sessionKey);
            session.connect(connectionTimeout);
        }

        List<SftpClient> clients = new ArrayList<>(numClients);
        List<Thread> transferThreads = new ArrayList<>(numClients);
        List<Thread> dataThreads = new ArrayList<>(numClients);
        for (int i = 0; i < numClients; i++) {
            if (manySessions) {
                session = sessionFactory.makeObject(sessionKey);
                session.connect(connectionTimeout);
            }
            final SftpClient sftpClient = session.getSftpClient();
            sftpClient.connect();
            clients.add(sftpClient);
            final PipedOutputStream bigOut = new PipedOutputStream();
            final PipedInputStream bigIn = new PipedInputStream(bigOut);
            final int clientNumber = i;
            transferThreads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long start = new Date().getTime();
                        sftpClient.upload(bigIn, "", "concurrencyTest" + clientNumber + ".txt", -1, 0, false, null, null);
                        long end = new Date().getTime();
                        logger.info("Time for client " + clientNumber + " = " + (end - start));
                        results.set(clientNumber, (end - start));
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (JSchException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (FileTransferException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }));
            dataThreads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    int numUploaded = 0;
                    while (numUploaded < amountToUpload) {
                        byte[] b = new byte[chunkSize];
                        new Random().nextBytes(b);
                        try {
                            bigOut.write(b);
                            numUploaded += b.length;
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                    try {
                        bigOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }));
        }

        for (int i = 0; i < numClients; i++) {
            dataThreads.get(i).start();
        }
        long start = new Date().getTime();
        for (int i = 0; i < numClients; i++) {
            transferThreads.get(i).start();
        }
        for (int i = 0; i < numClients; i++) {
            transferThreads.get(i).join();
        }
        long end = new Date().getTime();
        logger.info("Time = " + (end - start));

        StringBuilder sb = new StringBuilder("Results: \n");
        for (int i = 0; i < numClients; i++) {
            sb.append(results.get(i)).append(" ");
        }
        sb.append((end - start));
        logger.info(sb.toString());

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
