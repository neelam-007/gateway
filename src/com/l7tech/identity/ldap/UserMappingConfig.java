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

    public static class PasswdStrategy implements Serializable {
        public boolean equals(Object obj) {
            if (!(obj instanceof PasswdStrategy)) return false;
            PasswdStrategy otherone = (PasswdStrategy)obj;
            if (otherone.val == this.val) return true;
            return false;
        }

        public int hashCode() {
            return val;
        }

        private PasswdStrategy(int val) {
            this.val = val;
        }
        private int val;
    }
    public static final PasswdStrategy CLEAR = new PasswdStrategy(0);
    public static final PasswdStrategy HASHED = new PasswdStrategy(1);

    private String objClass;
    private String nameAttrName;
    private String loginAttrName;
    private String passwdAttrName;
    private String firstNameAttrName;
    private String lastNameAttrName;
    private String emailNameAttrName;
    private PasswdStrategy passwdType;
}
