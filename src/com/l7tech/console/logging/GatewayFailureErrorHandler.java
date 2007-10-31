package com.l7tech.console.logging;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.ErrorMessageDialog;
import com.l7tech.admin.GatewayRuntimeException;

import java.awt.Frame;
import java.util.logging.Level;

/**
 * @author: ghuang
 */
public class GatewayFailureErrorHandler implements ErrorHandler {
    private final static String errorMessage = "An unexpected error occurred on the SecureSpan Gateway, " +
            "please contact your gateway administrator.";
    /**
     * handle the error event
     *
     * @param event the error event
     */
    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        final Throwable throwable = event.getThrowable();

        if (ExceptionUtils.causedBy(throwable, GatewayRuntimeException.class)) {
            // disconnect manager
            event.getLogger().log(Level.WARNING, "Disconnecting from gateway, notifiying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();

            // display error dialog
            if (topParent != null) {
                topParent.repaint();
            }
            new ErrorMessageDialog(topParent, errorMessage, throwable).setVisible(true);
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}
