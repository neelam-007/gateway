package com.l7tech.console.action;

/**
 * An action is to expand assertion node(s).
 *
 * @author ghuang
 */
public class ExpandAssertionAction extends ExpandOrCollapseAssertionAction {
    public ExpandAssertionAction() {
        super(true);
    }

    @Override
    public String getName() {
        return "Expand Assertion";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/ExpandAll.gif";
    }
}
