package com.l7tech.server.log.syslog;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.ByteBuffer;

import com.l7tech.common.util.ResourceUtils;

/**
 * JUnit test for syslog manager
 *
 * @author Steve Jones
 */
public class SyslogManagerTest extends TestCase {

    public static final String REGEX_STANDARD = "<([1-9][0-9]{0,2})>([A-Za-z]{3} [ 1-9][0-9] [0-9]{2}:[0-9]{2}:[0-9]{2}) ([a-zA-Z\\-_0-9]{1,1024}) ([a-zA-Z0-9\\-_]{1,1024})\\[([0-9]{1,10})\\]: ([a-zA-Z0-9 ]{0,10000})[\\n]{0,1}";
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SyslogManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SyslogManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Full client/server test for message format
     */
    public void testSender() throws Exception {
        // create client
        System.out.println(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM).format(new Date()));
        final String hostname = "test";
        final int facility = 23;
        final SyslogSeverity severity = SyslogSeverity.INFORMATIONAL;
        final String process = "SSG-default_";
        final int threadId = 1;
        final String date = "Dec  1 14:59:59";
        final long time = sdf.parse("2007-12-01 14:59:59").getTime();
        final String message = "Test message";

        String result = sendMessage(1234, facility, severity, hostname, process, threadId, time, message);
        assertNotNull("Message received", result);

        Pattern pattern = Pattern.compile(REGEX_STANDARD);
        Matcher matcher = pattern.matcher(result);

        assertTrue("Regex matches log message", matcher.matches());
        int outPriority = Integer.parseInt(matcher.group(1));
        String outDateStr = matcher.group(2);
        String outHost = matcher.group(3);
        String outProc = matcher.group(4);
        long outThread = Long.valueOf(matcher.group(5));
        String outMess = matcher.group(6);

        assertEquals("Priority", (8*facility) + severity.getSeverity(), outPriority);
        assertEquals("Date", date, outDateStr);
        assertEquals("Host", hostname, outHost);
        assertEquals("Process", process, outProc);
        assertEquals("Thread", threadId, outThread);
        assertEquals("Message", message, outMess);
    }

    /**
     * Full client/server test for message format
     */
    public void testSenderMessageTruncation() throws Exception {
        System.setProperty(SyslogManagerTest.class.getPackage().getName() + ".maxLength", "1");  // 1 is not valid, so this uses min length

        // create client
        final String hostname = "test";
        final int facility = 23;
        final SyslogSeverity severity = SyslogSeverity.INFORMATIONAL;
        final String process = "SSG-default_";
        final int threadId = 1;
        final String date = "Dec  1 14:59:59";
        final long time = sdf.parse("2007-12-01 14:59:59").getTime();
        final String message =
                " 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789" +
                " 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789" +
                " 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789" +
                " 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789" +
                " 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789 123456789";

        String result = sendMessage(1234, facility, severity, hostname, process, threadId, time, message);
        assertNotNull("Message received", result);

        Pattern pattern = Pattern.compile(REGEX_STANDARD);
        Matcher matcher = pattern.matcher(result);

        assertTrue("Regex matches log message", matcher.matches());
        int outPriority = Integer.parseInt(matcher.group(1));
        String outDateStr = matcher.group(2);
        String outHost = matcher.group(3);
        String outProc = matcher.group(4);
        long outThread = Long.valueOf(matcher.group(5));
        String outMess = matcher.group(6);

        assertEquals("Priority", (8*facility) + severity.getSeverity(), outPriority);
        assertEquals("Date", date, outDateStr);
        assertEquals("Host", hostname, outHost);
        assertEquals("Process", process, outProc);
        assertEquals("Thread", threadId, outThread);
        assertTrue("Message start", message.startsWith(outMess));
        assertFalse("Message truncated", message.length() == outMess.length());
    }

    /**
     * Test notification of connection and disconnection events.
     */
    public void testClientConnectionEvents() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress address = new VmPipeAddress(1234);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(1);
        final AtomicInteger connectEventCount = new AtomicInteger(0);

        manager.setConnectionListener(new SyslogConnectionListener(){
            public void notifyConnected(SocketAddress address) {
                System.out.println("Connected: " + address);
                connectEventCount.incrementAndGet();
            }
            public void notifyDisconnected(SocketAddress address) {
                System.out.println("Disconnected: " + address);    
                endLatch.countDown();
            }
        });

        // start syslog server
        final IoSession[] sessionHolder = new IoSession[1];
        IoHandlerAdapter handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) throws Exception {
                sessionHolder[0] = session;
                System.out.println("Session created.");
                startLatch.countDown();
            }
            public void messageReceived(IoSession iosession, Object obj) throws Exception {
                System.out.print(((ByteBuffer)obj).getString(Charset.defaultCharset().newDecoder()));
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
            }
        };
        SocketConnectorConfig config = new SocketConnectorConfig();
        config.setThreadModel(ThreadModel.MANUAL);
        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.bind(address, handler, config);

        // create client
        Syslog syslog = manager.getSyslog(SyslogProtocol.VM, address, null, null, 23, "test.l7tech.com", null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatch.await(5, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 1");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 2");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 3");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 4");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 5");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 6");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 7");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 8");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 9");
            Thread.sleep(10); // see how many messages come through ...

            // server shuts down
            acceptor.unbindAll();
            sessionHolder[0].close();

            assertEquals("Connection events", 1, connectEventCount.get());

            if ( !endLatch.await(5, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect event.");
            }
        } finally {
            ResourceUtils.closeQuietly(syslog);
        }
    }

    /**
     * Test client reconnection
     */
    public void testClientReconnection() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress address = new VmPipeAddress(1234);
        final AtomicReference<CountDownLatch> startLatchRef = new AtomicReference(new CountDownLatch(1));
        final CountDownLatch endLatch = new CountDownLatch(3);
        final AtomicInteger connectEventCount = new AtomicInteger(0);

        manager.setConnectionListener(new SyslogConnectionListener(){
            public void notifyConnected(SocketAddress address) {
                System.out.println("Connected: " + address);
                connectEventCount.incrementAndGet();
            }
            public void notifyDisconnected(SocketAddress address) {
                System.out.println("Disconnected: " + address);
                endLatch.countDown();
            }
        });

        // start syslog server
        final IoSession[] sessionHolder = new IoSession[1];
        IoHandlerAdapter handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) throws Exception {
                sessionHolder[0] = session;
                System.out.println("Session created.");
                startLatchRef.get().countDown();
            }
            public void messageReceived(IoSession iosession, Object obj) throws Exception {
                System.out.print(((ByteBuffer)obj).getString(Charset.defaultCharset().newDecoder()));
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
            }
        };
        SocketConnectorConfig config = new SocketConnectorConfig();
        config.setThreadModel(ThreadModel.MANUAL);
        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.bind(address, handler, config);

        // create client
        Syslog syslog = manager.getSyslog(SyslogProtocol.VM, address, null, null, 23, "test.l7tech.com", null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatchRef.get().await(2, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 1");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 2");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 3");

            Thread.sleep(10); // see how many messages come through ...
            startLatchRef.set(new CountDownLatch(1));
            sessionHolder[0].close();

            // wait until connected
            assertTrue("Client connected", startLatchRef.get().await(2, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 4");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 5");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 6");

            Thread.sleep(10); // see how many messages come through ...
            startLatchRef.set(new CountDownLatch(1));
            sessionHolder[0].close();

            // wait until connected
            assertTrue("Client connected", startLatchRef.get().await(2, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 7");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 8");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 9");

            Thread.sleep(10); // see how many messages come through ...
            sessionHolder[0].close();

            if ( !endLatch.await(5, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect event.");
            }

            // should be at least 3, can be more, since the client will attempt to reconnect again after the final
            // disconnection and can succeed before the server is shutdown
            assertTrue("Connection events", 3 <= connectEventCount.get());
        } finally {
            ResourceUtils.closeQuietly(syslog);
            acceptor.unbindAll();
        }
    }

    /**
     * Test disconnection of out of use clients.
     */
    public void testClientDisconnect() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress address = new VmPipeAddress(1234);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(1);

        // start syslog server
        IoHandlerAdapter handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) throws Exception {
                System.out.println("Session created.");
                startLatch.countDown();
            }
            public void messageReceived(IoSession iosession, Object obj) throws Exception {
                System.out.print(((ByteBuffer)obj).getString(Charset.defaultCharset().newDecoder()));
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
                endLatch.countDown();
            }
        };
        SocketConnectorConfig config = new SocketConnectorConfig();
        config.setThreadModel(ThreadModel.MANUAL);
        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.bind(address, handler, config);

        // create client
        Syslog syslog = manager.getSyslog(SyslogProtocol.VM, address, null, null, 23, "test.l7tech.com", null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatch.await(5, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 1");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 2");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 3");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 4");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 5");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 6");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 7");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 8");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 9");
            Thread.sleep(10); // see how many messages come through ...

            // close client
            ResourceUtils.closeQuietly(syslog);
    
            if ( !endLatch.await(5, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect.");
            }
        } finally {
            ResourceUtils.closeQuietly(syslog);
            acceptor.unbindAll();
        }
    }

    /**
     * Test sharing of underlying transport by clients.
     */
    public void testClientsShareConnection() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress address = new VmPipeAddress(1234);
        final AtomicInteger sessionCount = new AtomicInteger(0);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch messagesLatch = new CountDownLatch(3);
        final CountDownLatch endLatch = new CountDownLatch(1);

        // start syslog server
        IoHandlerAdapter handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) throws Exception {
                System.out.println("Session created.");
                startLatch.countDown();
                sessionCount.incrementAndGet();
            }
            public void messageReceived(IoSession iosession, Object obj) throws Exception {
                String message = ((ByteBuffer)obj).getString(Charset.defaultCharset().newDecoder());
                if ( message.length() > 0 ) {
                    System.out.print(message);
                    messagesLatch.countDown();
                }
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
                endLatch.countDown();
            }
        };
        SocketConnectorConfig config = new SocketConnectorConfig();
        config.setThreadModel(ThreadModel.MANUAL);
        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.bind(address, handler, config);

        // create clients
        Syslog syslog1 = manager.getSyslog(SyslogProtocol.VM, address, null, null, 23, "test.l7tech.com", null, null);
        Syslog syslog2 = manager.getSyslog(SyslogProtocol.VM, address, null, null,  2, "test.l7tech.com", null, null);
        Syslog syslog3 = manager.getSyslog(SyslogProtocol.VM, address, null, null, 13, "test.l7tech.com", null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatch.await(2, TimeUnit.SECONDS));

            // send some messages
            syslog1.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1, System.currentTimeMillis(), "Test message 1");
            syslog2.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 2, System.currentTimeMillis(), "Test message 2");
            syslog3.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 3, System.currentTimeMillis(), "Test message 3");

            assertTrue("Messages received", messagesLatch.await(2, TimeUnit.SECONDS));

            // close client
            ResourceUtils.closeQuietly(syslog1);
            ResourceUtils.closeQuietly(syslog2);
            ResourceUtils.closeQuietly(syslog3);

            if ( !endLatch.await(5, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect.");
            }

            assertEquals("Session count", 1, sessionCount.get());
        } finally {
            ResourceUtils.closeQuietly(syslog1);
            ResourceUtils.closeQuietly(syslog2);
            ResourceUtils.closeQuietly(syslog3);
            acceptor.unbindAll();
        }
    }

    /**
     *
     */
    private String sendMessage(final int vmpipeport,
                               final int facility,
                               final SyslogSeverity severity,
                               final String hostname,
                               final String process,
                               final long threadId,
                               final long time,
                               final String message) throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress address = new VmPipeAddress(vmpipeport);
        final CountDownLatch latch = new CountDownLatch(1);

        // start syslog server
        final String[] holder = new String[1];
        IoHandlerAdapter handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) throws Exception {
                System.out.println("Session created.");
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
            }
            public void messageReceived(IoSession iosession, Object obj) throws Exception {
                holder[0] = ((ByteBuffer)obj).getString(Charset.defaultCharset().newDecoder());
                latch.countDown();
            }
        };
        SocketConnectorConfig config = new SocketConnectorConfig();
        config.setThreadModel(ThreadModel.MANUAL);
        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        Syslog syslog = null;
        try {
            acceptor.bind(address, handler, config);

            // create client
            syslog = manager.getSyslog(SyslogProtocol.VM, address, null, null, facility, hostname + ".l7tech.com", null, null);

            // log message
            syslog.log(severity, process, threadId, time, message.replace(' ', '\n')); // covers testing translation of \n to <SPACE>

            if (latch.await(1, TimeUnit.SECONDS) ) {
                System.out.println(holder[0]);
                return holder[0];
            } else {
                fail("Message not received before timeout.");
                throw new Exception("Message not received before timeout.");
            }
        } finally {
            ResourceUtils.closeQuietly(syslog);
            acceptor.unbindAll();
        }
    }
}
