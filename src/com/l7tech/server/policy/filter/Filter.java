package com.l7tech.server.policy.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;

import java.util.logging.Logger;

/**
 * A filter that implements an encapsulable rule.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 * $Id$
 */
public interface Filter {
    /**
     * Applies a rule that affects how an external requestor sees a policy.
     *
     * @param policyRequestor may be null if the requestor is anonymous
     * @param assertionTree
     * @return a filtered policy. may return null if the result of the filter is that the requestor may not
     * consume this service at all.
     */
    Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException;
}
