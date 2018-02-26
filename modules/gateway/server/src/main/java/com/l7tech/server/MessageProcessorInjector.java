package com.l7tech.server;

import com.l7tech.server.message.PolicyEnforcementContext;

public interface MessageProcessorInjector {
    /**
     * Executes pre service injections to inject into the given policy enforcement context
     *
     * @param context The policy enforcement context to inject into
     */
    void executePreServiceInjections(PolicyEnforcementContext context);

    /**
     * Executes posy service injections to inject into the given policy enforcement context
     *
     * @param context The policy enforcement context to inject into
     */
    void executePostServiceInjections(PolicyEnforcementContext context);
}
