package com.l7tech.console.logging;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.ErrorMessageDialog;
import com.l7tech.admin.LicenseRuntimeException;

import java.util.logging.Level;
import java.awt.Frame;

/**
 * Handle exception of license checking failed.
 *
 * @author: ghuang
 */
public class LicenseErrorHandler implements ErrorHandler {
    private final String errorMsg = "Invalid license (check the license and try it again).";
    /**
     * handle the error event
     * @param event the error event
     */
    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        Throwable throwable = event.getThrowable();

        if (ExceptionUtils.causedBy(throwable, LicenseRuntimeException.class)) {
            // disconnect manager
            event.getLogger().log(Level.WARNING, "Disconnected from gateway, notifiying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();

            // display error dialog
            if (topParent != null) {
                topParent.repaint();
            }
            new ErrorMessageDialog(topParent, errorMsg, ExceptionUtils.unnestToRoot(throwable)).setVisible(true);
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}