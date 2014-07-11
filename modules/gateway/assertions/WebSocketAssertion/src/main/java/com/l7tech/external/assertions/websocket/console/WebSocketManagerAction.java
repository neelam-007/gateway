package com.l7tech.external.assertions.websocket.console;

import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.gateway.common.security.rbac.AttemptedAnyOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/4/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketManagerAction extends SecureAction {
    public WebSocketManagerAction() {
        super(new AttemptedAnyOperation(EntityType.GENERIC));
    }

    @Override
    public String getName() {
        return WebSocketConstants.ACTION_MENU_NAME;
    }

    @Override
    protected String iconResource() {
        return WebSocketConstants.ACTION_MENU_ICON_PATH;
    }

    @Override
    protected void performAction() {
        ManageWebSocketConnectionsDialog dlg = new ManageWebSocketConnectionsDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg);
    }
}
