package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.admin.AdminSessionValidationRuntimeException;
import com.l7tech.gui.util.DialogDisplayer;

import java.awt.*;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 28, 2008
 * Time: 4:15:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdminSessionValidationErrorHandler implements ErrorHandler{
    private final String errorMsg = "Session invalidated. Please contact your administrator";
    /**
     * handle the error event
     * @param event the error event
     */
    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        Throwable throwable = event.getThrowable();

        if (ExceptionUtils.causedBy(throwable, AdminSessionValidationRuntimeException.class)) {
            // display error dialog
            DialogDisplayer.showMessageDialog(topParent, null, errorMsg,null);
            Logger.getLogger("ValidationErrorHandler").log(Level.WARNING, "User session invalidated");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();
            
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}