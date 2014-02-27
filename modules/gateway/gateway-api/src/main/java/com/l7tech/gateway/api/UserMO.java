package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "User")
@XmlType(name = "UserType", propOrder = {"login", "password", "firstName","lastName","email","department","subjectDn","extensions"})
@AccessorSupport.AccessibleResource(name = "users")
public class UserMO extends AccessibleObject {

    //- PUBLIC

    /**
     * The login for the user (required)
     *
     * @return The login
     */
    @XmlElement(name = "Login", required=true)
    public String getLogin() {
        return login;
    }

    /**
     * Set the login for the user.
     *
     * @param login The login to use.
     */
    public void setLogin( final String login ) {
        this.login = login;
    }

    /**
     * The password for the user
     *
     * @return The password
     */
    @XmlElement(name = "Password")
    public String getPassword() {
        return password;
    }

    /**
     * Set the password for the user, only for create.
     *
     * @param password The password to use.
     */
    public void setPassword( final String password ) {
        this.password = password;
    }

    /**
     * The the identity provider id for the user.
     *
     * @return The identity provider id.
     */
    @XmlAttribute
    public String getProviderId() {
        return providerId;
    }

    /**
     * Set the identity provider id for the user (required).
     *
     * @param providerId The identity provider id.
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * The first name for the user
     *
     * @return The first name (may be null)
     */
    @XmlElement(name = "FirstName")
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the first name for the user.
     *
     * @param firstName The first name to use.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * The lastName for the user
     *
     * @return The lastName (may be null)
     */
    @XmlElement(name = "LastName")
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the last name for the user.
     *
     * @param lastName The last name to use.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * The email for the user
     *
     * @return The email (may be null)
     */
    @XmlElement(name = "Email")
    public String getEmail() {
        return email;
    }

    /**
     * Set the email for the user.
     *
     * @param email The logon to use.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * The department for the user
     *
     * @return The department (may be null)
     */
    @XmlElement(name = "Department")
    public String getDepartment() {
        return department;
    }

    /**
     * Set the department for the user.
     *
     * @param department The department to use.
     */
    public void setDepartment(String department) {
        this.department = department;
    }

    /**
     * The subjectDn for the user
     *
     * @return The subjectDn (may be null)
     */
    @XmlElement(name = "SubjectDn")
    public String getSubjectDn() {
        return subjectDn;
    }

    /**
     * Set the subjectDn for the user.
     *
     * @param subjectDn The subjectDn to use.
     */
    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    //- PACKAGE

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    UserMO() {
    }

    //- PRIVATE

    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String subjectDn;
    private String providerId;

}
