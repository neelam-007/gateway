package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "User")
@XmlType(name = "UserType", propOrder = {"login", "password", "firstName","lastName","email","department","subjectDn","properties","extension","extensions"})
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
    public PasswordFormatted getPassword() {
        return password;
    }

    /**
     * Set the password for the user, only for create.
     *
     * @param password The password to use.
     */
    public void setPassword( final PasswordFormatted password ) {
        this.password = password;
    }

    /**
     * The the identity provider id for the user.
     *
     * @return The identity provider id.
     */
    @XmlAttribute( required = true)
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

    /**
     * Get the properties for this active connector
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    protected Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for this active connector.
     *
     * @param properties The properties to use
     */
    protected void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PACKAGE

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    protected void setExtension( final Extension extension ) {
        super.setExtension(extension);
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    /**
     * The name for the user
     *
     * @return The name (may be null)
     */
    @XmlTransient
    public String getName() {
        return getProperty("name");
    }

    /**
     * Set the name for the user.
     *
     * @param name The name to use.
     */
    public void setName(@Nullable String name) {
        setProperty("name", name);
    }

    public void setProperty(@NotNull final String key, @Nullable final Object value) {
        if(properties == null) {
            properties = new HashMap<>();
        }
        if(value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }
    }

    public <T> T getProperty(@NotNull final String key) {
        return properties == null || !properties.containsKey(key) ? null : (T)properties.get(key);
    }

    @XmlTransient
    public CertificateData getCertificateData() {
        return getUniqueExtension(new Functions.Unary<Boolean, Object>() {
            @Override
            public Boolean call(Object o) {
                return o instanceof CertificateData;
            }
        });
    }

    public void setCertificateData( final CertificateData certificateData ) {
        setUniqueExtension(certificateData, new Functions.Unary<Boolean, Object>() {
            @Override
            public Boolean call(Object o) {
                return o instanceof CertificateData;
            }
        });
    }

    UserMO() {
    }

    //- PRIVATE

    private String login;
    private PasswordFormatted password;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String subjectDn;
    private String providerId;
    private Map<String,Object> properties;

}
