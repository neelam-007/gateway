package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.GatewayAuditWindow;
import com.l7tech.console.security.LogonEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.logging.GenericLogAdmin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collection;


/**
 * The <code>ViewGatewayAuditsAction</code> invokes the audit viewer.
 *
 * @author mike
 */
public class ViewGatewayAuditsAction extends SecureAction {
    private GatewayAuditWindow gatewayAuditWindow;

    public ViewGatewayAuditsAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Analyze Gateway Audit Events";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View Gateway audit events";
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
        getGatewayAuditWindow().show();
        getGatewayAuditWindow().setState(Frame.NORMAL);
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


    private GatewayAuditWindow getGatewayAuditWindow() {
        if (gatewayAuditWindow != null) return gatewayAuditWindow;


        gatewayAuditWindow = new GatewayAuditWindow();


        gatewayAuditWindow.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                destroyGatewayLogWindow();
            }

            public void windowClosed(final WindowEvent e) {
                destroyGatewayLogWindow();
            }
        });

        Utilities.centerOnScreen(gatewayAuditWindow);
        return gatewayAuditWindow;
    }

    private void destroyGatewayLogWindow() {
        if (gatewayAuditWindow == null)
            return;
        gatewayAuditWindow.dispose();
        gatewayAuditWindow = null;
    }

    public void onLogon(LogonEvent e) {
        super.onLogon(e);
        if (gatewayAuditWindow == null)
              return;
          gatewayAuditWindow.onLogon(e);
    }

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        if (gatewayAuditWindow == null)
              return;
          gatewayAuditWindow.onLogoff(e);
    }

}
