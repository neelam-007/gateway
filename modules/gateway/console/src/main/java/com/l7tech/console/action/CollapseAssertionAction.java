package com.l7tech.console.action;

/**
 * An action is to collapse assertion node(s).
 *
 * @author ghuang
 */
public class CollapseAssertionAction extends ExpandOrCollapseAssertionAction {
    public CollapseAssertionAction() {
        super(false);
    }

    @Override
    public String getName() {
        return "Collapse Assertion";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/CollapseAll.gif";
    }
}
