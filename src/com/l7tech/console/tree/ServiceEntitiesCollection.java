package com.l7tech.console.tree;

import com.l7tech.service.ServiceAdmin;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;

/**
 * Class ServiceEntitiesCollection.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServiceEntitiesCollection implements EntitiesCollection {
    final ServiceAdmin manager;
    private boolean exhausted = false;

    ServiceEntitiesCollection(ServiceAdmin sm) {
        manager = sm;
    }

    /**
     * @return Returns the collection of <code>EntityHeader</code> instances
     * @throws RuntimeException thrown on error retrieving the user collection
     */
    public Collection getNextBatch() throws RuntimeException {
        if (exhausted) {
            return Collections.EMPTY_LIST;
        }
        try {
            exhausted = true;
            return Arrays.asList(manager.findAllPublishedServices());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
