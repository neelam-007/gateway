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

    public String groupNameAttribute() {
        return NAME_ATTR_NAME;
    }

    public String userObjectClass() {
        return USER_OBJCLASS;
    }

    public String descriptionAttribute() {
        return DESCRIPTION_ATTR;
    }

    public String userNameAttribute() {
        return NAME_ATTR_NAME;
    }

    public String userEmailAttribute() {
        return EMAIL_ATTR_NAME;
    }

    public String userFirstnameAttribute() {
        return FIRSTNAME_ATTR_NAME;
    }

    public String userLastnameAttribute() {
        return LASTNAME_ATTR_NAME;
    }

    public String userPasswordAttribute() {
        return PASSWD_ATTR_NAME;
    }

    static final String DESCRIPTION_ATTR = "description";
    static final String NAME_ATTR_NAME = "cn";
    static final String GROUPOBJ_MEMBER_ATTR = "memberUid";
    static final String USER_OBJCLASS = "inetOrgPerson";
    static final String LOGIN_ATTR_NAME = "uid";
    static final String GROUP_OBJCLASS = "posixGroup";
    static final String EMAIL_ATTR_NAME = "mail";
    static final String FIRSTNAME_ATTR_NAME = "givenName";
    static final String LASTNAME_ATTR_NAME = "sn";
    static final String PASSWD_ATTR_NAME = "userPassword";

}
