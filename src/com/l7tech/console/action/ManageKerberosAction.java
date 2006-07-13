package com.l7tech.console.action;

import com.l7tech.console.panels.KerberosDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;

import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 * The <code>ManageKerberosAction</code>
 *
 * @author $Author$
 * @version $Revision$
 */
public class ManageKerberosAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManageKerberosAction.class.getName());

    /**
     * create the aciton that disables the service
     */
    public ManageKerberosAction() {
        super(true, RequestWssKerberos.class);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Manage Kerberos Configuration";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "View Kerberos Configuration";
    }

    /**
     *
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        try {
            new KerberosDialog(TopComponents.getInstance().getMainWindow()).setVisible(true);
        }
        catch(RemoteException re) {
            throw new RuntimeException("Cannot create kerberos dialog.", re);
        }
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
