package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.CertManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;

import java.util.logging.Logger;


/**
 * The <code>ManageCertificatesAction</code>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ManageCertificatesAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageCertificatesAction.class.getName());

    /**
     * create the aciton that disables the service
     */
    public ManageCertificatesAction() {
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Manage Certificates";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and manage certificates";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/cert16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        CertManagerWindow cmw = new CertManagerWindow(TopComponents.getInstance().getMainWindow());
        Utilities.centerOnScreen(cmw);
        cmw.show();
        cmw.dispose();
    }

    /**
     * Return the required roles for this action, one of the roles. The base
     * implementation requires the strongest admin role.
     *
     * @return the list of roles that are allowed to carry out the action
     */
    protected String[] requiredRoles() {
        return new String[]{Group.ADMIN_GROUP_NAME, Group.OPERATOR_GROUP_NAME};
    }
}
