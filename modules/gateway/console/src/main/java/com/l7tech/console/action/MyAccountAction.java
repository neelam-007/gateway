package com.l7tech.console.action;

import com.l7tech.console.panels.MyAccountDialog;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gui.util.DialogDisplayer;

/**
 * View account info action.
 */
public class MyAccountAction extends BaseAction implements LogonListener {
    @Override
    public String getName() {
        return "My Account";
    }

    @Override
    protected void performAction() {
        if (Registry.getDefault().isAdminContextPresent() && Registry.getDefault().getSecurityProvider().getUser() != null) {
            final MyAccountDialog dialog = new MyAccountDialog(TopComponents.getInstance().getTopParent(),
                    Registry.getDefault().getSecurityProvider().getUser());
            dialog.pack();
            DialogDisplayer.display(dialog);
        }
    }

    @Override
    public void onLogon(final LogonEvent e) {
        setEnabled(true);
    }

    @Override
    public void onLogoff(final LogonEvent e) {
        setEnabled(false);
    }
}
