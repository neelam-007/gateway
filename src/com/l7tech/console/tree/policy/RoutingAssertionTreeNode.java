package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.RoutingAssertion;

/**
 * Class SpecificUserAssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class RoutingAssertionTreeNode extends LeafAssertionTreeNode {

    public RoutingAssertionTreeNode(RoutingAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        String url = ((RoutingAssertion)getUserObject()).getProtectedServiceUrl();
        if (url != null) {
            return "Route to "+url;
        }
        return "default service route";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/routing.gif";
    }
}