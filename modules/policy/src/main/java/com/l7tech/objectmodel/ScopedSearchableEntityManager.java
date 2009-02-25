package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Map;

/**
 * Finds headers and/or entities that are childs of another entitiy (their scope).
 */
public interface ScopedSearchableEntityManager<HT extends EntityHeader> {

    // todo: document filter criteria specification
    Collection<HT> findHeadersInScope(int offset, int windowSize, EntityHeader scope, Map<String,String> filters) throws FindException;

}