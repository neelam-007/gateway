package com.l7tech.server.message;

import com.l7tech.message.Message;

/**
 * Factory for PolicyEnforcementContexts
 */
public class PolicyEnforcementContextFactory {

    //- PUBLIC

    /**
     * Create a new PEC for the given messages.
     *
     * @param request The request message to use (optional, created if missing)
     * @param response  The response message to use (optional, created if missing)
     * @return The new PEC
     * @see #getCurrent
     */
    public static PolicyEnforcementContext createPolicyEnforcementContext( final Message request,
                                                                           final Message response ) {
        return createPolicyEnforcementContext( request, response, true );
    }

    /**
     * Create a new PEC for the given messages.
     *
     * @param request The request message to use (optional, created if missing)
     * @param response  The response message to use (optional, created if missing)
     * @param replyExpected True if a reply is expected for the context
     * @return The new PEC
     * @see #getCurrent
     */
    public static PolicyEnforcementContext createPolicyEnforcementContext( final Message request,
                                                                           final Message response,
                                                                           final boolean replyExpected ) {
        // Construct PEC
        final PolicyEnforcementContextImpl context = new PolicyEnforcementContextImpl( request, response );
        context.setReplyExpected( replyExpected );

        // Handle current instance tracking
        PolicyEnforcementContextFactory.instanceHolder.set(context);
        context.runOnClose( new Runnable(){
            @Override
            public void run() {
                PolicyEnforcementContextFactory.instanceHolder.set(null);
            }
        } );
        
        return context;
    }

    /**
     * Create a PEC that delegates to the given parent for messages and authentication.
     *
     * @param parent
     * @return
     */
    public static PolicyEnforcementContext createPolicyEnforcementContext( final PolicyEnforcementContext parent ) {
        final PolicyEnforcementContext context = new ChildPolicyEnforcementContext( parent, new PolicyEnforcementContextImpl(null, null) );

        // Handle current instance tracking
        PolicyEnforcementContextFactory.childInstanceHolder.set(context);
        context.runOnClose( new Runnable(){
            @Override
            public void run() {
                PolicyEnforcementContextFactory.childInstanceHolder.set(null);
            }
        } );

        return context;
    }

    /**
     * There is one PolicyEnforcementContext per request sent to the SSG.
     *
     * <p>This method allows the caller to retrieve the current PEC for the
     * thread which is this is being called from.</p>
     *
     * @return the last PEC constructed in the current thread, null if none
     */
    public static PolicyEnforcementContext getCurrent() {
        PolicyEnforcementContext current = childInstanceHolder.get();
        if ( current == null ) {
            current = instanceHolder.get();
        }
        return current;
    }

    //- PRIVATE

    public static ThreadLocal<PolicyEnforcementContext> instanceHolder = new ThreadLocal<PolicyEnforcementContext>();
    public static ThreadLocal<PolicyEnforcementContext> childInstanceHolder = new ThreadLocal<PolicyEnforcementContext>();

}
