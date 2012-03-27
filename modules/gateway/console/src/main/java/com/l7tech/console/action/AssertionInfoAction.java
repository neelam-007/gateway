package com.l7tech.console.action;

import com.l7tech.console.panels.AssertionInfoDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.Assertion;

/**
 * Displays information about an assertion in a dialog.
 */
public class AssertionInfoAction extends BaseAction {
    private final Assertion assertion;

    public AssertionInfoAction(final Assertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public String getName() {
        return "View Info";
    }

    @Override
    public String getDescription() {
        return "View Assertion Information";
    }

    @Override
    public String iconResource() {
        return "com/l7tech/console/resources/Info16.png";
    }

    @Override
    protected void performAction() {
        DialogDisplayer.display(new AssertionInfoDialog(TopComponents.getInstance().getTopParent(), assertion));
    }
}
