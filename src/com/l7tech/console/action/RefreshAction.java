package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;

import java.util.logging.Logger;


/**
 * The <code>RefreshAction</code> action is the generic refresh
 * node template action.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class RefreshAction extends NodeAction {
    static final Logger log = Logger.getLogger(RefreshAction.class.getName());

    /**
     * construct the node refresh action
     *
     * @param node the node to refresh
     */
    public RefreshAction(AbstractTreeNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Refresh";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Refresh Web Services";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Refresh16.gif";
    }
}
