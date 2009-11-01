package com.l7tech.uddi;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Default implementation of UDDISubscriptionResult
 */
class UDDISubscriptionResultsImpl implements UDDISubscriptionResults {

    //- PUBLIC

    @Override
    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public Collection<Result> getResults() {
        return Collections.unmodifiableCollection( results );
    }

    //- PACKAGE

    UDDISubscriptionResultsImpl( final String subscriptionKey,
                                 final long startTime,
                                 final long endTime ) {
        this.subscriptionKey = subscriptionKey;
        this.startTime = startTime;
        this.endTime = endTime;
        this.results = new ArrayList<Result>();
    }

    void add( final Result result ) {
        results.add( result );
    }

    static final class ResultImpl implements Result {
        private String entityKey;
        private boolean deleted;

        ResultImpl( final String entityKey,
                    final boolean deleted ) {
            this.entityKey = entityKey;
            this.deleted = deleted;
        }

        @Override
        public String getEntityKey() {
            return entityKey;
        }

        @Override
        public boolean isDeleted() {
            return deleted;
        }

    }

    //- PRIVATE

    private final String subscriptionKey;
    private final long startTime;
    private final long endTime;
    private final Collection<Result> results;
}
