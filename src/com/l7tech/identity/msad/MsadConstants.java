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
    private static final String DESCRIPTION_ATTR = "description";
    private static final String NAME_ATTR_NAME = "cn";
    private static final String EMAIL_ATTR_NAME = "mail";
    private static final String FIRSTNAME_ATTR_NAME = "givenName";
    private static final String LASTNAME_ATTR_NAME = "sn";
    private static final String PASSWD_ATTR_NAME = "userPassword";


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
}
