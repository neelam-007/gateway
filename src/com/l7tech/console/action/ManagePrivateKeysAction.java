package com.l7tech.console.action;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.console.panels.PrivateKeyManagerWindow;
import com.l7tech.console.util.TopComponents;

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
        super(new AttemptedAnyOperation(EntityType.SSG_KEY_ENTRY), KEYSTORE_FEATURESET_NAME);
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
     * Note: if the SSM encounters the server is unavailable,
     * then it is not allowed to show a private-key-manager window.
     */
    protected void performAction() {
        PrivateKeyManagerWindow pkmw = new PrivateKeyManagerWindow(TopComponents.getInstance().getTopParent());
        if (! pkmw.encounterServerUnavailable()) {
            pkmw.pack();
            Utilities.centerOnScreen(pkmw);
            DialogDisplayer.display(pkmw);
        }
    }
}
