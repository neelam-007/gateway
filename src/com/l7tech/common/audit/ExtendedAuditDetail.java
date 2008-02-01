package com.l7tech.common.audit;

/**
 * Interface for additional audit detail functionality.
 *
 * <p>Should only contain non-bean style methods.</p>
 *
 * @author steve
 */
public interface ExtendedAuditDetail {

    /**
     * Should this audit detail be persisted?
     *
     * @return true if this detail should be saved.
     */
    boolean shouldSave();
}
