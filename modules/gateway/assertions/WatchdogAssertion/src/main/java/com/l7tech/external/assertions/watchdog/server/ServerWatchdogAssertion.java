package com.l7tech.external.assertions.watchdog.server;

import com.l7tech.external.assertions.watchdog.WatchdogAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.SyspropUtil;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the WatchdogAssertion.
 *
 * @see com.l7tech.external.assertions.watchdog.WatchdogAssertion
 */
public class ServerWatchdogAssertion extends AbstractServerAssertion<WatchdogAssertion> {
    private static final Logger logger = Logger.getLogger(ServerWatchdogAssertion.class.getName());

    private static final Timer timer = new Timer();

    private final WatchdogAssertion assertion;
    private final long milliseconds;
    private final boolean interruptRequest;
    private final boolean logStackTrace;

    public ServerWatchdogAssertion(WatchdogAssertion assertion) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.milliseconds = assertion.getMilliseconds();
        this.interruptRequest = SyspropUtil.getBoolean("com.l7tech.watchdog.interruptRequest", false);
        this.logStackTrace = SyspropUtil.getBoolean("com.l7tech.watchdog.logStackTrace", true);
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final AtomicBoolean timerExpired = new AtomicBoolean();
        final AtomicReference<StackTraceElement[]> stackTrace = new AtomicReference<StackTraceElement[]>();

        // TODO:  To work properly with situations like Concurrent All, or other places where work is handed off
        // to a thread pool and the original thread blocks, change the context to keep track of the thread that
        // currently owns it, and use that one when the watchdog goes off
        final Thread requestThread = Thread.currentThread();

        final TimerTask alarmTask = new TimerTask() {
            @Override
            public void run() {
                cancel();
                timerExpired.set(true);
                if (logStackTrace)
                    stackTrace.set(requestThread.getStackTrace());
                if (interruptRequest)
                    requestThread.interrupt();
            }
        };

        context.runOnClose(new Runnable() {
            @Override
            public void run() {
                alarmTask.cancel();
                if (!timerExpired.get()) {
                    return;
                }

                StringBuilder sb = new StringBuilder("Request watchdog timer expired after " + milliseconds + "ms for request " + context.getRequestId() + " on " + requestThread.toString());

                StackTraceElement[] stack = stackTrace.get();
                if (stack != null) {
                    sb.append("\n\nRequest stack at time of watchdog expiry: \n");
                    for (StackTraceElement element : stack) {
                        sb.append("  - ").append(element).append("\n");
                    }
                }

                logger.log(Level.WARNING, sb.toString());
            }
        });

        timer.schedule(alarmTask, assertion.getMilliseconds());
        return AssertionStatus.NONE;
    }
}
