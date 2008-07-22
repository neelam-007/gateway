package com.l7tech.console.logging;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;

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
            // display error dialog
            DialogDisplayer.showMessageDialog(topParent, null, errorMsg, throwable);
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}