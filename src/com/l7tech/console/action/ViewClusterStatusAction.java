package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.ClusterStatusWindow;
import com.l7tech.console.security.LogonEvent;
import com.l7tech.identity.Group;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * The <code>ViewGatewayLogsAction</code> invokes the dcluster status
 * frame.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ViewClusterStatusAction extends SecureAction {
    private ClusterStatusWindow clusterStatusWindow;

    public ViewClusterStatusAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "View Cluster Status";
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
        return "com/l7tech/console/resources/ClusterServers.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        getClusterStatusWindow().show();
        getClusterStatusWindow().setState(Frame.NORMAL);
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


    private ClusterStatusWindow getClusterStatusWindow() {
        if (clusterStatusWindow != null) return clusterStatusWindow;

        clusterStatusWindow = new ClusterStatusWindow("SecureSpan Manager - Gateway Cluster Status");
        clusterStatusWindow.addWindowListener(new WindowAdapter() {
            public void windowClosed(final WindowEvent e) {
                clusterStatusWindow = null;
            }

            public void windowClosing(final WindowEvent e) {
                clusterStatusWindow.dispose();
                clusterStatusWindow = null;
            }
        });
        Utilities.centerOnScreen(clusterStatusWindow);
        return clusterStatusWindow;
    }

    public void onLogon(LogonEvent e) {
        super.onLogon(e);
        if (clusterStatusWindow == null)
              return;
          clusterStatusWindow.onLogon(e);
    }

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        if (clusterStatusWindow == null)
              return;
          clusterStatusWindow.onLogoff(e);
    }
}
