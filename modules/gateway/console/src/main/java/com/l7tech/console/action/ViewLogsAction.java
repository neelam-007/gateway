package com.l7tech.console.action;

import com.l7tech.console.panels.LogChooserWindow;
import com.l7tech.gateway.common.security.rbac.AttemptedOther;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Action to load saved audit or log records.
 */
public class ViewLogsAction extends SecureAction {

    public ViewLogsAction() {
        super(new AttemptedOther(EntityType.LOG_SINK, OtherOperationName.LOG_VIEWER.toString()));
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "View Logs";
    }

    /**
     * @return the aciton description
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
        LogChooserWindow chooserWindow = new LogChooserWindow();
        chooserWindow.pack();
        Utilities.centerOnScreen(chooserWindow);
        chooserWindow.setVisible(true);
    }
}
