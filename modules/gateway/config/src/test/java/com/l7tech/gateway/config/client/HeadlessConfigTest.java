package com.l7tech.gateway.config.client;


import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.HeadlessConfigBean;
import com.l7tech.gateway.config.client.beans.PropertiesAccessor;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import junit.framework.Assert;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class HeadlessConfigTest {
    @Mock
    HeadlessConfigBean headlessConfigBean;
    private InputStream oldIn;
    private PipedInputStream pipedInputStream;
    private PipedOutputStream configStream;
    private PrintWriter printWriter;
    private PrintStream oldOut;
    private PipedInputStream outPipedInputStream;
    private PipedOutputStream outConfigStream;
    private PrintStream outPrintStream;
    private StringWriter outputWriter;
    private CountDownLatch outputWriteLatch;

    @Before
    public void before() throws IOException, ConfigurationException {
        oldIn = System.in;
        pipedInputStream = new PipedInputStream();
        configStream = new PipedOutputStream(pipedInputStream);

        printWriter = new PrintWriter(configStream);

        System.setIn(pipedInputStream);

        oldOut = System.out;
        outPipedInputStream = new PipedInputStream();
        outConfigStream = new PipedOutputStream(outPipedInputStream);
        outPrintStream = new PrintStream(outConfigStream);
        System.setOut(outPrintStream);

        outputWriteLatch = new CountDownLatch(1);
        outputWriter = new StringWriter();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtils.copyStream(new InputStreamReader(outPipedInputStream), outputWriter);
                } catch (IOException e) {
                    fail("Unexpected message reading output stream");
                } finally {
                    outputWriteLatch.countDown();
                }
            }
        });
    }

    @After
    public void after() throws IOException {
        System.setIn(oldIn);

        printWriter.close();
        configStream.close();
        pipedInputStream.close();

        System.setOut(oldOut);

        outPrintStream.close();
        outConfigStream.close();
        outPipedInputStream.close();
    }

    @Test
    public void successPath() throws ConfigurationException {
        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        int databasePort = 1234;
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = "databaseHost";
        final String clusterPass = "clusterPass";
        printWriter.println(
                "#Headless config create-db answers file\n" +
                "#Fri Dec 19 16:32:59 PST 2014\n" +
                "cluster.host=" + clusterHost + "\n" +
                "#database.failover.port=" + databaseFailoverPort + "\n" +
                "database.name=" + databaseName + "\n" +
                "admin.pass=" + pmAdminUserPassword + "\n" +
                "database.admin.user=" + databaseAdminUser + "\n" +
                "database.port=" + databasePort + "\n" +
                "database.user=" + databaseUser + "\n" +
                "node.enable=" + nodeEnabled + "\n" +
                "admin.user=" + pmAdminUser + "\n" +
                "database.admin.pass=" + databaseAdminPass + "\n" +
                "#database.failover.host=" + databaseFailoverHost + "\n" +
                "database.pass=" + databasePass + "\n" +
                "database.host=" + databaseHost + "\n" +
                "cluster.pass=" + clusterPass + "\n");
        printWriter.flush();
        printWriter.close();

        HeadlessConfig.doHeadlessConfig(headlessConfigBean, new String[]{"create"});

        final Properties properties = new Properties();
        properties.setProperty("cluster.host", clusterHost);
        properties.setProperty("database.name", databaseName);
        properties.setProperty("admin.pass", pmAdminUserPassword);
        properties.setProperty("database.admin.user", databaseAdminUser);
        properties.setProperty("database.port", String.valueOf(databasePort));
        properties.setProperty("database.user", databaseUser);
        properties.setProperty("node.enable", String.valueOf(nodeEnabled));
        properties.setProperty("admin.user", pmAdminUser);
        properties.setProperty("database.admin.pass", databaseAdminPass);
        properties.setProperty("database.pass", databasePass);
        properties.setProperty("database.host", databaseHost);
        properties.setProperty("cluster.pass", clusterPass);
        Mockito.verify(headlessConfigBean).configure(Mockito.eq("create"), Mockito.isNull(String.class), Mockito.argThat(new CustomMatcher<PropertiesAccessor>("PropertiesAccessor Matcher") {
            @Override
            public boolean matches(Object o) {
                try {
                    return o instanceof PropertiesAccessor && properties.equals(((PropertiesAccessor) o).getProperties());
                } catch (ConfigurationException e) {
                    return false;
                }
            }
        }));
    }

    @Test
    public void successPathWithSubCommand() throws ConfigurationException {
        final String clusterHost = "test.cluster.hostname";
        String pmAdminUser = "pmAdminUser";
        String pmAdminUserPassword = "pmAdminUserPassword";
        String databaseFailoverPort = null;
        String databaseName = "databaseName";
        String databaseAdminUser = "databaseAdminUser";
        int databasePort = 1234;
        String databaseUser = "databaseUser";
        final Boolean nodeEnabled = true;
        String databaseAdminPass = "databaseAdminPass";
        String databaseFailoverHost = null;
        String databasePass = "databasePass";
        String databaseHost = "databaseHost";
        final String clusterPass = "clusterPass";
        printWriter.println(
                "#Headless config create-db answers file\n" +
                        "#Fri Dec 19 16:32:59 PST 2014\n" +
                        "cluster.host=" + clusterHost + "\n" +
                        "#database.failover.port=" + databaseFailoverPort + "\n" +
                        "database.name=" + databaseName + "\n" +
                        "admin.pass=" + pmAdminUserPassword + "\n" +
                        "database.admin.user=" + databaseAdminUser + "\n" +
                        "database.port=" + databasePort + "\n" +
                        "database.user=" + databaseUser + "\n" +
                        "node.enable=" + nodeEnabled + "\n" +
                        "admin.user=" + pmAdminUser + "\n" +
                        "database.admin.pass=" + databaseAdminPass + "\n" +
                        "#database.failover.host=" + databaseFailoverHost + "\n" +
                        "database.pass=" + databasePass + "\n" +
                        "database.host=" + databaseHost + "\n" +
                        "cluster.pass=" + clusterPass + "\n");
        printWriter.flush();
        printWriter.close();

        HeadlessConfig.doHeadlessConfig(headlessConfigBean, new String[]{"create", "help"});

        final Properties properties = new Properties();
        properties.setProperty("cluster.host", clusterHost);
        properties.setProperty("database.name", databaseName);
        properties.setProperty("admin.pass", pmAdminUserPassword);
        properties.setProperty("database.admin.user", databaseAdminUser);
        properties.setProperty("database.port", String.valueOf(databasePort));
        properties.setProperty("database.user", databaseUser);
        properties.setProperty("node.enable", String.valueOf(nodeEnabled));
        properties.setProperty("admin.user", pmAdminUser);
        properties.setProperty("database.admin.pass", databaseAdminPass);
        properties.setProperty("database.pass", databasePass);
        properties.setProperty("database.host", databaseHost);
        properties.setProperty("cluster.pass", clusterPass);
        Mockito.verify(headlessConfigBean).configure(Mockito.eq("create"), Mockito.eq("help"), Mockito.argThat(new CustomMatcher<PropertiesAccessor>("PropertiesAccessor Matcher") {
            @Override
            public boolean matches(Object o) {
                try {
                    return o instanceof PropertiesAccessor && properties.equals(((PropertiesAccessor) o).getProperties());
                } catch (ConfigurationException e) {
                    return false;
                }
            }
        }));
    }

    @Test
    public void inputStreamNotClosed() throws ConfigurationException, IOException {
        SyspropUtil.setProperty("com.l7tech.gateway.config.client.headlessConfig.loadPropertiesTimeout", "100");
        try {
            final String clusterHost = "test.cluster.hostname";
            String pmAdminUser = "pmAdminUser";
            String pmAdminUserPassword = "pmAdminUserPassword";
            String databaseFailoverPort = null;
            String databaseName = "databaseName";
            String databaseAdminUser = "databaseAdminUser";
            int databasePort = 1234;
            String databaseUser = "databaseUser";
            final Boolean nodeEnabled = true;
            String databaseAdminPass = "databaseAdminPass";
            String databaseFailoverHost = null;
            String databasePass = "databasePass";
            String databaseHost = "databaseHost";
            final String clusterPass = "clusterPass";
            printWriter.println(
                    "#Headless config create-db answers file\n" +
                            "#Fri Dec 19 16:32:59 PST 2014\n" +
                            "cluster.host=" + clusterHost + "\n" +
                            "#database.failover.port=" + databaseFailoverPort + "\n" +
                            "database.name=" + databaseName + "\n" +
                            "admin.pass=" + pmAdminUserPassword + "\n" +
                            "database.admin.user=" + databaseAdminUser + "\n" +
                            "database.port=" + databasePort + "\n" +
                            "database.user=" + databaseUser + "\n" +
                            "node.enable=" + nodeEnabled + "\n" +
                            "admin.user=" + pmAdminUser + "\n" +
                            "database.admin.pass=" + databaseAdminPass + "\n" +
                            "#database.failover.host=" + databaseFailoverHost + "\n" +
                            "database.pass=" + databasePass + "\n" +
                            "database.host=" + databaseHost + "\n" +
                            "cluster.pass=" + clusterPass + "\n");
            printWriter.flush();
            //printWriter.close();

            Mockito.doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    ((PropertiesAccessor)invocationOnMock.getArguments()[2]).getProperties();
                    return null;
                }
            }).when(headlessConfigBean).configure(Mockito.eq("create"), Mockito.isNull(String.class), Mockito.<PropertiesAccessor>any());

            HeadlessConfig.doHeadlessConfig(headlessConfigBean, new String[]{"create"});

            String out = getOutputString();

            assertThat("incorrect error message: ", out, Matchers.containsString("Could not load configuration properties"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            SyspropUtil.clearProperty("com.l7tech.gateway.config.client.headlessConfig.loadPropertiesTimeout");
        }
    }

    @Test
    public void noCommand() throws ConfigurationException, IOException, InterruptedException {

        Mockito.when(headlessConfigBean.getCommands()).thenReturn(CollectionUtils.set("command"));

        HeadlessConfig.doHeadlessConfig(headlessConfigBean, new String[]{});

        Mockito.verify(headlessConfigBean, new Times(0)).configure(Mockito.anyString(), Mockito.anyString(), Mockito.<PropertiesAccessor>any());

        String out = getOutputString();
        oldOut.println(out);

        assertThat("incorrect error message: ", out, Matchers.containsString("Must specify a command"));
    }

    @Test
    public void headlessCommandException() throws ConfigurationException, IOException, InterruptedException {

        Mockito.doThrow(new ConfigurationException("Test Exception")).when(headlessConfigBean).configure(Mockito.anyString(), Mockito.anyString(), Mockito.<PropertiesAccessor>any());

        HeadlessConfig.doHeadlessConfig(headlessConfigBean, new String[]{"create"});

        String out = getOutputString();

        assertThat("incorrect error message", out, Matchers.containsString("Test Exception"));
    }

    private String getOutputString() throws IOException, InterruptedException {
        outPrintStream.flush();
        outPrintStream.close();
        outputWriteLatch.await();
        return outputWriter.toString();
    }
}
