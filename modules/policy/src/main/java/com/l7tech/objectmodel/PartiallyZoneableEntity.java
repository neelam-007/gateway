package com.l7tech.objectmodel;

/**
 * Interface implemented by entities that are zoneable in some configurations but not others.
 */
public interface PartiallyZoneableEntity extends ZoneableEntity {
    /**
     * Check whether a zone predicate is permitted to bind on the current entity
     * in its current configuration.
     * <p/>
     * If an entity is not in a zoneable state, any security zone should be
     * ignored and not used to permit permissions to be scoped to it by a zone predicate.
     *
     * @return true if this entity is zoneable in its current state.
     *         false if this entity is not currently zoneable and its security zone (if any) should be ignored by the ZonePredicate.
     */
    boolean isZoneable();
}
