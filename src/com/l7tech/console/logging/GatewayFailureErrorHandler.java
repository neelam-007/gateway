package com.l7tech.console.logging;

import com.l7tech.admin.GatewayRuntimeException;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;

import java.awt.*;
import java.util.logging.Level;

/**
 * ErrorHandler for gateway errors (server side errors)
 *
 * @author ghuang
 */
public class GatewayFailureErrorHandler implements ErrorHandler {

    /**
     * Handle the error event
     *
     * @param event the error event
     */
    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        final Throwable throwable = event.getThrowable();
        final GatewayRuntimeException ex = ExceptionUtils.getCauseIfCausedBy(throwable, GatewayRuntimeException.class);

        // If top parent is null then this handler is not relevant
        if (ex != null && topParent != null) {
            // disconnect manager
            event.getLogger().log(Level.WARNING, "Disconnecting from gateway, notifiying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();

            // display error dialog
            topParent.repaint();
            DialogDisplayer.showMessageDialog(topParent, null, ex.getMessage(), null);
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}
