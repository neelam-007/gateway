package com.l7tech.console.tree.policy;

import com.l7tech.console.action.XslTransformationPropertiesAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.XslTransformation;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Policy tree node for XSL Transformation Assertion.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 11, 2004<br/>
 * $Id$<br/>
 */
public class XslTransformationTreeNode extends LeafAssertionTreeNode {
    public XslTransformationTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof XslTransformation) {
            nodeAssertion = (XslTransformation)assertion;
        } else
            throw new IllegalArgumentException("assertion passed must be of type " +
              XslTransformation.class.getName());
    }

    public String getName() {
        if (nodeAssertion != null) {
            String nodeName = "XSL transform ";
            if (nodeAssertion.getDirection() == XslTransformation.APPLY_TO_REQUEST) nodeName += "request messages";
            else if (nodeAssertion.getDirection() == XslTransformation.APPLY_TO_RESPONSE) nodeName += "response messages";
            return nodeName;
        } else return "XSL Transform";
    }

    protected String iconResource(boolean open) {
        // todo, a special icon for this assertion?
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(new XslTransformationPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new XslTransformationPropertiesAction(this);
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    public XslTransformation getAssertion() {return nodeAssertion;}

    private XslTransformation nodeAssertion;
}
