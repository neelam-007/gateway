package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.EnumSet;
import java.util.Set;

/**
 * Represents a grouping of entity instances for RBAC permission checking purposes.
 * <p/>
 * The intent is that most persistent entities will be placeable into security zones.
 */
@XmlRootElement(name = "SecurityZone")
@XmlType(propOrder = {"description", "permittedEntityTypes"})
@javax.persistence.Entity
@Proxy(lazy=false)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@Table(name="security_zone")
public class SecurityZone extends NamedEntityImp {
    private String description = "";
    private Set<EntityType> permittedEntityTypes = EnumSet.noneOf(EntityType.class);

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

    /**
     * @return the entity types that are permitted to be placed into this security zone.
     */
    @Column(name = "entity_types", nullable=false, length=4096)
    @Type(type = "com.l7tech.server.util.GenericEnumSetUserType", parameters = {@Parameter(name = "enumClass", value = "com.l7tech.objectmodel.EntityType")})
    public Set<EntityType> getPermittedEntityTypes() {
        return permittedEntityTypes;
    }

    public void setPermittedEntityTypes(Set<EntityType> permittedEntityTypes) {
        this.permittedEntityTypes = permittedEntityTypes;
    }

    /**
     * @param entityType entity type to check.  Required.
     * @return true if entity type are not specified for this zone; or this zone permits EntityType.ANY; or this zone permits the specified entity type.
     */
    public boolean permitsEntityType(@NotNull EntityType entityType) {
        return permittedEntityTypes == null || permittedEntityTypes.contains(EntityType.ANY) || permittedEntityTypes.contains(entityType);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityZone)) return false;
        if (!super.equals(o)) return false;

        SecurityZone that = (SecurityZone) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (permittedEntityTypes != null ? !permittedEntityTypes.equals(that.permittedEntityTypes) : that.permittedEntityTypes != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (permittedEntityTypes != null ? permittedEntityTypes.hashCode() : 0);
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
