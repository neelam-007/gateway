package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.PolicyDetail;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * AttributeExtensible extension for PolicyType properties.
 */
@XmlType(name="PolicyTypePropertyType")
public class AttributeExtensiblePolicyType extends AttributeExtensibleType.AttributeExtensible<PolicyDetail.PolicyType> {
    private PolicyDetail.PolicyType value;

    @Override
    @XmlValue
    public PolicyDetail.PolicyType getValue() {
        return value;
    }

    @Override
    public void setValue( final PolicyDetail.PolicyType value ) {
        this.value = value;
    }
}
