package com.l7tech.server;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static com.l7tech.util.Pair.pair;
import static java.lang.ThreadLocal.withInitial;

/**
 * Tracks all Goids + Version for entities updated on a single thread during an update event.
 * <p>
 * An update event originates in {@link EntityVersionChecker} either as a result of an entity updated on the node or
 * via an update detected from the database.
 */
public class EntitiesProcessedInBatch {

    private final ThreadLocal<Set<Pair<Goid, Long>>> entityAndVersionProcessedInBatch = withInitial(new Supplier<Set<Pair<Goid, Long>>>() {
        @Override
        public Set<Pair<Goid, Long>> get() {
            return new HashSet<>();
        }
    });

    public boolean wasProcessed( Goid goid, Long version ) {
        return entityAndVersionProcessedInBatch.get().contains(pair(goid, version));

    }

    public void addProcessedEntity( Goid goid, Long version ) {
        entityAndVersionProcessedInBatch.get().add(pair(goid, version));
    }

    /**
     * Reset should be called before any tracking is done to ensure there is no tracking information remaining from a
     * previous usage on the same thread.
     * <p>
     * Also call after tracking is done to avoid keeping state longer than needed
     */
    public void reset() {
        entityAndVersionProcessedInBatch.get().clear();
    }


}
