/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.tree.policy;

import javax.swing.event.TreeModelEvent;

/**
 * Encapsulates information describing changes to a policy tree model, and
 * used to notify policy tree model listeners of the change. The event type indicates
 * that there was an policy assertion node change that consist of changed properties,
 * rather then default TreeModel node change (that causes redisplay).
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version Mar 9, 2004
 * @see PolicyTreeModel#assertionTreeNodeChanged(com.l7tech.console.tree.policy.AssertionTreeNode)
 */
public class PolicyTreeModelEvent extends TreeModelEvent {
    public PolicyTreeModelEvent(Object source, Object[] path, int[] childIndices, Object[] children) {
        super(source, path, childIndices, children);
    }
}