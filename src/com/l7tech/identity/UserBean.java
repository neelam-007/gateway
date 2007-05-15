/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.identity;

import com.l7tech.common.util.HexUtils;

import java.io.Serializable;

/**
 * @author alex
 */
public class UserBean implements User, Serializable {
    public UserBean() {
    }

    public UserBean(long providerId, String login) {
        this.providerId = providerId;
        this.name = login;
        this.login = login;
    }

    public UserBean(String login) {
        this(IdentityProviderConfig.DEFAULT_OID, login);
    }

    public String getId() {
        return uniqueId;
    }

    public void setUniqueIdentifier( String uid ) {
        this.uniqueId = uid;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public long getProviderId() {
        return providerId;
    }

    public boolean isEquivalentId(Object thatId) {
        return uniqueId != null && uniqueId.equals(thatId);
    }

    public void setProviderId( long providerId ) {
        this.providerId = providerId;
    }

    public void setPassword(String password) throws IllegalStateException {
        setPassword(password, false);
    }

    /**
     * Set the password for this user
     *
     * @param password the password (clear or encoded)
     * @param hintIsClear true if you want to communicate that the password is in clear text
     */
    public void setPassword(String password, boolean hintIsClear) throws IllegalStateException {
        if (password != null && (hintIsClear || !HexUtils.containsOnlyHex(password))) {
            if (login == null) throw new IllegalStateException("login must be set prior to encoding the password");
            this.password = HexUtils.encodePasswd(login, password);
        }
        else this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * {@link User} implementations that delegate their bean properties to {@link UserBean} <b>must</b> override
     * {@link #equals} and {@link #hashCode} to include their own identity information!
     *
     * NOTE: if you regenerate this method, make sure the {@link #uniqueId} property is NOT included!
     * Particular {@link User} implementations have their own logic for identity equality.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserBean userBean = (UserBean) o;

        if (providerId != userBean.providerId) return false;
        if (department != null ? !department.equals(userBean.department) : userBean.department != null)
            return false;
        if (email != null ? !email.equals(userBean.email) : userBean.email != null) return false;
        if (firstName != null ? !firstName.equals(userBean.firstName) : userBean.firstName != null) return false;
        if (lastName != null ? !lastName.equals(userBean.lastName) : userBean.lastName != null) return false;
        if (login != null ? !login.equals(userBean.login) : userBean.login != null) return false;
        if (name != null ? !name.equals(userBean.name) : userBean.name != null) return false;

        return true;
    }

    /**
     * {@link User} implementations that delegate their bean properties to {@link UserBean} <b>must</b> override
     * {@link #equals} and {@link #hashCode} to include their own identity information!
     *
     * NOTE: if you regenerate this method, make sure the {@link #uniqueId} property is NOT included!
     * Particular {@link User} implementations have their own logic for identity equality.
     */
    public int hashCode() {
        int result;
        result = (int) (providerId ^ (providerId >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (login != null ? login.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (department != null ? department.hashCode() : 0);
        return result;
    }

    private static final long serialVersionUID = -2689153614711342567L;

    protected long providerId = IdentityProviderConfig.DEFAULT_OID;
    protected String uniqueId;
    protected String name;
    protected String login;
    protected String password;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String department;
    protected String subjectDn;
    protected int version;
}
