package com.l7tech.objectmodel;

import java.util.Collection;

/**
 *
 */
public interface ScopedSearchableEntityManager<HT extends EntityHeader> {

    Collection<HT> findHeadersInScope(int offset, int windowSize, EntityHeader scope, String filter) throws FindException;

}