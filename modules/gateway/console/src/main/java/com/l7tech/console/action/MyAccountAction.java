package com.l7tech.console.action;

import com.l7tech.console.panels.MyAccountDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;

public class MyAccountAction extends SecureAction {
    public MyAccountAction() {
        super(null);
    }

    @Override
    public String getName() {
        return "My Account";
    }

    @Override
    protected void performAction() {
        final MyAccountDialog dialog = new MyAccountDialog(TopComponents.getInstance().getTopParent(),
                Registry.getDefault().getSecurityProvider().getUser());
        dialog.pack();
        DialogDisplayer.display(dialog);
    }
}
