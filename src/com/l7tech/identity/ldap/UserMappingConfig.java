package com.l7tech.identity.ldap;

import java.io.Serializable;

/**
 * Tells the ldap provider how to use a specific object class to describe a user.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class UserMappingConfig implements Serializable {

    public String getObjClass() {
        return objClass;
    }

    public void setObjClass(String objClass) {
        this.objClass = objClass;
    }

    public String getNameAttrName() {
        return nameAttrName;
    }

    public void setNameAttrName(String nameAttrName) {
        this.nameAttrName = nameAttrName;
    }

    public String getLoginAttrName() {
        return loginAttrName;
    }

    public void setLoginAttrName(String loginAttrName) {
        this.loginAttrName = loginAttrName;
    }

    public String getPasswdAttrName() {
        return passwdAttrName;
    }

    public void setPasswdAttrName(String passwdAttrName) {
        this.passwdAttrName = passwdAttrName;
    }

    public String getFirstNameAttrName() {
        return firstNameAttrName;
    }

    public void setFirstNameAttrName(String firstNameAttrName) {
        this.firstNameAttrName = firstNameAttrName;
    }

    public String getLastNameAttrName() {
        return lastNameAttrName;
    }

    public void setLastNameAttrName(String lastNameAttrName) {
        this.lastNameAttrName = lastNameAttrName;
    }

    public String getEmailNameAttrName() {
        return emailNameAttrName;
    }

    public void setEmailNameAttrName(String emailNameAttrName) {
        this.emailNameAttrName = emailNameAttrName;
    }

    public PasswdStrategy getPasswdType() {
        return passwdType;
    }

    public void setPasswdType(PasswdStrategy passwdType) {
        this.passwdType = passwdType;
    }

    private String objClass;
    private String nameAttrName;
    private String loginAttrName;
    private String passwdAttrName;
    private String firstNameAttrName;
    private String lastNameAttrName;
    private String emailNameAttrName;
    private PasswdStrategy passwdType = null;
}
