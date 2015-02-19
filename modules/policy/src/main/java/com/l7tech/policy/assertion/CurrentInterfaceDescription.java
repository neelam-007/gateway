package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;

import java.util.concurrent.Callable;

/**
 * Holds a thread local interface description, in the form of a (possibly ephemeral) @{link EncapsulatedAssertionConfig}
 * instance.  Used during policy validation to provide information about additional variables which should be considered
 * to "magically" exist before the start of policy, and to "magically" be used after the end of the policy.
 */
public class CurrentInterfaceDescription {
    /**
     * Run the specified code with a thread-local default interface description provided.
     *
     * @param at  an AssertionTranslator to use when examining policies whenever one isn't explicitly provided.  May be null.
     * @param callable  code to run with the specified assertion translator in place.  Required.
     * @return the result of running the callable.  May be null if and only if the callable may return null.
     * @throws Exception if the callable throws an exception
     */
    public static <T> T doWithInterfaceDescription( EncapsulatedAssertionConfig at, Callable<T> callable ) throws Exception {
        final EncapsulatedAssertionConfig oldDesc = threadInterfaceDesc.get();
        threadInterfaceDesc.set( at );
        try {
            return callable.call();
        } finally {
            threadInterfaceDesc.set( oldDesc );
        }
    }

    /**
     * @return the current thread-local AssertionTranslator for this thread, or null.
     */
    public static EncapsulatedAssertionConfig get() {
        return threadInterfaceDesc.get();
    }

    private static final ThreadLocal<EncapsulatedAssertionConfig> threadInterfaceDesc = new ThreadLocal<>();

}
