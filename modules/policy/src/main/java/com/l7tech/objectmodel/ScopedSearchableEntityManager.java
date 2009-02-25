package com.l7tech.objectmodel;

import java.util.Collection;
import java.util.Map;

/**
 * Finds headers and/or entities that are childs of another entities (their scope). The search criteria applies to the scope.
 */
public interface ScopedSearchableEntityManager<HT extends EntityHeader> {

    Collection<HT> findHeadersInScope(int offset, int windowSize, EntityHeader scope, Map<String,String> filters) throws FindException;

}