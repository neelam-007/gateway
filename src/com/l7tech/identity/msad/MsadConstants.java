/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.msad;

import com.l7tech.identity.ldap.AbstractLdapConstants;

/**
 * @author alex
 * @version $Revision$
 */
public class MsadConstants extends AbstractLdapConstants {
    private static final String GROUPOBJ_MEMBER_ATTR = "member";
    private static final String LOGIN_ATTR_NAME = "mailNickName";
    private static final String GROUP_OBJCLASS = "group";
    private static final String USER_OBJCLASS = "user";


    public String groupMemberAttribute() {
        return GROUPOBJ_MEMBER_ATTR;
    }

    public String userLoginAttribute() {
        return LOGIN_ATTR_NAME;
    }

    public String groupObjectClass() {
        return GROUP_OBJCLASS;
    }

    public String userObjectClass() {
        return USER_OBJCLASS;
    }
}
