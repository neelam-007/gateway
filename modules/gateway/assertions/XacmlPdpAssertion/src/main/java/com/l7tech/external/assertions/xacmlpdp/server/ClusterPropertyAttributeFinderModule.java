package com.l7tech.external.assertions.xacmlpdp.server;

import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.attr.AttributeDesignator;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.EvaluationCtx;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.gateway.common.cluster.ClusterProperty;

import java.util.Set;
import java.util.HashSet;
import java.net.URI;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 18-Mar-2009
 * Time: 9:29:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClusterPropertyAttributeFinderModule extends AttributeFinderModule {
    public static final String PREFIX = "urn:layer7tech.com:clusterProperty:";

    private ClusterPropertyCache clusterPropertyCache;

    public ClusterPropertyAttributeFinderModule(ClusterPropertyCache clusterPropertyCache) {
        this.clusterPropertyCache = clusterPropertyCache;
    }

    public boolean isDesignatorSupported() {
        return true;
    }

    public Set getSupportedDesignatorTypes() {
        HashSet set = new HashSet();
        set.add(new Integer(AttributeDesignator.ENVIRONMENT_TARGET));
        return set;
    }

    public EvaluationResult findAttribute(URI attributeType, URI attributeId,
                                          URI issuer, URI subjectCategory,
                                          EvaluationCtx context,
                                          int designatorType) {
        // we only know about environment attributes
        if (designatorType != AttributeDesignator.ENVIRONMENT_TARGET)
            return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);

        // figure out which attribute we're looking for
        String attrName = attributeId.toString();
        if(!attrName.startsWith(PREFIX)) {
            return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);
        } else {
            attrName = attrName.substring(PREFIX.length());
        }

        ClusterProperty clusterProperty = clusterPropertyCache.getCachedEntityByName(attrName, 30 * 1000);
        if(clusterProperty == null) {
            return AttributeFinderModuleUtils.createEmptyEvaluationResult(attributeType);
        }

        return new EvaluationResult(AttributeFinderModuleUtils.createSingleStringBag(clusterProperty.getValue()));
    }
}
