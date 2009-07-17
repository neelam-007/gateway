package com.l7tech.external.assertions.xacmlpdp.server;

import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.attr.IntegerAttribute;
import com.sun.xacml.attr.BooleanAttribute;

import java.net.URI;
import java.util.HashSet;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 18-Mar-2009
 * Time: 9:27:09 PM
 */
class AttributeFinderModuleUtils {
    public static EvaluationResult createEmptyEvaluationResult(URI attributeType) {
        return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
    }

    public static BagAttribute createSingleStringBag(String value) {
        HashSet<StringAttribute> set = new HashSet<StringAttribute>();
        StringAttribute attribute = new StringAttribute(value);
        set.add(attribute);
        return new BagAttribute(attribute.getType(), set);
    }

    public static BagAttribute createMultipleStringBag(String[] values) {
        HashSet<StringAttribute> set = new HashSet<StringAttribute>();

        for(String value : values) {
            set.add(new StringAttribute(value));
        }

        return new BagAttribute(new StringAttribute("").getType(), set);
    }

    public static BagAttribute createSingleIntegerBag(int value) {
        HashSet<IntegerAttribute> set = new HashSet<IntegerAttribute>();
        IntegerAttribute attribute = new IntegerAttribute(value);
        set.add(attribute);
        return new BagAttribute(attribute.getType(), set);
    }

    public static BagAttribute createSingleBooleanBag(boolean value) {
        HashSet<BooleanAttribute> set = new HashSet<BooleanAttribute>();
        BooleanAttribute attribute = BooleanAttribute.getInstance(value);
        set.add(attribute);
        return new BagAttribute(attribute.getType(), set);
    }
}
