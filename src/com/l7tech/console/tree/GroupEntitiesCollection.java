package com.l7tech.console.tree;

import com.l7tech.identity.GroupManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;
import java.util.Collections;

/**
 * Class UserEntitiesCollection.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class GroupEntitiesCollection implements EntitiesCollection {
    final GroupManager manager;
    private boolean exhausted = false;

    GroupEntitiesCollection(GroupManager um) {
        manager = um;
    }

    /**
     * @return Returns the collection of <code>EntityHeader</code> instances represnting
     *         the group elements
     * @throws RuntimeException thrown on error retrieving the user collection
     */
    public Collection getNextBatch() throws RuntimeException {
        if (exhausted) {
            return Collections.EMPTY_LIST;
        }
        try {
            exhausted = true;
            return manager.findAllHeaders();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
