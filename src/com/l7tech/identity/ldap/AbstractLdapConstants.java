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
public abstract class AbstractLdapConstants {
    public String descriptionAttribute() {return DESCRIPTION_ATTR;}

    public abstract String groupObjectClass();
    public String groupNameAttribute() { return NAME_ATTR_NAME;}
    public abstract String groupMemberAttribute();

    public abstract String userObjectClass();
    public abstract String userLoginAttribute();
    public String userNameAttribute() {return NAME_ATTR_NAME;}
    public String userEmailAttribute() {return EMAIL_ATTR_NAME;}
    public String userFirstnameAttribute() {return FIRSTNAME_ATTR_NAME;}
    public String userLastnameAttribute() {return LASTNAME_ATTR_NAME;}
    public String userPasswordAttribute() {return PASSWD_ATTR_NAME;}

    protected static final String NAME_ATTR_NAME = "cn";
    protected static final String DESCRIPTION_ATTR = "description";
    protected static final String LASTNAME_ATTR_NAME = "sn";
    protected static final String EMAIL_ATTR_NAME = "mail";
    protected static final String FIRSTNAME_ATTR_NAME = "givenName";
    protected static final String PASSWD_ATTR_NAME = "userPassword";

}
