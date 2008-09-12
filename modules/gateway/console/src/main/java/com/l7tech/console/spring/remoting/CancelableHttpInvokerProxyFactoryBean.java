package com.l7tech.console.spring.remoting;

import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Spring remoting client that throws up a Cancel dialog if a blocking call is made on the Swing
 * event thread once it has waited more than a fraction of a second for an answer.
 */
public class CancelableHttpInvokerProxyFactoryBean extends HttpInvokerProxyFactoryBean {
    private static final Logger logger = Logger.getLogger(CancelableHttpInvokerProxyFactoryBean.class.getName());
    private static final String PROP_BASE = "com.l7tech.console.spring.remoting.";
    private static final boolean NO_CANCEL_DIALOGS = SyspropUtil.getBoolean(PROP_BASE + "suppressRemoteInvocationCancelDialog");
    private static final long MS_BEFORE_DLG = SyspropUtil.getLong(PROP_BASE + "remoteInvocationCancelDialogDelayMillis", 600L);

    private static class ThrowableWrapper extends Exception {
        private ThrowableWrapper(Throwable cause) {
            super(cause);
        }
    }

    public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        if (NO_CANCEL_DIALOGS || !SwingUtilities.isEventDispatchThread() || Utilities.isCurrentThreadDoingWithDelayedCancelDialog())
            return super.invoke(methodInvocation);

        // Running on Swing event thread, and no delayed cancel dialog already pending
        try {
            return CancelableOperationDialog.doWithDelayedCancelDialog(new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        return CancelableHttpInvokerProxyFactoryBean.super.invoke(methodInvocation);
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Throwable throwable) {
                        throw new ThrowableWrapper(throwable);
                    }
                }
            }, "Waiting for Server", "Waiting for response from Gateway...", MS_BEFORE_DLG);
        } catch (InterruptedException e) {
            String msg = "Remote invocation canceled by user: " + ExceptionUtils.getMessage(e);

            // We log and throw here because the exception is at risk of getting eaten without being
            // logged if the SSM is running as an untrusted applet and the runtime exception makes it all the
            // way back to the Swing event pump without being caught.
            logger.log(Level.WARNING, msg);
            throw new RemoteInvocationCanceledException(msg, e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getCause();
            if (target instanceof ThrowableWrapper) {
                ThrowableWrapper wrapper = (ThrowableWrapper) target;
                throw wrapper.getCause();
            } else if (target != null) {
                throw target;
            }
            throw e;
        }
    }
}
