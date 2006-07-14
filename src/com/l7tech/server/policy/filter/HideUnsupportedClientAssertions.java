package com.l7tech.server.policy.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.xmlsec.*;

import java.util.Iterator;

/**
 * Hide all non-whitelisted assertions.
 */
public class HideUnsupportedClientAssertions implements Filter {
    public Assertion filter(User policyRequestor, Assertion assertionTree) throws FilteringException {
        if (assertionTree == null) return null;
        applyRules(assertionTree, null);
        return assertionTree;
    }

    public HideUnsupportedClientAssertions() {
        super();
    }

    /**
     * returns true if one or more assertion was deleted amoungs the siblings of this assertion
     */
    private boolean applyRules(Assertion arg, Iterator parentIterator) throws FilteringException {
        // apply rules on this one
        if (arg instanceof CompositeAssertion) {
            // apply rules to children
            CompositeAssertion root = (CompositeAssertion)arg;
            Iterator i = root.getChildren().iterator();
            while (i.hasNext()) {
                Assertion kid = (Assertion)i.next();
                applyRules(kid, i);
            }
            // if all children of this composite were removed, we have to remove it from it's parent
            if (root.getChildren().isEmpty() && parentIterator != null) {
                parentIterator.remove();
                return true;
            }
        } else {
            Class assertionClass = arg.getClass();
            for (int i = 0; i < supported.length; i++) {
                Class c = supported[i];
                if (c.isAssignableFrom(assertionClass))
                    return false;
            }
            if (parentIterator == null) {
                throw new RuntimeException("Invalid policy, all policies must have a composite assertion at the root");
            }
            parentIterator.remove();
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
        HttpDigest.class,
        WssBasic.class,
        EncryptedUsernameTokenAssertion.class,
        RequestWssX509Cert.class,
        SecureConversation.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class,
        //RequestXpathAssertion.class, fla, please leave commented - this is causing mucho problems
        //ResponseXpathAssertion.class, fla, please leave commented - this is causing mucho problems
        RequestWssReplayProtection.class,
        RequestWssSaml.class,
        RequestWssKerberos.class,
        WssTimestamp.class,
        CookieCredentialSourceAssertion.class,
    };
}
