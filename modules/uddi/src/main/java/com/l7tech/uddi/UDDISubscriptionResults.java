package com.l7tech.uddi;

import java.util.Collection;

/**
 * Interface for UDDI subscription results.
 */
public interface UDDISubscriptionResults {

    /**
     * Get the subscription key for the results.
     *
     * @return The subscription key
     */
    String getSubscriptionKey();

    /**
     * Get the start time for the results
     *
     * @return The start time.
     */
    long getStartTime();

    /**
     * Get the end time for the results.
     *
     * @return The end time.
     */
    long getEndTime();

    /**
     * Get the subscription results.
     *
     * @return the result collection.
     */
    Collection<Result> getResults();

    /**
     * Interface for an indivdual result.
     */
    interface Result {
        /**
         * Get the key for the entity.
         *
         * @return the entity key.
         */
        String getEntityKey();

        /**
         * Is the entity deleted.
         *
         * @return True if deleted
         */
        boolean isDeleted();
    }
}
