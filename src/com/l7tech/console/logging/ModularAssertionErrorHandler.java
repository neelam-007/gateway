package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.policy.ConsoleAssertionRegistry;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;

/**
 * Handles errors that appear to be caused by modular assertions being unloaded.
 */
public class ModularAssertionErrorHandler implements ErrorHandler {
    public void handle(ErrorEvent e) {
        if (!handled(e))
            e.handle();
    }

    private boolean handled(ErrorEvent e) {
        Throwable t = e.getThrowable();

        ConsoleAssertionRegistry assreg = TopComponents.getInstance().getAssertionRegistry();
        if (assreg == null || !assreg.isAnyModularAssertionRegistered())
            return false;

        NoClassDefFoundError ncdfe = (NoClassDefFoundError)ExceptionUtils.getCauseIfCausedBy(t, NoClassDefFoundError.class);
        if (ncdfe != null) {
            String path = ncdfe.getMessage();

            final String message;
            String modName = assreg.getModuleNameMatchingPackage(path);
            if (modName != null) {
                message = "Unable to load component.  It may be that module " + modName + " is incomplete or was recently unloaded on the Gateway.";
            } else {
                message = "Unable to load component.  It may be from a module which was recently unloaded on the Gateway.";
            }

            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null, message, t);
            return true;
        }

        return false;
    }
}
