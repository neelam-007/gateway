package com.l7tech.console.action;

import com.l7tech.console.panels.LogChooserWindow;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.AttemptedOther;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Action to load saved audit or log records.
 */
public class ViewLogsAction extends SecureAction {

    private LogChooserWindow chooserWindow;

    public ViewLogsAction() {
        super(new AttemptedOther(EntityType.LOG_SINK, OtherOperationName.LOG_VIEWER.getOperationName()));
        Registry.getDefault().getLicenseManager().addLicenseListener(this);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "View Logs";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "View log events";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/AnalyzeGatewayLog16x16.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        LogChooserWindow cw = chooserWindow;

        if (cw == null) {
            cw = new LogChooserWindow();
            cw.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(final WindowEvent e) {
                    chooserWindow = null;
                }

                @Override
                public void windowClosing(final WindowEvent e) {
                    chooserWindow = null;
                }
            });
            cw.pack();
            Utilities.centerOnScreen(cw);
            cw.setVisible(true);
            chooserWindow = cw;
        }
        else {
            // ensure not minimized
            cw.setState(Frame.NORMAL);
            cw.toFront();
        }
    }
}
