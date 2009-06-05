package com.l7tech.external.assertions.xacmlpdp.server;

import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderResult;
import com.sun.xacml.Policy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.MatchResult;
import com.l7tech.common.io.XmlUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 16-Mar-2009
 * Time: 4:57:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConstantPolicyModule extends PolicyFinderModule {
    private Policy policy;

    public ConstantPolicyModule(String policyString) throws ParsingException, IOException, SAXException {
        policy = Policy.getInstance(XmlUtil.parse(new ByteArrayInputStream(policyString.getBytes())).getDocumentElement());
    }

    public void init(PolicyFinder policyFinder) {
    }

    public PolicyFinderResult findPolicy(EvaluationCtx context) {
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
