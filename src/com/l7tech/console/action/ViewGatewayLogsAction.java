package com.l7tech.console.action;

import com.l7tech.identity.Group;
import com.l7tech.console.GatewayLogWindow;
import com.l7tech.console.security.LogonEvent;
import com.l7tech.common.gui.util.Utilities;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;


/**
 * The <code>ViewGatewayLogsAction</code> invokes the log browser.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ViewGatewayLogsAction extends SecureAction {
    private GatewayLogWindow gatewayLogWindow;

    public ViewGatewayLogsAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Analyze Gateway Log";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View the Gateway cluster status and Web service statistics log";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/AnalyzeGatewayLog16x16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        getGatewayLogWindow().show();
        getGatewayLogWindow().setState(Frame.NORMAL);
    }

     /**
     * Return the required roles for this action, one of the roles. The base
     * implementatoinm requires the strongest admin role.
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME};
    }


    private GatewayLogWindow getGatewayLogWindow() {
        if (gatewayLogWindow != null) return gatewayLogWindow;

        gatewayLogWindow = new GatewayLogWindow("SecureSpan Manager - Gateway Log");
        gatewayLogWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                destroyGatewayLogWindow();
            }

            public void windowClosed(final WindowEvent e) {
                destroyGatewayLogWindow();
            }
        });

        Utilities.centerOnScreen(gatewayLogWindow);
        return gatewayLogWindow;
    }

    private void destroyGatewayLogWindow() {
        if (gatewayLogWindow == null)
            return;
        gatewayLogWindow.dispose();
        gatewayLogWindow = null;
    }

    public void onLogon(LogonEvent e) {
        super.onLogon(e);
        if (gatewayLogWindow == null)
              return;
          gatewayLogWindow.onLogon(e);
    }

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        if (gatewayLogWindow == null)
              return;
          gatewayLogWindow.onLogoff(e);
    }
}
