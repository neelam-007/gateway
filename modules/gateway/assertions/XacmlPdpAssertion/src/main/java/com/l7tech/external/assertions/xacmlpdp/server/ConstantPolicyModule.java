package com.l7tech.external.assertions.xacmlpdp.server;

import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderResult;
import com.sun.xacml.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;

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
class ConstantPolicyModule extends PolicyFinderModule {
    private final Policy policy;
    private final boolean policyMatchesAllRequests;

    public ConstantPolicyModule( final String policyString ) throws ParsingException, SAXException {
        policy = buildPolicy( policyString );
        Target t = policy.getTarget();
        policyMatchesAllRequests = t==null || (t.getActions() == null && t.getSubjects() == null && areResourcesEmtpy(t.getResources()));
    }

    private Policy buildPolicy( final String policyString ) throws ParsingException, SAXException {
        try {
            return Policy.getInstance(XmlUtil.parse(policyString).getDocumentElement());
        } catch ( IllegalArgumentException iae ) {
            throw new ParsingException("Invalid XACML policy '"+ ExceptionUtils.getMessage(iae) + "'", iae);    
        } catch ( NullPointerException npe ) {
            throw new ParsingException("Invalid XACML policy", npe);
        }
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
    
    @Override
    public void init(PolicyFinder policyFinder) {
    }

    @Override
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

    @Override
    public boolean isRequestSupported() {
        return true;
    }
}
