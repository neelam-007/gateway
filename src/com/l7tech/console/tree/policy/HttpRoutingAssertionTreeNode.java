package com.l7tech.console.tree.policy;


import com.l7tech.console.action.HttpRoutingAssertionPropertiesAction;
import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class SpecificUserAssertionTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class HttpRoutingAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpRoutingAssertionTreeNode(RoutingAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String url = ((HttpRoutingAssertion)getUserObject()).getProtectedServiceUrl();
        if (url != null) {
            return "Route to " + url;
        }
        return "";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        EditKeyAliasForAssertion privateKeyAction = new EditKeyAliasForAssertion(this);
        boolean usesPrivateKey = isUsingPrivateKey();
        if (!usesPrivateKey) {
            privateKeyAction.setEnabled(false);
            privateKeyAction.putValue(Action.SHORT_DESCRIPTION, "Disabled because the URL is not HTTPS");
        }

        java.util.List<Action> list = new ArrayList<Action>();
        Action a = new HttpRoutingAssertionPropertiesAction(this);
        list.add(a);
        list.add(privateKeyAction);
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[0]);
    }

    private boolean isUsingPrivateKey() {
        HttpRoutingAssertion assertion = ((HttpRoutingAssertion)getUserObject());
        if (assertion == null)
            return false;
        String url = assertion.getProtectedServiceUrl();
        return url != null && url.toLowerCase().startsWith("https:");
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new HttpRoutingAssertionPropertiesAction(this);
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/server16.gif";
    }

}