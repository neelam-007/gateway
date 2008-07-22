package com.l7tech.console.action;

import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.AttemptedReadAny;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.console.ClusterStatusWindow;

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
        super(new AttemptedReadAny(EntityType.CLUSTER_INFO));
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Gateway (Cluster) Status";
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
        return "com/l7tech/console/resources/ViewClusterStatus16x16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        ClusterStatusWindow csw = getClusterStatusWindow();
        csw.setVisible(true);
        csw.setState(Frame.NORMAL);
        csw.toFront();
    }


    private ClusterStatusWindow getClusterStatusWindow() {
        if (clusterStatusWindow != null) return clusterStatusWindow;

        clusterStatusWindow = new ClusterStatusWindow("SecureSpan Manager - Gateway Cluster Status");
        clusterStatusWindow.addWindowListener(new WindowAdapter() {
            public void windowClosed(final WindowEvent e) {
                clusterStatusWindow = null;
            }

            public void windowClosing(final WindowEvent e) {
                clusterStatusWindow = null;
            }
        });
        clusterStatusWindow.pack();
        Utilities.centerOnScreen(clusterStatusWindow);
        return clusterStatusWindow;
    }

    public void onLogon(LogonEvent e) {
        super.onLogon(e);
        ClusterStatusWindow csw = clusterStatusWindow;
        if (csw != null) {
            csw.onLogon(e);
        }
    }

    public void onLogoff(LogonEvent e) {
        super.onLogoff(e);
        ClusterStatusWindow csw = clusterStatusWindow;
        if (csw != null) {
            csw.onLogoff(e);
        }
    }
}
