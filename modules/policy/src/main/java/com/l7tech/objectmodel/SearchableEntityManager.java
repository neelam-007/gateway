package com.l7tech.objectmodel;

import java.util.Collection;

/**
 *
 */
public interface SearchableEntityManager<ET extends Entity, HT extends EntityHeader> {

    Collection<HT> findHeaders(int offset, int windowSize, String filter) throws FindException;

}
