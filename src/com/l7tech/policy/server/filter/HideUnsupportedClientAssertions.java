package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.xmlsec.*;

import java.util.Iterator;

/**
 * Hide all non-whitelisted assertions.
 */
public class HideUnsupportedClientAssertions extends Filter {
    public Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException {
        if (assertionTree == null) return null;
        applyRules(assertionTree);
        return assertionTree;
    }

    public HideUnsupportedClientAssertions() {
        super();
    }

    /**
     * returns true if one or more assertion was deleted amoungs the siblings of this assertion
     */
    private boolean applyRules(Assertion arg) throws FilteringException {
        // apply rules on this one
        if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                boolean res = applyRules(kid);
                // the children were affected
                if (res) {
                    // if all children of this composite were removed, we have to remove it from it's parent
                    if (root.getChildren().isEmpty()) {
                        removeSelfFromParent(root, false);
                        return true;
                    }
                    // otherwise continue, but reget the iterator because the list of children is affected
                    else i = root.getChildren().iterator();
                }
            }
        } else {
            Class assertionClass = arg.getClass();
            for (int i = 0; i < supported.length; i++) {
                Class c = supported[i];
                if (c.isAssignableFrom(assertionClass))
                    return false;
            }
            removeSelfFromParent(arg, false);
            return true;
        }

        return false;
    }

    // This is the whitelist of non-composite assertions that we will allow through to the client.
    // This list must be kept up-to-date as the SSG wishes to publish additional assertions.
    private static final Class[] supported = {
        FalseAssertion.class,
        SslAssertion.class,
        TrueAssertion.class,
        HttpBasic.class,
        HttpClientCert.class,
        HttpDigest.class,
        WssBasic.class,
        WssDigest.class,
        RequestWssX509Cert.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class,
        RequestXpathAssertion.class,
        ResponseXpathAssertion.class,
        RequestWssReplayProtection.class,
    };
}
