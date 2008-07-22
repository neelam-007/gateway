package com.l7tech.server.log.syslog.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;

import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.vmpipe.VmPipeConnector;

import com.l7tech.util.Functions;
import com.l7tech.server.log.syslog.SyslogSeverity;
import com.l7tech.server.log.syslog.SyslogProtocol;
import com.l7tech.server.log.syslog.ManagedSyslog;
import com.l7tech.server.log.syslog.SyslogConnectionListener;

/**
 * MINA implementation for syslog.
 *
 * @author Steve Jones
 */
public class MinaManagedSyslog extends ManagedSyslog {

    //- PUBLIC

    /**
     * Create a new MinaSyslog with the given target.
     *
     * @param protocol The protocol to use
     * @param address The target address
     */
    public MinaManagedSyslog(final SyslogProtocol protocol,
                             final SocketAddress address) {
        sender = new MessageSender(messageQueue, protocol, address);
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
    public void log(final SyslogFormat format,
                    final int facility,
                    final SyslogSeverity severity,
                    final String host,
                    final String process,
                    final long threadId,
                    final long time,
                    final String message) {
        if ( !sender.isRunning() )
            throw new IllegalStateException("Message logged when stopped");

        try {
            messageQueue.put(buildMessage(format, facility, severity, host, process, threadId, time, message));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Close this syslog and dispose resources.
     *
     * <p>Attempts to use this Syslog after calling close will fail.</p>
     */
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
    protected void init() {
        if ( !initialized ) {
            initialized = true;
            sender.setSyslogConnectionListener(getSyslogConnectionListener());
            Thread thread = new Thread(sender, "Syslog" + sender.toString());
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
        ExceptionMonitor.setInstance(new ExceptionMonitor(){
            // Called for exceptions on interrupted, IOException on close, etc.
            public void exceptionCaught(Throwable cause) {}
        });
    }

    private static final int QUEUE_CAPACITY = 200;
    private static final int DROP_BATCH_SIZE = 20;
    private static final long DEFAULT_RECONNECT_SLEEP = 1000L;
    private static final long MAX_RECONNECT_SLEEP = 60000L;

    private final BlockingQueue<FormattedSyslogMessage> messageQueue = new ArrayBlockingQueue(QUEUE_CAPACITY);
    private final MessageSender sender;
    private boolean initialized = false;

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
    private static class MessageSender implements Runnable {
        private final BlockingQueue<FormattedSyslogMessage> messageQueue;
        private final List<SyslogMessage> dropList = new ArrayList(DROP_BATCH_SIZE);;
        private final SyslogProtocol protocol;
        private final SocketAddress address;
        private final IoConnector connector;
        private final MinaSyslogHandler handler;
        private final AtomicBoolean run = new AtomicBoolean(true);
        private final AtomicBoolean reconnect = new AtomicBoolean(true);
        private final AtomicReference<IoSession> sessionRef = new AtomicReference<IoSession>();
        private final AtomicReference<SyslogConnectionListener> listener = new AtomicReference();
        private long reconnectSleep = DEFAULT_RECONNECT_SLEEP;

        public MessageSender(final BlockingQueue<FormattedSyslogMessage> messageQueue,
                             final SyslogProtocol protocol,
                             final SocketAddress address) {
            this.messageQueue = messageQueue;
            this.protocol = protocol;
            this.address = address;

            // create IO handler
            this.handler = new MinaSyslogHandler(new Functions.UnaryVoid<IoSession>(){
                public void call(final IoSession session) {
                    setSession(session);
                }
            });

            // create TCP or UDP connector
            switch ( protocol ) {
                case TCP:
                    this.connector = new SocketConnector();
                    break;
                case UDP:
                    this.connector = new DatagramConnector();
                    break;
                case VM:
                    this.connector = new VmPipeConnector();
                    break;
                default:
                    throw new IllegalArgumentException("invalid protocol " + protocol);
            }
        }

        public void run() {
            sendMessages();
        }

        /**
         * Get a string representation of this sender, this should contain
         * info to allow the threads to be identified. 
         */
        public String toString() {
            StringBuilder builder = new StringBuilder(128);
            builder.append("MessageSender-");
            builder.append(protocol.name());
            builder.append('-');
            builder.append(address);
            return builder.toString();
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
        private void setSession(final IoSession session) {
            sessionRef.set(session);
            if ( session == null ) {
                fireDisconnected();
                try {
                    Thread.sleep(getAndIncReconnectSleep());
                    reconnect.set(true);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                fireConnected();
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
         * Reset the sleep used between connection attempts
         */
        private void resetReconnectSleep() {
            reconnectSleep = DEFAULT_RECONNECT_SLEEP;
        }

        /**
         * Set the reconnect flag after a delay
         */
        private void flagReconnectAfterDelay() {
            try {
                Thread.sleep(getAndIncReconnectSleep());
                reconnect.set( true );
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        }

        /**
         * Initiate connection to the syslog host
         */
        private void connect() {
            // reset flag
            reconnect.set( false );

            // build connector config
            SocketConnectorConfig config = new SocketConnectorConfig();
            config.setConnectTimeout(30000);
            config.setThreadModel(ThreadModel.MANUAL);

            // start connect operation
            try {
                final ConnectFuture connectFuture = connector.connect( address, handler, config );
                connectFuture.addListener(new IoFutureListener() {
                    public void operationComplete(IoFuture future) {
                        if ( !connectFuture.isConnected() ) {
                            flagReconnectAfterDelay();
                        } else {
                            resetReconnectSleep();
                        }
                    }
                });
            } catch (UnresolvedAddressException uae) {
                fireDisconnected(); // needed for audit since no session is ever created
                flagReconnectAfterDelay();
            }
        }

        /**
         * Main message loop
         */
        private void sendMessages() {
            try {
                while( run.get() ) {
                    try {
                        if ( reconnect.get() ) {
                            connect();
                        }

                        IoSession session = sessionRef.get();
                        if ( session != null && session.isConnected() && !session.isClosing() ) {
                            FormattedSyslogMessage message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                            if ( message != null ) {
                                // format
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
                                MinaSyslogTextEncoder.TextMessage textMessage =
                                        new MinaSyslogTextEncoder.TextMessage(
                                                message.getFormat().getCharset(),
                                                message.getFormat().getDelimiter(),
                                                message.getFormat().getMaxLength(),
                                                formattedMessage);

                                // send
                                session.write(textMessage);
                            }
                        } else {
                            // check if the queue is full, and drop some.
                            if ( messageQueue.remainingCapacity() == 0 ) {
                                dropList.clear();
                                messageQueue.drainTo(dropList, DROP_BATCH_SIZE);
                                dropList.clear();
                            } else {
                                Thread.sleep(1);
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
                        session.close();
                    }
                }
            }
        }

        /**
         * Fire connected notification (unless UDP)
         */
        private void fireConnected() {
            SyslogConnectionListener listener = this.listener.get();
            if ( listener != null && SyslogProtocol.UDP != protocol ) {
                listener.notifyConnected(address);
            }
        }

        /**
         * Fire disconnected notification (unless UDP)
         */
        private void fireDisconnected() {
            SyslogConnectionListener listener = this.listener.get();
            if ( listener != null && SyslogProtocol.UDP != protocol  ) {
                listener.notifyDisconnected(address);
            }
        }
    }
}
