package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Map;

/**
 * Finds headers and/or entities and filters result based on criteria specified for their properties.
 */
public interface PropertySearchableEntityManager<HT extends EntityHeader> extends SearchableEntityManager {

    // todo: document filter criteria specification
    Collection<HT> findHeaders(int offset, int windowSize, Map<String,String> filters) throws FindException;

}
