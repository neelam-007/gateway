package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.xmppassertion.XMPPConstants;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 07/03/12
 * Time: 10:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPConnectionManagerAction extends SecureAction {
    public XMPPConnectionManagerAction() {
        super(new AttemptedAnyOperation(EntityType.GENERIC));
    }

    public String getName() {
        return XMPPConstants.ACTION_MENU_NAME;
    }

    protected String iconResource() {
        return XMPPConstants.ACTION_MENU_ICON_PATH;
    }

    @Override
    protected void performAction() {
        ManageXMPPConnectionEntitiesDialog dlg = new ManageXMPPConnectionEntitiesDialog(TopComponents.getInstance().getTopParent());
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
