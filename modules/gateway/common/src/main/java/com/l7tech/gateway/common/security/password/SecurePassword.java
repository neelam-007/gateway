package com.l7tech.gateway.common.security.password;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * Holds an encrypted password.
 */
@Entity
@Proxy(lazy=false)
@Table(name="secure_password")
public class SecurePassword extends NamedEntityImp {
    private String description;
    private String encodedPassword;
    private Date lastUpdate;
    private boolean usageFromVariable;
    
    public SecurePassword() {
    }

    public SecurePassword(String name, long lastUpdate) {
        setName(name);
        this.lastUpdate = new Date(lastUpdate);
    }

    public SecurePassword(String name) {
        this(name, new Date().getTime());
    }

    @Column(name = "description", length = 256, nullable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(name = "usage_from_variable")
    public boolean isUsageFromVariable() {
        return usageFromVariable;
    }

    public void setUsageFromVariable(boolean usageFromVariable) {
        this.usageFromVariable = usageFromVariable;
    }

    @Column(name = "encoded_password", length = 256, nullable = false)
    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    @Column(name = "last_update")
    public long getLastUpdate() {
        return lastUpdate == null ? 0 : lastUpdate.getTime();
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = new Date(lastUpdate);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Transient
    public Date getLastUpdateAsDate() {
        return lastUpdate;
    }

    @Override
    public String toString() {
        return getName();
    }
}
