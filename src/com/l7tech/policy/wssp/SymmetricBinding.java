/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import org.apache.ws.policy.PrimitiveAssertion;

import javax.xml.namespace.QName;

/**
 * @author mike
 */
public class SymmetricBinding extends PrimitiveAssertion {
    

    public SymmetricBinding(QName qname) {
        super(qname);
    }

    public SymmetricBinding(QName qname, Object value) {
        super(qname, value);
    }
}
