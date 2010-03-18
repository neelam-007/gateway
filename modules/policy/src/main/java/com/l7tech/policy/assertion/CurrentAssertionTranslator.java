package com.l7tech.policy.assertion;

import java.util.concurrent.Callable;

/**
 * Holds a thread-local @{link AssertionTranslator}.
 */
public class CurrentAssertionTranslator {

    /**
     * Run the specified code with a thread-local default AssertionTranslator provided.
     *
     * @param at  an AssertionTranslator to use when examining policies whenever one isn't explicitly provided.  May be null.
     * @param callable  code to run with the specified assertion translator in place.  Required.
     * @return the result of running the callable.  May be null if and only if the callable may return null.
     * @throws Exception if the callable throws an exception
     */
    public static <T> T doWithAssertionTranslator(AssertionTranslator at, Callable<T> callable) throws Exception {
        final AssertionTranslator oldat = threadAssertionTranslator.get();
        threadAssertionTranslator.set(at);
        try {
            return callable.call();
        } finally {
            threadAssertionTranslator.set(oldat);
        }
    }

    /**
     * @return the current thread-local AssertionTranslator for this thread, or null.
     */
    public static AssertionTranslator get() {
        return threadAssertionTranslator.get();
    }

    private static final ThreadLocal<AssertionTranslator> threadAssertionTranslator = new ThreadLocal<AssertionTranslator>();
}
