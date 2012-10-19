package com.l7tech.console.action;

import com.l7tech.console.panels.PrivateKeyManagerWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

import java.util.logging.Logger;

/**
 * Action to invoke PrivateKeyManagerWindow.
 */
public class ManagePrivateKeysAction extends SecureAction {
    static final Logger log = Logger.getLogger(ManagePrivateKeysAction.class.getName());

    /**
     * create the aciton that disables the service
     */
    public ManagePrivateKeysAction() {
        super(null, KEYSTORE_FEATURESET_NAME);
    }

    @Override
    public boolean isAuthorized() {        
        return canAttemptOperation(new AttemptedAnyOperation(EntityType.SSG_KEY_ENTRY));
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Manage Private Keys";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View and manage private keys";
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
        PrivateKeyManagerWindow pkmw = new PrivateKeyManagerWindow(TopComponents.getInstance().getTopParent());
        pkmw.pack();
        Utilities.centerOnScreen(pkmw);
        DialogDisplayer.display(pkmw);
    }
}
