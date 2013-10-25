package com.l7tech.server.log.syslog;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.vmpipe.VmPipeAcceptor;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.junit.AfterClass;
import org.junit.Test;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

/**
 * JUnit test for syslog manager
 *
 * @author Steve Jones
 */
public class SyslogManagerTest {

    public static final String REGEX_STANDARD = "<([1-9][0-9]{0,2})>([A-Za-z]{3} [ 1-9][0-9] [0-9]{2}:[0-9]{2}:[0-9]{2}) ([a-zA-Z\\-_0-9]{1,1024}) ([a-zA-Z0-9\\-_]{1,1024})\\[([0-9]{1,10})\\]: ([a-zA-Z0-9 ]{0,10000})[\\n]{0,1}";
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            SyslogManagerTest.class.getPackage().getName() + ".maxLength"
        );
    }

    /**
     * Full client/server test for message format
     */
    @Test
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

        String result = sendMessage(1234, facility, severity, hostname, process, (long) threadId, time, message);
        assertNotNull("Message received", result);

        Pattern pattern = Pattern.compile(REGEX_STANDARD);
        Matcher matcher = pattern.matcher(result);

        System.out.println("Checking for regex match [" + result + "]");
        assertTrue("Regex matches log message", matcher.matches());
        int outPriority = Integer.parseInt(matcher.group(1));
        String outDateStr = matcher.group(2);
        String outHost = matcher.group(3);
        String outProc = matcher.group(4);
        long outThread = Long.valueOf(matcher.group(5));
        String outMess = matcher.group(6);

        assertEquals("Priority", (long) ((8 * facility) + severity.getSeverity()), (long) outPriority );
        assertEquals("Date", date, outDateStr);
        assertEquals("Host", hostname, outHost);
        assertEquals("Process", process, outProc);
        assertEquals("Thread", (long) threadId, outThread);
        assertEquals("Message", message, outMess);
    }

    /**
     * Full client/server test for message format
     */
    @Test
    public void testSenderMessageTruncation() throws Exception {
        SyspropUtil.setProperty( SyslogManagerTest.class.getPackage().getName() + ".maxLength", "1" );  // 1 is not valid, so this uses min length

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

        String result = sendMessage(1234, facility, severity, hostname, process, (long) threadId, time, message);
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

        assertEquals("Priority", (long) ((8 * facility) + severity.getSeverity()), (long) outPriority );
        assertEquals("Date", date, outDateStr);
        assertEquals("Host", hostname, outHost);
        assertEquals("Process", process, outProc);
        assertEquals("Thread", (long) threadId, outThread);
        assertTrue("Message start", message.startsWith(outMess));
        assertThat( "Message truncated", message.length(), not( equalTo( outMess.length() ) ) );
    }

    /**
     * Test notification of connection and disconnection events.
     */
    @Test
    public void testClientConnectionEvents() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress[] addresses = new VmPipeAddress[] { new VmPipeAddress(1234) };
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
                System.out.print(((IoBuffer)obj).getString(Charset.defaultCharset().newDecoder()));
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
            }
        };

        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.setHandler(handler);
        acceptor.bind(addresses[0]);

        // create client
        Syslog syslog = manager.getSyslog(SyslogProtocol.VM, addresses, null, null, 23, "test.l7tech.com", null, null, null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatch.await( 5L, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 1");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 2");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 3");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 4");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 5");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 6");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 7");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 8");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 9");
            Thread.sleep( 10L ); // see how many messages come through ...

            // server shuts down
            acceptor.unbind();
            sessionHolder[0].close(true);

            assertEquals("Connection events", 1L, (long) connectEventCount.get() );

            if ( !endLatch.await( 5L, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect event.");
            }
        } finally {
            ResourceUtils.closeQuietly(syslog);
        }
    }

    /**
     * Test client reconnection
     */
    @Test
    public void testClientReconnection() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress[] addresses = new VmPipeAddress[] { new VmPipeAddress(1234) };
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
                System.out.print(((IoBuffer)obj).getString(Charset.defaultCharset().newDecoder()));
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
            }
        };

        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.setHandler(handler);
        acceptor.bind(addresses[0]);

        // create client
        Syslog syslog = manager.getSyslog(SyslogProtocol.VM, addresses, null, null, 23, "test.l7tech.com", null, null, null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatchRef.get().await( 2L, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 1");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 2");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 3");

            Thread.sleep( 10L ); // see how many messages come through ...
            startLatchRef.set(new CountDownLatch(1));
            sessionHolder[0].close(true);

            // wait until connected
            assertTrue("Client connected", startLatchRef.get().await( 2L, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 4");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 5");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 6");

            Thread.sleep( 10L ); // see how many messages come through ...
            startLatchRef.set(new CountDownLatch(1));
            sessionHolder[0].close(true);

            // wait until connected
            assertTrue("Client connected", startLatchRef.get().await( 2L, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 7");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 8");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 9");

            Thread.sleep( 10L ); // see how many messages come through ...
            sessionHolder[0].close(true);

            if ( !endLatch.await( 5L, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect event.");
            }

            // should be at least 3, can be more, since the client will attempt to reconnect again after the final
            // disconnection and can succeed before the server is shutdown
            assertTrue("Connection events", 3 <= connectEventCount.get());
        } finally {
            ResourceUtils.closeQuietly(syslog);
            acceptor.unbind();
        }
    }

    /**
     * Test disconnection of out of use clients.
     */
    @Test
    public void testClientDisconnect() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress[] addresses = new VmPipeAddress[] { new VmPipeAddress(1234) };
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(1);

        // start syslog server
        IoHandlerAdapter handler = new IoHandlerAdapter() {
            public void sessionCreated(IoSession session) throws Exception {
                System.out.println("Session created.");
                startLatch.countDown();
            }
            public void messageReceived(IoSession iosession, Object obj) throws Exception {
                System.out.print(((IoBuffer)obj).getString(Charset.defaultCharset().newDecoder()));
            }
            public void sessionClosed(IoSession iosession) throws Exception {
                System.out.println("Session closed.");
                endLatch.countDown();
            }
        };

        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.setHandler(handler);
        acceptor.bind(addresses[0]);

        // create client
        Syslog syslog = manager.getSyslog(SyslogProtocol.VM, addresses, null, null, 23, "test.l7tech.com", null, null, null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatch.await( 5L, TimeUnit.SECONDS));

            // send some messages
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 1");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 2");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 3");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 4");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 5");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 6");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 7");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 8");
            syslog.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 9");
            Thread.sleep( 10L ); // see how many messages come through ...

            // close client
            ResourceUtils.closeQuietly(syslog);
    
            if ( !endLatch.await( 5L, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect.");
            }
        } finally {
            ResourceUtils.closeQuietly(syslog);
            acceptor.unbind();
        }
    }

    /**
     * Test sharing of underlying transport by clients.
     */
    @Test
    public void testClientsShareConnection() throws Exception {
        SyslogManager manager = new SyslogManager();
        VmPipeAddress[] addresses = new VmPipeAddress[] { new VmPipeAddress(1234) };
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
                String message = ((IoBuffer)obj).getString(Charset.defaultCharset().newDecoder());
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

        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.setHandler(handler);
        acceptor.bind(addresses[0]);

        // create clients
        Syslog syslog1 = manager.getSyslog(SyslogProtocol.VM, addresses, null, null, 23, "test.l7tech.com", null, null, null, null);
        Syslog syslog2 = manager.getSyslog(SyslogProtocol.VM, addresses, null, null,  2, "test.l7tech.com", null, null, null, null);
        Syslog syslog3 = manager.getSyslog(SyslogProtocol.VM, addresses, null, null, 13, "test.l7tech.com", null, null, null, null);

        try {
            // wait until connected
            assertTrue("Client connected", startLatch.await( 2L, TimeUnit.SECONDS));

            // send some messages
            syslog1.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 1L, System.currentTimeMillis(), "Test message 1");
            syslog2.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 2L, System.currentTimeMillis(), "Test message 2");
            syslog3.log(SyslogSeverity.INFORMATIONAL, "SSG-default_", 3L, System.currentTimeMillis(), "Test message 3");

            assertTrue("Messages received", messagesLatch.await( 2L, TimeUnit.SECONDS));

            // close client
            ResourceUtils.closeQuietly(syslog1);
            ResourceUtils.closeQuietly(syslog2);
            ResourceUtils.closeQuietly(syslog3);

            if ( !endLatch.await( 5L, TimeUnit.SECONDS) ) {
                fail("Timeout waiting for disconnect.");
            }

            assertEquals("Session count", 1L, (long) sessionCount.get() );
        } finally {
            ResourceUtils.closeQuietly(syslog1);
            ResourceUtils.closeQuietly(syslog2);
            ResourceUtils.closeQuietly(syslog3);
            acceptor.unbind();
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
        VmPipeAddress[] addresses = new VmPipeAddress[] { new VmPipeAddress(vmpipeport) };
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
                String value = ((IoBuffer)obj).getString(Charset.defaultCharset().newDecoder());
                if ( value.length()!=0 ) {
                    holder[0] = value;
                    latch.countDown();
                }
            }
        };

        VmPipeAcceptor acceptor = new VmPipeAcceptor();
        acceptor.setHandler(handler);

        Syslog syslog = null;

        try {
            acceptor.bind(addresses[0]);

            // create client
            syslog = manager.getSyslog(SyslogProtocol.VM, addresses, null, null, facility, hostname + ".l7tech.com", null, null, null, null);

            // log message
            syslog.log(severity, process, threadId, time, message.replace(' ', '\n')); // covers testing translation of \n to <SPACE>

            if (latch.await( 1L, TimeUnit.SECONDS) ) {
                System.out.println(holder[0]);
                return holder[0];
            } else {
                fail("Message not received before timeout.");
                throw new Exception("Message not received before timeout.");
            }
        } finally {
            ResourceUtils.closeQuietly(syslog);
            acceptor.unbind();
        }
    }
}
