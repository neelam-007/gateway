package com.l7tech.identity.ldap;

import java.io.Serializable;

/**
 * Tells the ldap provider how to use a specific object class to describe a user.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 20, 2004<br/>
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

    public String getKerberosAttrName() {
        return kerberosAttrName;
    }

    public void setKerberosAttrName( String kerberosAttrName ) {
        this.kerberosAttrName = kerberosAttrName;
    }

    public String getKerberosEnterpriseAttrName() {
        return kerberosEnterpriseAttrName;
    }

    public void setKerberosEnterpriseAttrName( String kerberosEnterpriseAttrName ) {
        this.kerberosEnterpriseAttrName = kerberosEnterpriseAttrName;
    }

    public PasswdStrategy getPasswdType() {
        return passwdType;
    }

    public void setPasswdType(PasswdStrategy passwdType) {
        this.passwdType = passwdType;
    }

    public String getUserCertAttrName() {
        return userCertAttrName;
    }

    public void setUserCertAttrName(String userCertAttrName) {
        this.userCertAttrName = userCertAttrName;
    }    

    private String objClass;
    private String nameAttrName;
    private String loginAttrName;
    private String passwdAttrName;
    private String firstNameAttrName;
    private String lastNameAttrName;
    private String emailNameAttrName;
    private String kerberosAttrName;
    private String kerberosEnterpriseAttrName;
	private String userCertAttrName;
    private PasswdStrategy passwdType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserMappingConfig)) return false;

        final UserMappingConfig userMappingConfig = (UserMappingConfig) o;

        if (kerberosAttrName != null ? !kerberosAttrName.equals(userMappingConfig.kerberosAttrName) : userMappingConfig.kerberosAttrName != null) return false;
        if (kerberosEnterpriseAttrName != null ? !kerberosEnterpriseAttrName.equals(userMappingConfig.kerberosEnterpriseAttrName) : userMappingConfig.kerberosEnterpriseAttrName != null) return false;
        if (userCertAttrName != null ? !userCertAttrName.equals(userMappingConfig.userCertAttrName) : userMappingConfig.userCertAttrName != null) return false;
        if (emailNameAttrName != null ? !emailNameAttrName.equals(userMappingConfig.emailNameAttrName) : userMappingConfig.emailNameAttrName != null) return false;
        if (firstNameAttrName != null ? !firstNameAttrName.equals(userMappingConfig.firstNameAttrName) : userMappingConfig.firstNameAttrName != null) return false;
        if (lastNameAttrName != null ? !lastNameAttrName.equals(userMappingConfig.lastNameAttrName) : userMappingConfig.lastNameAttrName != null) return false;
        if (loginAttrName != null ? !loginAttrName.equals(userMappingConfig.loginAttrName) : userMappingConfig.loginAttrName != null) return false;
        if (nameAttrName != null ? !nameAttrName.equals(userMappingConfig.nameAttrName) : userMappingConfig.nameAttrName != null) return false;
        if (objClass != null ? !objClass.equals(userMappingConfig.objClass) : userMappingConfig.objClass != null) return false;
        if (passwdAttrName != null ? !passwdAttrName.equals(userMappingConfig.passwdAttrName) : userMappingConfig.passwdAttrName != null) return false;
        //noinspection RedundantIfStatement
        if (passwdType != null ? !passwdType.equals(userMappingConfig.passwdType) : userMappingConfig.passwdType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (objClass != null ? objClass.hashCode() : 0);
        result = 29 * result + (nameAttrName != null ? nameAttrName.hashCode() : 0);
        result = 29 * result + (loginAttrName != null ? loginAttrName.hashCode() : 0);
        result = 29 * result + (passwdAttrName != null ? passwdAttrName.hashCode() : 0);
        result = 29 * result + (firstNameAttrName != null ? firstNameAttrName.hashCode() : 0);
        result = 29 * result + (lastNameAttrName != null ? lastNameAttrName.hashCode() : 0);
        result = 29 * result + (emailNameAttrName != null ? emailNameAttrName.hashCode() : 0);
        result = 29 * result + (kerberosAttrName != null ? kerberosAttrName.hashCode() : 0);
        result = 29 * result + (kerberosEnterpriseAttrName != null ? kerberosEnterpriseAttrName.hashCode() : 0);
        result = 29 * result + (userCertAttrName != null ? userCertAttrName.hashCode() : 0);
        result = 29 * result + (passwdType != null ? passwdType.hashCode() : 0);
        return result;
    }
}
