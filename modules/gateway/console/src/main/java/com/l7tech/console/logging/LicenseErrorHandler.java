package com.l7tech.console.logging;

import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;

import java.awt.Frame;

/**
 * Handle exception of license checking failed.
 *
 * @author ghuang
 * @author jwilliams
 */
public class LicenseErrorHandler implements ErrorHandler {
    /**
     * handle the error event
     * @param event the error event
     */
    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        Throwable throwable = event.getThrowable();

        LicenseRuntimeException exception = ExceptionUtils.getCauseIfCausedBy(throwable, LicenseRuntimeException.class);

        if (null != exception) {
            ConsoleLicenseManager consoleLicenseManager = Registry.getDefault().getLicenseManager();

            // if there is no primary license installed tell the user, otherwise show them the problem details
            if (!consoleLicenseManager.isPrimaryLicenseInstalled()) {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "License Missing", "A valid primary license has not been installed.", null);
            } else {
                DialogDisplayer.showMessageDialog(topParent, null, throwable.getMessage(), throwable);
            }
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}