/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import org.springframework.context.ApplicationEvent;

/**
 * Event for policy cache notifications.
 *
 * @author steve
 */
public abstract class PolicyCacheEvent extends ApplicationEvent {

    //- PUBLIC

    /**
     * Get the policy that this event relates to.
     *
     * @return The policy (may be null for some event types [Invalid,Reset])
     */
    public Policy getPolicy() {
        return policy;
    }

    public static class Deleted extends PolicyCacheEvent {
        public Deleted( Object source, Policy policy ) {
            super( source, policy );
        }
    }

    public static class Updated extends PolicyCacheEvent {
        public Updated( Object source, Policy policy ) {
            super( source, policy );
        }
    }

    public static class Invalid extends PolicyCacheEvent {
        private final Goid policyId;
        private final Exception exception;

        public Invalid( Object source, Goid identifier, Exception exception ) {
            super( source, null );
            this.policyId = identifier;
            this.exception = exception;
        }

        public Goid getPolicyId() {
            return policyId;
        }

        public Exception getException() {
            return exception;
        }
    }

    public static class Started extends PolicyCacheEvent {
        public Started(final Object source) {
            super( source, null );
        }
    }

    public static class Reset extends PolicyCacheEvent {
        public Reset( Object source ) {
            super( source, null );
        }
    }

    //- PRIVATE

    private final Policy policy;

    private PolicyCacheEvent( final Object source,
                              final Policy policy ) {
        super( source );
        this.policy = policy;
    }
    
}