package com.l7tech.console.tree;

import com.l7tech.console.action.GroupPropertiesAction;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * The class represents a node element in the TreeModel.
 * It represents the group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class GroupNode extends EntityHeaderNode {
    /**
     * construct the <CODE>UserFolderNode</CODE> instance for
     * a given e.
     *
     * @param e the EntityHeader instance, must represent Group
     * @throws IllegalArgumentException thrown if the EntityHeader instance does not
     *                                  represent a group.
     */
    public GroupNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }


     /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
           java.util.List list = new ArrayList();
        list.add(new GroupPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));

        return (Action[]) list.toArray(new Action[]{});
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        GroupFolderNode parent = (GroupFolderNode)getParent();
        EntityHeader e = (EntityHeader)getUserObject();
        return new MemberOfGroup(parent.getProviderId(), e.getName());
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/group16.png";
    }
}
