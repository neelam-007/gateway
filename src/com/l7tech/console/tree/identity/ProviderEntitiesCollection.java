package com.l7tech.console.tree.identity;

import com.l7tech.console.tree.EntitiesCollection;
import com.l7tech.identity.IdentityProviderConfigManager;

import java.util.Collection;
import java.util.Collections;

/**
 * Class ProviderEntitiesCollection.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ProviderEntitiesCollection implements EntitiesCollection {
    final IdentityProviderConfigManager manager;
    private boolean exhausted = false;

    public ProviderEntitiesCollection(IdentityProviderConfigManager im) {
        manager = im;
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
            return manager.findAllHeaders();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
