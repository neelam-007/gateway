package com.l7tech.server.policy;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * This is the service that lets a client download a policy for consuming a web service.
 * Whether or not a requestor is allowed to download a policy depends on whether or not he is capable
 * of consuming it. This is determined by looking at the identity assertions inside the target policy
 * comparing them with the identity resuling from the authentication of the policy download request.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 23, 2004<br/>
 * $Id$<br/>
 */
public class PolicyService {

    protected PolicyService() {
        allCredentialAssertions = new ArrayList();
    }

    public Document respondToPolicyDownloadRequest(Document request) {
        // todo, all of below
        // inspect document
        // see which policy is requested
        // get the policy
        // make policy-policy
        // execute it
        // construct the response
        return null;
    }

    /**
     * Constructs a policy that determines if a requestor should be allowed to download a policy.
     * @param targetPolicy the policy targeted by a requestor.
     * @return the policy that should be validated by the policy download request for the passed target
     */
    AllAssertion constructPolicyPolicy(AllAssertion targetPolicy) {
        AllAssertion base = new AllAssertion();
        base.getChildren().add(new OneOrMoreAssertion(allCredentialAssertions));
        List allTargetIdentities = new ArrayList();
        addIdAssertionToList(targetPolicy, allTargetIdentities);
        base.getChildren().add(new OneOrMoreAssertion(allTargetIdentities));
        return base;
    }

    private void addIdAssertionToList(CompositeAssertion composite, List receptacle) {
        for (Iterator i = composite.getChildren().iterator(); i.hasNext();) {
            Assertion a = (Assertion)i.next();
            if (a instanceof IdentityAssertion) {
                receptacle.add(a);
            } else if (a instanceof CompositeAssertion) {
                addIdAssertionToList((CompositeAssertion)a, receptacle);
            }
        }
    }

    private final List allCredentialAssertions;
}
