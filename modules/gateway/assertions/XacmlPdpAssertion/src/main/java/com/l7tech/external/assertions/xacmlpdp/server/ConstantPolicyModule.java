package com.l7tech.external.assertions.xacmlpdp.server;

import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderResult;
import com.sun.xacml.*;
import com.l7tech.common.io.XmlUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Collection;

import org.xml.sax.SAXException;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 16-Mar-2009
 * Time: 4:57:22 PM
 */
public class ConstantPolicyModule extends PolicyFinderModule {
    private Policy policy;
    private final boolean policyMatchesAllRequests;

    public ConstantPolicyModule(String policyString) throws ParsingException, IOException, SAXException {
        policy = Policy.getInstance(XmlUtil.parse(new ByteArrayInputStream(policyString.getBytes())).getDocumentElement());
        Target t = policy.getTarget();
        policyMatchesAllRequests = t.getActions() == null && t.getSubjects() == null && areResourcesEmtpy(t.getResources());
    }

    private boolean areResourcesEmtpy(List resources){
        if(resources == null) return true;
        if(resources.isEmpty()) return true;
        boolean allEmpty = true;
        for(Object o: resources){
            if(o instanceof Collection){
                Collection coll = (Collection) o;
                if(!coll.isEmpty()) allEmpty = false;
            }
        }
        return allEmpty;
    }
    
    public void init(PolicyFinder policyFinder) {
    }

    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        //if this policy matches everything, return it
        if(policyMatchesAllRequests) return new PolicyFinderResult(policy);

        //otherwise check if we match the context
        MatchResult matchResult = policy.match(context);
        if(matchResult.getResult() == MatchResult.MATCH) {
            return new PolicyFinderResult(policy);
        } else {
            return new PolicyFinderResult();
        }
    }

    public boolean isRequestSupported() {
        return true;
    }
}
