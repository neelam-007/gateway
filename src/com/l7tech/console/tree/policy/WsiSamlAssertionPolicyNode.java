package com.l7tech.console.tree.policy;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.*;

import com.l7tech.policy.assertion.WsiSamlAssertion;
import com.l7tech.console.action.WsiSamlAssertionPropertiesAction;

/**
 * Policy node for WSI-SAML Token Profile assertion.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiSamlAssertionPolicyNode extends LeafAssertionTreeNode {

    //- PUBLIC

    public WsiSamlAssertionPolicyNode(WsiSamlAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    public WsiSamlAssertion getAssertion() {
        return assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "WS-I SAML Compliance.";
    }

    /**
     * Test if the node can be deleted.
     *
     * @return always true
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new WsiSamlAssertionPropertiesAction(this);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        List list = new ArrayList();
        list.add(new WsiSamlAssertionPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[list.size()]);
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }

    //- PRIVATE

    private WsiSamlAssertion assertion;
}
