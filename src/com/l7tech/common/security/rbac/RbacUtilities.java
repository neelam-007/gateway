/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.rbac;

import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;

/**
 * User: megery
 * Date: Jul 31, 2006
 * Time: 4:55:46 PM
 */

public class RbacUtilities {
    public static final String SYSTEM_PROP_ENABLEROLEEDIT = "com.l7tech.rbac.allowEditRoles";

    public static boolean isEnableRoleEditing() {
        return StringUtils.equalsIgnoreCase(System.getProperty(SYSTEM_PROP_ENABLEROLEEDIT), "true");
    }

    public static String getDescriptionString(Role role, boolean asHtml) {

        String name = role.getName();
        String description = role.getDescription();
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(description))
            return "";

        return MessageFormat.format(description, asHtml?"<strong>" + name + "</strong>":name);
    }
}
