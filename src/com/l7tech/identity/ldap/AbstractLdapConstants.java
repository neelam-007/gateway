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
    public static String groupNameAttribute() { return NAME_ATTR_NAME;}
    public abstract String groupMemberAttribute();

    public abstract String userObjectClass();
    public abstract String userLoginAttribute();
    public static String userNameAttribute() {return NAME_ATTR_NAME;}
    public static String userEmailAttribute() {return EMAIL_ATTR_NAME;}
    public static String userFirstnameAttribute() {return FIRSTNAME_ATTR_NAME;}
    public static String userLastnameAttribute() {return LASTNAME_ATTR_NAME;}
    public static String userPasswordAttribute() {return PASSWD_ATTR_NAME;}
    public static String oUObjClassName() {return OU_OBJCLASS_NAME;}
    public static String oUObjAttrName() {return OU_ATTR_NAME;}

    protected static final String NAME_ATTR_NAME = "cn";
    protected static final String DESCRIPTION_ATTR = "description";
    protected static final String LASTNAME_ATTR_NAME = "sn";
    protected static final String EMAIL_ATTR_NAME = "mail";
    protected static final String FIRSTNAME_ATTR_NAME = "givenName";
    protected static final String PASSWD_ATTR_NAME = "userPassword";
    protected static final String OU_OBJCLASS_NAME = "organizationalUnit";
    protected static final String OU_ATTR_NAME = "ou";
}
