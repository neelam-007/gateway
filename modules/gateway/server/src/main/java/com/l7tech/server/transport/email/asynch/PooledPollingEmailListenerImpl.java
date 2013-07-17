package com.l7tech.server.transport.email.asynch;

import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.system.EmailEvent;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.transport.email.EmailListenerConfig;
import com.l7tech.server.transport.email.EmailListenerManager;
import com.l7tech.server.transport.email.EmailMessages;
import com.l7tech.server.transport.email.PollingEmailListener;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ThreadPool;
import com.l7tech.util.TimeUnit;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import java.beans.PropertyChangeEvent;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A PollingEmailListener that creates EmailTask's so that a thread pool can process the messages.
 */
public class PooledPollingEmailListenerImpl implements PollingEmailListener {
    private static final Logger _logger = Logger.getLogger(PooledPollingEmailListenerImpl.class.getName());

    private static final String SOCKET_FACTORY_CLASSNAME = SslClientHostnameAwareSocketFactory.class.getName();

    /*
     * Is there a better place for these properties?
     */
    protected static final int MAXIMUM_OOPSES = 5;
    protected static final long RECEIVE_TIMEOUT = 5000L;
    protected static final long SHUTDOWN_TIMEOUT = 7000L;
    protected static final int OOPS_RETRY = 5000; // Five seconds
    protected static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    protected static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    protected static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    protected static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 mins;

    private EmailListenerConfig emailListenerCfg;
    private final ThreadPoolBean threadPoolBean;

    private EmailListenerManager emailListenerManager;

    /** The listener thread that performs the polling loop */
    protected final ListenerThread _listener;
    
    /** Flag specifying whether the listener has started */
    private boolean _started;

    // Runtime stuff
    private boolean _stop;
    private final Object sync = new Object();
    private Thread _thread;
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;

    private Session emailSession;
    private Store messageStore;
    private Folder emailFolder;

    /**
     * Constructor.
     *
     * @param emailListenerCfg attributes for the email listener configuration
     * @param emailListenerManager the manager object for EmailLister's
     * @param threadPoolBean thread pool bean for email listeners
     * @param connectionTimeout Socket connection timeout value in milliseconds.
     * @param timeout Socket I/O timeout value in milliseconds.
     */
    public PooledPollingEmailListenerImpl(final EmailListenerConfig emailListenerCfg,
                                          final EmailListenerManager emailListenerManager,
                                          final ThreadPoolBean threadPoolBean,
                                          long connectionTimeout,
                                          long timeout) {
        this.emailListenerCfg = emailListenerCfg;
        this.emailListenerManager = emailListenerManager;
        this.threadPoolBean = threadPoolBean;

        if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.POP3) {
            Properties props = new Properties();
            props.setProperty("mail.pop3.connectiontimeout", Long.toString(connectionTimeout));
            props.setProperty("mail.pop3.timeout", Long.toString(timeout));
            if(emailListenerCfg.getEmailListener().isUseSsl()) {
                props.setProperty("mail.pop3s.socketFactory.fallback", "false");
                props.setProperty("mail.pop3s.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            }
            emailSession = Session.getInstance(props);
        } else if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.IMAP) {
            Properties props = new Properties();
            props.setProperty("mail.imap.connectiontimeout", Long.toString(connectionTimeout));
            props.setProperty("mail.imap.timeout", Long.toString(timeout));
            if(emailListenerCfg.getEmailListener().isUseSsl()) {
                props.setProperty("mail.imaps.socketFactory.fallback", "false");
                props.setProperty("mail.imaps.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            }
            emailSession = Session.getInstance(props);
        }
        // create the ListenerThread
        this._listener = new ListenerThread();
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("email-listener-");
        s.append(emailListenerCfg.getEmailListener().getName());
        return s.toString();
    }

    /**
     * Method used to ensure that the email folder has been opened.
     *
     * @throws MessagingException when the email folder could not be opened
     */
    protected void ensureConnectionStarted() throws MessagingException {
        synchronized(sync) {
            boolean ok = true;
            String message = null;
            try {
                if(messageStore == null) {
                    if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.POP3) {
                        messageStore = emailSession.getStore(new URLName(emailListenerCfg.getEmailListener().isUseSsl() ? "pop3s" : "pop3",
                                                                         emailListenerCfg.getEmailListener().getHost(),
                                                                         emailListenerCfg.getEmailListener().getPort(),
                                                                         null,
                                                                         emailListenerCfg.getEmailListener().getUsername(),
                                                                         ServerVariables.expandSinglePasswordOnlyVariable(new LoggingAudit(_logger), emailListenerCfg.getEmailListener().getPassword())));
                    } else if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.IMAP) {
                        messageStore = emailSession.getStore(new URLName(emailListenerCfg.getEmailListener().isUseSsl() ? "imaps" : "imap",
                                                                         emailListenerCfg.getEmailListener().getHost(),
                                                                         emailListenerCfg.getEmailListener().getPort(),
                                                                         null,
                                                                         emailListenerCfg.getEmailListener().getUsername(),
                                                                         ServerVariables.expandSinglePasswordOnlyVariable(new LoggingAudit(_logger), emailListenerCfg.getEmailListener().getPassword())));
                    }
                }

                if(messageStore != null && !messageStore.isConnected()) {
                    messageStore.connect();
                }

                if(messageStore != null && messageStore.isConnected() && (emailFolder == null || !emailFolder.isOpen())) {
                    emailFolder = messageStore.getFolder(emailListenerCfg.getEmailListener().getFolder());
                    emailFolder.open(Folder.READ_WRITE);
                }

                ok = true;
            } catch (MessagingException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } catch (RuntimeException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } catch (FindException e) {
                message = ExceptionUtils.getMessage(e);
                throw new RuntimeException(e);
            } finally {
                if (ok) {
                    if (!_started) {
                        _started = true;
                        fireConnected();
                    }
                } else {
                    fireConnectError(message);
                }
            }
        }
    }

    /**
     * Starts the listener thread.
     *
     * @throws com.l7tech.server.LifecycleException when an error is encountered in the thread startup
     */
    @Override
    public void start() throws LifecycleException {
        synchronized(sync) {
            log(Level.FINE, EmailMessages.INFO_LISTENER_START, toString());
            _thread.start();
            log(Level.FINE, EmailMessages.INFO_LISTENER_STARTED, toString());
        }
    }

    /**
     * Tells the listener thread to stop.
     */
    @Override
    public void stop() {
        synchronized(sync) {
            log(Level.FINE, EmailMessages.INFO_LISTENER_STOP, toString());
            _stop = true;
            lastStopRequestedTime = System.currentTimeMillis();
        }
    }

    /**
     * Give the listener thread a set amount of time to shutdown, before it gets interrupted.
     */
    @Override
    public void ensureStopped() {
        long stopRequestedTime;

        synchronized(sync) {
            stop();
            stopRequestedTime = lastStopRequestedTime;
        }

        try {
            long waitTime = SHUTDOWN_TIMEOUT - (System.currentTimeMillis() - stopRequestedTime);
            if ( waitTime > 10L ) {
                _thread.join( SHUTDOWN_TIMEOUT );
            }
        } catch ( InterruptedException ie ) {
            Thread.currentThread().interrupt();
        }

        if ( _thread.isAlive() ) {
            log(Level.WARNING, EmailMessages.WARN_LISTENER_THREAD_ALIVE, this);
        }
    }

    /**
     * Perform cleanup of resources and reset the listener status.
     */
    protected void cleanup() {
        /*
         * Moved responsibility for cleanup to implementation class
         */
        _started = false;
    }

    /**
     * Returns flag specifying whether the listener is stopped.
     *
     * @return boolean flag
     */
    protected boolean isStop() {
        synchronized(sync) {
            return _stop;
        }
    }

    public EmailListenerConfig getEmailListenerConfig() {
        return this.emailListenerCfg;
    }

    @Override
    public Goid getEmailListenerGoid() {
        return this.emailListenerCfg.getEmailListener().getGoid();
    }

    /**
     * Perform the processing on an email message.  This is the point where an implementation
     * of the PollingEmailListener would work in a synchronous / asynchronous manner.
     *
     * @param message the message to process
     */
    protected void handleMessage(MimeMessage message) {
        // create the EmailTask
        EmailTask task = newEmailTask(message);

        // fire-and-forget
        try {
            threadPoolBean.submitTask(task);
        } catch (RejectedExecutionException reject) {
            _logger.log(Level.WARNING, "Email listener ThreadPool size limit reached.  Unable to add new EmailTask: {0}", new String[] {ExceptionUtils.getMessage(reject)});
            throw new RuntimeException(reject);
        } catch (ThreadPool.ThreadPoolShutDownException e) {
            _logger.log(Level.WARNING, "Cannot submit EmailTask to queue as it has been shutdown", ExceptionUtils.getDebugException(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an email task for a request message.  To be sent to the threadPoolBean for processing.
     *
     * @param message the message for the task to run
     * @return new EmailTask instance for the given request
     */
    protected EmailTask newEmailTask(MimeMessage message) {
        // create the work task
        return new EmailTask(getEmailListenerConfig(), message);
    }

    /**
     * Listener thread responsible for receiving messages from the Email listener.
     */
    private class ListenerThread implements Runnable {
        /**
         * Default constructor.
         */
        private ListenerThread() {
            _thread = new Thread(this, toString());
            _thread.setDaemon(true);
        }

        /**
         *
         */
        @Override
        public final void run() {
            int oopses = 0;
            log(Level.INFO, EmailMessages.INFO_LISTENER_POLLING_START, emailListenerCfg.getEmailListener().getName());

            try {
                MimeMessage message;
                while ( !isStop() ) {
                    Message messages[];
                    long startTime = System.currentTimeMillis();
                    int lastMessageId = 0;

                    try {
                        PooledPollingEmailListenerImpl.this.ensureConnectionStarted();

                        messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                       emailListenerManager.updateLastPolled(emailListenerCfg.getEmailListener().getGoid());
                        long minMessageId = emailListenerCfg.getEmailListener().getEmailListenerState().getLastMessageId() == null ? 0L : emailListenerCfg.getEmailListener().getEmailListenerState().getLastMessageId();
                        for(Message m : messages) {
                            try {
                                message = (MimeMessage)m;

                                if( (long) message.getMessageNumber() <= minMessageId) {
                                    continue; // Skip this message since it should have already been seen
                                }

                                if(message.getMessageNumber() > lastMessageId) {
                                    lastMessageId = message.getMessageNumber();
                                }

                                if ( !isStop() ) {
                                    log(Level.FINE, EmailMessages.INFO_LISTENER_RECEIVE_MSG, emailListenerCfg.getEmailListener().getName());
                                    oopses = 0;

                                    MimeMessage messageToProcess = new MimeMessage(message);

                                    // process on the message
                                    handleMessage(messageToProcess);
                                }
                            } catch(Throwable e) {
                                log(Level.WARNING, EmailMessages.WARN_LISTENER_FAILED_TO_PROCESS, new Object[] {m.getMessageNumber(), emailListenerCfg.getEmailListener().getName()});
                            }
                        }

                        if(messages.length > 0) {
                            try {
                                Flags flags = new Flags(Flags.Flag.SEEN);
                                if(emailListenerCfg.getEmailListener().isDeleteOnReceive()) {
                                    flags.add(Flags.Flag.DELETED);
                                }

                                emailFolder.setFlags(messages, flags, true);

                                if(emailListenerCfg.getEmailListener().isDeleteOnReceive()) {
                                    emailFolder.close(true);
                                }
                            } catch(MessagingException e) {
                                log(Level.WARNING, EmailMessages.WARN_LISTENER_FAILED_MARK_READ, emailListenerCfg.getEmailListener().getName());
                            } catch(Throwable th) {
                                log(Level.SEVERE, "Failed to update messages for {0}", emailListenerCfg.getEmailListener().getName());
                            }
                        }

                        // Update the last polling time and the last message id
                        emailListenerCfg.getEmailListener().getEmailListenerState().setLastPollTime(System.currentTimeMillis());
                        if(emailListenerCfg.getEmailListener().isDeleteOnReceive()) { // Message IDs can change on expunge
                            emailListenerCfg.getEmailListener().getEmailListenerState().setLastMessageId(0L);
                        } else if(lastMessageId > 0) { // At least one message was found
                            emailListenerCfg.getEmailListener().getEmailListenerState().setLastMessageId((long)lastMessageId);
                        }
                        emailListenerManager.updateState(emailListenerCfg.getEmailListener().getEmailListenerState());

                        long now = System.currentTimeMillis();
                        if(now - startTime < (long) (emailListenerCfg.getEmailListener().getPollInterval() * 1000) ) {
                            Thread.sleep( (long) (emailListenerCfg.getEmailListener().getPollInterval() * 1000) - (now - startTime));
                        }
                    } catch ( Throwable e ) {
                        if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                            continue;
                        }

                        log(Level.WARNING, formatMessage(
                                EmailMessages.WARN_LISTENER_RECEIVE_ERROR,
                                emailListenerCfg.getEmailListener().getName()
                        ));

                        cleanup();

                        if ( ++oopses < MAXIMUM_OOPSES ) {
                            // sleep for a short period of time before retrying
                            try {
                                Thread.sleep( (long) OOPS_RETRY );
                            } catch ( InterruptedException e1 ) {
                                log(Level.INFO, EmailMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[] {"retry interval"});
                            }
                        } else {
                            // max oops reached .. sleep for a longer period of time before retrying
                            int sleepTime = emailListenerCfg.getEmailListener().getPollInterval() * 1000;

                            log(Level.WARNING, EmailMessages.WARN_LISTENER_MAX_OOPS_REACHED, new Object[] {emailListenerCfg.getEmailListener().getName(), MAXIMUM_OOPSES, sleepTime });

                            try {
                                Thread.sleep( (long) sleepTime );
                            } catch ( InterruptedException e1 ) {
                                log(Level.INFO, EmailMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[] {"sleep interval"});
                            }
                        }
                    }

                    if(emailFolder != null && emailFolder.isOpen()) {
                        try {
                            emailFolder.close(emailListenerCfg.getEmailListener().isDeleteOnReceive());
                            messageStore.close();
                        } catch(MessagingException me) {
                            log(Level.FINE, "Failed to close the email folder and connection for {0}.", emailListenerCfg.getEmailListener().getName());
                        }
                    }
                }
            } finally {
                log(Level.INFO, EmailMessages.INFO_LISTENER_POLLING_STOP, emailListenerCfg.getEmailListener().getName());
                cleanup();
            }
        }
    }

    protected void fireConnected() {
        lastAuditErrorTime = 0L;
        fireEvent(new EmailEvent(this, Level.INFO, null,
                formatMessage(EmailMessages.INFO_EVENT_CONNECT_SUCCESS, emailListenerCfg.getEmailListener().getName())));
    }

    protected void fireConnectError(String message) {
        fireEvent(new EmailEvent(this, Level.WARNING,  null, formatMessage(
                        EmailMessages.INFO_EVENT_CONNECT_FAIL,
                        new Object[] {emailListenerCfg.getEmailListener().getName(), message}))
        );
    }

    /**
     * Fires an email event.
     *
     * @param event the email event to fire
     */
    protected void fireEvent(EmailEvent event) {
        if (emailListenerCfg.getApplicationContext() != null) {
            long timeNow = System.currentTimeMillis();

            if ((lastAuditErrorTime + (long) OOPS_AUDIT) < timeNow) {
                lastAuditErrorTime = timeNow;
                emailListenerCfg.getApplicationContext().publishEvent(event);

            } else {
                log(Level.INFO, EmailMessages.INFO_EVENT_NOT_PUBLISHED, new Object[0]);
            }

        } else {
            log(Level.WARNING, EmailMessages.INFO_EVENT_NOT_PUBLISHABLE, event.getMessage());
        }
    }

    protected void log(Level level, String jmsMessageKey, Object parm) {
        if (parm == null)
            log(level, jmsMessageKey, new Object[0]);
        else
            log(level, jmsMessageKey, new Object[] {parm});
    }

    protected void log(Level level, String jmsMessageKey, Object[] parm) {
        _logger.log(level, jmsMessageKey, parm);
    }

    protected void log(Level level, String jmsMessageKey) {
        _logger.log(level, jmsMessageKey);
    }

    protected void log(Level level, String jmsMessageKey, Throwable ex) {
        _logger.log(level, jmsMessageKey, ex);
    }

    protected String formatMessage(String messageKey, Object[] parm) {

        return java.text.MessageFormat.format(messageKey, parm);
    }

    protected String formatMessage(String messageKey, Object parm) {

        return formatMessage(messageKey, new Object[] {parm});
    }
}
