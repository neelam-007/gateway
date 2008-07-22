package com.l7tech.console.action;

import com.l7tech.console.panels.KerberosDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;
import com.l7tech.gui.util.DialogDisplayer;

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
        super(null, RequestWssKerberos.class);
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
        DialogDisplayer.display(new KerberosDialog(TopComponents.getInstance().getTopParent()));
    }

}
