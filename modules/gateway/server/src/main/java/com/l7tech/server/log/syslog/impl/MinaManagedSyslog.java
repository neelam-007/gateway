package com.l7tech.server.log.syslog.impl;

import com.l7tech.objectmodel.Goid;
import com.l7tech.server.log.syslog.ManagedSyslog;
import com.l7tech.server.log.syslog.SyslogConnectionListener;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.SyslogSeverity;
import com.l7tech.util.Functions;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.apache.mina.transport.vmpipe.VmPipeConnector;
import org.apache.mina.util.ExceptionMonitor;

import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MINA implementation for syslog.
 *
 * @author Steve Jones
 */
public class MinaManagedSyslog extends ManagedSyslog {

    private static final Logger logger = Logger.getLogger(MinaManagedSyslog.class.getName());

    //- PUBLIC

    /**
     * Create a new MinaSyslog with the given target including SSL with client auth properties
     *
     * @param protocol The protocol to use
     * @param addresses The target address
     * @param sslKeystoreAlias The keystore alias to use for SSLContext
     * @param sslKeystoreId The keystore Id to use for the SSLContext
     */
    public MinaManagedSyslog(final SyslogProtocol protocol, final SocketAddress[] addresses,
                             final String sslKeystoreAlias, final Goid sslKeystoreId) {
        this.protocol = protocol;
        this.syslogAddresses = addresses;
        this.sslKeystoreAlias = sslKeystoreAlias;
        this.sslKeystoreId = sslKeystoreId;
        this.sender = new MessageSender(messageQueue, protocol, addresses, sslKeystoreAlias, sslKeystoreId);
        this.hasFailover = (addresses.length > 1);
    }

    /**
     * Log a message to syslog.
     *
     * @param format   The format to use
     * @param facility The facility part of the priority
     * @param severity The severity part of the priority
     * @param host     The host to log as (may be null)
     * @param process  The process to log
     * @param threadId The identifier for the log message
     * @param time     The time for the log message
     * @param message  The log message
     */
    @Override
    public void log(final SyslogFormat format,
                    final int facility,
                    final SyslogSeverity severity,
                    final String host,
                    final String process,
                    final long threadId,
                    final long time,
                    final String message) {
        if ( sender.isRunning() ) {
            try {
                messageQueue.put(buildMessage(format, facility, severity, host, process, threadId, time, message));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Close this syslog and dispose resources.
     *
     * <p>Attempts to use this Syslog after calling close will fail.</p>
     */
    @Override
    public void close() {
        sender.stop();
    }

    //- PROTECTED

    /**
     * Initialize this syslog.
     *
     * <p>Initialization is not thread safe, you should initialize before using
     * the Syslog.</p>
     *
     * <p>Once initialized, futher calls will have no affect.</p>
     */
    @Override
    protected void init() {
        if ( !initialized ) {
            initialized = true;
            // start the Message sender process
            sender.setSyslogConnectionListener(getSyslogConnectionListener());
            Thread thread;
            if (sender.isSSL)
                thread = new Thread(sender, "Syslog-SSL" + sender.toString());
            else
                thread = new Thread(sender, "Syslog" + sender.toString());
            thread.setDaemon(true);
            thread.start();
        }
    }

    //- PRIVATE

    static  {
        //
        // Unfortunately there is a common exception monitor for MINA, we set
        // this handler to ensure that errors during logging can never cause
        // logging.
        //
        ExceptionMonitor.setInstance(new ExceptionMonitor() {
            // Called for exceptions on interrupted, IOException on close, etc.
            @Override
            public void exceptionCaught(Throwable cause) {
            }
        });
    }

    private static final int QUEUE_CAPACITY = 200;
    private static final int DROP_BATCH_SIZE = 20;
    private static final long DEFAULT_RECONNECT_SLEEP = 1000L;
    private static final long MAX_RECONNECT_SLEEP = 60000L;

    private final BlockingQueue<FormattedSyslogMessage> messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final MessageSender sender;
    private boolean initialized = false;

    /** The protocol used to communicate with the syslog */
    private SyslogProtocol protocol;
    /** The SSL keystore - used for client auth only */
    private String sslKeystoreAlias;
    /** The SSL keystore id - used for client auth only */
    private Goid sslKeystoreId;
    /** List of all configured syslog host addresses (for failover)  */
    private SocketAddress[] syslogAddresses;
    /** Flag specifying whether there are failover syslog destinations */
    private final boolean hasFailover;
    /** Counter for the number of failover addresses attempts */
    private int failoverAttempts;
    /** synch lock for failover */
    private final Object failoverLock = new Object();
    /** Holder of the last message that failed to be sent */
    private MinaSyslogTextEncoder.TextMessage failedSend;

    /**
     * Construct a syslog message for the given information
     */
    private FormattedSyslogMessage buildMessage(final SyslogFormat format,
                                                final int facility,
                                                final SyslogSeverity severity,
                                                final String host,
                                                final String process,
                                                final long threadId,
                                                final long time,
                                                final String message) {
        return new FormattedSyslogMessage(format, facility, severity.getSeverity(), host, process, threadId, time, message);
    }

    /**
     * Extended version of syslog message with an associated format.
     */
    private static class FormattedSyslogMessage extends SyslogMessage {
        private final SyslogFormat format;

        FormattedSyslogMessage(final SyslogFormat format,
                               final int facility,
                               final int severity,
                               final String host,
                               final String process,
                               final long threadId,
                               final long time,
                               final String message) {
            super(facility, severity, host, process, threadId, time, message);
            this.format = format;
        }

        SyslogFormat getFormat() {
            return format;
        }
    }

    /**
     * Pulls messages off the queue and sends them.
     *
     * Will drop messages if they cannot be sent and the queue is full
     */
    private class MessageSender implements Runnable {
        private final BlockingQueue<FormattedSyslogMessage> messageQueue;
        private final List<SyslogMessage> dropList = new ArrayList<>(DROP_BATCH_SIZE);
        private final SyslogProtocol protocol;
        private final SocketAddress[] addressList;
        private IoConnector connector;
        private MinaSyslogHandler handler;
        private final AtomicBoolean run = new AtomicBoolean(true);
        private final AtomicBoolean reconnect = new AtomicBoolean(true);
        private final AtomicReference<IoSession> sessionRef = new AtomicReference<>();
        private final AtomicReference<SyslogConnectionListener> listener = new AtomicReference<>();
        private final boolean isSSL;

        /** Index for the currently running syslog in the syslogAddresses list **/
        private int syslogIndex;
        /** Sleep interval between reconnect attempts to a syslog host */
        private long reconnectSleep = DEFAULT_RECONNECT_SLEEP;
        /** String describing the MessageSender instance */
        private String senderString;

        public MessageSender(final BlockingQueue<FormattedSyslogMessage> messageQueue,
                             final SyslogProtocol protocol,
                             final SocketAddress[] addresses,
                             final String sslKeystoreAlias,
                             final Goid sslKeystoreId) {
            this.messageQueue = messageQueue;
            this.protocol = protocol;
            this.addressList = addresses;
            this.isSSL = SyslogProtocol.SSL.equals(protocol);

            // create IO handler
            Functions.BinaryVoid<IoSession, String> callback = getCallback();

            // create the handler and connector instances
            if (isSSL) {

                if (SyslogSslClientSupport.isInitialized()) {
                    this.handler = new MinaSecureSyslogHandler(callback, getCurrentSessionCallback(), sslKeystoreAlias, sslKeystoreId);
                    this.connector = new NioSocketConnector();
                    MinaSecureSyslogHandler.class.cast(this.handler).setupConnectorForSSL(this.connector);
                }

            } else {

                this.handler = new MinaSyslogHandler(callback);

                // create TCP or UDP connector
                switch ( protocol ) {
                    case TCP:
                        this.connector = new NioSocketConnector();
                        break;
                    case SSL:
                        // won't happen -- covered by the isSSL check
                        break;
                    case UDP:
                        this.connector = new NioDatagramConnector();
                        break;
                    case VM:
                        this.connector = new VmPipeConnector();
                        break;
                    default:
                        throw new IllegalArgumentException("invalid protocol " + protocol);
                }
            }
        }

        @Override
        public void run() {
            sendMessages();
        }

        /**
         * Get a string representation of this sender, this should contain
         * info to allow the threads to be identified.
         */
        @Override
        public String toString() {
            if (senderString == null) {
                StringBuilder builder = new StringBuilder(128);
                builder.append("MessageSender-");
                builder.append(protocol.name());
                builder.append('-');
                builder.append(getAddress());
                senderString = builder.toString();
            }
            return senderString;
        }

        /**
         * Set the connection listener for this sender.
         *
         * @param listener The connection listener to use.
         */
        private void setSyslogConnectionListener(final SyslogConnectionListener listener) {
            this.listener.set(listener);
        }

        /**
         * Signal stop
         */
        private void stop() {
            run.set(false);
        }

        /**
         * Check if stopped or stopping
         */
        private boolean isRunning() {
            return run.get();
        }

        /**
         * Set/clear the active session.
         *
         * If set to null an attempt is made to reconnect to the
         * server.
         */
        private void setSession(final IoSession session, final String sessionId) {

            // this check is in place to handle closing off sessions is not currently in use
            if (session == null && sessionRef.get() != null && !sessionId.equals(sessionRef.get().toString())) {
                // fireDisconnected();
                return; // do not reconnect
            }

            if ( session == null ) {
                sessionRef.set(null);
                fireDisconnected();
                try {
                    Thread.sleep(getAndIncReconnectSleep());
                    reconnect.set(true);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                if (sessionRef.get() != null) {
                    // strict replacement
                    sessionRef.set(session);
                } else {
                    sessionRef.set(session);
                    fireConnected();
                }
            }
        }

        /**
         * Get the sleep time to use before the next reconnect attempt.
         *
         * Increment the sleep time
         */
        private long getAndIncReconnectSleep() {
            long value = reconnectSleep;

            if ( reconnectSleep < MAX_RECONNECT_SLEEP) {
                long newReconnectSleep = reconnectSleep * 2;
                if ( newReconnectSleep > MAX_RECONNECT_SLEEP ) {
                    newReconnectSleep = MAX_RECONNECT_SLEEP;
                }
                reconnectSleep = newReconnectSleep;
            }

            return value;
        }

        /**
         * Reset the sleep used between connection attempts and the failover attempts after a successful syslog
         * connection has been established.
         */
        private void resetAfterConnect() {
            reconnectSleep = DEFAULT_RECONNECT_SLEEP;
            failoverAttempts = 0;
        }

        /**
         * Set the reconnect flag after a delay.  Sleeps the message sender thread for a
         * period of time that gets incrementally longer up to the MAX_RECONNECT_SLEEP.
         *
         * When failover host(s) are configured, the thread will switch to the next syslog host
         * only when the MAX_RECONNECT_SLEEP is reached for each host in the list (60 seconds).
         */
        private void flagReconnectAfterDelay() {
            try {
                if (hasFailover && reconnectSleep >= MAX_RECONNECT_SLEEP) {
                    failover();
                } else {
                    Thread.sleep(getAndIncReconnectSleep());
                }
                reconnect.set(true);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Performs the failover to the next syslog host in the list.
         */
        private void failover() {

            if (failoverAttempts > syslogAddresses.length) {
                logger.log(Level.WARNING, "All Syslog failover hosts have been attempted");
            }

            final String oldSender = toString();
            synchronized (failoverLock) {

                senderString = null;
                failoverAttempts++;

                // advance to the next address in the list
                syslogIndex = (syslogIndex + 1) % addressList.length;

                // reset reconnect sleep
                reconnectSleep = DEFAULT_RECONNECT_SLEEP;

                logger.log(Level.WARNING, "Syslog failover: {0} ===> {1}", new Object[] {oldSender, toString()});
            }
        }

        /**
         * Initiate connection to the syslog host
         */
        private void connect() {
            // reset flag
            reconnect.set( false );

            // build connector config
            connector.setConnectTimeoutMillis(30000L);
            connector.setHandler(handler);

            // start connect operation
            try {
                final String senderStr = toString();
                final ConnectFuture connectFuture = connector.connect(getAddress());

                // only perform this for non-SSL
                connectFuture.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        if ( !connectFuture.isConnected() ) {
                            logger.log(Level.WARNING, "Syslog connection attempt failed (" + senderStr + ")" );
                            flagReconnectAfterDelay();
                        } else {
                            resetAfterConnect();
                        }
                    }
                });
            } catch (UnresolvedAddressException uae) {
                fireDisconnected(); // needed for audit since no session is ever created
                flagReconnectAfterDelay();
            }
        }

        /**
         * Initiate SSL connection to the syslog host
         */
        private void connectSSL() {

            if (sessionRef.get() != null) {
                sessionRef.get().close(true);
            }

            if (connector == null) {
                if (SyslogSslClientSupport.isInitialized()) {
                    MinaSecureSyslogHandler sslHandler =
                            new MinaSecureSyslogHandler(getCallback(), getCurrentSessionCallback(), sslKeystoreAlias, sslKeystoreId);
                    this.handler = sslHandler;
                    this.connector = new NioSocketConnector();
                    sslHandler.setupConnectorForSSL(this.connector);
                } else {
                    return;
                }
            }

            // reset flag
            reconnect.set( false );

            // build connector config
            connector.setConnectTimeoutMillis(30000L);
            connector.setHandler(handler);

            // start connect operation
            try {
                final long connStart = System.currentTimeMillis();

                final String senderStr = toString();
                final ConnectFuture connectFuture = connector.connect(getAddress());

                // only perform this for non-SSL
                connectFuture.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        if ( !connectFuture.isConnected() ) {
                            logger.log(Level.WARNING, "Syslog SSL connection attempt failed (" + senderStr + ")" );
                            flagReconnectAfterDelay();
                        }
                    }
                });

                // for SSL, we will wait for the connection to complete before moving on
                connectFuture.awaitUninterruptibly(1000L);

                if (logger.isLoggable(Level.FINE) && connectFuture.isDone()) {
                    StringBuilder sb = new StringBuilder("Connection complete, ttc=");
                    sb.append(System.currentTimeMillis() - connStart).append("\n");

                    IoSession sess = connectFuture.getSession();
                    sb.append("SessionCreate: ").append(sess.getCreationTime()).append("\n");
                    sb.append("Connected: ").append(sess.isConnected()).append("\n");
                    sb.append("isClosing: ").append(sess.isClosing()).append("\n");
                    sb.append("Transport: ").append(sess.getTransportMetadata()).append("\n");
                    sb.append("SSLSession: ").append(((MinaSecureSyslogHandler) handler).getSSLSession(sess)).append("\n");
                    sb.append("SocketAddress: ").append(getAddress()).append("\n");
                    sb.append("Sessions for address: ").append(getManagedSessionsByAddress(getAddress()));
                    logger.fine(sb.toString());
                }
            } catch (UnresolvedAddressException uae) {
                fireDisconnected(); // needed for audit since no session is ever created
                flagReconnectAfterDelay();
            }
        }

        private Set<IoSession> getManagedSessionsByAddress(SocketAddress address) {
            // N.B. managedSessions is an UnmodifiableMap of a ConcurrentHashMap so it's thread safe
            Map<Long, IoSession> managedSessions = connector.getManagedSessions();

            Set<IoSession> sessionsForAddress = new HashSet<>();

            for (IoSession session : managedSessions.values()) {
                if (session.getRemoteAddress().equals(address)) {
                    sessionsForAddress.add(session);
                }
            }

            return sessionsForAddress;
        }

        /**
         * Main message loop
         */
        private void sendMessages() {
            try {
                boolean initialWrite = true;
                while( run.get() ) {
                    try {
                        if ( reconnect.get() ) {
                            if (isSSL)
                                connectSSL();
                            else
                                connect();
                            initialWrite = true;
                        }

                        IoSession session = sessionRef.get();
                        if ( isMessagePending() && sessionValid(session, initialWrite) ) {

                            // get the next message to send
                            MinaSyslogTextEncoder.TextMessage message = pollForMessage();

                            // send the message
                            if ( message != null ) {

                                WriteFuture writeFuture = session.write(message);

                                // For SSL, we check the writeFuture to ensure the message was written out successfully
                                if (isSSL && initialWrite) {
                                    writeFuture.awaitUninterruptibly(1000L);
                                    if (writeFuture.isWritten()) {
                                        // all fine
                                        initialWrite = false;
                                    } else {
                                        // increment failure attempt
                                        failedSend = message;
                                        flagReconnectAfterDelay();
                                    }
                                } else {
                                    initialWrite = false;
                                }
                            }
                        } else {
                            // check if the queue is full, and drop some.
                            if ( messageQueue.remainingCapacity() == 0 ) {
                                dropList.clear();
                                messageQueue.drainTo(dropList, DROP_BATCH_SIZE);
                                dropList.clear();
                            } else {
                                Thread.sleep(15L);
                            }
                        }
                    } catch (InterruptedException ie) {
                        run.set(false);
                        break;
                    } catch (Exception e) {
                        System.err.println("Unexpected error sending syslog messages " + this);
                        e.printStackTrace();
                    }
                }
            } finally {
                if ( run.get() ) {
                    System.err.println("Unexpected error sending syslog messages, sender exiting " + this);
                } else {
                    IoSession session = sessionRef.get();
                    if ( session != null ) {
                        session.close(true);
                    }
                }
            }
        }

        /**
         * Validates the IoSession parameter for whether it is ready for writing to.
         *
         * @param sess the ioSession to check
         * @param isFirstWriteForSession flag indicating whether the message is for the first write for a newly created ioSession instance
         * @return true if the IoSession is ready for writing, false otherwise
         */
        private boolean sessionValid(IoSession sess, boolean isFirstWriteForSession) {

            if (isSSL && isFirstWriteForSession) {
                return (sess != null && sess.isConnected() && !sess.isClosing());
            }
            return handler.verifySession(sess);
        }

        /**
         * Returns whether there is a pending message to be sent.
         *
         * @return true if there are pending message(s) in the message queue or held in the failedSend variable
         */
        private boolean isMessagePending() {
            return (!messageQueue.isEmpty() || failedSend != null);
        }

        /**
         * Polls the messageQueue for the next syslog message to be sent.
         *
         * @return the fully formatted syslog message instance
         * @throws InterruptedException caused when the polling operation is interrupted
         */
        private MinaSyslogTextEncoder.TextMessage pollForMessage() throws InterruptedException {

            if (failedSend != null) {
                MinaSyslogTextEncoder.TextMessage resendMsg = failedSend;
                failedSend = null;
                return resendMsg;
            }

            FormattedSyslogMessage message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
            if ( message != null ) {
                String formattedMessage =
                        formatMessage(message.getFormat(),
                                      message.getFacility(),
                                      message.getSeverity(),
                                      message.getPriority(),
                                      message.getHost(),
                                      message.getProcess(),
                                      message.getThreadId(),
                                      message.getTime(),
                                      message.getMessage());

                // create text message with encoding info
                return new MinaSyslogTextEncoder.TextMessage(
                                message.getFormat().getCharset(),
                                message.getFormat().getDelimiter(),
                                message.getFormat().getMaxLength(),
                                formattedMessage);
            }
            return null;
        }

        /**
         * Fire connected notification (unless UDP)
         */
        private void fireConnected() {
            SyslogConnectionListener listener = this.listener.get();
            if ( listener != null && SyslogProtocol.UDP != protocol ) {
                listener.notifyConnected(getAddress());
            }
        }

        /**
         * Fire disconnected notification (unless UDP)
         */
        private void fireDisconnected() {
            SyslogConnectionListener listener = this.listener.get();
            if ( listener != null && SyslogProtocol.UDP != protocol  ) {
                listener.notifyDisconnected(getAddress());
            }
        }

        /**
         * Gets the current SocketAddress that the sender is connected to.
         *
         * @return the currently referenced SocketAddress
         */
        private SocketAddress getAddress() {
            return addressList[syslogIndex];
        }

        /**
         * Create the syslog handler callback function.
         *
         * @return the callback function to be passed into the SyslogHandler
         */
        private Functions.BinaryVoid<IoSession, String> getCallback() {

            return new Functions.BinaryVoid<IoSession, String>() {
                public void call(final IoSession session, final String sessionId) {
                    setSession(session, sessionId);
                }
            };
        }

        /**
         * Create the syslog handler callback function that checks an IoSession against the one
         * held in sessionRef.
         *
         * @return the callback function to be passed into the SyslogHandler
         */
        private Functions.Unary<Boolean, IoSession> getCurrentSessionCallback() {

            return new Functions.Unary<Boolean, IoSession>() {
                public Boolean call(final IoSession session) {
                    return (sessionRef.get() != null && sessionRef.get().equals(session));
                }
            };
        }
    }
}
