package com.l7tech.server.message;

import com.l7tech.message.Message;
import com.l7tech.util.TimeSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

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
    public static PolicyEnforcementContext createPolicyEnforcementContext( @Nullable final Message request,
                                                                           @Nullable final Message response ) {
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
    public static PolicyEnforcementContext createPolicyEnforcementContext( @Nullable final Message request,
                                                                           @Nullable final Message response,
                                                                           final boolean replyExpected ) {
        // Construct PEC
        final PolicyEnforcementContext context =
                createUnregisteredPolicyEnforcementContext( request, response, replyExpected );

        return registerThreadLocal(context, instanceHolder);
    }

    /**
     * Create a new PEC for the given messages, without replacing any current thread-local PEC that is already
     * registered.
     *
     * @param request The request message to use (optional, created if missing)
     * @param response  The response message to use (optional, created if missing)
     * @param replyExpected True if a reply is expected for the context
     * @return The new unregistered PEC
     */
    public static PolicyEnforcementContext createUnregisteredPolicyEnforcementContext( @Nullable final Message request,
                                                                                       @Nullable final Message response,
                                                                                       final boolean replyExpected ) {
        // Construct PEC
        final PolicyEnforcementContextImpl context = new PolicyEnforcementContextImpl( request, response, timeSource);
        context.setReplyExpected( replyExpected );

        return context;
    }

    /**
     * Create a PEC that delegates to the given parent for messages and authentication.
     *
     * @param parent parent context.  Required.
     * @return the new child PEC.  Never null.
     */
    public static PolicyEnforcementContext createPolicyEnforcementContext( final PolicyEnforcementContext parent ) {
        final PolicyEnforcementContext context = createUnregisteredPolicyEnforcementContext(parent);

        return registerThreadLocal(context, childInstanceHolder);
    }

    /**
     * Create a PEC that delegates to the given parent for messages and authentication, without replacing any current
     * thread-local child or instance PEC that is already registered.
     *
     * @param parent parent context.  Required.
     * @return the new child PEC.  Never null.
     */
    public static PolicyEnforcementContext createUnregisteredPolicyEnforcementContext( final PolicyEnforcementContext parent ) {
        return new ChildPolicyEnforcementContext( parent, new PolicyEnforcementContextImpl(null, null, timeSource, parent.getRequestId()) );
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

    /**
     * Perform an action on the current thread with the specific context registered as the current
     * thread-local context, restoring any previous context before returning.
     *
     * @param context the context to use.  Required.
     * @param callable stuff to do while the thread-local context is set.
     * @return the result from the callable
     * @throws Exception if the callable throws an exception
     */
    public static <T> T doWithCurrentContext( PolicyEnforcementContext context, Callable<T> callable ) throws Exception {
        PolicyEnforcementContext prev = childInstanceHolder.get();
        try {
            childInstanceHolder.set(context);
            return callable.call();
        } finally {
            childInstanceHolder.set(prev);
        }
    }

    /**
     * Configure the TimeSource. Called via spring and reflection in test cases.
     * @param timeSource TimeSource to set
     */
    public static void setTimeSource(@NotNull TimeSource timeSource) {
        PolicyEnforcementContextFactory.timeSource = timeSource;
    }

    //- PRIVATE

    private static PolicyEnforcementContext registerThreadLocal( final PolicyEnforcementContext context, final ThreadLocal<PolicyEnforcementContext> threadLocal ) {
        // Handle current instance tracking
        threadLocal.set(context);
        context.runOnClose( new Runnable(){
            @Override
            public void run() {
                threadLocal.set(null);
            }
        } );
        return context;
    }

    /**
     * Has default value to support simple usages in test cases.
     */
    @NotNull
    private static TimeSource timeSource = new TimeSource();

    public static final ThreadLocal<PolicyEnforcementContext> instanceHolder = new ThreadLocal<PolicyEnforcementContext>();
    public static final ThreadLocal<PolicyEnforcementContext> childInstanceHolder = new ThreadLocal<PolicyEnforcementContext>();

}
