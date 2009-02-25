package com.l7tech.objectmodel;

/**
 * Base interface for searchable entity managers.
 */
public interface SearchableEntityManager {

    // generic query / filter; each manager should match its value against what they consider relevant for each entity type
    public static final String DEFAULT_SEARCH_NAME = "<default>";
}
