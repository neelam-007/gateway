package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Proxy;

import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a grouping of entity instances for RBAC permission checking purposes.
 * <p/>
 * The intent is that most persistent entities will be placeable into security zones.
 */
@XmlRootElement(name = "SecurityZone")
@XmlType(propOrder = {"description"})
@javax.persistence.Entity
@Proxy(lazy=false)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Table(name="security_zone")
public class SecurityZone extends NamedEntityImp {
    private String description;

    /**
     * @return description of security zone, or null.
     */
    public String getDescription() {
        return description;
    }

    @Column(name = "description", nullable=false, length=255)
    public void setDescription(String description) {
        this.description = description;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityZone)) return false;
        if (!super.equals(o)) return false;

        SecurityZone that = (SecurityZone) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SecurityZone{" +
            "oid=" + getOid() +
            ", name='" + getName() + "'" +
            ", description='" + description + '\'' +
            '}';
    }
}
