package com.l7tech.console.tree;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.EntityType;

import java.util.Collection;
import java.util.Collections;

/**
 * Class <code>IdentityEntitiesCollection</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityEntitiesCollection implements EntitiesCollection {
    final IdentityProvider identityProvider;
    private boolean exhausted = false;
    private EntityType[] searchTypes;

    IdentityEntitiesCollection(IdentityProvider ip, EntityType[] et) {
        identityProvider= ip;
        searchTypes = et;
    }

    /**
     * @return Returns the collection of <code>EntityHeader</code> instances that
     *         represent the
     * @throws RuntimeException thrown on error retrieving the user collection
     */
    public Collection getNextBatch() throws RuntimeException {
        if (exhausted) {
            return Collections.EMPTY_LIST;
        }
        try {
            exhausted = true;
            return identityProvider.search(searchTypes, "*");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
