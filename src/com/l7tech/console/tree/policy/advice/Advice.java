package com.l7tech.console.tree.policy.advice;

import com.l7tech.console.tree.policy.PolicyChange;
import com.l7tech.console.tree.policy.PolicyException;

/**
 * Intercepts Policy changes. Advice implementations can delegate
 * to the next advice in the chain (or the policy if the end of the chain
 * has been reached) by calling the <code>PolicyChange.proceed()</code>
 * method.
 * Implementations may modify argument values, throw or catch exceptions,
 * etc. The following is an example of a debugging
 * advice that prints the policy change:
 * <p/>
 * <pre>
 * public class DebuggingAdvice implements Advice {
 * <p/>
 *     public void proceed(PolicyChange pc)
 *             throws Throwable {
 *         System.out.println(pc);
 *         pc.proceed();
 *     }
 * <p/>
 * }
 * </pre>
 * 
 * @author <a href="mailto:emarceta@layer7tech.com>Emil Marceta</a>
 * @version 1.0
 */
public interface Advice {
    /**
     * Intercepts a policy change.
     * 
     * @param pc The policy change.
     */
    void proceed(PolicyChange pc) throws PolicyException;
}
