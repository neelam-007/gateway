package com.l7tech.console.tree.policy;

import javax.swing.event.TreeModelEvent;

/**
 * Encapsulates information describing changes to a policy tree model, and
 * used to notify policy tree model listeners of the change. The event type indicates
 * that there was an policy assertion node change that consist of changed properties,
 * rather then default TreeModel node change (that causes redisplay).
 *
 * @author Emil Marceta
 * @see PolicyTreeModel#assertionTreeNodeChanged(com.l7tech.console.tree.policy.AssertionTreeNode)
 */
public class PolicyTreeModelEvent extends TreeModelEvent {

    public PolicyTreeModelEvent( final Object source,
                                 final Object[] path,
                                 final int[] childIndices,
                                 final Object[] children ) {
        super( source,
               path, 
               childIndices==null ? new int[0] : childIndices,
               children==null ? new Object[0] : children );
    }
}