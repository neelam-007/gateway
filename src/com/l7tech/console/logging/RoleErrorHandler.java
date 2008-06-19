/**
 * The class handles all kind of problems related to manage roles.  Handling NoRoleException is one of examples.
 */
package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.security.NoRoleException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.gui.util.DialogDisplayer;

import java.awt.*;
import java.util.logging.Level;

/**
 * @auther: ghuang
 */
public class RoleErrorHandler implements ErrorHandler {

    public void handle(ErrorEvent event) {
        final Frame topParent = TopComponents.getInstance().getTopParent();
        final Throwable throwable = ExceptionUtils.unnestToRoot(event.getThrowable());

        if (throwable instanceof NoRoleException) {
            event.getLogger().log(Level.WARNING, "Disconnected from gateway, notifiying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();

            DialogDisplayer.showMessageDialog(topParent, null, "You are logged out, since currently you do not have any roles.", null);
        } else {
            // pass to next handle in the handle chain
            event.handle();
        }
    }
}
