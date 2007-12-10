package com.l7tech.console.logging;

import com.l7tech.admin.GatewayRuntimeException;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;

import java.awt.*;
import java.util.logging.Level;

/**
 * @author: ghuang
 */
public class GatewayFailureErrorHandler implements ErrorHandler {
    /**
     * handle the error event
     *
     * @param event the error event
     */
    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        final Throwable throwable = event.getThrowable();
        GatewayRuntimeException ex = ExceptionUtils.getCauseIfCausedBy(throwable, GatewayRuntimeException.class);

        if (ex != null) {
            // disconnect manager
            event.getLogger().log(Level.WARNING, "Disconnecting from gateway, notifiying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();

            // display error dialog
            if (topParent != null) {
                topParent.repaint();
            }
            DialogDisplayer.showMessageDialog(topParent, null, ex.getMessage(), null);
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}
