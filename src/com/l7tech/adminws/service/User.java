package com.l7tech.adminws.service;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 */
public class User {
    public long getOid() {
        return oid;
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
    public String getTitle() {
        return title;
    }
    public Header[] getGroups() {
        return groups;
    }
    public void setOid(long oid) {
        this.oid = oid;
    }
    public void setLogin( String login ) {
        this.login = login;
    }
    public void setPassword( String password ) {
        this.password = password;
    }
    public void setFirstName( String firstName ) {
        this.firstName = firstName;
    }
    public void setLastName( String lastName ) {
        this.lastName = lastName;
    }
    public void setEmail( String email ) {
        this.email = email;
    }
    public void setTitle( String title ) {
        this.title = title;
    }
    public void setGroups(Header[] groups) {
        this.groups = groups;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private long oid;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String title;
    private Header[] groups;
}
