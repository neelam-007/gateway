package com.l7tech.server.ems.user;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.UniqueConstraint;

/**
 *  Represents a single property name/value pair for a user.
 */
@Entity
@Table(name="user_property",
       uniqueConstraints=@UniqueConstraint(columnNames={"provider", "user_id", "propkey"}))
public class UserProperty extends PersistentEntityImp {

    //- PUBLIC
    
    @Column(name="provider", nullable=false)
    public long getProvider() {
        return provider;
    }

    public void setProvider(long providerId) {
        this.provider = providerId;
    }

    @Column(name="user_id", nullable=false, length=255)
    public String getUserId() {
        return userId;
    }

    public void setUserId( String userId ) {
        this.userId = userId;
    }

    @Column(name="login", length=255)
    public String getLogin() {
        return login;
    }

    public void setLogin( String userLogin ) {
        this.login = userLogin;
    }

    @Column(name="propkey", nullable=false, length=255)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name="propvalue", nullable=false, length=4096)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    //- PRIVATE

    private long provider;
    private String login;
    private String userId;
    private String name;
    private String value;
}
