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

    /**
     * self contained group object class name(s)
     * @return an array of object class names
     */
    public abstract String[] groupObjectClass();
    public abstract String groupNameAttribute();

    /**
     * attribute name in the self contained group that refers to the user member of a group
     * @return
     */
    public abstract String[] groupMemberAttribute();

    public abstract String userObjectClass();
    public abstract String userLoginAttribute();
    public abstract String userNameAttribute();
    public String userEmailAttribute() {return EMAIL_ATTR_NAME;}
    public String userFirstnameAttribute() {return FIRSTNAME_ATTR_NAME;}
    public String userLastnameAttribute() {return LASTNAME_ATTR_NAME;}
    public String userPasswordAttribute() {return PASSWD_ATTR_NAME;}
    public String oUObjClassName() {return OU_OBJCLASS_NAME;}
    public String oUObjAttrName() {return OU_ATTR_NAME;}

    protected static final String DESCRIPTION_ATTR = "description";
    protected static final String LASTNAME_ATTR_NAME = "sn";
    protected static final String EMAIL_ATTR_NAME = "mail";
    protected static final String FIRSTNAME_ATTR_NAME = "givenName";
    protected static final String PASSWD_ATTR_NAME = "userPassword";
    protected static final String OU_OBJCLASS_NAME = "organizationalUnit";
    protected static final String OU_ATTR_NAME = "ou";
    public static final String OBJCLASS_ATTR = "objectClass";
}
