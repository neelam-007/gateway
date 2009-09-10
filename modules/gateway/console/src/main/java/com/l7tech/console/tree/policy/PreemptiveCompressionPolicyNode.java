package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.console.action.PreemptiveCompressionAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * the policy node
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 3, 2008<br/>
 */
public class PreemptiveCompressionPolicyNode extends LeafAssertionTreeNode<PreemptiveCompression> {
    public PreemptiveCompressionPolicyNode(PreemptiveCompression assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName(final boolean decorate) {
        return "XML VPN Client Will Compress Requests";
    }

    /*public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[]{});
    }*/

    public Action getPreferredAction() {
        return new PreemptiveCompressionAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/authentication.gif";
    }
}
