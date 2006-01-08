package com.l7tech.policy.assertion.identity;

import com.l7tech.policy.assertion.Assertion;

public class MappingAssertion extends Assertion {
    private long attributeConfigOid;

    public long getAttributeConfigOid() {
        return attributeConfigOid;
    }

    public void setAttributeConfigOid(long attributeConfigOid) {
        this.attributeConfigOid = attributeConfigOid;
    }
}
