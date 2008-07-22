/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp.pre32;

import org.w3c.dom.Element;

/**
 * A mapping that will happily read anything and ignore the properties.
 */
public class Pre32FakeMapping extends Pre32BeanTypeMapping {

    public Pre32FakeMapping(Class clazz, String externalName) {
        super(clazz, externalName);
    }

    public Pre32FakeMapping(Class clazz, String externalName, String nsUri, String nsPrefix) {
        super(clazz, externalName, nsUri, nsPrefix);
    }

    protected void populateObject(Pre32TypedReference object, Element source, Pre32WspVisitor visitor) throws Pre32InvalidPolicyStreamException {
        // Do nothing for fake objects
    }
}
