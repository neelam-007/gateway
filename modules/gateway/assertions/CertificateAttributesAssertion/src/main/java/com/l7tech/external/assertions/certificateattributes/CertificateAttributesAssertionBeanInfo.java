package com.l7tech.external.assertions.certificateattributes;

import java.beans.SimpleBeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;

/**
 * BeanInfo for CertificateAttributesAssertion.
 */
public class CertificateAttributesAssertionBeanInfo extends SimpleBeanInfo {
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[] {
                new PropertyDescriptor("variablePrefix", CertificateAttributesAssertion.class, "getVariablePrefix", "setVariablePrefix") {
                    public String getDisplayName() {
                        return "Variable Prefix";
                    }
                }
            };
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}
