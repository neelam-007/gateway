package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * [class_desc]
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 5, 2004<br/>
 * $Id$<br/>
 *
 */
public class SchemaValidationTreeNode extends LeafAssertionTreeNode {
    public SchemaValidationTreeNode(Assertion assertion) {
        super(assertion);
        if (assertion instanceof SchemaValidation) {
            originalAssertion = (SchemaValidation)assertion;
        } else throw new IllegalArgumentException("assertion passed must be of type " +
                                                   SchemaValidation.class.getName());

    }
    public String getName() {
        return "[Validate message's schema]";
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
        // todo plug in our custom action here
        //list.add(new SamlPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        // todo plug in our custom action here
        return null;
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    private SchemaValidation originalAssertion;
}
