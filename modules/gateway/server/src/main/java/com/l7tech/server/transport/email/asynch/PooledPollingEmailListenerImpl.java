package com.l7tech.server.transport.email.asynch;

import com.l7tech.server.event.system.EmailEvent;
import com.l7tech.server.transport.email.*;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.LifecycleException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;

import javax.mail.internet.MimeMessage;
import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.beans.PropertyChangeEvent;

/**
 * A PollingEmailListener that creates EmailTask's so that a thread pool can process the messages.
 */
public class PooledPollingEmailListenerImpl implements PollingEmailListener {
    private static final Logger _logger = Logger.getLogger(PooledPollingEmailListenerImpl.class.getName());

    private static final String SOCKET_FACTORY_CLASSNAME = SslClientSocketFactory.class.getName();

    /*
     * Is there a better place for these properties?
     */
    protected static final String PROPERTY_ERROR_SLEEP = "ioEmailListenerErrorSleep";
    protected static final String PROPERTY_MAX_SIZE = "ioEmailListenerMessageMaxBytes";
    protected static final int MAXIMUM_OOPSES = 5;
    protected static final long RECEIVE_TIMEOUT = 5 * 1000;
    protected static final long SHUTDOWN_TIMEOUT = 7 * 1000;
    protected static final int OOPS_RETRY = 5000; // Five seconds
    protected static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    protected static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    protected static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    protected static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 mins;
    public static final int DEFAULT_MAX_SIZE = 5242880;

    private EmailListenerConfig emailListenerCfg;

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
     */
    public PooledPollingEmailListenerImpl(final EmailListenerConfig emailListenerCfg, EmailListenerManager emailListenerManager) {
        this.emailListenerCfg = emailListenerCfg;
        this.emailListenerManager = emailListenerManager;

        if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.POP3) {
            Properties props = new Properties();
            props.setProperty("mail.pop3.connectiontimeout", "30000");
            props.setProperty("mail.pop3.timeout", "30000");
            if(emailListenerCfg.getEmailListener().isUseSsl()) {
                props.setProperty("mail.pop3s.socketFactory.fallback", "false");
                props.setProperty("mail.pop3s.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            }
            emailSession = Session.getInstance(props);
        } else if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.IMAP) {
            Properties props = new Properties();
            props.setProperty("mail.imap.connectiontimeout", "30000");
            props.setProperty("mail.imap.timeout", "30000");
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
                                                                         emailListenerCfg.getEmailListener().getPassword()));
                    } else if(emailListenerCfg.getEmailListener().getServerType() == EmailServerType.IMAP) {
                        messageStore = emailSession.getStore(new URLName(emailListenerCfg.getEmailListener().isUseSsl() ? "imaps" : "imap",
                                                                         emailListenerCfg.getEmailListener().getHost(),
                                                                         emailListenerCfg.getEmailListener().getPort(),
                                                                         null,
                                                                         emailListenerCfg.getEmailListener().getUsername(),
                                                                         emailListenerCfg.getEmailListener().getPassword()));
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
    public void ensureStopped() {
        long stopRequestedTime;

        synchronized(sync) {
            stop();
            stopRequestedTime = lastStopRequestedTime;
        }

        try {
            long waitTime = SHUTDOWN_TIMEOUT - (System.currentTimeMillis() - stopRequestedTime);
            if ( waitTime > 10 ) {
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

    public long getEmailListenerOid() {
        return this.emailListenerCfg.getEmailListener().getOid();
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
        EmailListenerThreadPool.getInstance().newTask(task);

        // how to handle errors, if any ???
    }

    /**
     * Creates an email task for a request message.  To be sent to the EmailListenerThreadPool for processing.
     *
     * @param message the message for the task to run
     * @return new EmailTask instance for the given request
     */
    protected EmailTask newEmailTask(MimeMessage message) {
        // create the work task
        return new EmailTask(getEmailListenerConfig(), message);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        if (PROPERTY_MAX_SIZE.equals(evt.getPropertyName())) {
            String stringValue = (String) evt.getNewValue();
            int newMaxSize = DEFAULT_MAX_SIZE;

            try {
                newMaxSize = Integer.parseInt( stringValue );
            } catch (NumberFormatException nfe) {
                _logger.log(Level.WARNING, "Ignoring invalid email message max size ''{0}'' (using default).", stringValue);
            }

            if ( newMaxSize < 0 ) {
                _logger.log(Level.WARNING, "Ignoring invalid email message max size ''{0}'' (using 0).", stringValue);
                newMaxSize = 0;
            }

            _logger.log(Level.CONFIG, "Updated email message max size to {0}.", newMaxSize);
            emailListenerCfg.setMessageMaxSize(newMaxSize);
        }

        /*
         * What about the thread pool cluster properties?
         */
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
        public final void run() {
            int oopses = 0;
            log(Level.INFO, EmailMessages.INFO_LISTENER_POLLING_START, emailListenerCfg.getEmailListener().getName());

            try {
                MimeMessage message = null;
                HashSet<Integer> messageIdsToSkip = new HashSet<Integer>();
                while ( !isStop() ) {
                    Message messages[] = null;
                    long startTime = System.currentTimeMillis();
                    int lastMessageId = 0;

                    try {
                        PooledPollingEmailListenerImpl.this.ensureConnectionStarted();

                        messages = emailFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                        emailListenerManager.updateLastPolled(emailListenerCfg.getEmailListener().getOid());
                        long minMessageId = emailListenerCfg.getEmailListener().getLastMessageId() == null ? 0 : emailListenerCfg.getEmailListener().getLastMessageId();
                        for(Message m : messages) {
                            try {
                                message = (MimeMessage)m;

                                if(message.getMessageNumber() <= minMessageId) {
                                    continue; // Skip this message since it should have already been seen
                                }

                                if(message.getMessageNumber() > lastMessageId) {
                                    lastMessageId = message.getMessageNumber();
                                }

                                if ( message != null && !isStop() ) {
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
                        emailListenerCfg.getEmailListener().setLastPollTime(System.currentTimeMillis());
                        if(emailListenerCfg.getEmailListener().isDeleteOnReceive()) { // Message IDs can change on expunge
                            emailListenerCfg.getEmailListener().setLastMessageId(new Long(0));
                        } else if(lastMessageId > 0) { // At least one message was found
                            emailListenerCfg.getEmailListener().setLastMessageId(new Long(lastMessageId));
                        }
                        emailListenerManager.update(emailListenerCfg.getEmailListener());

                        long now = System.currentTimeMillis();
                        if(now - startTime < emailListenerCfg.getEmailListener().getPollInterval() * 1000) {
                            Thread.sleep(emailListenerCfg.getEmailListener().getPollInterval() * 1000 - (now - startTime));
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
                                Thread.sleep(OOPS_RETRY);
                            } catch ( InterruptedException e1 ) {
                                log(Level.INFO, EmailMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[] {"retry interval"});
                            }
                        } else {
                            // max oops reached .. sleep for a longer period of time before retrying
                            int sleepTime = emailListenerCfg.getEmailListener().getPollInterval() * 1000;

                            log(Level.WARNING, EmailMessages.WARN_LISTENER_MAX_OOPS_REACHED, new Object[] {emailListenerCfg.getEmailListener().getName(), MAXIMUM_OOPSES, sleepTime });

                            try {
                                Thread.sleep(sleepTime);
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

            if ((lastAuditErrorTime + OOPS_AUDIT) < timeNow) {
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
