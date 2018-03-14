package com.l7tech.server.messageprocessor.injection;

import com.l7tech.server.message.PolicyEnforcementContext;

public interface MessageProcessorInjector {
    /**
     * Executes pre service injections to inject into the given policy enforcement context
     *
     * @param context The policy enforcement context to inject into
     * @return false to stop service processing. This will return an AssertionStatus of NONE. If you populated the response message that response will be returned to the requester. True to continue service processing
     */
    boolean executePreServiceInjections(PolicyEnforcementContext context);

    /**
     * Executes posy service injections to inject into the given policy enforcement context
     *
     * @param context The policy enforcement context to inject into
     * @return false to falsify service processing. True to continue
     */
    boolean executePostServiceInjections(PolicyEnforcementContext context);
}
