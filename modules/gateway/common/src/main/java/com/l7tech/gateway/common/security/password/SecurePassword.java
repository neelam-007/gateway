package com.l7tech.gateway.common.security.password;

import com.l7tech.objectmodel.imp.ZoneableNamedGoidEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlEnumValue;
import java.util.Date;

/**
 * Holds an encrypted password.
 */
@Entity
@Proxy(lazy=false)
@Table(name="secure_password")
public class SecurePassword extends ZoneableNamedGoidEntityImp {

    public static enum SecurePasswordType {
        @XmlEnumValue("Password")
        PASSWORD,

        @XmlEnumValue("PEM Private Key")
        PEM_PRIVATE_KEY
    }

    private String description;
    private String encodedPassword;
    private Date lastUpdate;
    private boolean usageFromVariable;
    private SecurePasswordType type;
    
    public SecurePassword() {
    }

    public SecurePassword(String name, long lastUpdate) {
        setName(name);
        this.lastUpdate = new Date(lastUpdate);
    }

    public SecurePassword(String name) {
        this(name, new Date().getTime());
    }

    @Size(min=1,max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    @Column(name = "description", length = 256, nullable = true)
    @Size(max=256)
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

    @Lob
    @Column(name = "encoded_password", length = 65535, nullable = false)
    @NotNull
    @Size(max=65535)
    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    @Column(name = "last_update")
    public long getLastUpdate() {
        return lastUpdate == null ? 0L : lastUpdate.getTime();
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = new Date(lastUpdate);
    }

    @Column(name = "type", length = 64, nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    public SecurePasswordType getType() {
        return type;
    }

    public void setType(SecurePasswordType type) {
        this.type = type;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Transient
    public Date getLastUpdateAsDate() {
        return lastUpdate;
    }

    public void setLastUpdateAsDate( final Date lastUpdate ) {
        this.lastUpdate = lastUpdate==null ? null : new Date( lastUpdate.getTime() );
    }

    @Override
    public String toString() {
        return getName();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurePassword)) return false;
        if (!super.equals(o)) return false;

        SecurePassword that = (SecurePassword) o;

        if (usageFromVariable != that.usageFromVariable) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (encodedPassword != null ? !encodedPassword.equals(that.encodedPassword) : that.encodedPassword != null)
            return false;
        if (lastUpdate != null ? !lastUpdate.equals(that.lastUpdate) : that.lastUpdate != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (securityZone != null ? !securityZone.equals(that.securityZone) : that.securityZone != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (encodedPassword != null ? encodedPassword.hashCode() : 0);
        result = 31 * result + (lastUpdate != null ? lastUpdate.hashCode() : 0);
        result = 31 * result + (usageFromVariable ? 1 : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (securityZone != null ? securityZone.hashCode() : 0);
        return result;
    }
}
