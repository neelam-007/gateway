/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

/**
 * @author alex
 * @version $Revision$
 */
public class LdapConstants extends AbstractLdapConstants {
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

    static final String GROUPOBJ_MEMBER_ATTR = "memberUid";
    static final String USER_OBJCLASS = "inetOrgPerson";
    static final String LOGIN_ATTR_NAME = "uid";
    static final String GROUP_OBJCLASS = "posixGroup";

}
