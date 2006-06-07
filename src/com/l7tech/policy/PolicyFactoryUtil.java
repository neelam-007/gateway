/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

/**
 * @author alex
 * @version $Revision$
 */
public class PolicyFactoryUtil {
    public static final String PACKAGE_PREFIX = "com.l7tech.policy.assertion";

    public static String getProductClassname(Class genericAssertionClass, String productRootPackageName, String productClassnamePrefix) {
        String genericAssertionClassname = genericAssertionClass.getName();
        int ppos = genericAssertionClassname.lastIndexOf(".");
        if (ppos <= 0) throw new RuntimeException("Invalid classname " + genericAssertionClassname);
        String genericPackage = genericAssertionClassname.substring(0, ppos);
        String genericName = genericAssertionClassname.substring(ppos + 1);

        StringBuffer specificClassName = new StringBuffer(productRootPackageName);

        if (genericPackage.equals(PACKAGE_PREFIX)) {
            specificClassName.append(".");
            specificClassName.append(productClassnamePrefix);
            specificClassName.append(genericName);
        } else if (genericPackage.startsWith(PACKAGE_PREFIX)) {
            specificClassName.append(genericPackage.substring(PACKAGE_PREFIX.length()));
            specificClassName.append(".");
            specificClassName.append(productClassnamePrefix);
            specificClassName.append(genericName);
        } else
            throw new RuntimeException("Couldn't handle " + genericAssertionClassname);

        return specificClassName.toString();
    }
}
